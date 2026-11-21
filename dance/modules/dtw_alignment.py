"""
模块三：时序对齐（DTW）
功能：
1. 使用动态时间规整（DTW）计算最优非线性对齐路径
2. 使用关节角度差异作为距离度量（对身高、体型、站位不敏感）
3. 引入Sakoe-Chiba带宽约束防止错误对齐
4. 支持时长差异大的视频先做粗对齐再细粒度DTW
"""
import numpy as np
from config import DTW_RADIUS
from utils import compute_dtw_path_bending_cost


class DTWAligner:
    """DTW时序对齐器"""

    def __init__(self, radius=DTW_RADIUS, use_sakoe_chiba=True):
        """
        radius: Sakoe-Chiba带宽约束
        use_sakoe_chiba: 是否启用带宽约束
        """
        self.radius = radius
        self.use_sakoe_chiba = use_sakoe_chiba
        self.alignment_path = None
        self.distance_matrix = None

    def _angle_distance(self, angles_a, angles_b, confs_a=None, confs_b=None):
        """
        计算两帧关节角度之间的加权距离
        对低置信度关节降权
        """
        diff = np.abs(angles_a - angles_b)
        # 角度差异归一化到0-1（最大角度差180度）
        diff = diff / 180.0

        if confs_a is not None and confs_b is not None:
            # 置信度越低，权重越小
            weights = (confs_a + confs_b) / 2.0
            weights = np.clip(weights, 0.01, 1.0)
            if np.sum(weights) > 0:
                dist = np.sum(diff * weights) / np.sum(weights)
            else:
                dist = np.mean(diff)
        else:
            dist = np.mean(diff)

        return dist

    def align(self, teacher_angles, student_angles,
              teacher_confs=None, student_confs=None):
        """
        对教师和学生的角度序列做DTW对齐
        使用带Sakoe-Chiba带宽约束的精确DTW（O(n*radius)，速度快且精度有保障）。
        说明：fastdtw是近似算法，对相同/近相同序列会产生次优非对角路径，
        导致相同视频的偏差曲线前段非零、后段归零。精确DTW在带宽内找到最优路径，
        相同序列必走对角线（距离=0）。

        teacher_angles: (T1, N) 教师关节角度序列
        student_angles: (T2, N) 学生关节角度序列
        返回: alignment_path [(i, j), ...], distance
        """
        return self._custom_dtw(teacher_angles, student_angles,
                                teacher_confs, student_confs)

    def _custom_dtw(self, seq_a, seq_b, confs_a=None, confs_b=None):
        """
        自定义DTW实现（带Sakoe-Chiba约束）
        当FastDTW不可用时使用
        """
        n, m = len(seq_a), len(seq_b)

        # 初始化距离矩阵
        INF = float("inf")
        D = np.full((n + 1, m + 1), INF)
        D[0, 0] = 0

        # 回溯路径
        trace = np.zeros((n + 1, m + 1, 2), dtype=int)
        trace[0, 0] = [-1, -1]

        for i in range(1, n + 1):
            # Sakoe-Chiba带宽约束：以缩放对角线 j_c = i*m/n 为中心开窗，
            # 保证时长差异大的序列最优路径可达（同时长时 j_c=i，与原行为一致）
            if self.use_sakoe_chiba:
                band = int(n * self.radius / max(n, m))
                j_center = i * m / n
                j_start = max(1, int(j_center) - band)
                j_end = min(m, int(j_center) + band)
            else:
                j_start, j_end = 1, m

            for j in range(j_start, j_end + 1):
                ca = confs_a[i - 1] if confs_a is not None else None
                cb = confs_b[j - 1] if confs_b is not None else None
                cost = self._angle_distance(
                    seq_a[i - 1], seq_b[j - 1], ca, cb
                )

                # DTW递推
                candidates = [
                    (D[i - 1, j - 1], i - 1, j - 1),  # 对角
                    (D[i - 1, j], i - 1, j),            # 上
                    (D[i, j - 1], i, j - 1),            # 左
                ]
                min_val, pi, pj = min(candidates, key=lambda x: x[0])

                D[i, j] = cost + min_val
                trace[i, j] = [pi, pj]

        # 回溯路径
        path = []
        i, j = n, m
        while i > 0 and j > 0:
            path.append((i - 1, j - 1))
            pi, pj = trace[i, j]
            i, j = int(pi), int(pj)
        path.reverse()

        distance = D[n, m]
        self.alignment_path = path
        self.distance_matrix = D[1:, 1:]

        print(f"[DTWAligner] 自定义DTW对齐完成: 路径长度={len(path)}, 距离={distance:.4f}")
        return path, distance

    def compute_rhythm_cost(self, path=None):
        """
        计算DTW路径的弯曲成本（用于节奏掌握度评分）
        路径越弯曲说明节奏偏差越大
        """
        if path is None:
            path = self.alignment_path
        if path is None:
            return 0.0
        return compute_dtw_path_bending_cost(path)

    def align_part(self, teacher_part_angles, student_part_angles,
                   teacher_confs=None, student_confs=None):
        """
        对单个身体部位的角度序列做DTW对齐
        teacher_part_angles: (T1, K) 该部位的关节角度序列
        student_part_angles: (T2, K) 学生该部位的关节角度序列
        返回: path, distance
        """
        if len(teacher_part_angles) == 0 or len(student_part_angles) == 0:
            return [], 0.0

        # 保存当前路径
        saved_path = self.alignment_path

        path, distance = self.align(
            teacher_part_angles, student_part_angles,
            teacher_confs, student_confs
        )

        # 恢复
        self.alignment_path = saved_path

        return path, distance

    def get_aligned_frames(self, path, teacher_seq, student_seq):
        """
        根据DTW对齐路径获取对齐后的帧对
        返回: aligned_teacher (T_aligned, ...), aligned_student (T_aligned, ...)
        """
        aligned_teacher = []
        aligned_student = []

        for i, j in path:
            aligned_teacher.append(teacher_seq[i])
            aligned_student.append(student_seq[j])

        return np.array(aligned_teacher), np.array(aligned_student)

    def compute_keyframe_timing_deviation(self, path, teacher_fps=30, student_fps=30):
        """
        计算关键帧时间偏差（用于节奏评分）
        通过DTW路径与线性对角线的偏离程度衡量
        """
        if path is None or len(path) == 0:
            return 0.0

        path = np.array(path)
        # 计算每个对齐点的实际时间偏差
        teacher_times = path[:, 0] / teacher_fps
        student_times = path[:, 1] / student_fps

        # 时间偏差的标准差，归一化
        total_duration = max(teacher_times[-1], student_times[-1], 1e-6)
        timing_devs = np.abs(teacher_times - student_times) / total_duration

        return float(np.mean(timing_devs))
