"""
舞蹈AI评分 FastAPI 服务
供艺术培训系统Java后端通过HTTP调用：
- POST /api/score            上传教师参考视频+学生练习视频，返回task_id
- GET  /api/result/{task_id} 轮询评分结果
- GET  /api/health           健康检查

启动: python api_server.py  (监听 127.0.0.1:8000)
"""
import os
import re
import sys
import json
import math
import time
import uuid
import threading
import traceback

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

# Windows控制台中文输出保护
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import FileResponse
import uvicorn

from config import OUTPUT_DIR, JOINT_ANGLE_DEFS
from process_video import DanceScoringPipeline

HOST = os.environ.get("DANCE_API_HOST", "127.0.0.1")  # 本机默认回环；服务器设 DANCE_API_HOST=0.0.0.0
PORT = int(os.environ.get("DANCE_API_PORT", "8000"))
TASKS_DIR = os.path.join(OUTPUT_DIR, "api_tasks")
VIDEO_EXTS = {".mp4", ".mov", ".webm", ".avi", ".mkv", ".wmv"}
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".webp"}
# 关节名 -> JOINT_ANGLE_DEFS 角度索引（与 dunhuang_reference.DUNHUANG_KEY_POSES 特征key对应）
ANGLE_INDEX = {name: i for i, (name, _v, _a, _b) in enumerate(JOINT_ANGLE_DEFS)}
# 允许通过 /api/files 下载的任务产物白名单（防路径穿越）
DOWNLOADABLE_FILES = {
    "comparison.mp4": "video/mp4",
    "teacher_overlay.mp4": "video/mp4",
    "student_overlay.mp4": "video/mp4",
    "score_chart.png": "image/png",
    "frames.json": "application/json",
    "dtw_visualization.png": "image/png",
    "joint_heatmap.png": "image/png",
}
# 动态产物文件名模式（偏差最大帧截图 worst_frame_N.jpg）
DOWNLOADABLE_PATTERNS = {
    re.compile(r"^worst_frame_\d+\.jpg$"): "image/jpeg",
}

app = FastAPI(title="舞蹈AI评分服务", version="1.0.0")

# 流水线单例锁：MediaPipe/YOLO实例非线程安全，任务串行执行
_pipeline = None
_pipeline_lock = threading.Lock()
TASKS = {}  # task_id -> {task_id, status, error, result, created_at}

# 敦煌舞参考系统单例（惰性加载，只读展馆数据）
_dunhuang_ref = None
_dunhuang_lock = threading.Lock()

# 关键姿态挑战用：静态图像MediaPipe Pose单例（static_image_mode，与视频追踪实例隔离）
_pose_static = None
_pose_static_lock = threading.Lock()


def _get_dunhuang():
    """惰性加载敦煌舞参考系统（pkl不存在时仍可用，类别状态标记为未导入）"""
    global _dunhuang_ref
    with _dunhuang_lock:
        if _dunhuang_ref is None:
            from modules.dunhuang_reference import DunhuangReferenceSystem
            _dunhuang_ref = DunhuangReferenceSystem()
        return _dunhuang_ref


def _get_pipeline():
    """惰性加载并复用流水线（避免每次任务重复加载模型）"""
    global _pipeline
    with _pipeline_lock:
        if _pipeline is None:
            _pipeline = DanceScoringPipeline()
        return _pipeline


def _task_file(task_id):
    return os.path.join(TASKS_DIR, f"{task_id}.json")


def _save_task(task_id):
    try:
        with open(_task_file(task_id), "w", encoding="utf-8") as f:
            json.dump(TASKS.get(task_id, {}), f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"[api_server] 任务结果持久化失败: {e}")


def _load_task(task_id):
    """从磁盘恢复任务（服务重启后仍可查询历史结果）"""
    path = _task_file(task_id)
    if not os.path.exists(path):
        return None
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None


