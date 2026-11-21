<template>
  <div>
    <!-- 核心指标区：总分仪表盘 / 维度雷达 / 部位得分 -->
    <div class="ov-grid">
      <div ref="gaugeRef" class="ov-chart"></div>
      <div ref="radarRef" class="ov-chart"></div>
      <div ref="barRef" class="ov-chart"></div>
    </div>

    <!-- 趋势概览：全时间轴偏差曲线 + dataZoom，点击跳转视频 -->
    <div v-if="frames && frames.length" ref="trendRef" class="ov-trend"></div>

    <!-- 改进建议（数据口径与部位得分一致） -->
    <div v-if="report.feedback && report.feedback.length" class="fb-list">
      <div v-for="f in report.feedback" :key="f.part" class="fb-item">
        <el-tag :type="tagType(f.score)" size="small">{{ f.level }}</el-tag>
        <span class="fb-part">{{ f.part }}</span>
        <span class="fb-score" :style="{ color: scoreColor(f.score) }">{{ f.score }}分</span>
        <span class="fb-desc">{{ f.description }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  report: { type: Object, required: true },        // ai_report解析对象
  frames: { type: Array, default: () => [] },      // 逐帧偏差序列
  worstFrames: { type: Array, default: () => [] }  // 偏差最大帧元数据
})
const emit = defineEmits(['seek'])

const PART_ORDER = ['左上肢', '右上肢', '左下肢', '右下肢', '躯干', '头颈']
const gaugeRef = ref(null)
const radarRef = ref(null)
const barRef = ref(null)
const trendRef = ref(null)
let charts = []

const scoreColor = (s) => s >= 85 ? '#2c9678' : s >= 75 ? '#1685a9' : s >= 60 ? '#c89a2e' : '#c13a2a'
const tagType = (s) => s >= 85 ? 'success' : s >= 75 ? 'primary' : s >= 60 ? 'warning' : 'danger'

const buildGauge = (el) => {
  const c = echarts.init(el)
  c.setOption({
  series: [{
    type: 'gauge', startAngle: 210, endAngle: -30, min: 0, max: 100,
    radius: '95%', center: ['50%', '58%'],
    progress: { show: true, width: 12, itemStyle: { color: scoreColor(props.report.totalScore) } },
    axisLine: { lineStyle: { width: 12, color: [[1, '#e4d9c3']] } },
    axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
    pointer: { show: false }, anchor: { show: false },
    title: { fontSize: 13, color: '#6b5a4a', offsetCenter: [0, '32%'] },
    detail: { fontSize: 30, fontWeight: 600, color: scoreColor(props.report.totalScore), offsetCenter: [0, '-4%'], formatter: '{value}' },
    data: [{ value: props.report.totalScore, name: 'AI综合总分' }]
  }]
  })
  return c
}

const buildRadar = (el) => {
  const ds = props.report.dimensionScores || {}
  const names = Object.keys(ds)
  const c = echarts.init(el)
  c.setOption({
    title: { text: '评价维度', textStyle: { fontSize: 12, color: '#6b5a4a' }, left: 'center', top: 2 },
    radar: {
      indicator: names.map(n => ({ name: n, max: 100 })),
      radius: '62%', center: ['50%', '56%'],
      axisName: { fontSize: 11, color: '#6b5a4a' },
      splitArea: { areaStyle: { color: ['#f7f1e3', '#f1e9d8'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: names.map(n => ds[n]),
        areaStyle: { color: 'rgba(64,158,255,0.25)' },
        lineStyle: { color: '#1685a9' }, itemStyle: { color: '#1685a9' }
      }]
    }]
  })
  return c
}

