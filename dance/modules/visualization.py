"""
可视化模块
功能：
1. 生成对比视频：老师视频和学生视频并排展示
2. 在视频上叠加姿态骨架
3. 实时显示评分
4. 标注偏差最大的关节部位（热力图效果）
"""
import os
import json
import cv2
import numpy as np
from config import POSE_CONNECTIONS, BODY_PARTS, JOINT_ANGLE_DEFS
from utils import get_score_color, get_score_label, get_part_keypoints


class VideoVisualizer:
    """视频可视化器"""

    # 关键点到身体部位的映射（用于热力图着色）
    KEYPOINT_PART_MAP = {}
    for part, info in BODY_PARTS.items():
        for kp_idx in info["keypoints"]:
            KEYPOINT_PART_MAP[kp_idx] = part

    # 关节角度索引到身体部位的映射（与DanceScorer一致，用于逐帧偏差定位）
    JOINT_PART_MAP = {}
    for _idx, (_name, _v, _a, _b) in enumerate(JOINT_ANGLE_DEFS):
        if "left_elbow" in _name or "left_shoulder" in _name:
            JOINT_PART_MAP[_idx] = "左上肢"
        elif "right_elbow" in _name or "right_shoulder" in _name:
            JOINT_PART_MAP[_idx] = "右上肢"
        elif "left_knee" in _name or "left_hip" in _name:
            JOINT_PART_MAP[_idx] = "左下肢"
        elif "right_knee" in _name or "right_hip" in _name:
            JOINT_PART_MAP[_idx] = "右下肢"
        elif "trunk" in _name:
            JOINT_PART_MAP[_idx] = "躯干"
        elif "neck" in _name:
            JOINT_PART_MAP[_idx] = "头颈"

    # 骨架连接的颜色（按身体部位）
    PART_COLORS = {
        "左上肢": (255, 100, 100),    # 浅红
        "右上肢": (100, 100, 255),    # 浅蓝
        "左下肢": (100, 255, 100),    # 浅绿
        "右下肢": (255, 255, 100),    # 浅黄
        "躯干": (255, 100, 255),      # 紫
        "头颈": (100, 255, 255),      # 青
    }

    def __init__(self):
        self.font = cv2.FONT_HERSHEY_SIMPLEX

    def draw_skeleton(self, frame, keypoints, confidences=None,
                      part_scores=None, min_conf=0.3):
        """
        在帧上绘制骨架
        keypoints: (33, 2) 像素坐标
        confidences: (33,) 置信度
        part_scores: dict 部位->得分，用于热力图着色
        """
        h, w = frame.shape[:2]
        overlay = frame.copy()

        # 绘制骨架连接线
        for (a, b) in POSE_CONNECTIONS:
            if a >= len(keypoints) or b >= len(keypoints):
                continue
            pt_a = keypoints[a]
            pt_b = keypoints[b]

            # 置信度检查
            conf_a = confidences[a] if confidences is not None else 1.0
            conf_b = confidences[b] if confidences is not None else 1.0

            if conf_a < min_conf or conf_b < min_conf:
                continue

            # 确定颜色
            part_a = self.KEYPOINT_PART_MAP.get(a, "躯干")
            part_b = self.KEYPOINT_PART_MAP.get(b, "躯干")

            if part_scores and part_a in part_scores:
                score = part_scores[part_a]["score"] if isinstance(part_scores.get(part_a), dict) else part_scores.get(part_a, 100)
                color = get_score_color(score)
            else:
                color = self.PART_COLORS.get(part_a, (200, 200, 200))

            cv2.line(overlay, tuple(pt_a.astype(int)), tuple(pt_b.astype(int)),
                     color, 3, cv2.LINE_AA)

        # 绘制关键点
        for i, pt in enumerate(keypoints):
            conf = confidences[i] if confidences is not None else 1.0
            if conf < min_conf:
                continue

            part = self.KEYPOINT_PART_MAP.get(i, "躯干")
            if part_scores and part in part_scores:
                score = part_scores[part]["score"] if isinstance(part_scores.get(part), dict) else part_scores.get(part, 100)
                color = get_score_color(score)
            else:
                color = self.PART_COLORS.get(part, (200, 200, 200))

            cv2.circle(overlay, tuple(pt.astype(int)), 5, color, -1, cv2.LINE_AA)

        # 混合
        result = cv2.addWeighted(overlay, 0.7, frame, 0.3, 0)
        return result

    def draw_score_panel(self, frame, part_scores, total_score, dimension_scores=None):
        """
        在帧上绘制评分面板
        """
        h, w = frame.shape[:2]
        panel_w = 320
        panel_h = min(h, 60 + len(part_scores) * 35 + 80)

        # 半透明面板
        overlay = frame.copy()
        cv2.rectangle(overlay, (w - panel_w - 10, 10),
                       (w - 10, panel_h), (20, 20, 20), -1)
        frame = cv2.addWeighted(overlay, 0.7, frame, 0.3, 0)

        y = 30
        # 总分
        color = get_score_color(total_score)
        label = get_score_label(total_score)
        cv2.putText(frame, f"Total: {total_score:.1f} ({label})",
                    (w - panel_w, y), self.font, 0.6, color, 2)
        y += 30

        # 分隔线
        cv2.line(frame, (w - panel_w, y), (w - 20, y), (100, 100, 100), 1)
        y += 25

        # 各部位得分
        for part_name, ps in part_scores.items():
            score = ps["score"] if isinstance(ps, dict) else ps
            color = get_score_color(score)
            # 部位名（截断）
            display_name = part_name[:8]
            cv2.putText(frame, f"{display_name}: {score:.1f}",
                        (w - panel_w, y), self.font, 0.45, color, 1)

            # 得分条
            bar_x = w - panel_w
            bar_y = y + 5
            bar_w = int((panel_w - 20) * score / 100)
            cv2.rectangle(frame, (bar_x, bar_y), (bar_x + bar_w, bar_y + 4),
                         color, -1)
            y += 35

        # 维度得分
        if dimension_scores:
            y += 5
            cv2.line(frame, (w - panel_w, y), (w - 20, y), (100, 100, 100), 1)
            y += 25
            for dim_name, dim_score in dimension_scores.items():
                color = get_score_color(dim_score)
                cv2.putText(frame, f"{dim_name}: {dim_score:.1f}",
                            (w - panel_w, y), self.font, 0.4, color, 1)
                y += 25

        return frame

    def create_comparison_video(self, teacher_video, student_video,
                                teacher_kpts, student_kpts,
                                teacher_confs, student_confs,
                                alignment_path, scores,
                                output_path, fps=30,
                                teacher_overlay_path=None,
                                student_overlay_path=None):
        """
        创建对比视频
        左侧: 教师视频+骨架
        右侧: 学生视频+骨架（对齐到教师帧）
        显示实时评分
        同时可分别输出教师/学生单方骨架叠加视频（预处理视频）
        """
        cap_t = cv2.VideoCapture(teacher_video)
        cap_s = cv2.VideoCapture(student_video)

        fps = cap_t.get(cv2.CAP_PROP_FPS) or fps
        w_t = int(cap_t.get(cv2.CAP_PROP_FRAME_WIDTH))
        h_t = int(cap_t.get(cv2.CAP_PROP_FRAME_HEIGHT))
        w_s = int(cap_s.get(cv2.CAP_PROP_FRAME_WIDTH))
        h_s = int(cap_s.get(cv2.CAP_PROP_FRAME_HEIGHT))

        # 统一高度（H.264要求偶数尺寸，奇数会导致编码器静默失败）
        target_h = max(h_t, h_s, 360)
        target_h = min(target_h, 720)
        target_h -= target_h % 2
        scale_t = target_h / h_t
        scale_s = target_h / h_s
        target_w_t = int(w_t * scale_t) // 2 * 2
        target_w_s = int(w_s * scale_s) // 2 * 2
        total_w = target_w_t + target_w_s + 10

        # mp4v编码写入（各环境OpenCV均可写），生成后统一转码H.264保证浏览器可播放
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        writer = cv2.VideoWriter(output_path, fourcc, fps,
                                 (total_w, target_h))

        # 单方骨架叠加视频（预处理视频）；isOpened校验失败则置None跳过
        writer_t = cv2.VideoWriter(teacher_overlay_path, fourcc, fps,
                                   (target_w_t, target_h)) \
            if teacher_overlay_path else None
        if writer_t is not None and not writer_t.isOpened():
            print(f"[VideoVisualizer] 教师overlay写入器打开失败，跳过: {teacher_overlay_path}")
            writer_t = None
        writer_s = cv2.VideoWriter(student_overlay_path, fourcc, fps,
                                   (target_w_s, target_h)) \
            if student_overlay_path else None
        if writer_s is not None and not writer_s.isOpened():
            print(f"[VideoVisualizer] 学生overlay写入器打开失败，跳过: {student_overlay_path}")
            writer_s = None

        part_scores = scores.get("part_scores", {})
        dimension_scores = scores.get("dimension_scores", {})
        total_score = scores.get("total_score", 0)

        # 学生预处理视频按学生原生时间轴渲染（帧k配骨架k），保证与同列原视频严格同相；
        # DTW对齐映射仅用于对比视频，避免"原视频↔预处理视频"出现相位差
        if writer_s is not None:
            cap_s_native = cv2.VideoCapture(student_video)
            k = 0
            while True:
                ret_k, frame_k = cap_s_native.read()
                if not ret_k:
                    break
                frame_k = cv2.resize(frame_k, (target_w_s, target_h))
                if k < len(student_kpts):
                    frame_k = self.draw_skeleton(
                        frame_k,
                        student_kpts[k] * np.array([scale_s, scale_s]),
                        student_confs[k],
                        part_scores
                    )
                cv2.putText(frame_k, "Student", (10, 25),
                            self.font, 0.7, (0, 255, 255), 2)
                writer_s.write(frame_k)
                k += 1
            cap_s_native.release()
            print(f"[VideoVisualizer] 学生overlay原生帧数: {k}")

        # 构建对齐路径映射：教师帧 -> 学生帧
        aligned_student_idx = {}
        for i, j in alignment_path:
            if i not in aligned_student_idx:
                aligned_student_idx[i] = j

        teacher_frame_idx = 0
        student_frames_cache = {}

        while True:
            ret_t, frame_t = cap_t.read()
            if not ret_t:
                break

            # 获取对应的学生帧
            s_idx = aligned_student_idx.get(teacher_frame_idx, None)
            if s_idx is not None:
                # 读取到对应的学生帧（带缓存避免反复seek）
                if s_idx not in student_frames_cache:
                    cap_s.set(cv2.CAP_PROP_POS_FRAMES, s_idx)
                    ret_s, frame_s = cap_s.read()
                    if ret_s:
                        student_frames_cache[s_idx] = frame_s
                    else:
                        # 回退到上一帧
                        if student_frames_cache:
                            s_idx = max(student_frames_cache.keys())
                            frame_s = student_frames_cache[s_idx]
                        else:
                            frame_s = np.zeros((h_s, w_s, 3), dtype=np.uint8)
                frame_s = student_frames_cache.get(
                    s_idx, np.zeros((h_s, w_s, 3), dtype=np.uint8)
                )
            else:
                frame_s = np.zeros((h_s, w_s, 3), dtype=np.uint8)

            # 缩放
            frame_t_resized = cv2.resize(frame_t, (target_w_t, target_h))
            frame_s_resized = cv2.resize(frame_s, (target_w_s, target_h))

            # 获取当前帧的关键点
            if teacher_frame_idx < len(teacher_kpts):
                t_kpts = teacher_kpts[teacher_frame_idx]
                t_conf = teacher_confs[teacher_frame_idx]
                # 缩放关键点坐标
                t_kpts_scaled = t_kpts * scale_t

                # 计算当前帧的实时部位得分
                frame_part_scores = {}
                for part_name, ps in part_scores.items():
                    frame_part_scores[part_name] = ps

                frame_t_resized = self.draw_skeleton(
                    frame_t_resized, t_kpts_scaled, t_conf,
                    frame_part_scores
                )

            if s_idx is not None and s_idx < len(student_kpts):
                s_kpts = student_kpts[s_idx]
                s_conf = student_confs[s_idx]
                s_kpts_scaled = s_kpts * np.array([scale_s, scale_s])

                frame_s_resized = self.draw_skeleton(
                    frame_s_resized, s_kpts_scaled, s_conf,
                    part_scores
                )

            # 添加标签
            cv2.putText(frame_t_resized, "Teacher", (10, 25),
                       self.font, 0.7, (0, 255, 0), 2)
            cv2.putText(frame_s_resized, "Student", (10, 25),
                       self.font, 0.7, (0, 255, 255), 2)

            # 先输出单方骨架叠加视频（不含评分面板，保持画面干净）
            # 教师overlay在主循环按教师原生时间轴写入；学生overlay已按学生原生时间轴独立写入
            if writer_t is not None:
                writer_t.write(frame_t_resized)

            # 绘制评分面板
            frame_s_resized = self.draw_score_panel(
                frame_s_resized, part_scores, total_score,
                dimension_scores
            )

            # 拼接
            gap = np.full((target_h, 10, 3), 50, dtype=np.uint8)
            combined = np.hstack([frame_t_resized, gap, frame_s_resized])

            writer.write(combined)
            teacher_frame_idx += 1

        cap_t.release()
        cap_s.release()
        writer.release()
        if writer_t is not None:
            writer_t.release()
            print(f"[VideoVisualizer] 教师预处理视频生成完成: {teacher_overlay_path}")
        if writer_s is not None:
            writer_s.release()
            print(f"[VideoVisualizer] 学生预处理视频生成完成: {student_overlay_path}")

        print(f"[VideoVisualizer] 对比视频生成完成: {output_path}")

        # 统一转码H.264（mp4v浏览器无法解码；conda环境OpenCV 5.x的avc1走硬件编码器不可用）
        for p in (output_path, teacher_overlay_path, student_overlay_path):
            self._transcode_to_h264(p)

        return output_path

    @staticmethod
    def _transcode_to_h264(path):
        """用ffmpeg把视频转码为H.264(yuv420p, faststart)；失败保留原文件"""
        if not path:
            return
        import subprocess
        ffmpeg = None
        for cmd in ["ffmpeg",
                    os.path.expanduser("~/miniconda3/envs/dance/bin/ffmpeg"),
                    "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg"]:
            try:
                r = subprocess.run([cmd, "-version"], capture_output=True, timeout=5)
                if r.returncode == 0:
                    ffmpeg = cmd
                    break
            except Exception:
                continue
        if ffmpeg is None:
            print(f"[VideoVisualizer] ffmpeg不可用，跳过转码: {path}")
            return
        tmp = path + ".h264.mp4"
        try:
            subprocess.run([ffmpeg, "-y", "-i", path,
                            "-c:v", "libx264", "-pix_fmt", "yuv420p",
                            "-preset", "veryfast", "-crf", "23",
                            "-movflags", "+faststart", tmp],
                           capture_output=True, timeout=600, check=False)
            if os.path.exists(tmp) and os.path.getsize(tmp) > 0:
                os.replace(tmp, path)
                print(f"[VideoVisualizer] H.264转码完成: {path}")
            else:
                print(f"[VideoVisualizer] 转码输出为空，保留原文件: {path}")
        except Exception as e:
            print(f"[VideoVisualizer] 转码失败: {e}")
        finally:
            if os.path.exists(tmp):
                try:
                    os.remove(tmp)
                except Exception:
                    pass

    def create_worst_frame_snapshots(self, teacher_video, student_video,
                                     teacher_kpts, student_kpts,
                                     teacher_confs, student_confs,
                                     alignment_path,
                                     teacher_angles, student_angles,
                                     teacher_ac, student_ac,
                                     output_dir, top_n=3):
        """
        生成动作偏差最大的对比截图（师生并排+骨架叠加，按帧局部偏差着色）
        沿DTW对齐路径逐帧计算置信度加权关节角度差异（与评分同一度量），
        取偏差最大的top_n个瞬间（间隔至少1秒避免内容雷同）。
        返回: [{"rank","filename","teacher_time","student_time","worst_part","deviation"}]
        """
        # 逐帧偏差：加权平均角度差(0-1) + 偏差最大部位
        entries = []
        for i, j in alignment_path:
            if i >= len(teacher_angles) or j >= len(student_angles):
                continue
            t_a, s_a = teacher_angles[i], student_angles[j]
            t_c, s_c = teacher_ac[i], student_ac[j]
            part_dev_sum, part_w_sum = {}, {}
            total_dev, total_w = 0.0, 0.0
            for idx in range(len(JOINT_ANGLE_DEFS)):
                part = self.JOINT_PART_MAP.get(idx)
                if part is None:
                    continue
                diff = abs(float(t_a[idx]) - float(s_a[idx])) / 180.0
                conf = max(min(float(t_c[idx]), float(s_c[idx])), 0.01)
                part_dev_sum[part] = part_dev_sum.get(part, 0.0) + diff * conf
                part_w_sum[part] = part_w_sum.get(part, 0.0) + conf
                total_dev += diff * conf
                total_w += conf
            if total_w <= 0:
                continue
            part_devs = {p: part_dev_sum[p] / part_w_sum[p]
                         for p in part_dev_sum if part_w_sum[p] > 0}
            worst_part = max(part_devs, key=part_devs.get) if part_devs else None
            entries.append((total_dev / total_w, i, j, worst_part, part_devs))

        if not entries:
            print("[VideoVisualizer] 无有效对齐帧，跳过偏差截图")
            return []

        # 偏差降序选取，相邻选取间隔至少1秒
        entries.sort(key=lambda e: e[0], reverse=True)
        cap_t = cv2.VideoCapture(teacher_video)
        cap_s = cv2.VideoCapture(student_video)
        fps = cap_t.get(cv2.CAP_PROP_FPS) or 30
        min_gap = max(int(fps), 1)
        selected = []
        for e in entries:
            if all(abs(e[1] - s[1]) >= min_gap for s in selected):
                selected.append(e)
                if len(selected) >= top_n:
                    break

        h_s, w_s = int(cap_s.get(cv2.CAP_PROP_FRAME_HEIGHT)), \
            int(cap_s.get(cv2.CAP_PROP_FRAME_WIDTH))
        results = []
        for rank, (dev, i, j, worst_part, part_devs) in enumerate(selected, 1):
            cap_t.set(cv2.CAP_PROP_POS_FRAMES, i)
            ret_t, frame_t = cap_t.read()
            cap_s.set(cv2.CAP_PROP_POS_FRAMES, j)
            ret_s, frame_s = cap_s.read()
            if not ret_t or not ret_s:
                continue

            h_t, w_t = frame_t.shape[:2]
            target_h = min(max(h_t, h_s, 360), 720)
            target_h -= target_h % 2
            scale_t = target_h / h_t
            scale_s = target_h / h_s
            target_w_t = int(w_t * scale_t) // 2 * 2
            target_w_s = int(w_s * scale_s) // 2 * 2

            # 帧局部部位得分（偏差越大越红）
            frame_part_scores = {
                p: {"score": max(0.0, 100.0 * (1.0 - d))}
                for p, d in part_devs.items()
            }

            frame_t = cv2.resize(frame_t, (target_w_t, target_h))
            frame_s = cv2.resize(frame_s, (target_w_s, target_h))
            if i < len(teacher_kpts):
                frame_t = self.draw_skeleton(
                    frame_t, teacher_kpts[i] * scale_t,
                    teacher_confs[i], frame_part_scores)
            if j < len(student_kpts):
                frame_s = self.draw_skeleton(
                    frame_s, student_kpts[j] * np.array([scale_s, scale_s]),
                    student_confs[j], frame_part_scores)

            cv2.putText(frame_t, "Teacher", (10, 25),
                        self.font, 0.7, (0, 255, 0), 2)
            cv2.putText(frame_s, "Student", (10, 25),
                        self.font, 0.7, (0, 255, 255), 2)

            gap = np.full((target_h, 10, 3), 50, dtype=np.uint8)
            combined = np.hstack([frame_t, gap, frame_s])

            # 顶部信息条（OpenCV不支持中文，用英文标注；中文说明由前端按元数据展示）
            header = np.zeros((40, combined.shape[1], 3), dtype=np.uint8)
            part_en = self.PART_NAME_EN.get(worst_part, str(worst_part))
            info = (f"Worst #{rank}   t={i / fps:.1f}s   "
                    f"max deviation: {part_en} ({dev * 180:.0f} deg)")
            cv2.putText(header, info, (10, 27), self.font, 0.6,
                        (255, 255, 255), 1, cv2.LINE_AA)
            combined = np.vstack([header, combined])

            filename = f"worst_frame_{rank}.jpg"
            path = os.path.join(output_dir, filename)
            cv2.imwrite(path, combined)
            results.append({
                "rank": rank,
                "filename": filename,
                "teacher_time": round(i / fps, 2),
                "student_time": round(j / fps, 2),
                "worst_part": worst_part,
                "deviation": round(dev, 4),
            })
            print(f"[VideoVisualizer] 偏差截图#{rank}: t={i / fps:.1f}s "
                  f"部位={worst_part} 偏差={dev:.3f}")

        cap_t.release()
        cap_s.release()
        return results

    def export_frame_deviations(self, alignment_path,
                                teacher_angles, student_angles,
                                teacher_ac, student_ac,
                                fps, output_path):
        """
        导出逐帧偏差序列（供前端实时可视化与视频播放联动）
        沿DTW对齐路径逐帧计算置信度加权角度差（与评分同一度量），
        每个教师帧取首次对齐；帧率不足以确定时按30fps。
        输出: {"fps": fps, "frames": [{"t", "st", "d", "p": {部位: 偏差}}]}
        t=教师帧时间, st=对齐学生帧时间（前端据此做DTW内容级播放对齐）
        """
        fps = fps or 30
        frames = []
        seen = set()
        for i, j in alignment_path:
            if i in seen or i >= len(teacher_angles) or j >= len(student_angles):
                continue
            seen.add(i)
            t_a, s_a = teacher_angles[i], student_angles[j]
            t_c, s_c = teacher_ac[i], student_ac[j]
            part_dev_sum, part_w_sum = {}, {}
            total_dev, total_w = 0.0, 0.0
            for idx in range(len(JOINT_ANGLE_DEFS)):
                part = self.JOINT_PART_MAP.get(idx)
                if part is None:
                    continue
                diff = abs(float(t_a[idx]) - float(s_a[idx])) / 180.0
                conf = max(min(float(t_c[idx]), float(s_c[idx])), 0.01)
                part_dev_sum[part] = part_dev_sum.get(part, 0.0) + diff * conf
                part_w_sum[part] = part_w_sum.get(part, 0.0) + conf
                total_dev += diff * conf
                total_w += conf
            if total_w <= 0:
                continue
            frames.append({
                "t": round(i / fps, 3),
                "st": round(j / fps, 3),
                "d": round(total_dev / total_w, 4),
                "p": {p: round(part_dev_sum[p] / part_w_sum[p], 4)
                      for p in part_dev_sum if part_w_sum[p] > 0},
            })

        payload = {"fps": fps, "frames": frames}
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))
        print(f"[VideoVisualizer] 逐帧偏差序列导出: {output_path} "
              f"({len(frames)}帧)")
        return payload

    def create_heatmap_frame(self, frame, keypoints, confidences,
                             part_scores, min_conf=0.3):
        """
        生成热力图帧：标注偏差最大的关节部位
        绿色=优秀, 黄色=良好, 橙色=一般, 红色=差
        """
        return self.draw_skeleton(frame, keypoints, confidences,
                                  part_scores, min_conf)

    # 中文部位名到英文的映射（用于matplotlib图表，避免字体缺失）
    PART_NAME_EN = {
        "左上肢": "L.UpperLimb",
        "右上肢": "R.UpperLimb",
        "左下肢": "L.LowerLimb",
        "右下肢": "R.LowerLimb",
        "躯干": "Trunk",
        "头颈": "Head&Neck",
    }
    DIM_NAME_EN = {
        "动作规范性": "Posture",
        "节奏掌握度": "Rhythm",
        "整体协调性": "Coordination",
    }

    def create_score_chart(self, scores, output_path):
        """
        生成评分图表（使用matplotlib）
        使用英文标签避免中文字体缺失
        """
        try:
            import matplotlib
            matplotlib.use("Agg")
            import matplotlib.pyplot as plt
        except ImportError:
            print("[VideoVisualizer] matplotlib未安装，跳过图表生成")
            return None

        part_scores = scores.get("part_scores", {})
        dimension_scores = scores.get("dimension_scores", {})
        total_score = scores.get("total_score", 0)

        fig, axes = plt.subplots(1, 2, figsize=(14, 5))

        # 图1：部位得分柱状图
        parts = list(part_scores.keys())
        parts_en = [self.PART_NAME_EN.get(p, p) for p in parts]
        part_vals = [part_scores[p]["score"] for p in parts]
        colors = [tuple(c / 255 for c in get_score_color(v)) for v in part_vals]

        axes[0].barh(parts_en, part_vals, color=colors)
        axes[0].set_xlim(0, 100)
        axes[0].set_xlabel("Score")
        axes[0].set_title("Part Scores")
        for i, v in enumerate(part_vals):
            axes[0].text(v + 1, i, f"{v:.1f}", va="center")

        # 图2：维度得分雷达图
        dims = list(dimension_scores.keys())
        dims_en = [self.DIM_NAME_EN.get(d, d) for d in dims]
        dim_vals = [dimension_scores[d] for d in dims]
        angles = np.linspace(0, 2 * np.pi, len(dims), endpoint=False).tolist()
        dim_vals_plot = dim_vals + [dim_vals[0]]
        angles_plot = angles + [angles[0]]

        ax2 = plt.subplot(1, 2, 2, projection="polar")
        ax2.plot(angles_plot, dim_vals_plot, "o-", linewidth=2)
        ax2.fill(angles_plot, dim_vals_plot, alpha=0.25)
        ax2.set_xticks(angles)
        ax2.set_xticklabels(dims_en)
        ax2.set_ylim(0, 100)
        ax2.set_title("Dimension Scores", pad=20)

        plt.suptitle(f"Dance Score Report - Total: {total_score:.1f}",
                     fontsize=14, fontweight="bold")
        plt.tight_layout()
        plt.savefig(output_path, dpi=150, bbox_inches="tight")
        plt.close()

        print(f"[VideoVisualizer] 评分图表生成完成: {output_path}")
        return output_path

    def create_dtw_visualization(self, alignment_path, teacher_angles,
                                 student_angles, dtw_distance, output_path):
        """
        生成DTW对齐可视化图：cost矩阵热力图 + 弹性扭曲路径
        展示系统如何找到两段视频的最优时间对齐
        """
        try:
            import matplotlib
            matplotlib.use("Agg")
            import matplotlib.pyplot as plt
        except ImportError:
            print("[VideoVisualizer] matplotlib未安装，跳过DTW可视化")
            return None

        ta = np.array(teacher_angles)
        sa = np.array(student_angles)
        T1, N = ta.shape
        T2 = sa.shape[0]

        # 降采样保证cost矩阵在合理尺寸（200以内）
        max_size = 200
        step_t = max(1, T1 // max_size)
        step_s = max(1, T2 // max_size)
        ta_sub = ta[::step_t]
        sa_sub = sa[::step_s]
        T1s, T2s = len(ta_sub), len(sa_sub)

        # 计算cost矩阵（与DTW相同的距离度量）
        diff = np.abs(ta_sub[:, None, :] - sa_sub[None, :, :]) / 180.0
        cost_matrix = np.mean(diff, axis=2)  # (T1s, T2s)

        # 对齐路径降采样
        path = np.array(alignment_path)
        path_sub = path[::max(1, len(path) // max_size)]
        path_sub = path_sub.astype(float)
        path_sub[:, 0] = np.clip(path_sub[:, 0] / step_t, 0, T1s - 1)
        path_sub[:, 1] = np.clip(path_sub[:, 1] / step_s, 0, T2s - 1)

        fig, axes = plt.subplots(1, 2, figsize=(14, 5.5),
                                 facecolor='#0d1117')
        for ax in axes:
            ax.set_facecolor('#161b22')

        # 左图：cost矩阵热力图 + warping path叠加
        im = axes[0].imshow(cost_matrix.T, origin='lower', aspect='auto',
                            cmap='inferno', interpolation='nearest',
                            extent=[0, T1s, 0, T2s])
        axes[0].plot(path_sub[:, 0], path_sub[:, 1], 'cyan',
                    linewidth=2.5, alpha=0.9, label='Optimal path')
        axes[0].plot([0, min(T1s, T2s)], [0, min(T1s, T2s)],
                    'w--', alpha=0.4, linewidth=1, label='Diagonal (ideal)')
        axes[0].set_xlabel('Teacher Frame Index', color='#c9d1d9')
        axes[0].set_ylabel('Student Frame Index', color='#c9d1d9')
        axes[0].set_title('DTW Cost Matrix & Optimal Warping Path',
                          color='#f0f6fc', fontsize=12, fontweight='bold')
        axes[0].legend(loc='upper left', fontsize=8, facecolor='#161b22',
                       edgecolor='#30363d', labelcolor='#c9d1d9')
        axes[0].tick_params(colors='#8b949e')
        for spine in axes[0].spines.values():
            spine.set_color('#30363d')
        cbar0 = plt.colorbar(im, ax=axes[0], shrink=0.8)
        cbar0.set_label('Cost', color='#c9d1d9')
        cbar0.ax.tick_params(colors='#8b949e')
        cbar0.outline.set_edgecolor('#30363d')

        # 右图：弹性扭曲路径（phase offset着色）
        offsets = path_sub[:, 1] - path_sub[:, 0]
        sc = axes[1].scatter(path_sub[:, 0], path_sub[:, 1], c=offsets,
                            cmap='RdYlGn_r', s=8, alpha=0.85,
                            edgecolors='none')
        axes[1].plot(path_sub[:, 0], path_sub[:, 1],
                    color='#8b949e', linewidth=0.5, alpha=0.4)
        axes[1].plot([0, T1s], [0, T1s], 'w--', alpha=0.4,
                    linewidth=1, label='Diagonal (ideal)')
        axes[1].set_xlabel('Teacher Frame Index', color='#c9d1d9')
        axes[1].set_ylabel('Student Frame Index', color='#c9d1d9')
        axes[1].set_title(f'Elastic Warping Path (DTW dist={dtw_distance:.4f})',
                         color='#f0f6fc', fontsize=12, fontweight='bold')
        axes[1].legend(loc='upper left', fontsize=8, facecolor='#161b22',
                       edgecolor='#30363d', labelcolor='#c9d1d9')
        axes[1].tick_params(colors='#8b949e')
        for spine in axes[1].spines.values():
            spine.set_color('#30363d')
        cbar1 = plt.colorbar(sc, ax=axes[1], shrink=0.8)
        cbar1.set_label('Phase offset (j-i)', color='#c9d1d9')
        cbar1.ax.tick_params(colors='#8b949e')
        cbar1.outline.set_edgecolor('#30363d')

        plt.suptitle('DTW Temporal Alignment Analysis',
                    fontsize=14, fontweight='bold', color='#f0f6fc')
        plt.tight_layout()
        plt.savefig(output_path, dpi=150, bbox_inches='tight',
                    facecolor='#0d1117')
        plt.close()
        print(f"[VideoVisualizer] DTW可视化图生成完成: {output_path}")
        return output_path

    def create_joint_heatmap(self, teacher_angles, student_angles,
                             teacher_confs, student_confs,
                             alignment_path, teacher_kpts, output_path):
        """
        生成关节偏差热力图：在骨架上按平均角度偏差着色
        绿=准确 / 黄=轻微偏差 / 红=明显偏差
        """
        try:
            import matplotlib
            matplotlib.use("Agg")
            import matplotlib.pyplot as plt
            from matplotlib.colors import LinearSegmentedColormap
        except ImportError:
            print("[VideoVisualizer] matplotlib未安装，跳过关节热力图")
            return None

        ta = np.array(teacher_angles)
        sa = np.array(student_angles)
        N = ta.shape[1]

        # 计算每个关节角度的平均偏差（度）
        avg_dev = np.zeros(N)
        for k in range(N):
            diffs = []
            for i, j in alignment_path:
                if i < ta.shape[0] and j < sa.shape[0]:
                    d = abs(float(ta[i, k]) - float(sa[j, k]))
                    diffs.append(d)
            avg_dev[k] = np.mean(diffs) if diffs else 0.0

        # 将关节偏差映射到MediaPipe 33关键点
        kp_dev = np.zeros(33)
        for k_idx, (name, vertex, a, b) in enumerate(JOINT_ANGLE_DEFS):
            if k_idx >= N:
                continue
            dev = avg_dev[k_idx]
            kp_dev[vertex] = max(kp_dev[vertex], dev)
            kp_dev[a] = max(kp_dev[a], dev)
            kp_dev[b] = max(kp_dev[b], dev)

        # 教师第一帧关键点作为参考骨架
        ref_kpts = np.array(teacher_kpts[0]) if len(teacher_kpts) > 0 \
            else np.zeros((33, 2))

        # 颜色映射：绿->黄->红（0-30度范围）
        cmap = LinearSegmentedColormap.from_list(
            'dev', ['#00e676', '#ffeb3b', '#ff1744'])
        norm_dev = np.clip(kp_dev / 30.0, 0, 1)

        fig, ax = plt.subplots(figsize=(7, 9), facecolor='#0d1117')
        ax.set_facecolor('#0d1117')

        # 绘制骨架连接线（按偏差着色 + 发光效果）
        for (a_idx, b_idx) in POSE_CONNECTIONS:
            if a_idx >= len(ref_kpts) or b_idx >= len(ref_kpts):
                continue
            pt_a = ref_kpts[a_idx]
            pt_b = ref_kpts[b_idx]
            dev = max(norm_dev[a_idx], norm_dev[b_idx])
            color = cmap(dev)
            # 外层发光
            ax.plot([pt_a[0], pt_b[0]], [pt_a[1], pt_b[1]],
                    color=color, linewidth=8, alpha=0.15)
            ax.plot([pt_a[0], pt_b[0]], [pt_a[1], pt_b[1]],
                    color=color, linewidth=4, alpha=0.85)

        # 绘制关键点
        for i, pt in enumerate(ref_kpts):
            dev = norm_dev[i]
            color = cmap(dev)
            # 外层光晕
            glow = plt.Circle((pt[0], pt[1]), 14, color=color, alpha=0.2, zorder=4)
            ax.add_patch(glow)
            circle = plt.Circle((pt[0], pt[1]), 8, color=color, zorder=5)
            ax.add_patch(circle)

        # 标注偏差较大的关节（>12度）
        annotated = set()
        for k_idx, (name, vertex, a, b) in enumerate(JOINT_ANGLE_DEFS):
            if k_idx >= N or vertex in annotated:
                continue
            if avg_dev[k_idx] > 12:
                pt = ref_kpts[vertex]
                ax.annotate(
                    f'{name} {avg_dev[k_idx]:.1f} deg',
                    (pt[0], pt[1]), fontsize=7, ha='center',
                    xytext=(15, 15), textcoords='offset points',
                    color='#f0f6fc', fontweight='bold',
                    bbox=dict(boxstyle='round,pad=0.3',
                             facecolor='#1a1a2e', edgecolor=color, alpha=0.9),
                    arrowprops=dict(arrowstyle='->', color=color, lw=1))
                annotated.add(vertex)

        # 设置坐标范围（翻转Y轴适配图像坐标系）
        margin = 40
        ax.set_xlim(ref_kpts[:, 0].min() - margin,
                    ref_kpts[:, 0].max() + margin)
        ax.set_ylim(ref_kpts[:, 1].max() + margin,
                    ref_kpts[:, 1].min() - margin)
        ax.set_aspect('equal')
        ax.set_title('Joint Deviation Heatmap', color='#f0f6fc',
                    fontsize=14, fontweight='bold', pad=15)
        ax.tick_params(colors='#8b949e')
        for spine in ax.spines.values():
            spine.set_color('#30363d')

        # 色条
        sm = plt.cm.ScalarMappable(cmap=cmap, norm=plt.Normalize(0, 30))
        sm.set_array([])
        cbar = plt.colorbar(sm, ax=ax, shrink=0.5, pad=0.02)
        cbar.set_label('Deviation (deg)', color='#c9d1d9')
        cbar.ax.tick_params(colors='#8b949e')
        cbar.outline.set_edgecolor('#30363d')

        plt.tight_layout()
        plt.savefig(output_path, dpi=150, bbox_inches='tight',
                    facecolor='#0d1117')
        plt.close()
        print(f"[VideoVisualizer] 关节偏差热力图生成完成: {output_path}")
        return output_path
