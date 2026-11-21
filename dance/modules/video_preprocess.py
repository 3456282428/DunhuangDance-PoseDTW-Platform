"""
模块一：视频预处理
功能：
1. 使用ffmpeg统一帧率至30fps
2. 使用YOLO单人检测裁剪人物区域
3. 对关键点序列做平滑处理
"""
import os
import subprocess
import cv2
import numpy as np
from config import TARGET_FPS, YOLO_CONF_THRESHOLD, CROP_PADDING


class VideoPreprocessor:
    """视频预处理：帧率统一 + 人物裁剪"""

    def __init__(self, yolo_model_path=None):
        """
        初始化YOLO检测器
        yolo_model_path: YOLO模型路径，优先使用服务器已有模型
        """
        self.yolo_model = None
        self._init_yolo(yolo_model_path)

    def _init_yolo(self, model_path):
        """初始化YOLO模型，优先使用项目目录/服务器已有的模型"""
        try:
            from ultralytics import YOLO
        except ImportError:
            print("[VideoPreprocessor] ultralytics未安装，跳过YOLO人物裁剪")
            self.yolo_model = None
            return

        # 优先使用指定路径，然后项目models目录，然后服务器已有模型
        candidates = []
        if model_path:
            candidates.append(model_path)
        # 项目models目录下的YOLO模型（优先用轻量的nano模型）
        from config import MODELS_DIR
        candidates.extend([
            os.path.join(MODELS_DIR, "yolov8n.pt"),
            os.path.join(MODELS_DIR, "yolo11n.pt"),
            os.path.join(MODELS_DIR, "yolo11x.pt"),
        ])
        # 服务器已有的YOLO模型
        candidates.extend([
            os.path.expanduser("~/yolov8n.pt"),
            os.path.expanduser("~/yolo11n.pt"),
            os.path.expanduser("~/yolo11x.pt"),
            os.path.expanduser("~/yolo26n.pt"),
            "yolov8n.pt",
            "yolo11n.pt",
        ])

        for path in candidates:
            try:
                self.yolo_model = YOLO(path)
                print(f"[VideoPreprocessor] YOLO模型加载成功: {path}")
                return
            except Exception:
                continue

        # 如果都失败，使用ultralytics默认模型（自动从GitHub下载）
        try:
            self.yolo_model = YOLO("yolo11n.pt")
            print("[VideoPreprocessor] 使用默认YOLO模型(yolo11n.pt)")
        except Exception as e:
            print(f"[VideoPreprocessor] YOLO模型加载失败: {e}")
            self.yolo_model = None

    def normalize_fps(self, input_path, output_path=None, target_fps=TARGET_FPS):
        """
        使用ffmpeg统一帧率至target_fps
        使用minterpolate滤镜进行帧插值
        """
        if output_path is None:
            base, ext = os.path.splitext(input_path)
            output_path = f"{base}_normalized{ext}"

        # 检查ffmpeg是否可用
        ffmpeg_cmd = self._get_ffmpeg_cmd()
        if ffmpeg_cmd is None:
            print("[VideoPreprocessor] ffmpeg不可用，使用OpenCV重采样")
            return self._normalize_fps_cv(input_path, output_path, target_fps)

        # 先检测源帧率，已接近目标帧率则直接复制（避免无意义重编码）
        try:
            probe = subprocess.run(
                [ffmpeg_cmd.replace("ffmpeg", "ffprobe"), "-v", "error",
                 "-select_streams", "v:0", "-show_entries", "stream=r_frame_rate",
                 "-of", "default=noprint_wrappers=1:nokey=1", input_path],
                capture_output=True, timeout=10, text=True
            )
            if probe.returncode == 0 and probe.stdout.strip():
                num, den = probe.stdout.strip().split("/")
                src_fps = float(num) / float(den) if float(den) != 0 else 0
                if abs(src_fps - target_fps) < 1.0:
                    # 帧率已达标，直接复制流（秒级完成）
                    cmd = [ffmpeg_cmd, "-y", "-i", input_path,
                           "-c", "copy", output_path]
                    subprocess.run(cmd, capture_output=True, timeout=120, check=False)
                    if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
                        print(f"[VideoPreprocessor] 帧率已达标({src_fps:.1f}fps)，直接复制")
                        return output_path
        except Exception:
            pass

        # 帧率不达标时用快速帧丢弃/复制统一帧率（不用minterpolate运动补偿，那个极慢）
        cmd = [
            ffmpeg_cmd, "-y", "-i", input_path,
            "-r", str(target_fps),
            "-vsync", "cfr",
            "-c:a", "copy",
            output_path
        ]

        try:
            subprocess.run(cmd, capture_output=True, timeout=120,
                          check=False)
            if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
                print(f"[VideoPreprocessor] 帧率统一完成: {target_fps}fps")
                return output_path
        except Exception as e:
            print(f"[VideoPreprocessor] ffmpeg处理失败: {e}")

        # 回退到OpenCV
        return self._normalize_fps_cv(input_path, output_path, target_fps)

    def _get_ffmpeg_cmd(self):
        """获取ffmpeg可执行文件路径"""
        for cmd in ["ffmpeg", "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg"]:
            try:
                result = subprocess.run([cmd, "-version"],
                                      capture_output=True, timeout=5)
                if result.returncode == 0:
                    return cmd
            except Exception:
                continue
        return None

    def _normalize_fps_cv(self, input_path, output_path, target_fps):
        """使用OpenCV进行帧率重采样（ffmpeg不可用时的回退方案）"""
        cap = cv2.VideoCapture(input_path)
        original_fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        writer = cv2.VideoWriter(output_path, fourcc, target_fps, (w, h))

        # 计算重采样比例
        ratio = target_fps / max(original_fps, 1)
        frame_idx = 0
        next_sample = 0.0

        while True:
            ret, frame = cap.read()
            if not ret:
                break
            current_pos = frame_idx / max(original_fps, 1)
            while next_sample <= current_pos + 1e-6:
                writer.write(frame)
                next_sample += 1.0 / target_fps
            frame_idx += 1

        cap.release()
        writer.release()
        print(f"[VideoPreprocessor] OpenCV帧率重采样完成: {target_fps}fps")
        return output_path

    def detect_and_crop_person(self, input_path, output_path=None):
        """
        使用YOLO检测人物区域并裁剪
        减少背景干扰
        """
        if output_path is None:
            base, ext = os.path.splitext(input_path)
            output_path = f"{base}_cropped{ext}"

        if self.yolo_model is None:
            print("[VideoPreprocessor] YOLO未加载，跳过裁剪")
            return input_path

        cap = cv2.VideoCapture(input_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        # 在前10帧中检测最大人物区域，作为固定裁剪框
        sample_frames = min(10, total_frames)
        best_box = None
        best_area = 0

        for i in range(sample_frames):
            ret, frame = cap.read()
            if not ret:
                break
            results = self.yolo_model(frame, verbose=False)
            for r in results:
                boxes = r.boxes
                for box in boxes:
                    if box.conf[0] >= YOLO_CONF_THRESHOLD and int(box.cls[0]) == 0:
                        # person class = 0
                        x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                        area = (x2 - x1) * (y2 - y1)
                        if area > best_area:
                            best_area = area
                            best_box = (int(x1), int(y1), int(x2), int(y2))

        cap.release()

        if best_box is None:
            print("[VideoPreprocessor] 未检测到人物，跳过裁剪")
            return input_path

        # 扩展裁剪框，加入边距
        x1, y1, x2, y2 = best_box
        x1 = max(0, x1 - CROP_PADDING)
        y1 = max(0, y1 - CROP_PADDING)
        x2 = min(w, x2 + CROP_PADDING)
        y2 = min(h, y2 + CROP_PADDING)

        crop_w = x2 - x1
        crop_h = y2 - y1

        # 裁剪视频
        cap = cv2.VideoCapture(input_path)
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        writer = cv2.VideoWriter(output_path, fourcc, fps, (crop_w, crop_h))

        while True:
            ret, frame = cap.read()
            if not ret:
                break
            cropped = frame[y1:y2, x1:x2]
            writer.write(cropped)

        cap.release()
        writer.release()
        print(f"[VideoPreprocessor] 人物裁剪完成: 区域({x1},{y1})-({x2},{y2})")
        return output_path

    def preprocess(self, video_path, output_dir=None):
        """
        完整预处理流水线
        1. 帧率统一至30fps
        （人物裁剪已按需求移除，直接使用原画面）
        返回: 预处理后的视频路径
        """
        if output_dir is None:
            output_dir = os.path.dirname(video_path)
        os.makedirs(output_dir, exist_ok=True)

        basename = os.path.splitext(os.path.basename(video_path))[0]

        # 帧率统一
        fps_normalized = os.path.join(output_dir, f"{basename}_preprocessed.mp4")
        print(f"[VideoPreprocessor] 帧率统一 -> {fps_normalized}")
        fps_normalized = self.normalize_fps(video_path, fps_normalized)

        print(f"[VideoPreprocessor] 预处理完成: {fps_normalized}")
        return fps_normalized
