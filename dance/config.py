"""
舞蹈AI评分系统 - 全局配置
"""
import os

# ===== 路径配置 =====
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE_DIR, "models")
OUTPUT_DIR = os.path.join(BASE_DIR, "output")
UPLOADS_DIR = os.path.join(BASE_DIR, "uploads")

# ===== 视频预处理配置 =====
TARGET_FPS = 30  # 统一帧率
YOLO_MODEL = "yolo11n.pt"  # YOLO单人检测模型（使用服务器已有的YOLO）
YOLO_CONF_THRESHOLD = 0.5  # YOLO置信度阈值
CROP_PADDING = 30  # 裁剪边距（像素）
SMOOTH_WINDOW = 5  # 关键点平滑窗口大小

# ===== 姿态提取配置 =====
# MediaPipe Pose 33个关键点
MEDIAPIPE_MODEL_COMPLEXITY = 1  # 0=轻量, 1=中等(full模型内置无需下载), 2=全量(最精确，需联网下载heavy模型)
MEDIAPIPE_MIN_DETECTION_CONFIDENCE = 0.5
MEDIAPIPE_MIN_TRACKING_CONFIDENCE = 0.5
MIN_VISIBLE_CONFIDENCE = 0.3  # 低于此置信度的关键点标记为低置信度

# ===== DTW对齐配置 =====
DTW_RADIUS = 30  # Sakoe-Chiba带宽约束（帧数）
DTW_WINDOW_SIZE = 100  # DTW搜索窗口大小

# ===== 评分配置 =====
SCORE_SCALE = 100  # 百分制
# 身体部位分组（MediaPipe Pose 33关键点索引）
# 参考: https://google.github.io/mediapipe/solutions/pose.html
BODY_PARTS = {
    "左上肢": {
        "keypoints": [11, 13, 15],  # 左肩, 左肘, 左腕
        "joints": [("shoulder", 11, 13, 15)],  # 关节名称及构成角度的三个点
        "description": "左臂动作规范性"
    },
    "右上肢": {
        "keypoints": [12, 14, 16],  # 右肩, 右肘, 右腕
        "joints": [("shoulder", 12, 14, 16)],
        "description": "右臂动作规范性"
    },
    "左下肢": {
        "keypoints": [23, 25, 27],  # 左髋, 左膝, 左踝
        "joints": [("knee", 23, 25, 27), ("hip", 25, 27, 23)],
        "description": "左腿步伐与控制"
    },
    "右下肢": {
        "keypoints": [24, 26, 28],  # 右髋, 右膝, 右踝
        "joints": [("knee", 24, 26, 28), ("hip", 26, 28, 24)],
        "description": "右腿步伐与控制"
    },
    "躯干": {
        "keypoints": [11, 12, 23, 24],  # 左右肩, 左右髋
        "joints": [("trunk", 11, 12, 23, 24)],
        "description": "身体中轴稳定性"
    },
    "头颈": {
        "keypoints": [0, 7, 8, 11, 12],  # 鼻, 左右耳, 左右肩
        "joints": [("neck", 0, 11, 12)],
        "description": "头部朝向与颈部姿态"
    },
}

# 默认部位权重（可由教师动态调整）
DEFAULT_PART_WEIGHTS = {
    "左上肢": 0.15,
    "右上肢": 0.15,
    "左下肢": 0.20,
    "右下肢": 0.20,
    "躯干": 0.20,
    "头颈": 0.10,
}

# 关节角度定义： (名称, 顶点索引, 端点A索引, 端点B索引)
# 计算端点A-顶点-端点B的夹角
JOINT_ANGLE_DEFS = [
    # 上肢
    ("left_elbow", 13, 11, 15),     # 左肘: 左肩-左肘-左腕
    ("right_elbow", 14, 12, 16),    # 右肘: 右肩-右肘-右腕
    ("left_shoulder", 11, 13, 23),  # 左肩: 左肘-左肩-左髋
    ("right_shoulder", 12, 14, 24), # 右肩: 右肘-右肩-右髋
    # 下肢
    ("left_knee", 25, 23, 27),      # 左膝: 左髋-左膝-左踝
    ("right_knee", 26, 24, 28),     # 右膝: 右髋-右膝-右踝
    ("left_hip", 23, 25, 11),       # 左髋: 左膝-左髋-左肩
    ("right_hip", 24, 26, 12),      # 右髋: 右膝-右髋-右肩
    # 躯干
    ("trunk_inclination", 11, 23, 0),  # 躯干前倾角: 鼻-左肩-左髋
    # 头颈
    ("neck", 11, 0, 12),            # 颈部: 右肩-鼻-左肩
]

# ===== 可视化配置 =====
# MediaPipe Pose骨架连接关系（用于绘制骨架）
POSE_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 7),    # 左眼到左耳
    (0, 4), (4, 5), (5, 6), (6, 8),    # 右眼到右耳
    (9, 10),                            # 嘴
    (11, 12),                           # 左右肩
    (11, 13), (13, 15),                 # 左臂
    (12, 14), (14, 16),                 # 右臂
    (11, 23), (12, 24),                 # 躯干
    (23, 24),                           # 左右髋
    (23, 25), (25, 27), (27, 29), (27, 31), (29, 31),  # 左腿
    (24, 26), (26, 28), (28, 30), (28, 32), (30, 32),  # 右腿
]

# 偏差热力图颜色（绿-黄-红渐变）
SCORE_COLORS = {
    "excellent": (0, 255, 0),    # 绿色: 优秀
    "good": (255, 255, 0),       # 黄色: 良好
    "fair": (255, 165, 0),       # 橙色: 一般
    "poor": (255, 0, 0),         # 红色: 差
}

# 确保目录存在
for d in [MODELS_DIR, OUTPUT_DIR, UPLOADS_DIR]:
    os.makedirs(d, exist_ok=True)
