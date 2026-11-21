<template>
  <div class="skel-wrap">
    <!-- 控制条：播放/暂停 + 帧进度 + 速度 + 帧计数 + 复位视角 -->
    <div class="skel-toolbar">
      <el-button size="small" circle @click="togglePlay">
        <el-icon><VideoPause v-if="playing" /><VideoPlay v-else /></el-icon>
      </el-button>
      <el-slider class="skel-slider" :model-value="frameIdx" :max="Math.max(0, frameCount - 1)"
        :show-tooltip="false" @input="onSeek" />
      <el-radio-group v-model="speed" size="small">
        <el-radio-button :value="0.5">0.5x</el-radio-button>
        <el-radio-button :value="1">1x</el-radio-button>
        <el-radio-button :value="2">2x</el-radio-button>
      </el-radio-group>
      <span class="skel-count">{{ frameIdx + 1 }}/{{ frameCount }} · {{ timeText }}</span>
      <el-button size="small" plain @click="resetView">复位视角</el-button>
    </div>

    <!-- 骨架画布（近方形） -->
    <div ref="boxRef" class="skel-canvas-box">
      <canvas ref="canvasRef" class="skel-canvas" />
      <div class="skel-legend">
        <span><i style="background:#f85149" />头</span>
        <span><i style="background:#8c959f" />躯干</span>
        <span><i style="background:#3fb950" />左臂</span>
        <span><i style="background:#54aeff" />右臂</span>
        <span><i style="background:#d29922" />左腿</span>
        <span><i style="background:#a371f7" />右腿</span>
      </div>
      <div class="skel-hint">左键拖拽旋转 · 滚轮缩放</div>
      <div v-if="!frameCount" class="skel-empty">暂无骨架数据</div>
    </div>
  </div>
</template>

<script setup>
/**
 * 敦煌舞参考骨架动画播放器（零依赖Canvas自研渲染，复用KbGraph3D投影/交互模式）：
 * - 输入MediaPipe 33关键点序列（640×480像素坐标，未检测点为[0,0]需过滤）
 * - 归一化：肩髋四点中心为原点，躯干长度（全序列中位数）为单位尺度，y轴翻转向上
 * - 播放：RAF按fps×speed推进帧；拖拽旋转/滚轮缩放；双驱动循环保证后台标签页可渲染
 */
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

const props = defineProps({
  frames: { type: Array, default: () => [] },   // (T, 33, 2)
  fps: { type: Number, default: 30 },
  confidences: { type: Array, default: null },  // (T, 33) 预留
})

/* MediaPipe关键点索引（合成数据仅含 0, 11-16, 23-28，其余为[0,0]无效点） */
const JOINT_PART = {
  0: 'head',
  11: 'torso', 12: 'torso', 23: 'torso', 24: 'torso',
  13: 'larm', 15: 'larm', 14: 'rarm', 16: 'rarm',
  25: 'lleg', 27: 'lleg', 26: 'rleg', 28: 'rleg',
}
const PART_COLOR = {
  head: '#f85149', torso: '#8c959f',
  larm: '#3fb950', rarm: '#54aeff',
  lleg: '#d29922', rleg: '#a371f7',
}
/* 骨架连线：躯干 / 左臂 / 右臂 / 左腿 / 右腿 */
const BONES = [
  [11, 12, 'torso'], [11, 23, 'torso'], [12, 24, 'torso'], [23, 24, 'torso'],
  [11, 13, 'larm'], [13, 15, 'larm'],
  [12, 14, 'rarm'], [14, 16, 'rarm'],
  [23, 25, 'lleg'], [25, 27, 'lleg'],
  [24, 26, 'rleg'], [26, 28, 'rleg'],
]
const VALID_IDS = Object.keys(JOINT_PART).map(Number)

/* ================= 数据归一化 ================= */
let normFrames = []  // 每帧: Map(idx -> {x,y,z})，肩髋中心为原点，躯干长为1，y向上
const frameCount = ref(0)

