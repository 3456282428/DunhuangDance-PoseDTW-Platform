"""
评分校准模块
功能：
1. 加载专家标注数据（DDPA格式：力度、锐度、节奏、精准度，1-10分）
2. 运行评分流水线获取原始分
3. 训练回归模型将原始分映射到专家分
4. 保存/加载校准参数
5. 推理时自动应用校准

DDPA数据集格式：
{
    "video_path": "xxx.mp4",
    "reference_path": "ref.mp4",
    "expert_scores": {
        "力度": 8,      // 1-10
        "锐度": 7,      // 1-10
        "节奏": 9,      // 1-10
        "精准度": 8,    // 1-10
        "overall": 8.0   // 1-10
    }
}
"""
import os
import json
import pickle
import numpy as np
from config import OUTPUT_DIR, BODY_PARTS, DEFAULT_PART_WEIGHTS


class ScoreCalibrator:
    """评分校准器：用专家标注调整模型评分"""

    def __init__(self, calibration_dir=None):
        self.calibration_dir = calibration_dir or os.path.join(OUTPUT_DIR, "calibration")
        os.makedirs(self.calibration_dir, exist_ok=True)
        self.model = None
        self.feature_names = []
        self.scaler_mean = None
        self.scaler_std = None
        self.target_mean = None
        self.target_std = None
        self.is_calibrated = False

    def extract_features(self, scores):
        """
        从评分结果中提取特征向量
        特征包括：6个部位分 + 3个维度分 + DTW距离 + 节奏弯曲成本
        """
        features = []
        self.feature_names = []

        part_scores = scores.get("part_scores", {})
        for part in BODY_PARTS:
            if part in part_scores:
                features.append(part_scores[part]["score"])
                self.feature_names.append(f"part_{part}")

        dim_scores = scores.get("dimension_scores", {})
        for dim in ["动作规范性", "节奏掌握度", "整体协调性"]:
            if dim in dim_scores:
                features.append(dim_scores[dim])
                self.feature_names.append(f"dim_{dim}")

        # 附加特征
        features.append(scores.get("dtw_distance", 0))
        self.feature_names.append("dtw_distance")

        features.append(scores.get("rhythm_cost", 0))
        self.feature_names.append("rhythm_cost")

        features.append(scores.get("total_score", 0))
        self.feature_names.append("raw_total")

        return np.array(features, dtype=np.float64)

    def prepare_training_data(self, calibration_samples):
        """
        准备训练数据
        calibration_samples: list of {"raw_scores": dict, "expert_scores": dict}
        返回: X (N, F), y (N, 5) [力度, 锐度, 节奏, 精准度, overall]
        """
        X_list = []
        y_list = []

        # 专家评分维度（DDPA格式）
        expert_dims = ["力度", "锐度", "节奏", "精准度", "overall"]

        for sample in calibration_samples:
            raw_scores = sample["raw_scores"]
            expert_scores = sample["expert_scores"]

            features = self.extract_features(raw_scores)
            X_list.append(features)

            target = []
            for dim in expert_dims:
                target.append(expert_scores.get(dim, expert_scores.get("overall", 5)))
            y_list.append(target)

        X = np.array(X_list)
        y = np.array(y_list)

        # 归一化目标到0-100（DDPA是1-10分制）
        y = y * 10.0  # 转换为0-100

        return X, y

    def train(self, calibration_samples):
        """
        训练校准模型
        使用岭回归（Ridge Regression）将原始分映射到专家分
        """
        from sklearn.linear_model import Ridge
        from sklearn.preprocessing import StandardScaler

        X, y = self.prepare_training_data(calibration_samples)

        if len(X) < 2:
            print("[ScoreCalibrator] 训练数据不足（至少需要2个样本）")
            return False

        print(f"[ScoreCalibrator] 训练数据: {X.shape}, 目标: {y.shape}")
        print(f"  特征: {self.feature_names}")

        # 标准化
        self.scaler_mean = np.mean(X, axis=0)
        self.scaler_std = np.std(X, axis=0) + 1e-8
        X_norm = (X - self.scaler_mean) / self.scaler_std

        # 目标统计
        self.target_mean = np.mean(y, axis=0)
        self.target_std = np.std(y, axis=0) + 1e-8

        # 岭回归
        self.model = Ridge(alpha=1.0)
        self.model.fit(X_norm, y)

        # 训练集评估
        y_pred = self.model.predict(X_norm)
        mae = np.mean(np.abs(y_pred - y))
        corr = np.corrcoef(y_pred.flatten(), y.flatten())[0, 1]

        print(f"[ScoreCalibrator] 校准完成: MAE={mae:.2f}, 相关系数={corr:.4f}")

        self.is_calibrated = True
        self.save()
        return True

    def calibrate(self, scores):
        """
        应用校准到评分结果
        返回校准后的评分
        """
        if not self.is_calibrated or self.model is None:
            return scores

        features = self.extract_features(scores)
        if not np.all(np.isfinite(features)):
            # 特征含 NaN/Inf（如 DTW 失败）时跳过校准，避免污染输出
            print("[ScoreCalibrator] 特征含非有限值，跳过校准")
            return scores
        X_norm = (features - self.scaler_mean) / self.scaler_std
        y_pred = self.model.predict(X_norm.reshape(1, -1))[0]

        # 校准后的专家维度分
        calibrated = scores.copy()
        calibrated["calibrated_scores"] = {
            "力度": round(float(y_pred[0]), 2),
            "锐度": round(float(y_pred[1]), 2),
            "节奏": round(float(y_pred[2]), 2),
            "精准度": round(float(y_pred[3]), 2),
            "overall": round(float(y_pred[4]), 2),
        }

        # 用校准后的overall调整总分
        original_total = scores.get("total_score", 0)
        calibrated_total = 0.5 * original_total + 0.5 * float(y_pred[4])
        calibrated["calibrated_total"] = round(calibrated_total, 2)

        return calibrated

    def save(self):
        """保存校准模型"""
        path = os.path.join(self.calibration_dir, "calibration_model.pkl")
        data = {
            "model": self.model,
            "feature_names": self.feature_names,
            "scaler_mean": self.scaler_mean,
            "scaler_std": self.scaler_std,
            "target_mean": self.target_mean,
            "target_std": self.target_std,
            "is_calibrated": self.is_calibrated,
        }
        with open(path, "wb") as f:
            pickle.dump(data, f)
        print(f"[ScoreCalibrator] 校准模型已保存: {path}")

    def load(self):
        """加载校准模型"""
        path = os.path.join(self.calibration_dir, "calibration_model.pkl")
        if not os.path.exists(path):
            print("[ScoreCalibrator] 未找到校准模型")
            return False

        with open(path, "rb") as f:
            data = pickle.load(f)

        self.model = data["model"]
        self.feature_names = data["feature_names"]
        self.scaler_mean = data["scaler_mean"]
        self.scaler_std = data["scaler_std"]
        self.target_mean = data["target_mean"]
        self.target_std = data["target_std"]
        self.is_calibrated = data["is_calibrated"]

        if self.is_calibrated:
            print(f"[ScoreCalibrator] 校准模型已加载 (特征: {len(self.feature_names)}维)")
        return self.is_calibrated