def _run_task(task_id, teacher_path, student_path,
              generate_video, generate_chart):
    """后台评分线程：调用评分流水线，写回任务状态（含阶段进度）"""
    def _progress(pct, stage):
        TASKS[task_id]["progress"] = pct
        TASKS[task_id]["stage"] = stage
        _save_task(task_id)

    try:
        _progress(5, "任务已受理，模型加载中")
        pipeline = _get_pipeline()
        result = pipeline.process(
            teacher_path, student_path,
            output_dir=os.path.join(TASKS_DIR, task_id),
            generate_video=generate_video,
            generate_chart=generate_chart,
            progress_cb=_progress,
        )
        payload = {
            "total_score": round(float(result.get("total_score", 0)), 2),
            "part_scores": {
                part: {
                    "score": round(float(ps["score"]), 2),
                    "description": ps.get("description", ""),
                }
                for part, ps in result.get("part_scores", {}).items()
            },
            "dimension_scores": {
                dim: round(float(v), 2)
                for dim, v in result.get("dimension_scores", {}).items()
            },
            "feedback": result.get("feedback", []),
            "worst_frames": result.get("worst_frames", []),
            "analysis": result.get("analysis"),
            "rhythm": result.get("rhythm"),
            "dtw_distance": result.get("dtw_distance"),
            "rhythm_cost": result.get("rhythm_cost"),
            "processing_time": result.get("processing_time"),
        }
        TASKS[task_id]["result"] = payload
        TASKS[task_id]["session_dir"] = result.get("output_dir")
        TASKS[task_id]["status"] = "completed"
        TASKS[task_id]["progress"] = 100
        TASKS[task_id]["stage"] = "评分完成"
        print(f"[api_server] 任务完成: {task_id} 总分 {payload['total_score']}")
    except Exception as e:
        traceback.print_exc()
        TASKS[task_id]["status"] = "failed"
        TASKS[task_id]["error"] = str(e)
        print(f"[api_server] 任务失败: {task_id} - {e}")
    finally:
        _save_task(task_id)
        # 清理临时上传视频
        for p in (teacher_path, student_path):
            try:
                if os.path.exists(p):
                    os.remove(p)
            except Exception:
                pass


@app.get("/api/health")
def health():
    return {"status": "ok", "service": "dance-ai-score", "version": "1.0.0"}


@app.post("/api/score")
async def submit_score(
    teacher_video: UploadFile = File(..., description="教师参考视频"),
    student_video: UploadFile = File(..., description="学生练习视频"),
    generate_video: bool = Form(False, description="是否生成对比视频"),
    generate_chart: bool = Form(False, description="是否生成评分图表"),
):
    """提交双视频评分任务，立即返回task_id"""
    # 校验视频扩展名
    for f in (teacher_video, student_video):
        ext = os.path.splitext(f.filename or "")[1].lower()
        if ext not in VIDEO_EXTS:
            raise HTTPException(
                status_code=400,
                detail=f"不支持的视频格式: {f.filename}（支持: {', '.join(sorted(VIDEO_EXTS))}）",
            )

    task_id = uuid.uuid4().hex
    task_dir = os.path.join(TASKS_DIR, task_id)
    os.makedirs(task_dir, exist_ok=True)

    teacher_path = os.path.join(task_dir, "teacher" + os.path.splitext(teacher_video.filename)[1].lower())
    student_path = os.path.join(task_dir, "student" + os.path.splitext(student_video.filename)[1].lower())
    for f, path in ((teacher_video, teacher_path), (student_video, student_path)):
        with open(path, "wb") as out:
            out.write(await f.read())

    TASKS[task_id] = {
        "task_id": task_id,
        "status": "processing",
        "error": None,
        "result": None,
        "progress": 0,
        "stage": "排队中",
        "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
    }
    _save_task(task_id)

    threading.Thread(
        target=_run_task,
        args=(task_id, teacher_path, student_path, generate_video, generate_chart),
        daemon=True,
    ).start()
    print(f"[api_server] 新任务: {task_id}")
    return {"task_id": task_id, "status": "processing"}


def _sanitize_json(obj):
    """递归清洗 NaN/Inf，避免 JSON 序列化崩溃"""
    if isinstance(obj, float):
        return obj if math.isfinite(obj) else None
    if isinstance(obj, dict):
        return {k: _sanitize_json(v) for k, v in obj.items()}
    if isinstance(obj, (list, tuple)):
        return [_sanitize_json(v) for v in obj]
    return obj