const preprocess = () => {
  const src = props.frames || []
  const T = src.length
  normFrames = []
  frameCount.value = T
  if (!T) return

  // 第一遍：每帧肩髋中心 + 躯干长度候选（肩中点-髋中点距离）
  const centers = [], torsoLens = []
  for (let t = 0; t < T; t++) {
    const f = src[t]
    let cx = 0, cy = 0, cn = 0
    const ids = [11, 12, 23, 24]
    const got = {}
    for (const i of ids) {
      const p = f[i]
      if (p && (p[0] !== 0 || p[1] !== 0)) { got[i] = p; cx += p[0]; cy += p[1]; cn++ }
    }
    centers.push(cn ? { x: cx / cn, y: cy / cn } : null)
    let len = 0
    if (got[11] && got[12] && got[23] && got[24]) {
      const smx = (got[11][0] + got[12][0]) / 2, smy = (got[11][1] + got[12][1]) / 2
      const hmx = (got[23][0] + got[24][0]) / 2, hmy = (got[23][1] + got[24][1]) / 2
      len = Math.hypot(smx - hmx, smy - hmy)
    }
    torsoLens.push(len)
  }
  // 全局尺度：躯干长度中位数（避免逐帧缩放抖动）
  const sorted = torsoLens.filter(v => v > 1).sort((a, b) => a - b)
  const scale = sorted.length ? sorted[Math.floor(sorted.length / 2)] : 1

  // 第二遍：归一化每帧有效点
  for (let t = 0; t < T; t++) {
    const f = src[t]
    const c = centers[t]
    const m = new Map()
    if (c) {
      for (const i of VALID_IDS) {
        const p = f[i]
        if (!p || (p[0] === 0 && p[1] === 0)) continue
        m.set(i, { x: (p[0] - c.x) / scale, y: -(p[1] - c.y) / scale, z: 0 })
      }
    }
    normFrames.push(m)
  }
}

watch(() => props.frames, () => {
  preprocess()
  frameIdx.value = 0
  playAcc = 0
}, { immediate: false })

/* ================= 播放控制 ================= */
const playing = ref(true)
const frameIdx = ref(0)
const speed = ref(1)
let playAcc = 0

const togglePlay = () => { playing.value = !playing.value }
const onSeek = (v) => { frameIdx.value = v; playAcc = 0; playing.value = false }
const timeText = computed(() => {
  const t = frameIdx.value / Math.max(1, props.fps)
  const mm = String(Math.floor(t / 60)).padStart(2, '0')
  const ss = String(Math.floor(t % 60)).padStart(2, '0')
  return `${mm}:${ss}`
})

/* ================= 透视投影渲染 ================= */
const canvasRef = ref(null)
const boxRef = ref(null)
let ctx = null, W = 0, H = 0, dpr = 1
let yaw = 0, pitch = 0, zoom = 1
const FOCAL = 720
const resetView = () => { yaw = 0; pitch = 0; zoom = 1 }

const project = (x, y, z) => {
  const cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch)
  const x1 = x * cy + z * sy
  const z1 = -x * sy + z * cy
  const y2 = y * cp - z1 * sp
  const z2 = y * sp + z1 * cp
  const depth = z2 + 920
  const s = (FOCAL * zoom) / depth
  return { sx: W / 2 + x1 * s * 300, sy: H / 2 - y2 * s * 300, s }
}

const draw = () => {
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)
  const frame = normFrames[frameIdx.value]
  if (!frame || !frame.size) return

  // 地面参考椭圆（增强空间感）
  ctx.globalAlpha = 0.35
  ctx.strokeStyle = '#d0d7de'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.ellipse(W / 2, H * 0.88, W * 0.3, H * 0.05, 0, 0, Math.PI * 2)
  ctx.stroke()
  ctx.globalAlpha = 1

  // 投影所有有效点
  const proj = new Map()
  for (const [i, p] of frame) proj.set(i, project(p.x, p.y, p.z))

  // 连线（两端均有效才画）
  ctx.lineCap = 'round'
  for (const [a, b, part] of BONES) {
    const pa = proj.get(a), pb = proj.get(b)
    if (!pa || !pb) continue
    ctx.strokeStyle = PART_COLOR[part]
    ctx.lineWidth = Math.max(2, 4.5 * Math.min(1.6, (pa.s + pb.s) / 2))
    ctx.globalAlpha = 0.88
    ctx.beginPath(); ctx.moveTo(pa.sx, pa.sy); ctx.lineTo(pb.sx, pb.sy); ctx.stroke()
  }
  // 头-肩中点连线（颈部近似）
  const head = proj.get(0), ls = proj.get(11), rs = proj.get(12)
  if (head && ls && rs) {
    ctx.strokeStyle = PART_COLOR.head
    ctx.lineWidth = 3
    ctx.beginPath(); ctx.moveTo((ls.sx + rs.sx) / 2, (ls.sy + rs.sy) / 2); ctx.lineTo(head.sx, head.sy); ctx.stroke()
  }
  // 关节点
  for (const [i, pr] of proj) {
    ctx.globalAlpha = 1
    ctx.fillStyle = PART_COLOR[JOINT_PART[i]] || '#6e7781'
    const r = Math.max(2.5, 5 * pr.s) * (i === 0 ? 1.5 : 1)
    ctx.beginPath(); ctx.arc(pr.sx, pr.sy, r, 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = 'rgba(255,255,255,.9)'
    ctx.lineWidth = 1.2
    ctx.stroke()
  }
  ctx.globalAlpha = 1
}

