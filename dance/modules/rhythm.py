"""
模块五：节奏对齐分析（视觉+听觉融合）
方法论借鉴（BeatDance节拍感知 / M2Beats运动节奏 / 舞蹈生成节拍对齐F1评估）：
1. 音乐节拍跟踪：从视频音轨提取节拍序列（librosa优先，scipy回退）
2. 动作节拍提取：从关键点序列计算动作能量曲线，能量峰值即动作节拍
3. 动作-节拍对齐：命中率（±150ms容差）、平均偏差、速度一致性 → 节奏对齐得分
无音轨时自动降级：available=False，不阻断主评分流水线。
"""
import os
import subprocess

import numpy as np
from scipy.ndimage import gaussian_filter1d
from scipy.signal import find_peaks

from config import TARGET_FPS

# 节拍命中容差（毫秒）：舞蹈节拍对齐评估常用±70~200ms，取150ms
BEAT_TOLERANCE_MS = 150
# 动作能量曲线降采样点数（图表用，控制JSON体积）
CHART_POINTS = 240
# 音乐节拍/动作节拍序列导出上限
MAX_BEAT_POINTS = 600
# 动作关键点索引（躯干四肢，排除面部细碎抖动）
MOTION_KPT_IDS = [11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32]
# 动作节拍最小间隔（秒）：对应最高240BPM
MIN_BEAT_INTERVAL_S = 0.25