const buildBar = (el) => {
  const ps = props.report.partScores || {}
  const vals = PART_ORDER.map(p => ps[p] ? ps[p].score : 0)
  const c = echarts.init(el)
  c.setOption({
    title: { text: '身体部位得分', textStyle: { fontSize: 12, color: '#6b5a4a' }, left: 'center', top: 2 },
    grid: { left: 56, right: 34, top: 28, bottom: 24 },
    xAxis: { type: 'value', min: 0, max: 100, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: PART_ORDER, axisLabel: { fontSize: 11 } },
    series: [{
      type: 'bar', barWidth: 12,
      data: vals.map(v => ({ value: v, itemStyle: { color: scoreColor(v) } })),
      label: { show: true, position: 'right', fontSize: 10 }
    }]
  })
  return c
}

const buildTrend = (el) => {
  const chart = echarts.init(el)
  const data = props.frames.map(f => [f.t, +(f.d * 180).toFixed(1)])
  const worstMarks = props.worstFrames.map(w => ({
    coord: [w.teacher_time, +(w.deviation * 180).toFixed(1)],
    value: '#' + w.rank,
    itemStyle: { color: '#c13a2a' }
  }))
  chart.setOption({
    title: { text: '全过程偏差趋势（°）· 点击曲线跳转视频对应时刻', textStyle: { fontSize: 12, color: '#6b5a4a' }, left: 4, top: 2 },
    grid: { left: 44, right: 16, top: 30, bottom: 48 },
    tooltip: { trigger: 'axis', valueFormatter: (v) => v + '°' },
    xAxis: { type: 'value', min: 0, axisLabel: { formatter: (v) => v.toFixed(1) + 's', fontSize: 10 } },
    yAxis: { type: 'value', min: 0, axisLabel: { fontSize: 10 } },
    dataZoom: [
      { type: 'inside', throttle: 50 },
      { type: 'slider', height: 18, bottom: 6 }
    ],
    series: [{
      type: 'line', data, showSymbol: false, smooth: 0.2,
      lineStyle: { width: 1.5, color: '#1685a9' }, areaStyle: { opacity: 0.1, color: '#1685a9' },
      markPoint: { data: worstMarks, symbolSize: 34, label: { fontSize: 10, color: '#fff' } }
    }]
  })
  // 跨模块联动：点击趋势图任意位置（像素→时间换算）跳转全部视频
  chart.getZr().on('click', (e) => {
    if (e.offsetX == null) return
    const t = chart.convertFromPixel({ xAxisIndex: 0 }, e.offsetX)
    if (t != null && !isNaN(t)) emit('seek', Math.max(0, t))
  })
  return chart
}

onMounted(() => {
  charts = [buildGauge(gaugeRef.value), buildRadar(radarRef.value), buildBar(barRef.value)]
  if (trendRef.value && props.frames.length) charts.push(buildTrend(trendRef.value))
  window.addEventListener('resize', resize)
})

// 重新评分后报告对象替换 → 重建全部图表
watch(() => props.report, () => {
  charts.forEach(c => c.dispose())
  charts = [buildGauge(gaugeRef.value), buildRadar(radarRef.value), buildBar(barRef.value)]
  if (trendRef.value && props.frames.length) charts.push(buildTrend(trendRef.value))
})

// frames异步到达时补建趋势图（nextTick确保v-if容器已挂载且有宽高）
watch(() => props.frames.length, async (n) => {
  if (n > 0) {
    await nextTick()
    if (trendRef.value && !charts.some(c => c && c.getDom() === trendRef.value)) {
      charts.push(buildTrend(trendRef.value))
    }
  }
})

const resize = () => charts.forEach(c => c && c.resize())

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.forEach(c => c && c.dispose())
  charts = []
})
</script>

<style scoped>
.ov-grid { display: grid; grid-template-columns: 1fr 1fr 1.4fr; gap: 14px; }
.ov-chart { height: 215px; }
.ov-trend { height: 220px; margin-top: 8px; }
.fb-list { margin-top: 12px; border-top: 1px dashed #e4d9c3; padding-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.fb-item { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #6b5a4a; }
.fb-part { font-weight: 600; min-width: 56px; }
.fb-score { font-weight: 600; }
.fb-desc { color: #6b5a4a; }
@media (max-width: 1100px) { .ov-grid { grid-template-columns: 1fr; } }
</style>