/* ================= 主循环（RAF + 定时器双驱动） ================= */
let rafId = 0, bgTimer = 0, lastTs = 0
const tick = (ts) => {
  const dt = lastTs ? Math.min(0.1, (ts - lastTs) / 1000) : 0
  lastTs = ts
  if (playing.value && frameCount.value > 1) {
    playAcc += dt * speed.value * props.fps
    const adv = Math.floor(playAcc)
    if (adv > 0) {
      playAcc -= adv
      frameIdx.value = (frameIdx.value + adv) % frameCount.value
    }
  }
  draw()
}
const loop = (ts) => { tick(ts || performance.now()); rafId = requestAnimationFrame(loop) }

/* ================= 交互：拖拽旋转 / 滚轮缩放 ================= */
let dragging = false, lastX = 0, lastY = 0
const onPointerDown = ev => {
  dragging = true; lastX = ev.clientX; lastY = ev.clientY
  try { canvasRef.value.setPointerCapture(ev.pointerId) } catch (e) { /* 忽略 */ }
}
const onPointerMove = ev => {
  if (!dragging) return
  const dx = ev.clientX - lastX, dy = ev.clientY - lastY
  yaw += dx * 0.005
  pitch = Math.max(-1.4, Math.min(1.4, pitch - dy * 0.005))
  lastX = ev.clientX; lastY = ev.clientY
}
const onPointerUp = () => { dragging = false }
const onWheel = ev => {
  ev.preventDefault()
  zoom = Math.max(0.4, Math.min(3, zoom * (ev.deltaY < 0 ? 1.1 : 0.9)))
}

/* ================= 尺寸自适应 ================= */
let ro = null
const resize = () => {
  if (!boxRef.value) return
  const r = boxRef.value.getBoundingClientRect()
  dpr = window.devicePixelRatio || 1
  W = Math.max(200, r.width); H = Math.max(200, r.height)
  canvasRef.value.width = W * dpr; canvasRef.value.height = H * dpr
  canvasRef.value.style.width = W + 'px'; canvasRef.value.style.height = H + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

onMounted(async () => {
  await nextTick()
  ctx = canvasRef.value.getContext('2d')
  preprocess()
  resize()
  ro = new ResizeObserver(resize)
  ro.observe(boxRef.value)
  canvasRef.value.addEventListener('pointerdown', onPointerDown)
  canvasRef.value.addEventListener('pointermove', onPointerMove)
  canvasRef.value.addEventListener('pointerup', onPointerUp)
  canvasRef.value.addEventListener('wheel', onWheel, { passive: false })
  loop()
  bgTimer = setInterval(() => tick(performance.now()), 300)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  clearInterval(bgTimer)
  ro?.disconnect()
  canvasRef.value?.removeEventListener('pointerdown', onPointerDown)
  canvasRef.value?.removeEventListener('pointermove', onPointerMove)
  canvasRef.value?.removeEventListener('pointerup', onPointerUp)
  canvasRef.value?.removeEventListener('wheel', onWheel)
})
</script>

<style scoped>
.skel-wrap { display: flex; flex-direction: column; gap: 8px; }
.skel-toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.skel-slider { flex: 1; min-width: 140px; }
.skel-count { font-size: 12px; color: #57606a; font-variant-numeric: tabular-nums; white-space: nowrap; }
.skel-canvas-box { position: relative; width: 100%; aspect-ratio: 1 / 1; max-height: 560px;
  border: 1px solid var(--border, #d0d7de); border-radius: 8px; overflow: hidden;
  background: radial-gradient(ellipse at 50% 40%, #fafbfc 0%, #f0f2f5 70%, #e8ecf0 100%); }
.skel-canvas { position: absolute; inset: 0; cursor: grab; touch-action: none; }
.skel-canvas:active { cursor: grabbing; }
.skel-legend { position: absolute; left: 12px; top: 10px; display: flex; flex-wrap: wrap; gap: 4px 12px;
  font-size: 12px; color: #57606a; pointer-events: none; }
.skel-legend i { width: 9px; height: 9px; border-radius: 50%; display: inline-block; margin-right: 4px; }
.skel-hint { position: absolute; left: 12px; bottom: 10px; font-size: 12px; color: #8c959f; pointer-events: none; }
.skel-empty { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  color: #8c959f; font-size: 13px; }
</style>