@app.get("/api/result/{task_id}")
def get_result(task_id: str):
    """查询任务结果（前端轮询）"""
    task = TASKS.get(task_id)
    if task is None:
        task = _load_task(task_id)
        if task is None:
            raise HTTPException(status_code=404, detail=f"任务不存在: {task_id}")
        TASKS[task_id] = task
    return _sanitize_json({
        "task_id": task_id,
        "status": task.get("status"),
        "error": task.get("error"),
        "result": task.get("result"),
        "progress": task.get("progress", 0),
        "stage": task.get("stage"),
    })


@app.get("/api/files/{task_id}/{filename}")
def get_task_file(task_id: str, filename: str):
    """下载任务产物（对比视频/单方骨架叠加视频/评分图表/偏差截图），文件名白名单防路径穿越"""
    media_type = DOWNLOADABLE_FILES.get(filename)
    if media_type is None:
        for pattern, mt in DOWNLOADABLE_PATTERNS.items():
            if pattern.match(filename):
                media_type = mt
                break
    if media_type is None:
        raise HTTPException(status_code=404, detail=f"不支持的文件: {filename}")
    task = TASKS.get(task_id)
    if task is None:
        task = _load_task(task_id)
        if task is None:
            raise HTTPException(status_code=404, detail=f"任务不存在: {task_id}")
        TASKS[task_id] = task
    session_dir = task.get("session_dir")
    if not session_dir:
        raise HTTPException(status_code=404, detail="该任务无输出文件")
    path = os.path.join(session_dir, filename)
    if not os.path.exists(path):
        raise HTTPException(status_code=404, detail=f"文件不存在: {filename}")
    return FileResponse(path, media_type=media_type, filename=filename)


@app.get("/api/dunhuang/categories")
def dunhuang_categories():
    """敦煌舞数字展馆：8大动作类别元数据 + 标志性关键姿态特征"""
    try:
        from modules.dunhuang_reference import DUNHUANG_KEY_POSES
        ref = _get_dunhuang()
        return {"categories": ref.list_categories(), "key_poses": DUNHUANG_KEY_POSES}
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"敦煌参考数据加载失败: {e}")


@app.get("/api/dunhuang/reference/{category}")
def dunhuang_reference(category: str):
    """敦煌舞数字展馆：指定类别的参考骨架序列（MediaPipe 33关键点 × 帧，640×480像素坐标）"""
    try:
        import numpy as np
        ref = _get_dunhuang()
        data = ref.get_reference(category)
        if data is None:
            raise HTTPException(status_code=404, detail=f"类别不存在或未导入: {category}")
        kpts = np.asarray(data["keypoints"])   # (T, 33, 2)
        confs = data.get("confidences")
        return {
            "category": category,
            "info": data.get("category_info", {}),
            "metadata": data.get("metadata", {}),
            "num_frames": int(kpts.shape[0]),
            "keypoints": np.round(kpts, 2).tolist(),
            "confidences": np.round(np.asarray(confs), 3).tolist() if confs is not None else None,
        }
    except HTTPException:
        raise
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"敦煌参考数据读取失败: {e}")


def _get_static_pose():
    """惰性加载静态图像模式的MediaPipe Pose（模型复杂度1，避免联网下载heavy模型卡死）"""
    global _pose_static
    with _pose_static_lock:
        if _pose_static is None:
            import mediapipe as mp
            _pose_static = mp.solutions.pose.Pose(
                static_image_mode=True,
                model_complexity=1,
                enable_segmentation=False,
                min_detection_confidence=0.5,
            )
        return _pose_static


