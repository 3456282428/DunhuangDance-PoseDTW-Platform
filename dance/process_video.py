"""
舞蹈AI评分系统 - 主处理流水线
完整流程：双视频输入 → 预处理 → 姿态提取 → DTW对齐 → 分部位评分 → 可视化反馈
"""
import os
import json
import time
import numpy as np

from config import OUTPUT_DIR, TARGET_FPS
from modules.video_preprocess import VideoPreprocessor
from modules.pose_extraction import PoseExtractor
from modules.dtw_alignment import DTWAligner
from modules.scoring import DanceScorer
from modules.visualization import VideoVisualizer
from modules.analysis import MovementAnalyzer
from modules.rhythm import RhythmAnalyzer
from utils import smooth_keypoints, extract_joint_angles, infer_dynamic_weights


class DanceScoringPipeline:
    """舞蹈AI评分系统主流水线"""

    def __init__(self, yolo_model_path=None, use_dynamic_weights=True):
        print("=" * 60)
        print("舞蹈AI评分系统 - 初始化")
        print("=" * 60)

        # 初始化各模块
        self.preprocessor = VideoPreprocessor(yolo_model_path)
        self.pose_extractor = PoseExtractor()
        self.dtw_aligner = DTWAligner()
        self.scorer = DanceScorer(use_dynamic_weights=use_dynamic_weights)
        self.visualizer = VideoVisualizer()
        self.analyzer = MovementAnalyzer()
        self.rhythm_analyzer = RhythmAnalyzer(fps=TARGET_FPS)

        self.results = {}

    def process(self, teacher_video, student_video, output_dir,
                generate_video=True, generate_chart=True,
                progress_cb=None):
        """
        完整处理流程
        teacher_video: 教师参考视频路径
        student_video: 学生练习视频路径
        progress_cb: 可选回调 fn(percent:int, stage:str)，每完成一个阶段上报一次
        返回: 评分结果 + 对比视频路径 + 图表路径
        """
        def _report(pct, stage):
            if progress_cb:
                try:
                    progress_cb(pct, stage)
                except Exception:
                    pass

        start_time = time.time()
        os.makedirs(output_dir, exist_ok=True)

        timestamp = time.strftime("%Y%m%d_%H%M%S")
        session_dir = os.path.join(output_dir, f"session_{timestamp}")
        os.makedirs(session_dir, exist_ok=True)

        print("\n" + "=" * 60)
        print("步骤1/6: 视频预处理")
        print("=" * 60)

        teacher_preprocessed = self.preprocessor.preprocess(
            teacher_video, session_dir
        )
        student_preprocessed = self.preprocessor.preprocess(
            student_video, session_dir
        )
        _report(15, "视频预处理完成")

        print("\n" + "=" * 60)
        print("步骤2/6: 姿态提取 (MediaPipe Pose)")
        print("=" * 60)

        teacher_kpts, teacher_confs = self.pose_extractor.extract_from_video(
            teacher_preprocessed,
            progress_cb=lambda f: _report(int(15 + f * 15), "姿态提取中（教师视频）")
        )
        student_kpts, student_confs = self.pose_extractor.extract_from_video(
            student_preprocessed,
            progress_cb=lambda f: _report(int(30 + f * 15), "姿态提取中（学生视频）")
        )

        if teacher_kpts is None or student_kpts is None:
            raise RuntimeError("姿态提取失败，无法继续处理")
        _report(45, "姿态提取完成")

        print("\n" + "=" * 60)
        print("步骤3/6: 关键点平滑处理")
        print("=" * 60)

        teacher_kpts = smooth_keypoints(teacher_kpts)
        student_kpts = smooth_keypoints(student_kpts)
        _report(52, "关键点平滑完成")
        print(f"  教师关键点: {teacher_kpts.shape}")
        print(f"  学生关键点: {student_kpts.shape}")

        print("\n" + "=" * 60)
        print("步骤4/6: 提取关节角度序列")
        print("=" * 60)

        teacher_angles, teacher_ac = \
            self.pose_extractor.extract_angles_from_sequence(
                teacher_kpts, teacher_confs
            )
        student_angles, student_ac = \
            self.pose_extractor.extract_angles_from_sequence(
                student_kpts, student_confs
            )
        print(f"  教师角度序列: {teacher_angles.shape}")
        print(f"  学生角度序列: {student_angles.shape}")
        _report(62, "关节角度序列提取完成")

        print("\n" + "=" * 60)
        print("步骤5/6: DTW时序对齐")
        print("=" * 60)

        alignment_path, dtw_distance = self.dtw_aligner.align(
            teacher_angles, student_angles,
            teacher_ac, student_ac
        )
        rhythm_cost = self.dtw_aligner.compute_rhythm_cost(alignment_path)
        print(f"  DTW距离: {dtw_distance:.4f}")
        print(f"  节奏弯曲成本: {rhythm_cost:.4f}")
        _report(72, "DTW时序对齐完成")

        print("\n" + "=" * 60)
        print("步骤6/6: 分部位评分 + 多维评价")
        print("=" * 60)

        scores = self.scorer.score(
            teacher_kpts, student_kpts,
            teacher_confs, student_confs,
            alignment_path, teacher_angles
        )
        _report(82, "分部位评分完成")

        # 打印结果
        print("\n" + "=" * 60)
        print("评分结果")
        print("=" * 60)

        print("\n--- 部位得分 ---")
        for part, ps in scores.get("part_scores", {}).items():
            print(f"  {part}: {ps['score']:.1f} ({ps['description']})")

        print("\n--- 维度得分 ---")
        for dim, ds in scores.get("dimension_scores", {}).items():
            print(f"  {dim}: {ds:.1f}")

        print(f"\n--- 总分: {scores.get('total_score', 0):.1f} ---")

        print("\n--- 改进建议 ---")
        for fb in scores.get("feedback", [])[:3]:
            print(f"  [{fb['level']}] {fb['part']}: {fb['score']:.1f} - {fb['description']}")

        # 生成对比视频
        comparison_video = None
        teacher_overlay_video = None
        student_overlay_video = None
        worst_frames = []
        frames_json = None
        frames_payload = None
        if generate_video:
            print("\n" + "=" * 60)
            print("生成对比视频...")
            print("=" * 60)
            comparison_video = os.path.join(session_dir, "comparison.mp4")
            teacher_overlay_video = os.path.join(session_dir, "teacher_overlay.mp4")
            student_overlay_video = os.path.join(session_dir, "student_overlay.mp4")
            self.visualizer.create_comparison_video(
                teacher_preprocessed, student_preprocessed,
                teacher_kpts, student_kpts,
                teacher_confs, student_confs,
                alignment_path, scores,
                comparison_video,
                teacher_overlay_path=teacher_overlay_video,
                student_overlay_path=student_overlay_video
            )

            # 动作偏差最大帧对比截图
            worst_frames = self.visualizer.create_worst_frame_snapshots(
                teacher_preprocessed, student_preprocessed,
                teacher_kpts, student_kpts,
                teacher_confs, student_confs,
                alignment_path,
                teacher_angles, student_angles,
                teacher_ac, student_ac,
                session_dir
            )

            # 逐帧偏差序列（前端实时可视化与视频联动；预处理视频已归一30fps）
            frames_json = os.path.join(session_dir, "frames.json")
            frames_payload = self.visualizer.export_frame_deviations(
                alignment_path,
                teacher_angles, student_angles,
                teacher_ac, student_ac,
                30, frames_json
            )

            # DTW对齐可视化图（cost矩阵热力图 + 弹性扭曲路径）
            dtw_vis_path = os.path.join(session_dir, "dtw_visualization.png")
            self.visualizer.create_dtw_visualization(
                alignment_path, teacher_angles, student_angles,
                dtw_distance, dtw_vis_path
            )

            # 关节偏差热力图（骨架按平均偏差着色）
            joint_heatmap_path = os.path.join(session_dir, "joint_heatmap.png")
            self.visualizer.create_joint_heatmap(
                teacher_angles, student_angles,
                teacher_ac, student_ac,
                alignment_path, teacher_kpts, joint_heatmap_path
            )
            _report(95, "可视化产物生成完成")

        # 动作综合分析（总体评价/优点/不足/训练建议，全部由评分数据推导）
        analysis = self.analyzer.analyze(
            scores, frames_payload, dtw_distance, rhythm_cost
        )
        scores["analysis"] = analysis

        # 节奏对齐分析（视觉+听觉融合）：音乐节拍 × 动作节拍，独立维度不并入总分
        print("\n" + "=" * 60)
        print("节奏对齐分析（音乐节拍 × 动作节拍）")
        print("=" * 60)
        scores["rhythm"] = self.rhythm_analyzer.analyze(
            teacher_video, student_video, teacher_kpts, student_kpts,
            work_dir=session_dir
        )
        _report(88, "节奏对齐分析完成")

        # 生成评分图表
        chart_path = None
        if generate_chart:
            print("\n生成评分图表...")
            chart_path = os.path.join(session_dir, "score_chart.png")
            self.visualizer.create_score_chart(scores, chart_path)

        # 保存结果JSON
        result_json = os.path.join(session_dir, "scores.json")
        # 确保所有值可JSON序列化
        clean_scores = self._make_json_serializable(scores)
        clean_scores["teacher_video"] = teacher_video
        clean_scores["student_video"] = student_video
        clean_scores["dtw_distance"] = float(dtw_distance)
        clean_scores["rhythm_cost"] = float(rhythm_cost)
        clean_scores["processing_time"] = round(time.time() - start_time, 2)

        with open(result_json, "w", encoding="utf-8") as f:
            json.dump(clean_scores, f, ensure_ascii=False, indent=2)

        elapsed = time.time() - start_time
        print(f"\n{'=' * 60}")
        print(f"处理完成! 耗时: {elapsed:.1f}秒")
        print(f"结果目录: {session_dir}")
        print(f"{'=' * 60}")

        self.results = clean_scores
        self.results["output_dir"] = session_dir
        self.results["comparison_video"] = comparison_video
        self.results["teacher_overlay_video"] = teacher_overlay_video
        self.results["student_overlay_video"] = student_overlay_video
        self.results["worst_frames"] = worst_frames
        self.results["frames_json"] = frames_json
        self.results["chart_path"] = chart_path
        self.results["scores_json"] = result_json
        self.results["dtw_visualization"] = dtw_vis_path if generate_video else None
        self.results["joint_heatmap"] = joint_heatmap_path if generate_video else None

        return self.results

    def _make_json_serializable(self, obj):
        """确保对象可JSON序列化"""
        if isinstance(obj, dict):
            return {k: self._make_json_serializable(v) for k, v in obj.items()}
        elif isinstance(obj, (list, tuple)):
            return [self._make_json_serializable(v) for v in obj]
        elif isinstance(obj, (np.integer,)):
            return int(obj)
        elif isinstance(obj, (np.floating,)):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        elif isinstance(obj, (str, int, float, bool, type(None))):
            return obj
        else:
            return str(obj)

    def close(self):
        """释放资源"""
        self.pose_extractor.close()


def main():
    """命令行入口"""
    import argparse

    parser = argparse.ArgumentParser(description="舞蹈AI评分系统")
    parser.add_argument("--teacher", required=True, help="教师参考视频路径")
    parser.add_argument("--student", required=True, help="学生练习视频路径")
    parser.add_argument("--output", default=OUTPUT_DIR, help="输出目录")
    parser.add_argument("--no-video", action="store_true", help="不生成对比视频")
    parser.add_argument("--no-chart", action="store_true", help="不生成评分图表")

    args = parser.parse_args()

    pipeline = DanceScoringPipeline()
    try:
        results = pipeline.process(
            args.teacher, args.student,
            output_dir=args.output,
            generate_video=not args.no_video,
            generate_chart=not args.no_chart
        )
        print(f"\n对比视频: {results.get('comparison_video', 'N/A')}")
        print(f"评分图表: {results.get('chart_path', 'N/A')}")
        print(f"评分JSON: {results.get('scores_json', 'N/A')}")
    finally:
        pipeline.close()


if __name__ == "__main__":
    main()
