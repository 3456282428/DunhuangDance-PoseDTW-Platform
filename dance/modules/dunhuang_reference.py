"""
敦煌舞标准动作参考系统
功能：
1. 管理Chang-E数据集的8类敦煌舞动作参考
2. 支持加载动捕骨架数据作为教师参考模板
3. 敦煌舞特有关键姿态识别（反弹琵琶、飞天等）
4. 将3D动捕数据转换为MediaPipe 33关键点格式

Chang-E数据集8个类别：
1. Flying Apsaras (飞天) - 6min32s
2. Lotus Steps (莲花步) - 3min51s
3. Thirty-Six Postures Female (三十六式-女) - 5min29s
4. Thirty-Six Postures Male (三十六式-男) - 4min31s
5. Revelation Meditation (参禅) - 3min24s
6. Sogdian Whirl (胡旋) - 1min18s
7. Playing the Pipa Behind the Back (反弹琵琶) - 2min8s
8. Lei Gong Drum (雷公鼓) - 4min51s
"""
import os
import json
import pickle
import numpy as np

# Chang-E数据集8个敦煌舞类别
DUNHUANG_CATEGORIES = {
    "flying_apsaras": {
        "name_cn": "飞天",
        "name_en": "Flying Apsaras",
        "duration": "6min32s",
        "performer": "one female",
        "key_features": ["手臂延伸", "身姿飘逸", "腾空感"],
    },
    "lotus_steps": {
        "name_cn": "莲花步",
        "name_en": "Lotus Steps",
        "duration": "3min51s",
        "performer": "one female",
        "key_features": ["步伐轻盈", "手部姿态", "身体律动"],
    },
    "thirty_six_postures_f": {
        "name_cn": "三十六式(女)",
        "name_en": "Thirty-Six Postures (Female)",
        "duration": "5min29s",
        "performer": "two females",
        "key_features": ["姿态多变", "双人配合", "造型丰富"],
    },
    "thirty_six_postures_m": {
        "name_cn": "三十六式(男)",
        "name_en": "Thirty-Six Postures (Male)",
        "duration": "4min31s",
        "performer": "two males",
        "key_features": ["力量感", "姿态刚健", "造型丰富"],
    },
    "revelation_meditation": {
        "name_cn": "参禅",
        "name_en": "Revelation Meditation",
        "duration": "3min24s",
        "performer": "one female & one male",
        "key_features": ["静止造型", "内心表达", "缓慢运动"],
    },
    "sogdian_whirl": {
        "name_cn": "胡旋",
        "name_en": "Sogdian Whirl",
        "duration": "1min18s",
        "performer": "one male",
        "key_features": ["旋转动作", "节奏快速", "西域风格"],
    },
    "pipa_behind_back": {
        "name_cn": "反弹琵琶",
        "name_en": "Playing the Pipa Behind the Back",
        "duration": "2min8s",
        "performer": "one male",
        "key_features": ["手臂后伸", "特殊造型", "标志性姿态"],
    },
    "lei_gong_drum": {
        "name_cn": "雷公鼓",
        "name_en": "Lei Gong Drum",
        "duration": "4min51s",
        "performer": "two males",
        "key_features": ["击鼓动作", "节奏强烈", "力量表现"],
    },
}

# 敦煌舞特有关键姿态的关节角度特征
# 用于识别敦煌舞标志性动作
DUNHUANG_KEY_POSES = {
    "pipa_behind_back": {
        "name": "反弹琵琶姿态",
        "description": "手臂后伸至背后，肘关节高度弯曲",
        "angle_features": {
            "right_elbow": (30, 80),    # 右肘高度弯曲
            "right_shoulder": (80, 140), # 右肩后伸
            "left_arm_horizontal": True,  # 左臂水平伸展
        },
    },
    "flying_apsaras_pose": {
        "name": "飞天姿态",
        "description": "双臂向斜上方延伸，身体呈S曲线",
        "angle_features": {
            "left_shoulder": (120, 170),  # 左臂上举
            "right_shoulder": (120, 170), # 右臂上举
            "trunk_inclination": (10, 30), # 躯干微倾
        },
    },
    "sogdian_whirl_pose": {
        "name": "胡旋姿态",
        "description": "身体旋转，双臂展开保持平衡",
        "angle_features": {
            "left_shoulder": (80, 120),
            "right_shoulder": (80, 120),
            "trunk_inclination": (0, 15),  # 躯干接近直立
        },
    },
    "lotus_step_pose": {
        "name": "莲花步姿态",
        "description": "步伐轻盈，手部持莲花指",
        "angle_features": {
            "left_elbow": (60, 100),   # 左肘弯曲呈持花状
            "right_elbow": (60, 100),  # 右肘弯曲
            "left_knee": (150, 180),   # 左膝微曲
            "right_knee": (150, 180),
        },
    },
}