def _evaluate_key_poses(angles, angle_confs):
    """
    对一帧关节角度评估四大敦煌标志姿态的匹配度。
    返回 (detections[按分数降序], details{pose_id: 逐项特征结果})。
    数值特征按角度区间判定；布尔特征（如left_arm_horizontal）为定性描述，不参与计分。
    关键点不可见（置信度<0.3）的角度判为不通过，避免0度默认值虚高得分。
    """
    from modules.dunhuang_reference import DUNHUANG_KEY_POSES
    details = {}
    detections = []
    for pose_id, pose_info in DUNHUANG_KEY_POSES.items():
        feats = []
        passed = numeric = 0
        for joint, spec in pose_info["angle_features"].items():
            if isinstance(spec, bool):
                feats.append({"joint": joint, "qualitative": True,
                              "actual": None, "min": None, "max": None, "passed": None})
                continue
            idx = ANGLE_INDEX.get(joint)
            low, high = float(spec[0]), float(spec[1])
            actual = round(float(angles[idx]), 1) if idx is not None else None
            ok = (actual is not None and angle_confs[idx] >= 0.3
                  and low <= actual <= high)
            feats.append({"joint": joint, "qualitative": False,
                          "actual": actual, "min": low, "max": high,
                          "visible": bool(angle_confs[idx] >= 0.3) if idx is not None else False,
                          "passed": bool(ok)})
            numeric += 1
            passed += 1 if ok else 0
        score = round(100.0 * passed / numeric, 1) if numeric else 0.0
        details[pose_id] = {
            "name": pose_info["name"],
            "description": pose_info["description"],
            "score": score,
            "passed": passed,
            "total": numeric,
            "features": feats,
        }
        detections.append({"pose": pose_id, "name": pose_info["name"],
                           "description": pose_info["description"], "score": score})
    detections.sort(key=lambda d: d["score"], reverse=True)
    return detections, details


@app.post("/api/dunhuang/pose-challenge")
async def pose_challenge(file: UploadFile = File(..., description="学员挑战照片（摄像头抓拍或本地上传）")):
    """
    关键姿态挑战：上传一张全身照 → MediaPipe提取33关键点 →
    按四大敦煌标志姿态（反弹琵琶/飞天/胡旋/莲花步）的关节角度特征逐项评分。
    返回归一化关键点（前端在原图上叠加骨架）与每姿态逐项特征结果。
    """
    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in IMAGE_EXTS:
        raise HTTPException(status_code=400, detail=f"不支持的图片格式: {file.filename}")

    challenge_dir = os.path.join(TASKS_DIR, "challenge")
    os.makedirs(challenge_dir, exist_ok=True)
    img_path = os.path.join(challenge_dir, uuid.uuid4().hex + ext)
    try:
        with open(img_path, "wb") as out:
            out.write(await file.read())

        import cv2
        import numpy as np
        from utils import extract_joint_angles

        img = cv2.imdecode(np.fromfile(img_path, dtype=np.uint8), cv2.IMREAD_COLOR)
        if img is None:
            raise HTTPException(status_code=400, detail="图片解码失败，请重新拍摄或上传")
        h, w = img.shape[:2]
        rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

        pose = _get_static_pose()
        result = pose.process(rgb)
        if not result.pose_landmarks:
            return {"detected": False,
                    "message": "未检测到人体，请正对镜头、保证全身完整入镜后重试"}

        landmarks = result.pose_landmarks.landmark
        # 像素系关键点供角度计算；归一化坐标返回前端叠加
        kpts_px = np.array([[lm.x * w, lm.y * h] for lm in landmarks], dtype=np.float32)
        vis = np.array([float(lm.visibility) for lm in landmarks], dtype=np.float32)
        angles, angle_confs = extract_joint_angles(kpts_px, vis)
        detections, details = _evaluate_key_poses(angles, angle_confs)

        return {
            "detected": True,
            "width": int(w),
            "height": int(h),
            "keypoints": [[round(float(lm.x), 4), round(float(lm.y), 4),
                           round(float(lm.visibility), 3)] for lm in landmarks],
            "angles": {name: round(float(angles[i]), 1)
                       for i, (name, _v, _a, _b) in enumerate(JOINT_ANGLE_DEFS)},
            "detections": detections,
            "best": detections[0] if detections else None,
            "details": details,
        }
    except HTTPException:
        raise
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"姿态识别失败: {e}")
    finally:
        # 挑战照片即时处理即时删除，不留存用户影像
        try:
            if os.path.exists(img_path):
                os.remove(img_path)
        except Exception:
            pass


if __name__ == "__main__":
    os.makedirs(TASKS_DIR, exist_ok=True)
    print(f"舞蹈AI评分服务启动: http://{HOST}:{PORT}")
    print(f"任务结果目录: {TASKS_DIR}")
    uvicorn.run(app, host=HOST, port=PORT, log_level="info")
