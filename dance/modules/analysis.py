"""
模块：动作综合分析
基于评分结果与逐帧偏差序列，产出教学导向的结构化分析：
- 总体评价（等级 + 数据概述）
- 做得好的地方（带分数证据）
- 需要改进的地方（带分数与具体时间点证据）
- 训练建议（针对每个薄弱项的可执行练习，精确到时间点）
全部结论仅由评分数据推导，不引入主观臆测；数据不足时降级表述。
"""
import numpy as np

# 部位问题描述模板（偏差高时的典型表现）
PART_ISSUE = {
    "左上肢": "左臂动作角度与示范存在偏差，常见表现为伸展不到位、手肘弯曲或手位高低不一致",
    "右上肢": "右臂动作角度与示范存在偏差，常见表现为伸展不到位、手肘弯曲或手位高低不一致",
    "左下肢": "左下肢步伐与站位存在偏差，常见表现为出腿幅度不足、膝盖弯曲角度或脚位不准",
    "右下肢": "右下肢步伐与站位存在偏差，常见表现为出腿幅度不足、膝盖弯曲角度或脚位不准",
    "躯干": "躯干姿态控制存在偏差，常见表现为重心偏移、身体倾斜或核心稳定性不足",
    "头颈": "头颈姿态存在偏差，常见表现为头部朝向、视线方向或颈部延展不到位",
}

# 部位训练建议模板（舞蹈教学导向，可执行）
PART_DRILL = {
    "左上肢": "对镜练习左臂定位：展开时保持肩—肘—腕成一条直线，每个手位定格3秒，再缓慢进入下一动作；重点按下方时间点回看骨架叠加视频对比",
    "右上肢": "对镜练习右臂定位：展开时保持肩—肘—腕成一条直线，每个手位定格3秒，再缓慢进入下一动作；重点按下方时间点回看骨架叠加视频对比",
    "左下肢": "手扶把杆进行左腿出腿/收腿分解练习，对照教师脚位逐拍定格，确认站位宽度与膝盖角度后再连贯练习",
    "右下肢": "手扶把杆进行右腿出腿/收腿分解练习，对照教师脚位逐拍定格，确认站位宽度与膝盖角度后再连贯练习",
    "躯干": "加强核心收紧练习，慢速体会重心在两腿间的转移，动作全程保持双肩水平、躯干中正",
    "头颈": "保持颈椎向上延展，目光随主动手方向移动，避免低头看脚；可在静止姿态下先练3组每次10秒",
}

# 维度训练建议
DIM_DRILL = {
    "动作规范性": "采用0.75倍慢速跟练：先逐帧对照教师预处理视频校正身体各部位位置，动作定型后再恢复原速",
    "节奏掌握度": "配合节拍器或教师原视频口令练习，先在关键转折拍上用停顿确认节奏，再逐步取消停顿连贯完成",
    "整体协调性": "先单独强化薄弱部位，再做上下肢配合的分解组合练习，熟练后配合音乐完整完成",
}

PART_ORDER = ["左上肢", "右上肢", "左下肢", "右下肢", "躯干", "头颈"]
DIM_ORDER = ["动作规范性", "节奏掌握度", "整体协调性"]


