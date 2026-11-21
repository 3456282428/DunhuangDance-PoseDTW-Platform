"""
模块二：姿态提取
功能：
1. 使用MediaPipe Pose从每帧提取33个人体关键点（66维向量）
2. CPU单帧处理50ms以内，仅需普通RGB摄像头
3. 对低置信度关键点标记，后续评分时降权处理
"""
import cv2
import numpy as np
from config import (MEDIAPIPE_MODEL_COMPLEXITY,
                    MEDIAPIPE_MIN_DETECTION_CONFIDENCE,
                    MEDIAPIPE_MIN_TRACKING_CONFIDENCE,
                    MIN_VISIBLE_CONFIDENCE)


class PoseExtractor:
    """MediaPipe Pose 姿态提取器"""

    def __init__(self):
        self.pose = None
        self._init_mediapipe()

    def _init_mediapipe(self):
        """初始化MediaPipe Pose"""
        try:
            import mediapipe as mp
            self.mp_pose = mp.solutions.pose
            self.mp_drawing = mp.solutions.drawing_utils
            self.pose = self.mp_pose.Pose(
                static_image_mode=False,
                model_complexity=MEDIAPIPE_MODEL_COMPLEXITY,
                smooth_landmarks=True,
                enable_segmentation=False,
                min_detection_confidence=MEDIAPIPE_MIN_DETECTION_CONFIDENCE,
                min_tracking_confidence=MEDIAPIPE_MIN_TRACKING_CONFIDENCE,
            )
            print("[PoseExtractor] MediaPipe Pose初始化成功")
        except ImportError:
            print("[PoseExtractor] 警告: MediaPipe未安装，请运行 pip install mediapipe")
            self.pose = None
        except Exception as e:
            print(f"[PoseExtractor] MediaPipe初始化失败: {e}")
            self.pose = None

    def reset(self):
        """重置MediaPipe追踪状态，确保每个视频从干净状态开始。

        static_image_mode=False 时MediaPipe会复用前一帧的ROI预测，
        若同一个Pose对象连续处理教师和学生视频，教师视频的追踪状态
        会延续到学生视频，导致相同视频产生不同姿态估计。
        """
        if self.pose is not None:
            try:
                self.pose.close()
            except Exception:
                pass
        self._init_mediapipe()

    def extract_from_video(self, video_path, progress_cb=None):
        """
        从视频中提取姿态关键点序列
        video_path: 视频文件路径
        progress_cb: 可选回调 fn(frac:float)，frac为0-1的处理进度
        返回: keypoints_sequence (T, 33, 2), confidences (T, 33)
        """
        if self.pose is None:
            print("[PoseExtractor] MediaPipe未初始化")
            return None, None

        # 重置追踪状态，避免上一个视频的ROI预测影响本视频
        self.reset()

        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            print(f"[PoseExtractor] 无法打开视频: {video_path}")
            return None, None

        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        print(f"[PoseExtractor] 视频信息: {total_frames}帧, {fps:.1f}fps")

        keypoints_sequence = []
        confidences = []
        frame_count = 0

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            # 转换BGR -> RGB
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = self.pose.process(rgb_frame)

            if results.pose_landmarks is not None:
                # 提取33个关键点的 (x, y) 坐标
                # MediaPipe坐标已归一化到0-1，转换为像素坐标
                h, w = frame.shape[:2]
                kpts = np.zeros((33, 2), dtype=np.float32)
                confs = np.zeros(33, dtype=np.float32)

                for i, lm in enumerate(results.pose_landmarks.landmark):
                    kpts[i, 0] = lm.x * w
                    kpts[i, 1] = lm.y * h
                    confs[i] = lm.visibility

                keypoints_sequence.append(kpts)
                confidences.append(confs)
            else:
                # 未检测到姿态，使用上一帧或零填充
                if len(keypoints_sequence) > 0:
                    keypoints_sequence.append(keypoints_sequence[-1].copy())
                    confidences.append(np.zeros(33, dtype=np.float32))
                else:
                    keypoints_sequence.append(np.zeros((33, 2), dtype=np.float32))
                    confidences.append(np.zeros(33, dtype=np.float32))

            frame_count += 1
            if frame_count % 100 == 0:
                print(f"[PoseExtractor] 已处理 {frame_count}/{total_frames} 帧")
                if progress_cb and total_frames > 0:
                    try:
                        progress_cb(frame_count / total_frames)
                    except Exception:
                        pass

        cap.release()

        keypoints_sequence = np.array(keypoints_sequence)
        confidences = np.array(confidences)

        print(f"[PoseExtractor] 姿态提取完成: {len(keypoints_sequence)}帧, "
              f"平均置信度: {np.mean(confidences):.3f}")

        # 统计低置信度关键点
        low_conf_ratio = np.mean(confidences < MIN_VISIBLE_CONFIDENCE)
        print(f"[PoseExtractor] 低置信度关键点占比: {low_conf_ratio:.1%}")

        return keypoints_sequence, confidences

    def extract_angles_from_sequence(self, keypoints_sequence, confidences=None):
        """
        从关键点序列提取关节角度序列
        keypoints_sequence: (T, 33, 2)
        confidences: (T, 33) 可选
        返回: angles (T, N), angle_confidences (T, N)
        """
        from utils import extract_joint_angles

        T = len(keypoints_sequence)
        angle_list = []
        conf_list = []

        for t in range(T):
            kpts = keypoints_sequence[t]
            confs = confidences[t] if confidences is not None else None
            angles, ac = extract_joint_angles(kpts, confs)
            angle_list.append(angles)
            conf_list.append(ac)

        return np.array(angle_list), np.array(conf_list)

    def close(self):
        """释放MediaPipe资源"""
        if self.pose is not None:
            self.pose.close()
            self.pose = None

    def __del__(self):
        self.close()
