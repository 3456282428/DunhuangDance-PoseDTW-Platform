<template>
  <div class="ai-score-report">
    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      title="AI评分为辅助参考，最终成绩以教师评定为准" />

    <!-- 评分中 / 等待结果 -->
    <div v-if="showLoading" class="ai-loading">
      <el-skeleton :rows="5" animated />
      <p class="ai-loading-tip">AI评分进行中，约需1-3分钟，请耐心等待…</p>
    </div>

    <!-- 评分失败 -->
    <el-result v-else-if="status === '失败'" icon="error" title="AI评分失败" sub-title="请稍后重试或联系教师" />

    <!-- 评分报告 -->
    <template v-else-if="report">
      <!-- 顶部总分 -->
      <div class="ai-total">
        <el-progress type="dashboard" :percentage="totalPct" :width="110" :color="scoreColor">
          <template #default="{ percentage }">
            <span class="total-num" :style="{ color: scoreColor(percentage) }">{{ totalText }}</span>
            <span class="total-label">综合得分</span>
          </template>
        </el-progress>
        <div class="total-side">
          <p class="grade-text" :style="{ color: scoreColor(totalPct) }">{{ gradeText }}</p>
          <p class="grade-sub">舞蹈动作AI分析结果</p>
        </div>
      </div>

      <!-- 部位得分 -->
      <div class="ai-section">
        <h4>部位得分</h4>
        <template v-if="partScores.length">
          <div v-for="p in partScores" :key="p.label" class="part-row">
            <span class="part-name">{{ p.label }}</span>
            <el-progress class="part-bar" :percentage="p.score" :text-inside="true" :stroke-width="14"
              :color="scoreColor(p.score)" :format="() => p.score" />
            <span class="part-desc" :title="p.desc">{{ p.desc || '—' }}</span>
          </div>
        </template>
        <div v-else class="empty-hint">暂无部位得分数据</div>
      </div>

      <!-- 维度得分 -->
      <div class="ai-section">
        <h4>维度得分</h4>
        <template v-if="dimensions.length">
          <div class="dim-row">
            <div v-for="d in dimensions" :key="d.label" class="dim-item">
              <span class="dim-name">{{ d.label }}</span>
              <el-tag :color="scoreColor(d.score)" effect="dark" size="small" class="dim-tag">{{ d.score }} 分</el-tag>
            </div>
          </div>
        </template>
        <div v-else class="empty-hint">暂无维度得分数据</div>
      </div>

      <!-- 改进建议 -->
      <div class="ai-section">
        <h4>改进建议</h4>
        <template v-if="feedbacks.length">
          <div v-for="(f, i) in feedbacks" :key="i" class="fb-item">
            <el-tag :color="levelColor(f.level)" effect="dark" size="small" class="level-tag">{{ f.level || '建议' }}</el-tag>
            <el-tag v-if="f.part" size="small" type="info" effect="plain">{{ f.part }}</el-tag>
            <span class="fb-desc">{{ f.desc || '—' }}</span>
          </div>
        </template>
        <div v-else class="empty-hint">暂无改进建议</div>
      </div>

      <!-- 底部辅助信息 -->
      <div v-if="auxItems.length" class="aux-info">
        <span v-for="a in auxItems" :key="a.label">{{ a.label }}：{{ a.value }}</span>
      </div>
    </template>

    <!-- 空数据兜底 -->
    <el-empty v-else description="暂无AI评分数据" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  report: { type: Object, default: null },
  status: { type: String, default: '' }
})

const scoreColor = (v) => {
  const n = Number(v)
  if (isNaN(n)) return '#6b5a4a'
  if (n < 60) return '#c13a2a'
  if (n < 80) return '#b3542e'
  if (n < 90) return '#c89a2e'
  return '#2c9678'
}

const levelColor = (level) => {
  const s = String(level || '')
  if (s.includes('优秀')) return '#2c9678'
  if (s.includes('良好')) return '#c89a2e'
  if (s.includes('需改进')) return '#b3542e'
  if (s.includes('待改进')) return '#c13a2a'
  return '#6b5a4a'
}

const clampScore = (v) => {
  const n = Number(v)
  if (isNaN(n)) return 0
  return Math.min(100, Math.max(0, Math.round(n * 10) / 10))
}

const showLoading = computed(() => {
  if (props.status === '评分中') return true
  return !props.report && !!props.status && props.status !== '失败' && props.status !== '已完成'
})

