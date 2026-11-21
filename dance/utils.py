"""
舞蹈AI评分系统 - 工具函数
包含：关节角度计算、关键点平滑、身体部位映射等
"""
import numpy as np
from config import JOINT_ANGLE_DEFS, BODY_PARTS, SMOOTH_WINDOW


def calculate_angle(a, b, c):
    """
    计算三点构成的夹角（b为顶点）
    a, b, c: [x, y] 或 [x, y, z] 坐标
    返回: 角度（度），范围0-180
    """
    a = np.array(a, dtype=np.float64)
    b = np.array(b, dtype=np.float64)
    c = np.array(c, dtype=np.float64)

    ba = a - b
    bc = c - b

    cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-8)
    cosine = np.clip(cosine, -1.0, 1.0)
    angle = np.degrees(np.arccos(cosine))
    return angle


def extract_joint_angles(keypoints, confidence=None):
    """
    从单帧关键点提取所有关节角度
    keypoints: (33, 2或3) 数组，MediaPipe Pose 33个关键点坐标
    confidence: (33,) 置信度数组，可选
    返回: (N,) 角度数组, (N,) 置信度数组
    """
    angles = []
    confs = []
    kp = np.array(keypoints)

    for name, vertex, p_a, p_b in JOINT_ANGLE_DEFS:
        try:
            pt_v = kp[vertex]
            pt_a = kp[p_a]
            pt_b = kp[p_b]

            angle = calculate_angle(pt_a, pt_v, pt_b)
            angles.append(angle)

            if confidence is not None:
                # 关节角度的置信度取三个点中最低的
                c = min(confidence[vertex], confidence[p_a], confidence[p_b])
                confs.append(c)
            else:
                confs.append(1.0)
        except (IndexError, ValueError):
            angles.append(0.0)
            confs.append(0.0)

    return np.array(angles), np.array(confs)


def smooth_keypoints(keypoint_sequence, window_size=SMOOTH_WINDOW):
    """
    对关键点序列做滑动平均平滑
    keypoint_sequence: (T, 33, 2或3) 关键点时间序列
    返回: 平滑后的序列
    """
    seq = np.array(keypoint_sequence)
    if len(seq) < window_size:
        return seq

    smoothed = np.copy(seq)
    half = window_size // 2

    for t in range(len(seq)):
        start = max(0, t - half)
        end = min(len(seq), t + half + 1)
        smoothed[t] = np.mean(seq[start:end], axis=0)

    return smoothed


def get_part_keypoints(keypoints, part_name):
    """
    获取某个身体部位包含的关键点
    """
    if part_name in BODY_PARTS:
        indices = BODY_PARTS[part_name]["keypoints"]
        return np.array(keypoints)[indices]
    return np.array([])


def normalize_angles(angle_sequence):
    """
    将角度序列归一化到0-1范围
    angle_sequence: (T, N) 角度时间序列
    """
    arr = np.array(angle_sequence)
    min_val = np.min(arr, axis=0, keepdims=True)
    max_val = np.max(arr, axis=0, keepdims=True)
    range_val = np.where(max_val - min_val > 1e-8, max_val - min_val, 1.0)
    return (arr - min_val) / range_val


def compute_dtw_path_bending_cost(path):
    """
    计算DTW对齐路径的弯曲成本（用于节奏评估）
    路径越弯曲说明节奏偏差越大
    path: DTW对齐路径 [(i, j), ...]
    返回: 弯曲成本（0-1，0=完全线性，1=极度弯曲）
    """
    if len(path) < 2:
        return 0.0

    path = np.array(path)
    # 计算实际路径长度
    segment_lengths = np.sqrt(np.sum(np.diff(path, axis=0) ** 2, axis=1))
    actual_length = np.sum(segment_lengths)

    # 计算线性路径长度（起点到终点的直线距离）
    linear_length = np.sqrt(np.sum((path[-1] - path[0]) ** 2))

    if linear_length < 1e-8:
        return 0.0

    bending = (actual_length - linear_length) / actual_length
    return float(np.clip(bending, 0.0, 1.0))


def get_score_color(score):
    """
    根据分数获取颜色
    """
    if score >= 85:
        return (0, 255, 0)    # 绿
    elif score >= 70:
        return (255, 255, 0)  # 黄
    elif score >= 50:
        return (255, 165, 0)  # 橙
    else:
        return (255, 0, 0)   # 红


def get_score_label(score):
    """
    根据分数获取评级文字
    """
    if score >= 90:
        return "优秀"
    elif score >= 80:
        return "良好"
    elif score >= 60:
        return "及格"
    elif score >= 40:
        return "较弱"
    else:
        return "待改进"


def infer_dynamic_weights(teacher_angles):
    """
    根据教师参考动作中各部位关节角度变化幅度自动推断权重
    变化幅度越大的部位权重越高
    teacher_angles: (T, N) 教师动作的关节角度序列
    返回: dict 部位->权重
    """
    arr = np.array(teacher_angles)
    # 计算每个关节角度的标准差作为变化幅度指标
    stds = np.std(arr, axis=0)

    # 将关节角度映射到身体部位
    joint_to_part = {
        0: "左上肢", 2: "左上肢",       # left_elbow, left_shoulder
        1: "右上肢", 3: "右上肢",       # right_elbow, right_shoulder
        4: "左下肢", 6: "左下肢",       # left_knee, left_hip
        5: "右下肢", 7: "右下肢",       # right_knee, right_hip
        8: "躯干",                     # trunk_inclination
        9: "头颈",                     # neck
    }

    part_variation = {}
    for ji, part in joint_to_part.items():
        if ji < len(stds):
            part_variation[part] = part_variation.get(part, 0) + stds[ji]

    total = sum(part_variation.values())
    if total < 1e-8:
        # 如果几乎没变化，使用默认权重
        from config import DEFAULT_PART_WEIGHTS
        return DEFAULT_PART_WEIGHTS.copy()

    weights = {part: v / total for part, v in part_variation.items()}
    # 确保所有部位都有权重
    from config import DEFAULT_PART_WEIGHTS
    for part in DEFAULT_PART_WEIGHTS:
        if part not in weights:
            weights[part] = 0.01

    # 归一化
    total_w = sum(weights.values())
    weights = {k: v / total_w for k, v in weights.items()}
    return weights
