<template>
  <div class="live-root">
    <!-- 实时状态条：与视频播放状态毫秒级联动 -->
    <div class="live-status">
      <span class="live-dot" :class="{ on: playing }"></span>
      <span class="st">{{ playing ? '播放中' : '已暂停' }}</span>
      <span class="st">时间 {{ formatTime(currentTime) }}</span>
      <template v-if="curFrame">
        <span class="st">总偏差 <b :style="{ color: devColor(curFrame.d) }">{{ deg(curFrame.d) }}°</b></span>
        <span class="st" v-if="curWorstPart">最大偏差部位：
          <b :style="{ color: devColor(curWorstPart.val) }">{{ curWorstPart.name }} {{ deg(curWorstPart.val) }}°</b>
        </span>
      </template>
      <span class="st tip" v-else>暂无逐帧数据，重新评分后生成</span>
    </div>

    <div v-if="frames && frames.length" class="live-charts">
      <div ref="winRef" class="live-win"></div>
      <div ref="partRef" class="live-part"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  frames: { type: Array, default: () => [] },   // [{t, d, p:{部位:偏差}}]
  currentTime: { type: Number, default: 0 },     // 教师时间轴秒
  playing: { type: Boolean, default: false }
})
const emit = defineEmits(['seek'])

const PART_ORDER = ['左上肢', '右上肢', '左下肢', '右下肢', '躯干', '头颈']

const winRef = ref(null)
const partRef = ref(null)
let winChart = null
let partChart = null

const deg = (d) => Math.round((d || 0) * 180)
const devColor = (d) => {
  const g = (d || 0) * 180
  if (g >= 30) return '#c13a2a'
  if (g >= 15) return '#c89a2e'
  return '#2c9678'
}
// 0.1秒精度展示，直观体现≤200ms联动延迟
const formatTime = (sec) => {
  if (sec == null || isNaN(sec)) return '00:00.0'
  const m = Math.floor(sec / 60)
  const s = (sec % 60).toFixed(1)
  return `${String(m).padStart(2, '0')}:${s.padStart(4, '0')}`
}

// 当前时间对应的最近帧（二分查找）
const curFrame = computed(() => {
  const arr = props.frames
  if (!arr || !arr.length) return null
  let lo = 0, hi = arr.length - 1
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1
    if (arr[mid].t <= props.currentTime) lo = mid; else hi = mid - 1
  }
  return arr[lo]
})

const curWorstPart = computed(() => {
  const f = curFrame.value
  if (!f || !f.p) return null
  let name = null, val = -1
  for (const [k, v] of Object.entries(f.p)) {
    if (v > val) { val = v; name = k }
  }
  return name ? { name, val } : null
})

const winOption = (t) => {
  const lo = Math.max(0, t - 4), hi = t + 1
  const data = props.frames
    .filter(f => f.t >= lo && f.t <= hi)
    .map(f => [f.t, +(f.d * 180).toFixed(1)])
  return {
    animation: false,
    grid: { left: 44, right: 14, top: 26, bottom: 24 },
    title: { text: '实时偏差曲线（°）', textStyle: { fontSize: 12, color: '#6b5a4a' }, left: 4, top: 2 },
    xAxis: { type: 'value', min: lo, max: hi, axisLabel: { formatter: (v) => v.toFixed(1) + 's', fontSize: 10 } },
    yAxis: { type: 'value', min: 0, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', data, showSymbol: false, smooth: 0.2,
      lineStyle: { width: 2, color: '#1685a9' }, areaStyle: { opacity: 0.12, color: '#1685a9' },
      markLine: {
        symbol: 'none', animation: false,
        lineStyle: { color: '#c13a2a', width: 2 },
        label: { show: false },
        data: [{ xAxis: t }]
      }
    }]
  }
}

const partOption = (f) => {
  const vals = PART_ORDER.map(p => +(((f && f.p && f.p[p]) || 0) * 180).toFixed(1))
  return {
    animation: false,
    grid: { left: 56, right: 30, top: 26, bottom: 24 },
    title: { text: '当前帧部位偏差（°）', textStyle: { fontSize: 12, color: '#6b5a4a' }, left: 4, top: 2 },
    xAxis: { type: 'value', min: 0, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: PART_ORDER, axisLabel: { fontSize: 11 } },
    series: [{
      type: 'bar', data: vals.map(v => ({ value: v, itemStyle: { color: devColor(v / 180) } })),
      barWidth: 12,
      label: { show: true, position: 'right', fontSize: 10, formatter: '{c}°' }
    }]
  }
}

// 10Hz prop更新 → 刷新图表（setOption小数据量，延迟远低于200ms要求）
watch(() => props.currentTime, (t) => {
  if (!winChart || !props.frames.length) return
  winChart.setOption(winOption(t))
  partChart.setOption(partOption(curFrame.value))
})

const initCharts = async () => {
  if (winChart || !props.frames.length) return
  await nextTick()
  if (!winRef.value) return
  winChart = echarts.init(winRef.value)
  partChart = echarts.init(partRef.value)
  winChart.setOption(winOption(props.currentTime))
  partChart.setOption(partOption(curFrame.value))
  // 点击图表任意位置跳转视频（zrender全局点击+像素→时间换算，折线无symbol也可点）
  winChart.getZr().on('click', (e) => {
    if (e.offsetX == null) return
    const t = winChart.convertFromPixel({ xAxisIndex: 0 }, e.offsetX)
    if (t != null && !isNaN(t)) emit('seek', Math.max(0, t))
  })
}

// frames异步到达时补建图表（nextTick确保v-if容器已有宽高）
watch(() => props.frames.length, initCharts)

onMounted(() => {
  initCharts()
  window.addEventListener('resize', resize)
})

const resize = () => { winChart && winChart.resize(); partChart && partChart.resize() }

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  winChart && winChart.dispose()
  partChart && partChart.dispose()
})
</script>

<style scoped>
.live-root { height: 100%; display: flex; flex-direction: column; min-height: 0; }
.live-status { display: flex; flex-wrap: wrap; gap: 4px 14px; align-items: center; margin-bottom: 6px; font-size: 12px; color: #6b5a4a; }
.live-dot { width: 10px; height: 10px; border-radius: 50%; background: #a89880; display: inline-block; }
.live-dot.on { background: #2c9678; animation: pulse 1.2s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.35; } }
.st b { font-weight: 600; }
.tip { color: #a89880; }
.live-charts { flex: 1; display: flex; flex-direction: column; gap: 2px; min-height: 0; }
.live-win { flex: 1.15; min-height: 170px; }
.live-part { flex: 1; min-height: 150px; }
</style>
