<template>
  <div class="rhythm-live-root">
    <div class="rl-head">
      <span class="rl-title">实时节奏对齐曲线</span>
      <span class="rl-meta">
        音乐 <b>{{ fmt(rhythm.music_bpm) }}</b> BPM ·
        动作 <b>{{ fmt(rhythm.motion_bpm) }}</b> BPM ·
        命中 <b>{{ pct(rhythm.beat_hit_rate) }}</b>
      </span>
    </div>
    <div ref="chartRef" class="rl-chart" @click="onSeek"></div>
    <!-- 实时进度层：已播放区域高亮 + 游标（随共享播放时钟100ms刷新） -->
    <div class="rl-progress" :style="{ width: pctPos + '%' }"></div>
    <div class="rl-cursor" :style="{ left: pctPos + '%' }">
      <span class="rl-cursor-time">{{ fmt(curTime) }}s</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 报告中的rhythm对象（须available且含chart数据）
  rhythm: { type: Object, required: true },
  // 共享播放时钟（frames时间轴，秒）
  currentTime: { type: Number, default: 0 },
  playing: { type: Boolean, default: false }
})
const emit = defineEmits(['seek'])

const chartRef = ref(null)
let chart = null

const chartData = computed(() => props.rhythm?.chart || {})
const duration = computed(() => {
  const t = chartData.value.times
  return t && t.length ? t[t.length - 1] : 0
})
const curTime = computed(() => Math.max(0, Math.min(props.currentTime || 0, duration.value)))
const pctPos = computed(() => duration.value > 0 ? (curTime.value / duration.value) * 100 : 0)

const fmt = (v) => (v == null || isNaN(Number(v))) ? '--' : Number(v).toFixed(1).replace(/\.0$/, '')
const pct = (v) => (v == null || isNaN(Number(v))) ? '--' : Math.round(Number(v) * 100) + '%'

const renderChart = () => {
  const c = chartData.value
  if (!chartRef.value || !c.times || !c.times.length) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    grid: { left: 30, right: 8, top: 6, bottom: 14 },
    xAxis: {
      type: 'value', min: 0, max: duration.value,
      axisLabel: { fontSize: 9, color: '#a89880', formatter: (v) => v.toFixed(0) },
      axisLine: { show: false }, axisTick: { show: false }, splitLine: { show: false }
    },
    yAxis: {
      type: 'value', min: 0, max: 1.3,
      axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false },
      splitLine: { show: false }
    },
    series: [
      {
        name: '动作能量', type: 'line',
        data: c.times.map((t, i) => [t, c.energy[i]]),
        symbol: 'none', smooth: true,
        lineStyle: { width: 1.2, color: '#b9aef9' },
        areaStyle: { color: 'rgba(124,108,246,0.10)' }, z: 1, silent: true
      },
      {
        name: '音乐节拍', type: 'scatter', symbol: 'diamond', symbolSize: 6,
        data: (c.music_beats || []).map(b => [b, 1.22]),
        itemStyle: { color: '#c13a2a' }, z: 3, silent: true
      },
      {
        name: '动作节拍', type: 'scatter', symbol: 'triangle', symbolSize: 5,
        data: (c.motion_beats || []).map(b => [b, 1.08]),
        itemStyle: { color: '#c89a2e' }, z: 2, silent: true
      }
    ]
  }, true)
}

// 点击寻址：横坐标按比例映射回时间轴
const onSeek = (e) => {
  if (duration.value <= 0 || !chartRef.value) return
  const rect = chartRef.value.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  emit('seek', ratio * duration.value)
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
.rhythm-live-root {
  position: relative;
  width: 100%;
  margin-top: 6px;
  border: 1px solid #e4d9c3;
  border-radius: 6px;
  background: #fafbfc;
  overflow: hidden;
}
.rl-head {
  display: flex; align-items: baseline; gap: 10px;
  padding: 5px 10px 0;
}
.rl-title { font-size: 11px; font-weight: 600; color: #7c6cf6; letter-spacing: 0.5px; }
.rl-meta { margin-left: auto; font-size: 10px; color: #6b5a4a; }
.rl-meta b { color: #6b5a4a; font-variant-numeric: tabular-nums; }
/* 固定高度：图表区110px，不随容器伸缩，避免纵向拉伸变形 */
.rl-chart { width: 100%; height: 110px; cursor: crosshair; }

/* 实时进度层（指针事件穿透，点击落到图表上寻址） */
.rl-progress {
  position: absolute; left: 30px; top: 22px; bottom: 14px;
  background: rgba(64, 158, 255, 0.06);
  border-right: none;
  pointer-events: none;
}
.rl-cursor {
  position: absolute; top: 22px; bottom: 14px; width: 2px;
  background: #1685a9;
  pointer-events: none;
  transition: left 0.1s linear;
}
.rl-cursor-time {
  position: absolute; top: -2px; left: 4px;
  font-size: 9px; color: #1685a9; white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
</style>
