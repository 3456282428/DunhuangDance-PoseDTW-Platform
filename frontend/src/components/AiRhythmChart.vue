<template>
  <div class="rhythm-root">
    <!-- 不可用/降级状态 -->
    <el-empty v-if="!rhythm || !rhythm.available" :description="emptyText" :image-size="70" />

    <template v-else>
      <div class="rhythm-layout">
        <!-- 左：节拍-能量对齐图（近方形） -->
        <div class="chart-wrap">
          <div ref="chartRef" class="rhythm-chart" @click="onChartClick"></div>
          <p class="chart-tip">点击图上任意位置可跳转五路视频到对应时刻</p>
        </div>

        <!-- 右：节奏对齐指标 -->
        <div class="stat-panel">
          <div class="score-box" :style="{ borderColor: scoreColor }">
            <span class="score-label">节奏对齐得分</span>
            <b class="score-val" :style="{ color: scoreColor }">{{ fmt(rhythm.rhythm_score) }}</b>
          </div>
          <div class="stat-grid">
            <div class="stat">
              <span class="lb">音乐节拍</span>
              <b class="val">{{ fmt(rhythm.music_bpm) }} <i>BPM</i></b>
            </div>
            <div class="stat">
              <span class="lb">动作节拍</span>
              <b class="val">{{ fmt(rhythm.motion_bpm) }} <i>BPM</i></b>
            </div>
            <div class="stat">
              <span class="lb">节拍命中率</span>
              <b class="val">{{ pct(rhythm.beat_hit_rate) }}<i>（{{ rhythm.matched_beats }}/{{ rhythm.total_music_beats }}）</i></b>
            </div>
            <div class="stat">
              <span class="lb">平均踩拍偏差</span>
              <b class="val">{{ fmt(rhythm.avg_offset_ms) }} <i>ms</i></b>
            </div>
            <div class="stat">
              <span class="lb">速度一致性</span>
              <b class="val">{{ pct(rhythm.tempo_consistency) }}</b>
            </div>
            <div class="stat">
              <span class="lb">命中容差</span>
              <b class="val">±{{ rhythm.tolerance_ms }} <i>ms</i></b>
            </div>
          </div>
          <p class="music-src">音乐来源：{{ rhythm.music_source || '视频音轨' }}</p>
          <!-- 教师基线对比 -->
          <div v-if="rhythm.teacher" class="teacher-base">
            <span class="tb-title">教师示范基线</span>
            <span class="tb-item">命中率 <b>{{ pct(rhythm.teacher.hit_rate) }}</b></span>
            <span class="tb-item">偏差 <b>{{ fmt(rhythm.teacher.avg_offset_ms) }}ms</b></span>
            <span class="tb-item">动作 <b>{{ fmt(rhythm.teacher.motion_bpm) }}BPM</b></span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 报告中的rhythm对象：{available, rhythm_score, music_bpm, motion_bpm, beat_hit_rate,
  //  matched_beats, total_music_beats, avg_offset_ms, tempo_consistency, tolerance_ms,
  //  music_source, teacher, chart:{times, energy, music_beats, motion_beats}}
  rhythm: { type: Object, default: null }
})
const emit = defineEmits(['seek'])

const chartRef = ref(null)
let chart = null

const emptyText = computed(() =>
  (props.rhythm && props.rhythm.message) || '暂无节奏对齐数据，重新评分后生成')

const fmt = (v) => (v == null || isNaN(Number(v))) ? '--' : Number(v).toFixed(1).replace(/\.0$/, '')
const pct = (v) => (v == null || isNaN(Number(v))) ? '--' : Math.round(Number(v) * 100) + '%'

const scoreColor = computed(() => {
  const s = Number(props.rhythm?.rhythm_score)
  if (isNaN(s)) return '#6b5a4a'
  if (s >= 85) return '#2c9678'
  if (s >= 70) return '#1685a9'
  if (s >= 55) return '#c89a2e'
  return '#c13a2a'
})

