"""
舞蹈AI评分系统 - 模块包
"""
from .video_preprocess import VideoPreprocessor
from .pose_extraction import PoseExtractor
from .dtw_alignment import DTWAligner
from .scoring import DanceScorer
from .visualization import VideoVisualizer
from .calibration import ScoreCalibrator
from .dunhuang_reference import DunhuangReferenceSystem
