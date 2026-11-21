"""
模块四：分部位评分与反馈
功能：
1. 沿DTW对齐路径逐帧计算各关节角度差异
2. 按身体部位分组聚合，输出部位分和总分
3. 多维评分：动作规范性（空间）、节奏掌握度（时序）、整体协调性
4. 动态权重：根据参考动作中各部位变化幅度自动推断
"""
import numpy as np
from config import (BODY_PARTS, DEFAULT_PART_WEIGHTS,
                    JOINT_ANGLE_DEFS, SCORE_SCALE)
from utils import (extract_joint_angles, infer_dynamic_weights,
                   compute_dtw_path_bending_cost)


class DanceScorer:
    """舞蹈评分器：分部位 + 多维度评分"""

    def __init__(self, use_dynamic_weights=True):
        """
        use_dynamic_weights: 是否根据参考动作自动推断部位权重
        """
        self.use_dynamic_weights = use_dynamic_weights
        self.weights = DEFAULT_PART_WEIGHTS.copy()
        self.results = {}

    def compute_part_scores(self, teacher_kpts, student_kpts,
                            teacher_confs, student_confs,
                            alignment_path):
        """
        计算各身体部位的评分
        teacher_kpts: (T1, 33, 2) 教师关键点序列
        student_kpts: (T2, 33, 2) 学生关键点序列
        teacher_confs: (T1, 33) 教师置信度
        student_confs: (T2, 33) 学生置信度
        alignment_path: DTW对齐路径

        返回: dict 部位名-> {score, avg_deviation, details}
        """
        # 提取关节角度序列
        teacher_angles, teacher_ac = self._extract_angles(
            teacher_kpts, teacher_confs
        )
        student_angles, student_ac = self._extract_angles(
            student_kpts, student_confs
        )

        # 关节角度索引到身体部位的映射
        joint_to_part = self._build_joint_part_mapping()

        # 沿对齐路径计算逐帧角度差异
        part_deviations = {part: [] for part in BODY_PARTS}
        part_weights_sum = {part: 0.0 for part in BODY_PARTS}

        for i, j in alignment_path:
            if i >= len(teacher_angles) or j >= len(student_angles):
                continue

            t_angles = teacher_angles[i]
            s_angles = student_angles[i] if False else student_angles[j]
            t_conf = teacher_ac[i]
            s_conf = student_ac[j]

            for joint_idx in range(len(JOINT_ANGLE_DEFS)):
                part_name = joint_to_part.get(joint_idx)
                if part_name is None:
                    continue

                angle_diff = abs(t_angles[joint_idx] - s_angles[joint_idx])
                # 归一化到0-1（最大180度）
                norm_diff = angle_diff / 180.0

                # 置信度加权
                conf = min(t_conf[joint_idx], s_conf[joint_idx])
                conf = max(conf, 0.01)

                part_deviations[part_name].append(norm_diff * conf)
                part_weights_sum[part_name] += conf

        # 计算每个部位的加权平均偏差和得分
        part_scores = {}
        for part_name in BODY_PARTS:
            deviations = part_deviations[part_name]
            total_weight = part_weights_sum[part_name]

            if total_weight > 0:
                avg_deviation = sum(deviations) / total_weight
            else:
                avg_deviation = 1.0

            # 转换为百分制得分
            score = SCORE_SCALE * (1.0 - avg_deviation)
            score = max(0, min(SCORE_SCALE, score))

            part_scores[part_name] = {
                "score": round(score, 2),
                "avg_deviation": round(avg_deviation, 4),
                "num_frames": len(deviations),
                "description": BODY_PARTS[part_name]["description"],
            }

        self.results["part_scores"] = part_scores
        return part_scores

    def compute_dimension_scores(self, teacher_kpts, student_kpts,
                                 teacher_confs, student_confs,
                                 alignment_path, part_scores=None):
        """
        计算多维评分维度
        维度一：动作规范性（空间特性）
        维度二：节奏掌握度（时序特性）
        维度三：整体协调性

        返回: dict 维度名->得分
        """
        if part_scores is None:
            part_scores = self.results.get("part_scores", {})

        teacher_angles, _ = self._extract_angles(teacher_kpts, teacher_confs)
        student_angles, _ = self._extract_angles(student_kpts, student_confs)

        # ===== 维度一：动作规范性（空间特性） =====
        # 基于对齐后各部位关键点空间位置偏差
        spatial_deviations = []
        for i, j in alignment_path:
            if i >= len(teacher_kpts) or j >= len(student_kpts):
                continue
            # 关键点空间位置差异（归一化到画面尺寸）
            t_kpts = teacher_kpts[i]
            s_kpts = student_kpts[j]
            t_conf = teacher_confs[i]
            s_conf = student_confs[j]

            # 使用置信度加权的空间偏差
            conf_mask = (t_conf + s_conf) / 2
            conf_mask = np.clip(conf_mask, 0.01, 1.0)

            # 计算坐标差，按画面尺寸归一化
            coord_diff = np.sqrt(np.sum((t_kpts - s_kpts) ** 2, axis=1))
            # 归一化（假设画面尺寸约500像素）
            norm_coord_diff = coord_diff / 500.0
            norm_coord_diff = np.clip(norm_coord_diff, 0, 1)

            weighted_dev = np.sum(norm_coord_diff * conf_mask) / np.sum(conf_mask)
            spatial_deviations.append(weighted_dev)

        if spatial_deviations:
            avg_spatial_dev = np.mean(spatial_deviations)
            spatial_score = SCORE_SCALE * (1.0 - avg_spatial_dev)
        else:
            spatial_score = 0
        spatial_score = max(0, min(SCORE_SCALE, spatial_score))

        # ===== 维度二：节奏掌握度（时序特性） =====
        # 基于DTW路径弯曲成本 + 关键帧时间偏差
        bending_cost = compute_dtw_path_bending_cost(alignment_path)

        # 关键帧时间偏差
        timing_dev = self._compute_timing_deviation(alignment_path)

        # 节奏得分 = 100 * (1 - 节奏偏差)
        rhythm_dev = 0.5 * bending_cost + 0.5 * timing_dev
        rhythm_score = SCORE_SCALE * (1.0 - rhythm_dev)
        rhythm_score = max(0, min(SCORE_SCALE, rhythm_score))

        # ===== 维度三：整体协调性 =====
        # 各部位得分的方差衡量，方差越小协调性越好
        if part_scores:
            part_score_values = [ps["score"] for ps in part_scores.values()]
            score_variance = np.var(part_score_values)
            # 方差归一化（假设最大方差2500，即50分差的方差）
            norm_variance = min(score_variance / 2500.0, 1.0)
            coordination_score = SCORE_SCALE * (1.0 - norm_variance)
        else:
            coordination_score = 0
        coordination_score = max(0, min(SCORE_SCALE, coordination_score))

        dimension_scores = {
            "动作规范性": round(spatial_score, 2),
            "节奏掌握度": round(rhythm_score, 2),
            "整体协调性": round(coordination_score, 2),
        }

        self.results["dimension_scores"] = dimension_scores
        return dimension_scores

    def compute_total_score(self, part_scores=None, dimension_scores=None,
                            weights=None):
        """
        计算综合总分
        部位加权得分（60%） + 维度得分（40%）
        """
        if part_scores is None:
            part_scores = self.results.get("part_scores", {})
        if dimension_scores is None:
            dimension_scores = self.results.get("dimension_scores", {})
        if weights is None:
            weights = self.weights

        # 部位加权得分
        part_weighted_sum = 0
        part_weight_total = 0
        for part_name, ps in part_scores.items():
            w = weights.get(part_name, 0)
            part_weighted_sum += ps["score"] * w
            part_weight_total += w

        part_total = part_weighted_sum / max(part_weight_total, 1e-8)

        # 维度平均得分
        dim_scores = list(dimension_scores.values())
        dim_total = np.mean(dim_scores) if dim_scores else 0

        # 综合: 60% 部位分 + 40% 维度分
        total = 0.6 * part_total + 0.4 * dim_total
        total = max(0, min(SCORE_SCALE, total))

        self.results["total_score"] = round(total, 2)
        self.results["part_weighted_total"] = round(part_total, 2)
        return self.results["total_score"]

    def set_dynamic_weights(self, teacher_angles):
        """
        根据教师参考动作设置动态权重
        """
        if self.use_dynamic_weights:
            self.weights = infer_dynamic_weights(teacher_angles)
            print(f"[DanceScorer] 动态权重: {self.weights}")

    def get_feedback(self, part_scores=None):
        """
        生成改进建议
        找出得分最低的部位，给出针对性建议
        """
        if part_scores is None:
            part_scores = self.results.get("part_scores", {})

        if not part_scores:
            return []

        # 按得分排序
        sorted_parts = sorted(part_scores.items(), key=lambda x: x[1]["score"])
        feedback = []

        for part_name, ps in sorted_parts:
            score = ps["score"]
            if score < 60:
                level = "需要重点改进"
            elif score < 75:
                level = "有提升空间"
            elif score < 85:
                level = "表现良好"
            else:
                level = "表现优秀"

            feedback.append({
                "part": part_name,
                "score": score,
                "level": level,
                "description": ps["description"],
                "avg_deviation": ps["avg_deviation"],
            })

        return feedback

    def _extract_angles(self, keypoints_sequence, confidences=None):
        """提取关节角度序列"""
        T = len(keypoints_sequence)
        angle_list = []
        conf_list = []

        for t in range(T):
            angles, ac = extract_joint_angles(
                keypoints_sequence[t],
                confidences[t] if confidences is not None else None
            )
            angle_list.append(angles)
            conf_list.append(ac)

        return np.array(angle_list), np.array(conf_list)

    def _build_joint_part_mapping(self):
        """构建关节角度索引到身体部位的映射"""
        mapping = {}
        for idx, (name, vertex, p_a, p_b) in enumerate(JOINT_ANGLE_DEFS):
            if "left_elbow" in name or "left_shoulder" in name:
                mapping[idx] = "左上肢"
            elif "right_elbow" in name or "right_shoulder" in name:
                mapping[idx] = "右上肢"
            elif "left_knee" in name or "left_hip" in name:
                mapping[idx] = "左下肢"
            elif "right_knee" in name or "right_hip" in name:
                mapping[idx] = "右下肢"
            elif "trunk" in name:
                mapping[idx] = "躯干"
            elif "neck" in name:
                mapping[idx] = "头颈"
        return mapping

    def _compute_timing_deviation(self, path):
        """计算关键帧时间偏差（路径相对缩放对角线的归一化偏离，0-1）"""
        if path is None or len(path) < 2:
            return 0.0

        path = np.array(path)
        # 以路径终点构造缩放对角线：教师帧 i 期望对应学生帧 j = i * T2/T1
        end_i = max(float(path[-1, 0]), 1.0)
        end_j = max(float(path[-1, 1]), 1.0)
        diagonal_j = path[:, 0] * end_j / end_i
        deviations = np.abs(path[:, 1] - diagonal_j) / end_j

        return float(np.clip(np.mean(deviations), 0.0, 1.0))

    def score(self, teacher_kpts, student_kpts,
              teacher_confs, student_confs,
              alignment_path, teacher_angles=None):
        """
        完整评分流程
        返回完整评分结果
        """
        # 设置动态权重
        if teacher_angles is not None and self.use_dynamic_weights:
            self.set_dynamic_weights(teacher_angles)

        # 计算部位得分
        part_scores = self.compute_part_scores(
            teacher_kpts, student_kpts,
            teacher_confs, student_confs,
            alignment_path
        )

        # 计算维度得分
        dimension_scores = self.compute_dimension_scores(
            teacher_kpts, student_kpts,
            teacher_confs, student_confs,
            alignment_path, part_scores
        )

        # 计算总分
        total = self.compute_total_score(part_scores, dimension_scores)

        # 生成反馈
        feedback = self.get_feedback(part_scores)

        self.results["feedback"] = feedback
        return self.results