const buildOption = () => {
  const c = props.rhythm?.chart
  if (!c || !c.times || !c.times.length) return null
  return {
    grid: { left: 34, right: 12, top: 30, bottom: 26 },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const t = params[0]?.axisValue
        const lines = [`时间 ${fmt(t)}s`]
        params.forEach(p => {
          if (p.seriesType === 'line') lines.push(`${p.marker}动作能量 ${(Number(p.value) * 100).toFixed(0)}%`)
        })
        const mb = (c.music_beats || []).some(b => Math.abs(b - t) < 0.08)
        const mo = (c.motion_beats || []).some(b => Math.abs(b - t) < 0.08)
        if (mb) lines.push('● 音乐节拍')
        if (mo) lines.push('▲ 动作节拍')
        return lines.join('<br/>')
      }
    },
    legend: { top: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 11, color: '#6b5a4a' } },
    xAxis: {
      type: 'value', min: 0, max: c.times[c.times.length - 1],
      axisLabel: { fontSize: 10, color: '#6b5a4a', formatter: (v) => v.toFixed(0) },
      axisLine: { lineStyle: { color: '#d4c5a9' } }, axisTick: { show: false }
    },
    yAxis: {
      type: 'value', min: 0, max: 1.25, interval: 0.25,
      axisLabel: { fontSize: 10, color: '#6b5a4a', formatter: (v) => (v <= 1 ? (v * 100) + '%' : '') },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        name: '动作能量', type: 'line',
        data: c.times.map((t, i) => [t, c.energy[i]]),
        symbol: 'none', smooth: true,
        lineStyle: { width: 1.6, color: '#7c6cf6' },
        areaStyle: { color: 'rgba(124,108,246,0.12)' }, z: 1
      },
      {
        name: '音乐节拍', type: 'scatter', symbol: 'diamond', symbolSize: 9,
        data: (c.music_beats || []).map(b => [b, 1.18]),
        itemStyle: { color: '#c13a2a' }, z: 3
      },
      {
        name: '动作节拍', type: 'scatter', symbol: 'triangle', symbolSize: 8,
        data: (c.motion_beats || []).map(b => [b, 1.06]),
        itemStyle: { color: '#c89a2e' }, z: 2
      }
    ]
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const opt = buildOption()
  if (opt) chart.setOption(opt, true)
}

// 图表点击 → 按能量曲线时间轴比例映射并寻址五路视频
const onChartClick = (e) => {
  const c = props.rhythm?.chart
  if (!c || !c.times || !c.times.length || !chart) return
  const px = e.offsetX
  const rect = chartRef.value.getBoundingClientRect()
  const model = chart.convertFromPixel({ xAxisIndex: 0 }, px)
  if (model == null || isNaN(model)) return
  const t = Math.max(0, Math.min(Number(model), c.times[c.times.length - 1]))
  emit('seek', t)
}

watch(() => props.rhythm && props.rhythm.available, () => {
  nextTick(() => {
    renderChart()
    // 兜底重渲染（容器首帧宽度可能为0）
    setTimeout(renderChart, 240)
  })
}, { immediate: true })

const resize = () => chart && chart.resize()
onMounted(() => {
  window.addEventListener('resize', resize)
  nextTick(renderChart)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
.rhythm-root { width: 100%; }
.rhythm-layout { display: flex; gap: 16px; align-items: stretch; flex-wrap: wrap; }

/* 图表区：近方形呈现 */
.chart-wrap { flex: 0 0 auto; width: 100%; max-width: 430px; }
.rhythm-chart { width: 100%; height: 320px; }
.chart-tip { margin: 2px 0 0; font-size: 11px; color: #a89880; text-align: center; }

/* 指标面板 */
.stat-panel { flex: 1 1 260px; min-width: 250px; display: flex; flex-direction: column; gap: 10px; }
.score-box { border: 1px solid #e4d9c3; border-radius: 8px; padding: 10px 14px; display: flex; align-items: baseline; gap: 10px; background: #fafbfc; }
.score-label { font-size: 12px; color: #6b5a4a; }
.score-val { font-size: 30px; font-variant-numeric: tabular-nums; line-height: 1; }

.stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.stat { border: 1px solid #e4d9c3; border-radius: 8px; padding: 8px 10px; }
.stat .lb { display: block; font-size: 11px; color: #6b5a4a; letter-spacing: 0.5px; }
.stat .val { display: block; margin-top: 2px; font-size: 16px; color: #2c1810; font-variant-numeric: tabular-nums; }
.stat .val i { font-style: normal; font-size: 11px; color: #6b5a4a; font-weight: 400; }

.music-src { margin: 0; font-size: 12px; color: #6b5a4a; }

.teacher-base { display: flex; flex-wrap: wrap; gap: 4px 14px; align-items: center; padding: 8px 10px; border: 1px dashed #d9ecff; border-radius: 8px; background: #f4f9ff; }
.tb-title { font-size: 12px; font-weight: 600; color: #2c9678; }
.tb-item { font-size: 12px; color: #6b5a4a; }
.tb-item b { color: #2c1810; font-variant-numeric: tabular-nums; }

@media (max-width: 900px) {
  .chart-wrap { max-width: none; }
  .rhythm-chart { height: 260px; }
}
</style>