class MovementAnalyzer:
    """动作综合分析器：输入评分结果+逐帧偏差，输出结构化分析"""

    def analyze(self, scores, frames_payload=None,
                dtw_distance=None, rhythm_cost=None):
        """
        scores: DanceScorer输出（part_scores/dimension_scores/total_score）
        frames_payload: {"fps", "frames":[{"t","d","p":{部位:偏差}}]}
        """
        total = float(scores.get("total_score", 0))
        part_scores = scores.get("part_scores", {})
        dim_scores = scores.get("dimension_scores", {})

        # 逐帧统计：每部位均值/峰值时间点/热点时间点
        part_stats = self._part_frame_stats(frames_payload)
        avg_dev, early_dev, late_dev = self._overall_stats(frames_payload)

        level = self._level(total)
        best_part = min(part_scores.items(),
                        key=lambda kv: -kv[1]["score"])[0] if part_scores else None
        worst_part = min(part_scores.items(),
                         key=lambda kv: kv[1]["score"])[0] if part_scores else None

        strengths = self._strengths(part_scores, dim_scores)
        weaknesses = self._weaknesses(part_scores, dim_scores, part_stats)
        suggestions = self._suggestions(weaknesses, part_stats, early_dev, late_dev)

        summary = self._summary(
            total, level, best_part, worst_part, part_scores, dim_scores,
            avg_dev, early_dev, late_dev, rhythm_cost)

        return {
            "level": level,
            "summary": summary,
            "strengths": strengths,
            "weaknesses": weaknesses,
            "suggestions": suggestions,
            "stats": {
                "bestPart": best_part,
                "worstPart": worst_part,
                "avgDeviationDeg": round(avg_dev * 180, 1) if avg_dev is not None else None,
                "earlyDeviationDeg": round(early_dev * 180, 1) if early_dev is not None else None,
                "lateDeviationDeg": round(late_dev * 180, 1) if late_dev is not None else None,
                "rhythmCost": round(float(rhythm_cost), 4) if rhythm_cost is not None else None,
                "dtwDistance": round(float(dtw_distance), 4) if dtw_distance is not None else None,
            },
        }

    # ---------- 统计 ----------
    def _part_frame_stats(self, frames_payload):
        """每部位：平均偏差、峰值时间点、热点时间点（>均值+0.5σ，间隔≥0.8s，最多3个）"""
        stats = {}
        if not frames_payload:
            return stats
        frames = frames_payload.get("frames", [])
        for part in PART_ORDER:
            series = [(f["t"], f.get("p", {}).get(part))
                      for f in frames if f.get("p", {}).get(part) is not None]
            if not series:
                continue
            vals = np.array([v for _, v in series])
            mean, std = float(np.mean(vals)), float(np.std(vals))
            threshold = mean + 0.5 * std
            hotspots = []
            for t, v in sorted(series, key=lambda x: -x[1]):
                if v <= threshold:
                    break
                if all(abs(t - h) >= 0.8 for h in hotspots):
                    hotspots.append(t)
                if len(hotspots) >= 3:
                    break
            peak_t = max(series, key=lambda x: x[1])[0]
            stats[part] = {
                "avg": mean,
                "peak_time": peak_t,
                "hotspots": sorted(hotspots),
            }
        return stats

    def _overall_stats(self, frames_payload):
        if not frames_payload:
            return None, None, None
        ds = [f["d"] for f in frames_payload.get("frames", []) if "d" in f]
        if not ds:
            return None, None, None
        n = len(ds)
        k = max(1, n // 3)
        return (
            float(np.mean(ds)),
            float(np.mean(ds[:k])),
            float(np.mean(ds[-k:])),
        )

    # ---------- 等级 ----------
    @staticmethod
    def _level(total):
        if total >= 90:
            return "优秀"
        if total >= 80:
            return "良好"
        if total >= 70:
            return "合格"
        if total >= 60:
            return "待提高"
        return "需重点练习"

    # ---------- 优点（≥85） ----------
    def _strengths(self, part_scores, dim_scores):
        items = []
        for part in PART_ORDER:
            ps = part_scores.get(part)
            if ps and ps["score"] >= 85:
                items.append({
                    "type": "部位", "name": part, "score": ps["score"],
                    "comment": f"{part}动作与示范高度一致，得分{ps['score']}分，姿态控制稳定",
                })
        for dim in DIM_ORDER:
            s = dim_scores.get(dim)
            if s is not None and s >= 85:
                items.append({
                    "type": "维度", "name": dim, "score": s,
                    "comment": f"{dim}表现突出，得分{s}分",
                })
        items.sort(key=lambda x: -x["score"])
        # 若无≥85项，取最高部位作为相对优势（如实标注"相对较好"）
        if not items and part_scores:
            name, ps = max(part_scores.items(), key=lambda kv: kv[1]["score"])
            if ps["score"] >= 75:
                items.append({
                    "type": "部位", "name": name, "score": ps["score"],
                    "comment": f"{name}是本次完成相对较好的部位，得分{ps['score']}分",
                })
        return items[:4]

    # ---------- 不足（<80） ----------
    def _weaknesses(self, part_scores, dim_scores, part_stats):
        items = []
        for part in PART_ORDER:
            ps = part_scores.get(part)
            if ps and ps["score"] < 80:
                st = part_stats.get(part)
                items.append({
                    "type": "部位", "name": part, "score": ps["score"],
                    "comment": PART_ISSUE.get(part, "动作角度与示范存在偏差"),
                    "avgDeg": round(st["avg"] * 180, 1) if st else None,
                    "timePoints": st["hotspots"] if st else [],
                })
        for dim in DIM_ORDER:
            s = dim_scores.get(dim)
            if s is not None and s < 80:
                items.append({
                    "type": "维度", "name": dim, "score": s,
                    "comment": self._dim_issue(dim),
                    "avgDeg": None, "timePoints": [],
                })
        items.sort(key=lambda x: x["score"])
        return items

    @staticmethod
    def _dim_issue(dim):
        if dim == "动作规范性":
            return "整体姿态与示范仍有空间位置偏差，动作规范性需要加强"
        if dim == "节奏掌握度":
            return "动作与音乐节拍的吻合度不足，存在抢拍或拖拍，时序对齐弯曲较大"
        if dim == "整体协调性":
            return "各部位完成质量不均衡，整体配合的协调性需要加强"
        return "该维度需要加强"

    # ---------- 建议 ----------
    def _suggestions(self, weaknesses, part_stats, early_dev, late_dev):
        out = []
        seen_targets = set()
        for w in weaknesses:
            if w["type"] != "部位":
                continue
            part = w["name"]
            st = part_stats.get(part)
            out.append({
                "target": part,
                "action": PART_DRILL.get(part, "对照教师示范分解练习该部位动作"),
                "timePoints": st["hotspots"] if st else [],
            })
            seen_targets.add(part)
        for w in weaknesses:
            if w["type"] == "维度" and w["name"] in DIM_DRILL:
                out.append({"target": w["name"], "action": DIM_DRILL[w["name"]],
                            "timePoints": []})
        # 开头/收尾稳定性建议（前段偏差显著大于后段或反之）
        if early_dev is not None and late_dev is not None:
            if early_dev > late_dev * 1.3 and early_dev > 0.02:
                out.append({
                    "target": "开场姿态",
                    "action": "开场准备阶段偏差明显大于收尾，建议练习前先保持起始姿态5秒再启动动作，强化起势的稳定性",
                    "timePoints": [0.0],
                })
            elif late_dev > early_dev * 1.3 and late_dev > 0.02:
                out.append({
                    "target": "收尾控制",
                    "action": "动作后段偏差明显增大，通常与耐力下降有关，建议加强后半段核心控制练习，并在结束造型上定格保持3秒",
                    "timePoints": [],
                })
        if not out:
            out.append({
                "target": "整体巩固",
                "action": "整体完成质量较好，建议按原速反复练习以巩固肌肉记忆，并尝试合乐完整表演",
                "timePoints": [],
            })
        return out[:6]

    # ---------- 总评 ----------
    def _summary(self, total, level, best_part, worst_part,
                 part_scores, dim_scores, avg_dev, early_dev, late_dev,
                 rhythm_cost):
        parts = [f"综合得分{total:.1f}分，动作完成等级为「{level}」"]
        if best_part and worst_part and best_part != worst_part:
            parts.append(f"其中{best_part}表现最好（{part_scores[best_part]['score']}分），"
                         f"{worst_part}提升空间最大（{part_scores[worst_part]['score']}分）")
        if avg_dev is not None:
            parts.append(f"全过程关节角度平均偏差约{avg_dev * 180:.0f}度")
        rhythm = dim_scores.get("节奏掌握度")
        if rhythm is not None and rhythm < 80:
            parts.append("节奏与示范存在时序偏差")
        if early_dev is not None and late_dev is not None:
            if early_dev > late_dev * 1.3 and early_dev > 0.02:
                parts.append("开场阶段偏差相对集中")
            elif late_dev > early_dev * 1.3 and late_dev > 0.02:
                parts.append("收尾阶段稳定性不足")
        return "；".join(parts) + "。"