class DunhuangReferenceSystem:
    """敦煌舞标准动作参考系统"""

    def __init__(self, data_dir=None):
        from config import MODELS_DIR
        self.data_dir = data_dir or os.path.join(MODELS_DIR, "dunhuang")
        os.makedirs(self.data_dir, exist_ok=True)
        self.references = {}  # 类别 -> 关键点序列
        self.load_references()

    def load_references(self):
        """加载已存储的敦煌舞参考动作"""
        ref_file = os.path.join(self.data_dir, "dunhuang_references.pkl")
        if os.path.exists(ref_file):
            with open(ref_file, "rb") as f:
                self.references = pickle.load(f)
            print(f"[DunhuangRef] 已加载 {len(self.references)} 个敦煌舞参考动作")
        else:
            print("[DunhuangRef] 无预存参考动作，需导入Chang-E数据")

    def add_reference(self, category, keypoints, confidences=None, metadata=None):
        """
        添加敦煌舞参考动作
        category: DUNHUANG_CATEGORIES中的键
        keypoints: (T, 33, 2) MediaPipe格式关键点序列
        """
        if category not in DUNHUANG_CATEGORIES:
            print(f"[DunhuangRef] 未知类别: {category}")
            return

        self.references[category] = {
            "keypoints": np.array(keypoints),
            "confidences": np.array(confidences) if confidences is not None else None,
            "metadata": metadata or {},
            "category_info": DUNHUANG_CATEGORIES[category],
        }
        print(f"[DunhuangRef] 已添加参考: {DUNHUANG_CATEGORIES[category]['name_cn']}"
              f" ({len(keypoints)}帧)")

    def save_references(self):
        """保存参考动作"""
        ref_file = os.path.join(self.data_dir, "dunhuang_references.pkl")
        with open(ref_file, "wb") as f:
            pickle.dump(self.references, f)
        print(f"[DunhuangRef] 已保存 {len(self.references)} 个参考动作到 {ref_file}")

    def get_reference(self, category):
        """获取指定类别的参考动作"""
        if category in self.references:
            return self.references[category]
        print(f"[DunhuangRef] 类别 '{category}' 不存在")
        return None

    def list_categories(self):
        """列出所有可用类别"""
        available = []
        for cat, info in DUNHUANG_CATEGORIES.items():
            status = "已加载" if cat in self.references else "未导入"
            available.append({
                "id": cat,
                "name_cn": info["name_cn"],
                "name_en": info["name_en"],
                "duration": info["duration"],
                "performer": info["performer"],
                "key_features": info["key_features"],
                "status": status,
            })
        return available

    def detect_key_pose(self, keypoints, confidences=None):
        """
        识别当前帧是否包含敦煌舞标志性姿态
        返回检测到的姿态名称和置信度
        """
        from utils import extract_joint_angles

        angles, confs = extract_joint_angles(keypoints, confidences)

        # 关节角度索引（与config.py JOINT_ANGLE_DEFS对应）
        # 0: left_elbow, 1: right_elbow, 2: left_shoulder, 3: right_shoulder
        # 4: left_knee, 5: right_knee, 6: left_hip, 7: right_hip
        # 8: trunk_inclination, 9: neck

        detections = []
        for pose_id, pose_info in DUNHUANG_KEY_POSES.items():
            angle_feats = pose_info["angle_features"]
            match_score = 0
            total_checks = 0

            for joint_name, (low, high) in angle_feats.items():
                if "elbow" in joint_name:
                    idx = 0 if "left" in joint_name else 1
                elif "shoulder" in joint_name:
                    idx = 2 if "left" in joint_name else 3
                elif "knee" in joint_name:
                    idx = 4 if "left" in joint_name else 5
                elif "trunk" in joint_name:
                    idx = 8
                elif "neck" in joint_name:
                    idx = 9
                else:
                    continue

                total_checks += 1
                if idx < len(angles):
                    angle_val = angles[idx]
                    if low <= angle_val <= high:
                        match_score += 1

            confidence = match_score / max(total_checks, 1)
            if confidence >= 0.5:
                detections.append({
                    "pose": pose_id,
                    "name": pose_info["name"],
                    "description": pose_info["description"],
                    "confidence": round(confidence, 2),
                })

        detections.sort(key=lambda x: x["confidence"], reverse=True)
        return detections

    def convert_smpl_to_mediapipe(self, smpl_keypoints):
        """
        将SMPL格式3D关键点转换为MediaPipe 33关键点格式
        Chang-E数据集使用SMPL格式，需要转换以适配我们的系统

        SMPL有24个关节，MediaPipe有33个关键点
        通过插值和映射完成转换
        """
        # SMPL关节索引 -> MediaPipe索引的映射
        smpl_to_mp = {
            0: 0,    # Pelvis -> 鼻（中心参考）
            1: 12,   # L_Hip -> 右髋（镜像）
            2: 11,   # R_Hip -> 左髋
            3: 16,   # Spine1 -> 躯干中点(用右肩近似)
            4: 14,   # L_Knee -> 右膝
            5: 13,   # R_Knee -> 左膝
            6: 28,   # L_Ankle -> 右踝
            7: 27,   # R_Ankle -> 左踝
            8: 12,   # Spine2 -> 右肩(近似)
            9: 16,   # Neck -> 右肩(近似)
            10: 14,  # L_Collar -> 右肘(近似)
            11: 13,  # R_Collar -> 左肘(近似)
            12: 16,  # L_Shoulder -> 右腕(近似)
            13: 15,  # R_Shoulder -> 左腕(近似)
            14: 22,  # L_Elbow -> 右手(近似)
            15: 21,  # R_Elbow -> 左手(近似)
            16: 24,  # L_Wrist -> 右手小指(近似)
            17: 23,  # R_Wrist -> 左手小指(近似)
        }

        smpl_kpts = np.array(smpl_keypoints)  # (T, 24, 3)
        T = len(smpl_kpts)

        # 创建MediaPipe格式 (T, 33, 2)
        mp_kpts = np.zeros((T, 33, 2), dtype=np.float32)
        mp_confs = np.ones((T, 33), dtype=np.float32) * 0.8

        for t in range(T):
            for smpl_idx, mp_idx in smpl_to_mp.items():
                if smpl_idx < smpl_kpts.shape[1]:
                    # 取x,y坐标（投影到2D）
                    mp_kpts[t, mp_idx, 0] = smpl_kpts[t, smpl_idx, 0]
                    mp_kpts[t, mp_idx, 1] = smpl_kpts[t, smpl_idx, 1]

            # 对称填充：左半身关键点从右半身镜像
            mirror_pairs = [(11, 12), (13, 14), (15, 16), (23, 24), (25, 26), (27, 28)]
            for left, right in mirror_pairs:
                if mp_kpts[t, left, 0] == 0 and mp_kpts[t, right, 0] != 0:
                    mp_kpts[t, left] = mp_kpts[t, right]
                    mp_kpts[t, left, 0] *= -1  # x轴镜像
                elif mp_kpts[t, right, 0] == 0 and mp_kpts[t, left, 0] != 0:
                    mp_kpts[t, right] = mp_kpts[t, left]
                    mp_kpts[t, right, 0] *= -1

            # 头部关键点近似
            if mp_kpts[t, 0, 0] != 0:
                # 鼻子位置向上偏移作为眼睛和耳朵
                for idx in [1, 2, 3, 4, 5, 6, 7, 8]:  # 眼睛和耳朵
                    offset = np.random.normal(0, 5, 2)
                    mp_kpts[t, idx, 0] = mp_kpts[t, 0, 0] + offset[0]
                    mp_kpts[t, idx, 1] = mp_kpts[t, 0, 1] - 20 + offset[1]

        return mp_kpts, mp_confs

    def generate_synthetic_reference(self, category, num_frames=150, fps=30):
        """
        为指定敦煌舞类别生成合成参考动作（当Chang-E数据不可用时）
        基于该类别的关键特征模拟动作序列
        """
        np.random.seed(hash(category) % 2**32)

        if category not in DUNHUANG_CATEGORIES:
            print(f"[DunhuangRef] 未知类别: {category}")
            return None

        info = DUNHUANG_CATEGORIES[category]
        key_feats = info["key_features"]

        # 根据类别特征生成不同的动作模式
        kpts = np.zeros((num_frames, 33, 2), dtype=np.float32)

        for t in range(num_frames):
            phase = t / num_frames  # 0-1的相位

            # 基础姿态（站立）
            kpts[t, 0] = [320, 80]   # 鼻
            kpts[t, 11] = [280, 150] # 左肩
            kpts[t, 12] = [360, 150] # 右肩
            kpts[t, 23] = [290, 250] # 左髋
            kpts[t, 24] = [350, 250] # 右髋
            kpts[t, 25] = [285, 320] # 左膝
            kpts[t, 26] = [355, 320] # 右膝
            kpts[t, 27] = [280, 390] # 左踝
            kpts[t, 28] = [360, 390] # 右踝

            # 根据类别添加特征动作
            if category == "flying_apsaras":
                # 飞天：双臂上举延伸，身体微倾
                swing = 30 * np.sin(phase * np.pi * 4)
                kpts[t, 13] = [280 + swing, 80]   # 左肘上举
                kpts[t, 15] = [270 + swing, 30]   # 左腕更高
                kpts[t, 14] = [360 - swing, 80]   # 右肘上举
                kpts[t, 16] = [370 - swing, 30]   # 右腕更高

            elif category == "pipa_behind_back":
                # 反弹琵琶：手臂后伸，肘弯曲
                bend = 20 * np.sin(phase * np.pi * 3)
                kpts[t, 14] = [380, 170]           # 右肘向外
                kpts[t, 16] = [330, 200]           # 右腕后伸
                kpts[t, 13] = [260, 160]           # 左肘
                kpts[t, 15] = [240, 130]           # 左臂水平

            elif category == "sogdian_whirl":
                # 胡旋：旋转动作，双臂展开
                rotation = phase * np.pi * 4
                arm_extend = 60
                kpts[t, 13] = [280 - arm_extend * np.cos(rotation), 150]
                kpts[t, 15] = [280 - arm_extend * 1.5 * np.cos(rotation), 150]
                kpts[t, 14] = [360 + arm_extend * np.cos(rotation), 150]
                kpts[t, 16] = [360 + arm_extend * 1.5 * np.cos(rotation), 150]

            elif category == "lotus_steps":
                # 莲花步：步伐轻盈，手部持花
                step_h = 15 * np.sin(phase * np.pi * 6)
                kpts[t, 27, 1] += step_h   # 左脚抬起
                kpts[t, 28, 1] -= step_h   # 右脚
                kpts[t, 13] = [250, 140]   # 左肘弯曲
                kpts[t, 15] = [230, 120]   # 左腕持花
                kpts[t, 14] = [390, 140]   # 右肘弯曲
                kpts[t, 16] = [410, 120]   # 右腕

            else:
                # 默认：手臂自然摆动
                swing = 30 * np.sin(phase * np.pi * 4)
                kpts[t, 13] = [260, 200 + swing]
                kpts[t, 15] = [250, 260 + swing]
                kpts[t, 14] = [380, 200 - swing]
                kpts[t, 16] = [390, 260 - swing]

            # 添加自然抖动
            kpts[t] += np.random.normal(0, 2, kpts[t].shape)

        confs = np.ones((num_frames, 33), dtype=np.float32) * 0.85

        self.add_reference(category, kpts, confs,
                          metadata={"synthetic": True, "fps": fps})
        return kpts, confs

    def generate_all_synthetic_references(self):
        """为所有敦煌舞类别生成合成参考动作"""
        for cat_id in DUNHUANG_CATEGORIES:
            self.generate_synthetic_reference(cat_id)
        self.save_references()
        print(f"[DunhuangRef] 已生成 {len(DUNHUANG_CATEGORIES)} 个合成参考动作")