const totalScore = computed(() => {
  const n = Number(props.report?.totalScore)
  return isNaN(n) ? null : n
})
const totalPct = computed(() => (totalScore.value == null ? 0 : Math.min(100, Math.max(0, totalScore.value))))
const totalText = computed(() => (totalScore.value == null ? '--' : Number(totalScore.value.toFixed(1))))
const gradeText = computed(() => {
  if (totalScore.value == null) return '—'
  const n = totalScore.value
  if (n >= 90) return '优秀'
  if (n >= 80) return '良好'
  if (n >= 60) return '中等'
  return '待提高'
})

const partScores = computed(() => {
  const raw = props.report?.partScores ?? props.report?.parts
  // 数组形式 [{part, score, description}]
  if (Array.isArray(raw)) {
    return raw.map((it, i) => ({
      label: it.part || it.partName || it.name || it.label || `部位${i + 1}`,
      score: clampScore(it.score ?? it.value),
      desc: it.desc || it.description || it.comment || ''
    }))
  }
  // 对象映射形式 {"左上肢": {score, description}}
  if (raw && typeof raw === 'object') {
    return Object.entries(raw).map(([label, v]) => ({
      label,
      score: clampScore(v && typeof v === 'object' ? (v.score ?? v.value) : v),
      desc: v && typeof v === 'object' ? (v.description || v.desc || '') : ''
    }))
  }
  return []
})

const dimensions = computed(() => {
  const raw = props.report?.dimensionScores ?? props.report?.dimensions
  // 数组形式 [{dimension, score}]
  if (Array.isArray(raw)) {
    return raw.map((it, i) => ({
      label: it.dimension || it.name || it.dimName || it.label || `维度${i + 1}`,
      score: clampScore(it.score ?? it.value)
    }))
  }
  // 对象映射形式 {"动作规范性": 87.2}
  if (raw && typeof raw === 'object') {
    return Object.entries(raw).map(([label, v]) => ({
      label,
      score: clampScore(v && typeof v === 'object' ? (v.score ?? v.value) : v)
    }))
  }
  return []
})

const feedbacks = computed(() => {
  const arr = props.report?.feedback ?? props.report?.feedbacks
  if (!Array.isArray(arr)) return []
  return arr.map((it) => ({
    level: String(it.level || ''),
    part: it.part || it.partName || '',
    desc: it.desc || it.description || it.content || it.advice || ''
  }))
})

const fmtNum = (v) => {
  const n = Number(v)
  return isNaN(n) ? String(v ?? '--') : n.toFixed(2)
}

const fmtDuration = (v) => {
  const n = Number(v)
  if (isNaN(n)) return String(v ?? '--')
  return n >= 1000 ? `${(n / 1000).toFixed(1)}s` : `${n}s`
}

const auxItems = computed(() => {
  const r = props.report || {}
  const items = []
  if (r.dtwDistance != null) items.push({ label: 'DTW距离', value: fmtNum(r.dtwDistance) })
  if (r.rhythmCost != null) items.push({ label: '节奏成本', value: fmtNum(r.rhythmCost) })
  if (r.processingTime != null || r.processDuration != null || r.duration != null) {
    items.push({ label: '处理耗时', value: fmtDuration(r.processingTime ?? r.processDuration ?? r.duration) })
  }
  return items
})
</script>

<style scoped>
.ai-loading-tip { margin: 10px 0 0; color: #6b5a4a; font-size: 13px; }
.ai-total { display: flex; align-items: center; gap: 20px; margin-bottom: 6px; }
.total-num { font-size: 28px; font-weight: 700; display: block; line-height: 1.2; }
.total-label { font-size: 12px; color: #6b5a4a; }
.total-side .grade-text { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.total-side .grade-sub { margin: 0; color: #6b5a4a; font-size: 12px; }
.ai-section { margin-top: 14px; }
.ai-section h4 { margin: 0 0 10px; font-size: 14px; color: #2c1810; }
.part-row { display: grid; grid-template-columns: 60px 1fr 170px; align-items: center; gap: 10px; margin-bottom: 8px; }
.part-name { font-size: 13px; color: #6b5a4a; }
.part-bar { flex: 1; }
.part-desc { font-size: 12px; color: #6b5a4a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dim-row { display: flex; flex-wrap: wrap; gap: 16px; }
.dim-item { display: flex; align-items: center; gap: 8px; }
.dim-name { font-size: 13px; color: #6b5a4a; }
.dim-tag { border: none; }
.fb-item { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.level-tag { border: none; flex-shrink: 0; }
.fb-desc { font-size: 13px; color: #6b5a4a; line-height: 1.6; }
.aux-info { margin-top: 14px; padding-top: 10px; border-top: 1px dashed #e4d9c3; display: flex; flex-wrap: wrap; gap: 16px; }
.aux-info span { font-size: 12px; color: #6b5a4a; }
.empty-hint { font-size: 13px; color: #6b5a4a; padding: 6px 0; }
</style>