class RhythmAnalyzer:
    """节奏对齐分析器：音乐节拍 × 动作节拍"""

    def __init__(self, fps=TARGET_FPS):
        self.fps = fps
        self._librosa = None
        try:
            import librosa
            self._librosa = librosa
            print("[RhythmAnalyzer] 使用librosa节拍跟踪")
        except ImportError:
            print("[RhythmAnalyzer] librosa未安装，使用scipy回退算法")

    # ==================== 音频抽取与节拍跟踪 ====================

    def _get_ffmpeg_cmd(self):
        for cmd in ["ffmpeg", "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg"]:
            try:
                r = subprocess.run([cmd, "-version"], capture_output=True, timeout=5)
                if r.returncode == 0:
                    return cmd
            except Exception:
                continue
        return None

    def extract_audio(self, video_path, out_wav):
        """从视频抽取音轨：22050Hz单声道16bit WAV（scipy.io可读）；无音轨返回None"""
        ffmpeg = self._get_ffmpeg_cmd()
        if ffmpeg is None:
            print("[RhythmAnalyzer] ffmpeg不可用，无法抽取音轨")
            return None
        try:
            r = subprocess.run(
                [ffmpeg, "-y", "-i", video_path, "-vn", "-ac", "1",
                 "-ar", "22050", "-sample_fmt", "s16",
                 "-af", "aresample=async=1:first_pts=0", out_wav],
                capture_output=True, timeout=120, check=False)
            if r.returncode == 0 and os.path.exists(out_wav) and os.path.getsize(out_wav) > 1024:
                return out_wav
            # 退出码非0或文件过小：多为无音轨
            return None
        except Exception as e:
            print(f"[RhythmAnalyzer] 音频抽取失败: {e}")
            return None

    def track_beats(self, wav_path):
        """
        节拍跟踪：返回 (bpm, beat_times)
        librosa可用时用其动态规划节拍跟踪器；否则scipy频谱通量+自相关回退
        """
        if self._librosa is not None:
            try:
                y, sr = self._librosa.load(wav_path, sr=22050, mono=True)
                tempo, beats = self._librosa.beat.beat_track(y=y, sr=sr, units="time")
                bpm = float(np.atleast_1d(tempo)[0])
                times = [round(float(t), 3) for t in np.atleast_1d(beats)]
                if times:
                    return bpm, times
            except Exception as e:
                print(f"[RhythmAnalyzer] librosa节拍跟踪失败，回退scipy: {e}")
        return self._track_beats_scipy(wav_path)

    def _track_beats_scipy(self, wav_path):
        """scipy回退节拍跟踪：频谱通量起始点包络 + 自相关测速 + 节拍网格相位对齐"""
        from scipy.io import wavfile
        sr, y = wavfile.read(wav_path)
        y = y.astype(np.float64)
        if y.ndim > 1:
            y = y.mean(axis=1)
        peak = np.max(np.abs(y)) if len(y) else 0
        if peak < 1e-6:
            return 0.0, []
        y /= peak

        hop = 256
        n_fft = 1024
        # 分帧计算对数幅度谱 → 频谱通量（半波整流差分）
        # 注意：帧数按 len(y)-n_fft 截断，避免as_strided读越缓冲区末尾（会触发访问违例）
        if len(y) < n_fft * 2:
            return 0.0, []
        n_frames = 1 + (len(y) - n_fft) // hop
        win = np.hanning(n_fft)
        frames = np.lib.stride_tricks.as_strided(
            y, shape=(n_frames, n_fft),
            strides=(y.strides[0] * hop, y.strides[0])).copy()
        frames *= win
        spec = np.abs(np.fft.rfft(frames, axis=1))
        log_spec = np.log1p(10 * spec)
        flux = np.maximum(0.0, np.diff(log_spec, axis=0)).sum(axis=1)
        flux = np.concatenate([[0.0], flux])
        # 平滑
        flux = gaussian_filter1d(flux, sigma=4)
        if np.std(flux) < 1e-8:
            return 0.0, []

        # 自相关测速（60-180 BPM候选区间）
        fps_env = sr / hop
        min_lag = int(fps_env * 60 / 180)
        max_lag = int(fps_env * 60 / 60)
        max_lag = min(max_lag, len(flux) // 2)
        if max_lag <= min_lag:
            return 0.0, []
        centered = flux - flux.mean()
        ac = np.correlate(centered, centered, mode="full")[len(flux) - 1:]
        ac[:min_lag] = 0
        ac[max_lag:] = 0
        best_lag = int(np.argmax(ac))
        bpm = 60.0 * fps_env / best_lag

        # 节拍网格：测速周期内取起始点包络最强的相位
        period = fps_env * 60 / bpm
        n_beats = int(len(flux) / period)
        if n_beats < 2:
            return round(bpm, 2), []
        phases = np.arange(int(period))
        scores = np.zeros(len(phases))
        for ph in phases:
            idx = (ph + period * np.arange(n_beats)).astype(int)
            idx = idx[idx < len(flux)]
            scores[ph] = flux[idx].sum()
        best_phase = int(np.argmax(scores))
        beat_times = [round((best_phase + period * k) / fps_env, 3)
                      for k in range(n_beats)]
        return round(bpm, 2), beat_times

    # ==================== 动作节拍提取 ====================

    def extract_motion_beats(self, kpts):
        """
        从关键点序列提取动作能量与动作节拍
        kpts: (T, 33, 2+) 关键点序列
        返回: (energy归一曲线(T,), 动作节拍帧索引列表, 动作BPM)
        """
        kpts = np.asarray(kpts, dtype=np.float64)
        T = len(kpts)
        if T < 8:
            return np.zeros(T), [], 0.0

        # 躯干尺度归一（肩中心-髋中心距离），保证远/近景与体型无关
        shoulder_c = (kpts[:, 11] + kpts[:, 12]) / 2
        hip_c = (kpts[:, 23] + kpts[:, 24]) / 2
        torso = np.linalg.norm(shoulder_c - hip_c, axis=1)
        torso = np.where(torso < 1e-6, 1.0, torso)

        idx = [i for i in MOTION_KPT_IDS if i < kpts.shape[1]]
        disp = np.linalg.norm(np.diff(kpts[:, idx, :2], axis=0), axis=2)  # (T-1, n)
        energy = disp.mean(axis=1) / torso[1:]                            # (T-1,)
        energy = np.concatenate([[0.0], energy])
        energy = gaussian_filter1d(energy, sigma=2)

        # 峰值检测：最小间隔0.25s，显著性阈值随信号自适应
        distance = max(1, int(MIN_BEAT_INTERVAL_S * self.fps))
        prominence = max(float(np.std(energy)) * 0.5, float(np.max(energy)) * 0.05)
        peaks, _ = find_peaks(energy, distance=distance, prominence=prominence)
        if len(peaks) < 2:
            peaks, _ = find_peaks(energy, distance=distance,
                                  prominence=float(np.max(energy)) * 0.02)

        # 动作BPM：取相邻动作节拍间隔中位数（过滤异常间隔）
        bpm = 0.0
        if len(peaks) >= 2:
            intervals = np.diff(peaks) / self.fps
            med = float(np.median(intervals))
            if med > 0.05:
                bpm = round(60.0 / med, 1)
        return energy, [int(p) for p in peaks], bpm

    # ==================== 对齐分析 ====================

    # 偏移搜索范围（秒）：兼顾流水线级系统延迟（AAC起始/预处理时间轴，<100ms）
    # 与舞者相对节拍的相位游走（实测可达±450ms），界内吸收摆动、界外仍算脱拍
    OFFSET_SEARCH_RANGE_S = 0.45
    OFFSET_SEARCH_STEP_S = 0.01
    # 分段时长（秒）：舞者相对节拍存在±250ms级相位游走（非匀速漂移），
    # 全局单一偏移无法跟随，按窗口精化各段局部偏移吸收摆动
    SEGMENT_WINDOW_S = 10.0

    @classmethod
    def _align_metrics(cls, music_beats, motion_beat_times, tolerance_s,
                       tempo_ratio=1.0):
        """节拍命中统计（全局偏移估计 + 半速感知 + 分段相位精化）
        - 全局偏移：动作节拍整体时移Δ∈[-0.3,+0.3]s（10ms步进）取命中数最大者，
          补偿音轨抽取/预处理时间轴与姿态序列间的系统延迟
        - 半速感知：动作BPM≈音乐一半（0.35~0.65）时，音乐节拍按索引奇偶拆成
          两条半速网格，取命中更优的一条为基准——否则半速踩拍命中率上限仅50%
        - 分段精化：每10s窗口在±0.3s界内独立选局部最优偏移，吸收相位游走
        返回: (hit_rate, avg_offset_ms, hits, global_offset_ms, evaluated_total,
               segment_offsets_ms)
        """
        if not music_beats or not motion_beat_times:
            return 0.0, 0.0, 0, 0.0, 0, []
        mb = np.asarray(motion_beat_times, dtype=np.float64)
        music = np.asarray(music_beats, dtype=np.float64)

        def _count(ref_beats, shift):
            """基准节拍加shift后与动作节拍的命中数及命中偏差"""
            s = mb + shift
            pos = np.searchsorted(s, ref_beats)
            left = np.clip(pos - 1, 0, len(s) - 1)
            right = np.clip(pos, 0, len(s) - 1)
            d = np.minimum(np.abs(ref_beats - s[left]), np.abs(s[right] - ref_beats))
            ok = d <= tolerance_s
            return int(ok.sum()), d[ok]

        shifts = np.arange(-cls.OFFSET_SEARCH_RANGE_S,
                           cls.OFFSET_SEARCH_RANGE_S + 1e-9,
                           cls.OFFSET_SEARCH_STEP_S)
        # 半速时基准候选=两条奇偶半速网格；其余（1倍/2倍速）用完整音乐节拍
        if 0.35 <= tempo_ratio <= 0.65 and len(music) >= 4:
            candidates = [music[0::2], music[1::2]]
        else:
            candidates = [music]

        best = None  # (rate, hits, -mean_dist, shift, ref)：rate优先，同分取命中多、偏差小
        for shift in shifts:
            for ref in candidates:
                hits, dists = _count(ref, shift)
                cand = (hits / len(ref), hits,
                        -float(np.mean(dists)) if hits else -1e9,
                        float(shift), ref)
                if best is None or cand[:3] > best[:3]:
                    best = cand
        rate, hits, neg_md, g_shift, ref = best

        # 分段精化：各窗口独立选局部偏移（仍限±0.3s界，防止对随机噪声过拟合）
        seg_offsets, total_hits, dist_all = [], 0, []
        w = cls.SEGMENT_WINDOW_S
        n_seg = int(np.ceil(float(ref[-1]) / w)) if len(ref) else 0
        for k in range(n_seg):
            seg = ref[(ref >= k * w) & (ref < (k + 1) * w)]
            if len(seg) == 0:
                seg_offsets.append(0)
                continue
            sb = None
            for shift in shifts:
                h, d = _count(seg, shift)
                cand = (h, -float(np.mean(d)) if h else -1e9, float(shift))
                if sb is None or cand[:2] > sb[:2]:
                    sb = cand
            h, neg_d, shift = sb
            seg_offsets.append(round(shift * 1000))
            total_hits += h
            if h:
                dist_all.append(-neg_d)
        hit_rate = total_hits / len(ref)
        avg = float(np.mean(dist_all)) if dist_all else 0.0
        return (round(hit_rate, 4), round(avg * 1000, 1), total_hits,
                round(g_shift * 1000, 1), len(ref), seg_offsets)

    @staticmethod
    def _tempo_consistency(music_bpm, motion_bpm):
        """速度一致性：考虑半速/倍速舞动（动作BPM可为音乐BPM的0.5/1/2倍），1-相对偏差"""
        if not music_bpm or not motion_bpm:
            return 0.0
        best = 0.0
        for r in (0.5, 1.0, 2.0):
            target = music_bpm * r
            diff = abs(motion_bpm - target) / target
            best = max(best, 1.0 - min(diff, 1.0))
        return round(best, 4)

    @staticmethod
    def _rhythm_score(hit_rate, tempo_cons, avg_offset_ms):
        """节奏对齐得分（0-100独立维度）：命中50% + 速度30% + 偏差精度20%"""
        offset_score = 1.0 - min((avg_offset_ms or 0.0) / BEAT_TOLERANCE_MS, 1.0)
        score = 100.0 * (0.50 * hit_rate + 0.30 * tempo_cons + 0.20 * offset_score)
        return round(float(max(0.0, min(100.0, score))), 2)

    def _downsample_curve(self, energy):
        """能量曲线均匀降采样到CHART_POINTS点，归一化到0-1"""
        if len(energy) == 0:
            return [], []
        T = len(energy)
        n = min(CHART_POINTS, T)
        pos = np.linspace(0, T - 1, n).astype(int)
        e = energy[pos]
        peak = float(np.max(e)) if len(e) else 0.0
        if peak > 1e-8:
            e = e / peak
        times = [round(p / self.fps, 3) for p in pos]
        return times, [round(float(v), 4) for v in e]

    def _motion_side(self, kpts):
        """动作侧提取：能量/节拍帧/时间/BPM"""
        energy, peaks, bpm = self.extract_motion_beats(kpts)
        times = [round(p / self.fps, 3) for p in peaks]
        return energy, peaks, times, bpm

    def analyze(self, teacher_video, student_video, teacher_kpts, student_kpts,
                work_dir=None):
        """
        完整节奏对齐分析（不抛异常，失败降级为available=False）
        返回: rhythm dict（写入scores.json与API payload）
        """
        result = {
            "available": False,
            "message": "",
            "tolerance_ms": BEAT_TOLERANCE_MS,
            "rhythm_score": None,
            "music_bpm": None, "motion_bpm": None,
            "tempo_consistency": None,
            "beat_hit_rate": None, "matched_beats": None,
            "total_music_beats": None, "avg_offset_ms": None,
            "global_offset_ms": None, "segment_offsets_ms": None,
            "teacher": None,
            "chart": None,
        }
        try:
            os.makedirs(work_dir or ".", exist_ok=True)
            work = work_dir or "."
            t_wav = self.extract_audio(teacher_video, os.path.join(work, "teacher_audio.wav"))
            s_wav = self.extract_audio(student_video, os.path.join(work, "student_audio.wav"))
            if t_wav is None and s_wav is None:
                result["message"] = "教师与学生视频均无音轨，节奏对齐分析不可用"
                return result

            # 音乐节拍：学生音轨优先（学生练习所用音乐），无则回退教师音轨
            ref_wav, music_source = (s_wav, "学生视频音轨") if s_wav else (t_wav, "教师参考视频音轨")
            music_bpm, music_beats = self.track_beats(ref_wav)
            if not music_beats:
                result["message"] = "未能从音轨中检测到稳定节拍，节奏对齐分析不可用"
                return result

            # 动作节拍（学生）与教师基线（教师动作vs教师音轨）
            _, _, s_motion_times, s_motion_bpm = self._motion_side(student_kpts)
            if not s_motion_times:
                result["message"] = "未能从学生动作中检测到动作节拍（动作幅度过小或过碎）"
                result["music_bpm"] = round(float(music_bpm), 1)
                result["total_music_beats"] = len(music_beats)
                result["chart"] = self._build_chart(student_kpts, music_beats, [])
                return result

            tol_s = BEAT_TOLERANCE_MS / 1000.0
            ratio_s = (s_motion_bpm / music_bpm) if music_bpm else 1.0
            hit_rate, avg_ms, hits, g_off_ms, n_eval, seg_offs = self._align_metrics(
                music_beats, s_motion_times, tol_s, tempo_ratio=ratio_s)
            tempo_cons = self._tempo_consistency(music_bpm, s_motion_bpm)

            teacher_metrics = None
            if t_wav is not None:
                # 教师基线：教师动作对齐教师自己音轨的节拍（无独立音轨时复用参考节拍）
                if s_wav:
                    t_bpm_ref, t_beats_ref = self.track_beats(t_wav)
                else:
                    t_bpm_ref, t_beats_ref = music_bpm, music_beats
                _, _, t_motion_times, t_motion_bpm = self._motion_side(teacher_kpts)
                if t_motion_times:
                    ratio_t = (t_motion_bpm / t_bpm_ref) if t_bpm_ref else 1.0
                    t_hit, t_avg, _, t_off, _, _ = self._align_metrics(
                        t_beats_ref, t_motion_times, tol_s, tempo_ratio=ratio_t)
                    t_tempo = self._tempo_consistency(t_bpm_ref, t_motion_bpm)
                    teacher_metrics = {
                        "hit_rate": t_hit, "avg_offset_ms": t_avg,
                        "global_offset_ms": t_off,
                        "tempo_consistency": t_tempo,
                        "motion_bpm": round(float(t_motion_bpm), 1),
                        "rhythm_score": self._rhythm_score(t_hit, t_tempo, t_avg),
                    }

            result.update({
                "available": True,
                "message": "",
                "music_source": music_source,
                "rhythm_score": self._rhythm_score(hit_rate, tempo_cons, avg_ms),
                "music_bpm": round(float(music_bpm), 1),
                "motion_bpm": round(float(s_motion_bpm), 1),
                "tempo_consistency": tempo_cons,
                "beat_hit_rate": hit_rate,
                "matched_beats": hits,
                "total_music_beats": n_eval,
                "avg_offset_ms": avg_ms,
                "global_offset_ms": g_off_ms,
                "segment_offsets_ms": seg_offs,
                "teacher": teacher_metrics,
                "chart": self._build_chart(student_kpts, music_beats, s_motion_times),
            })
            print(f"[RhythmAnalyzer] 节奏对齐: 得分{result['rhythm_score']} "
                  f"音乐{result['music_bpm']}BPM 动作{result['motion_bpm']}BPM "
                  f"命中率{hit_rate * 100:.0f}% 平均偏差{avg_ms}ms "
                  f"全局偏移{g_off_ms}ms")
            return result
        except Exception as e:
            # 节奏分析异常隔离：不阻断主评分流水线
            print(f"[RhythmAnalyzer] 节奏分析异常（已降级跳过）: {e}")
            result["message"] = "节奏对齐分析异常，已跳过"
            return result

    def _build_chart(self, student_kpts, music_beats, motion_beat_times):
        """图表数据：动作能量曲线 + 音乐节拍 + 动作节拍"""
        energy, _peaks, _bpm = self.extract_motion_beats(student_kpts)
        times, values = self._downsample_curve(energy)
        return {
            "times": times,
            "energy": values,
            "music_beats": [round(float(b), 3) for b in music_beats[:MAX_BEAT_POINTS]],
            "motion_beats": [round(float(b), 3) for b in motion_beat_times[:MAX_BEAT_POINTS]],
        }