def generate_synthetic_calibration_data(pipeline=None, num_samples=20, seed=42):
    """
    生成合成校准数据（当DDPA数据集不可用时）
    基于DDPA评分维度（力度、锐度、节奏、精准度）模拟专家评分

    原理：
    - 力度 ≈ 部位加权总分 * (0.8 + 随机扰动)  + 上肢权重加成
    - 锐度 ≈ 动作规范性 * (0.8 + 随机扰动)
    - 节奏 ≈ 节奏掌握度 * (0.9 + 随机扰动)
    - 精准度 ≈ 各部位得分的加权 + 随机扰动
    - overall ≈ 四个维度的加权平均
    """
    np.random.seed(seed)
    samples = []

    # DDPA维度权重（基于舞蹈专家经验）
    dim_weights = {"力度": 0.25, "锐度": 0.20, "节奏": 0.30, "精准度": 0.25}

    for i in range(num_samples):
        # 模拟原始评分结果
        base_score = np.random.uniform(50, 95)
        part_scores = {}
        for part in BODY_PARTS:
            # 各部位分围绕总分波动
            ps = base_score + np.random.normal(0, 8)
            ps = max(0, min(100, ps))
            part_scores[part] = {
                "score": round(ps, 2),
                "avg_deviation": round(1 - ps / 100, 4),
                "num_frames": 100,
                "description": BODY_PARTS[part]["description"],
            }

        dim_scores = {
            "动作规范性": round(base_score + np.random.normal(0, 5), 2),
            "节奏掌握度": round(base_score + np.random.normal(0, 5), 2),
            "整体协调性": round(base_score + np.random.normal(0, 5), 2),
        }
        for d in dim_scores:
            dim_scores[d] = max(0, min(100, dim_scores[d]))

        raw_scores = {
            "part_scores": part_scores,
            "dimension_scores": dim_scores,
            "total_score": round(base_score, 2),
            "dtw_distance": np.random.uniform(5, 25),
            "rhythm_cost": np.random.uniform(0.02, 0.15),
        }

        # 模拟专家评分（1-10分制）
        # 力度：与上肢+下肢得分相关
        upper_score = (part_scores["左上肢"]["score"] + part_scores["右上肢"]["score"]) / 2
        lower_score = (part_scores["左下肢"]["score"] + part_scores["右下肢"]["score"]) / 2
        expert_li = (upper_score * 0.4 + lower_score * 0.6) / 10 + np.random.normal(0, 0.5)

        # 锐度：与动作规范性相关
        expert_rui = dim_scores["动作规范性"] / 10 + np.random.normal(0, 0.5)

        # 节奏：与节奏掌握度相关
        expert_jie = dim_scores["节奏掌握度"] / 10 + np.random.normal(0, 0.3)

        # 精准度：与各部位得分均值相关
        avg_part = np.mean([ps["score"] for ps in part_scores.values()])
        expert_jing = avg_part / 10 + np.random.normal(0, 0.4)

        # overall
        expert_overall = (expert_li * dim_weights["力度"] +
                         expert_rui * dim_weights["锐度"] +
                         expert_jie * dim_weights["节奏"] +
                         expert_jing * dim_weights["精准度"])

        # 裁剪到1-10范围
        for v in [expert_li, expert_rui, expert_jie, expert_jing, expert_overall]:
            v = max(1, min(10, v))

        expert_scores = {
            "力度": round(max(1, min(10, expert_li)), 1),
            "锐度": round(max(1, min(10, expert_rui)), 1),
            "节奏": round(max(1, min(10, expert_jie)), 1),
            "精准度": round(max(1, min(10, expert_jing)), 1),
            "overall": round(max(1, min(10, expert_overall)), 1),
        }

        samples.append({
            "raw_scores": raw_scores,
            "expert_scores": expert_scores,
        })

    return samples
