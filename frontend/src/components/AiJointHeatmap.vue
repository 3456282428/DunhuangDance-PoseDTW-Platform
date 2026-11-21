<template>
  <div class="jh-panel">
    <div class="jh-canvas-wrap" ref="wrapRef">
      <canvas ref="canvasRef" class="jh-canvas"></canvas>
      <div v-if="!frames || !frames.length" class="jh-tip">暂无逐帧数据，重新评分后生成</div>
    </div>
    <div class="jh-legend">
      <span class="lg"><i class="dot" style="background:#3fb950"></i>&lt;10° 准确</span>
      <span class="lg"><i class="dot" style="background:#d29922"></i>10-25° 轻微偏差</span>
      <span class="lg"><i class="dot" style="background:#f85149"></i>&gt;25° 明显偏差</span>
      <span class="lg live"><i class="dot pulse" :class="{ on: playing }"></i>随视频实时更新</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  frames: { type: Array, default: () => [] },   // [{t, d, p:{部位:偏差}}]
  currentTime: { type: Number, default: 0 },
  playing: { type: Boolean, default: false }
})

const PART_ORDER = ['左上肢', '右上肢', '左下肢', '右下肢', '躯干', '头颈']

// MediaPipe 33关键点正面模板（归一化坐标；人左侧=画面右侧，与摄像头视角一致）
const TEMPLATE = [
  [0.500, 0.062], // 0 nose
  [0.545, 0.040], [0.556, 0.038], [0.567, 0.040], // 1-3 left eye
  [0.455, 0.040], [0.444, 0.038], [0.433, 0.040], // 4-6 right eye
  [0.588, 0.056], [0.412, 0.056],                 // 7-8 ears
  [0.532, 0.084], [0.468, 0.084],                 // 9-10 mouth
  [0.655, 0.218], [0.345, 0.218],                 // 11-12 shoulders
  [0.725, 0.352], [0.275, 0.352],                 // 13-14 elbows
  [0.762, 0.482], [0.238, 0.482],                 // 15-16 wrists
  [0.778, 0.508], [0.222, 0.508],                 // 17-18 pinky
  [0.788, 0.502], [0.212, 0.502],                 // 19-20 index
  [0.766, 0.492], [0.234, 0.492],                 // 21-22 thumb
  [0.582, 0.532], [0.418, 0.532],                 // 23-24 hips
  [0.602, 0.702], [0.398, 0.702],                 // 25-26 knees
  [0.612, 0.866], [0.388, 0.866],                 // 27-28 ankles
  [0.638, 0.896], [0.362, 0.896],                 // 29-30 heels
  [0.664, 0.926], [0.336, 0.926]                  // 31-32 foot index
]

// 关键点→部位（肩/髋同时属躯干与上下肢，取偏差较大者着色）
const JOINT_PARTS = {
  0: ['头颈'], 1: ['头颈'], 2: ['头颈'], 3: ['头颈'], 4: ['头颈'], 5: ['头颈'],
  6: ['头颈'], 7: ['头颈'], 8: ['头颈'], 9: ['头颈'], 10: ['头颈'],
  11: ['左上肢', '躯干'], 12: ['右上肢', '躯干'],
  13: ['左上肢'], 14: ['右上肢'], 15: ['左上肢'], 16: ['右上肢'],
  17: ['左上肢'], 18: ['右上肢'], 19: ['左上肢'], 20: ['右上肢'],
  21: ['左上肢'], 22: ['右上肢'],
  23: ['左下肢', '躯干'], 24: ['右下肢', '躯干'],
  25: ['左下肢'], 26: ['右下肢'], 27: ['左下肢'], 28: ['右下肢'],
  29: ['左下肢'], 30: ['右下肢'], 31: ['左下肢'], 32: ['右下肢']
}

// 骨骼连接 [起点, 终点, 部位]
const BONES = [
  [11, 12, '躯干'], [11, 23, '躯干'], [12, 24, '躯干'], [23, 24, '躯干'],
  [11, 13, '左上肢'], [13, 15, '左上肢'], [15, 17, '左上肢'], [15, 19, '左上肢'], [15, 21, '左上肢'],
  [12, 14, '右上肢'], [14, 16, '右上肢'], [16, 18, '右上肢'], [16, 20, '右上肢'], [16, 22, '右上肢'],
  [23, 25, '左下肢'], [25, 27, '左下肢'], [27, 29, '左下肢'], [29, 31, '左下肢'], [27, 31, '左下肢'],
  [24, 26, '右下肢'], [26, 28, '右下肢'], [28, 30, '右下肢'], [30, 32, '右下肢'], [28, 32, '右下肢']
]

const GREEN = [63, 185, 80], YELLOW = [210, 153, 34], RED = [248, 81, 73], GRAY = [110, 118, 129]
const lerpC = (c1, c2, t) => c1.map((v, i) => Math.round(v + (c2[i] - v) * t))
// 偏差度数→颜色：0-15° 绿→黄，15-30° 黄→红（平滑插值）
const partColor = (deg) => {
  if (deg == null || isNaN(deg)) return GRAY
  if (deg <= 15) return lerpC(GREEN, YELLOW, Math.max(0, deg / 15))
  return lerpC(YELLOW, RED, Math.min(1, (deg - 15) / 15))
}
const rgba = (c, a) => `rgba(${c[0]},${c[1]},${c[2]},${a})`

const canvasRef = ref(null)
const wrapRef = ref(null)

// 当前时间对应的最近帧（二分查找）
const currentFrame = () => {
  const arr = props.frames
  if (!arr || !arr.length) return null
  let lo = 0, hi = arr.length - 1
  const t = props.currentTime
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1
    if (arr[mid].t <= t) lo = mid; else hi = mid - 1
  }
  return arr[lo]
}

const draw = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const w = canvas.clientWidth, h = canvas.clientHeight
  if (!w || !h) return
  const dpr = window.devicePixelRatio || 1
  if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) {
    canvas.width = Math.round(w * dpr)
    canvas.height = Math.round(h * dpr)
  }
  const ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, w, h)

  const padY = h * 0.06
  const sh = h - 2 * padY
  const sx = (x) => w / 2 + (x - 0.5) * sh
  const sy = (y) => padY + y * sh
  const P = (i) => [sx(TEMPLATE[i][0]), sy(TEMPLATE[i][1])]
  const u = sh * 0.01 // 基准单位

  const f = currentFrame()
  const partDeg = {}
  PART_ORDER.forEach(p => { partDeg[p] = f && f.p && f.p[p] != null ? f.p[p] * 180 : null })

  // 光源方向（归一化）：左上方45°
  const LIGHT = (() => { const x = -0.6, y = -0.8; const L = Math.hypot(x, y); return [x / L, y / L] })()

  // ---- 辅助：绘制锥形肢体段（3D圆柱渐变+高光+暗边） ----
  const drawLimbSeg = (p1, p2, w1, w2, color) => {
    const [x1, y1] = p1, [x2, y2] = p2
    const dx = x2 - x1, dy = y2 - y1
    const len = Math.hypot(dx, dy) || 1
    const nx = -dy / len, ny = dx / len
    const midX = (x1 + x2) / 2, midY = (y1 + y2) / 2
    const avgW = (w1 + w2) / 2
    // 法线与光源的点积：>0 说明法线方向朝向光源（亮侧），<0 则背光（暗侧）
    const dotNL = nx * LIGHT[0] + ny * LIGHT[1]
    const litSign = dotNL >= 0 ? 1 : -1 // 亮侧方向=+法线 或 -法线
    // 外发光层
    ctx.fillStyle = rgba(color, 0.1)
    ctx.beginPath()
    ctx.moveTo(x1 + nx * w1 * 1.7, y1 + ny * w1 * 1.7)
    ctx.lineTo(x2 + nx * w2 * 1.7, y2 + ny * w2 * 1.7)
    ctx.lineTo(x2 - nx * w2 * 1.7, y2 - ny * w2 * 1.7)
    ctx.lineTo(x1 - nx * w1 * 1.7, y1 - ny * w1 * 1.7)
    ctx.closePath(); ctx.fill()
    // 3D圆柱渐变：从亮侧→暗侧（沿法线方向，朝光源的一侧亮）
    const grad = ctx.createLinearGradient(
      midX + nx * avgW * 1.2 * litSign, midY + ny * avgW * 1.2 * litSign, // 亮侧
      midX - nx * avgW * 1.2 * litSign, midY - ny * avgW * 1.2 * litSign  // 暗侧
    )
    grad.addColorStop(0, rgba(color, 0.95))
    grad.addColorStop(0.35, rgba(color, 0.82))
    grad.addColorStop(0.7, rgba(color, 0.5))
    grad.addColorStop(1, rgba(color, 0.25))
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.moveTo(x1 + nx * w1, y1 + ny * w1)
    ctx.lineTo(x2 + nx * w2, y2 + ny * w2)
    ctx.lineTo(x2 - nx * w2, y2 - ny * w2)
    ctx.lineTo(x1 - nx * w1, y1 - ny * w1)
    ctx.closePath(); ctx.fill()
    // 圆端衔接
    ctx.fillStyle = rgba(color, 0.7)
    ctx.beginPath(); ctx.arc(x1, y1, w1, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(x2, y2, w2, 0, Math.PI * 2); ctx.fill()
    // 高光条：亮侧偏移，每端用各自宽度
    ctx.strokeStyle = 'rgba(255,255,255,0.2)'
    ctx.lineWidth = Math.max(1, avgW * 0.22)
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(x1 + nx * w1 * 0.35 * litSign, y1 + ny * w1 * 0.35 * litSign)
    ctx.lineTo(x2 + nx * w2 * 0.35 * litSign, y2 + ny * w2 * 0.35 * litSign)
    ctx.stroke()
    // 暗边描线（背光侧）
    ctx.strokeStyle = rgba(color, 0.4)
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(x1 - nx * w1 * litSign, y1 - ny * w1 * litSign)
    ctx.lineTo(x2 - nx * w2 * litSign, y2 - ny * w2 * litSign)
    ctx.stroke()
  }

  // ---- 辅助：绘制填充椭圆（3D径向渐变） ----
  const fillEllipse = (cx, cy, rx, ry, color, alpha = 0.82) => {
    ctx.fillStyle = rgba(color, alpha * 0.1)
    ctx.beginPath(); ctx.ellipse(cx, cy, rx * 1.5, ry * 1.5, 0, 0, Math.PI * 2); ctx.fill()
    const g = ctx.createRadialGradient(cx - rx * 0.3, cy - ry * 0.3, rx * 0.1, cx, cy, rx * 1.4)
    g.addColorStop(0, rgba(color, Math.min(1, alpha + 0.1)))
    g.addColorStop(0.6, rgba(color, alpha * 0.8))
    g.addColorStop(1, rgba(color, alpha * 0.3))
    ctx.fillStyle = g
    ctx.beginPath(); ctx.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2); ctx.fill()
  }

  // ---- 头部：径向渐变球体（3D球体光照） ----
  const headC = partColor(partDeg['头颈'])
  const nose = P(0), earL = P(7), earR = P(8)
  const headCx = (nose[0] + earL[0] + earR[0]) / 3
  const headCy = nose[1] - sh * 0.008
  const headW = Math.hypot(earL[0] - earR[0], earL[1] - earR[1]) * 0.62
  const headH = headW * 1.28
  // 外发光
  ctx.fillStyle = rgba(headC, 0.08)
  ctx.beginPath(); ctx.ellipse(headCx, headCy, headW * 1.5, headH * 1.4, 0, 0, Math.PI * 2); ctx.fill()
  // 头部3D径向渐变（左上方光源）
  const hGrad = ctx.createRadialGradient(
    headCx - headW * 0.3, headCy - headH * 0.35, headW * 0.1,
    headCx, headCy, headW * 1.4
  )
  hGrad.addColorStop(0, rgba(headC, 0.98))
  hGrad.addColorStop(0.4, rgba(headC, 0.85))
  hGrad.addColorStop(0.8, rgba(headC, 0.5))
  hGrad.addColorStop(1, rgba(headC, 0.25))
  ctx.fillStyle = hGrad
  ctx.beginPath(); ctx.ellipse(headCx, headCy, headW, headH, 0, 0, Math.PI * 2); ctx.fill()
  // 面部暗示：眉骨+鼻梁阴影
  ctx.strokeStyle = rgba(headC, 0.2)
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(headCx - headW * 0.3, headCy - headH * 0.1)
  ctx.quadraticCurveTo(headCx, headCy + headH * 0.2, headCx + headW * 0.3, headCy - headH * 0.1)
  ctx.stroke()

  // ---- 颈部：头底到肩中 ----
  const shL = P(11), shR = P(12)
  const shMid = [(shL[0] + shR[0]) / 2, (shL[1] + shR[1]) / 2]
  const neckTop = [headCx, headCy + headH * 0.85]
  drawLimbSeg(neckTop, shMid, 2.2 * u, 3.5 * u, headC)

  // ---- 躯干：肩→腰→髋 填充多边形 ----
  const hipL = P(23), hipR = P(24)
  const waistY = (shMid[1] + (hipL[1] + hipR[1]) / 2) / 2
  const waistNarrow = 0.88 // 腰部收窄比例
  const shHalf = Math.hypot(shR[0] - shL[0], shR[1] - shL[1]) / 2
  const hipHalf = Math.hypot(hipR[0] - hipL[0], hipR[1] - hipL[1]) / 2
  const shMidX = (shL[0] + shR[0]) / 2
  const hipMidX = (hipL[0] + hipR[0]) / 2
  const trunkC = partColor(partDeg['躯干'])
  // 外发光
  ctx.fillStyle = rgba(trunkC, 0.1)
  ctx.beginPath()
  ctx.moveTo(shL[0], shL[1] - 2 * u)
  ctx.quadraticCurveTo(shMidX - shHalf * 1.15, waistY, hipL[0] - 1.5 * u, hipL[1])
  ctx.lineTo(hipR[0] + 1.5 * u, hipR[1])
  ctx.quadraticCurveTo(shMidX + shHalf * 1.15, waistY, shR[0], shR[1] - 2 * u)
  ctx.closePath(); ctx.fill()
  // 躯干主体（肩圆弧→腰收窄→髋）
  ctx.fillStyle = rgba(trunkC, 0.85)
  ctx.beginPath()
  ctx.moveTo(shL[0], shL[1])
  ctx.quadraticCurveTo(shMidX - shHalf * waistNarrow, waistY, hipL[0], hipL[1])
  ctx.lineTo(hipR[0], hipR[1])
  ctx.quadraticCurveTo(shMidX + shHalf * waistNarrow, waistY, shR[0], shR[1])
  // 肩部弧线
  ctx.quadraticCurveTo(shMidX, shL[1] - 2.5 * u, shL[0], shL[1])
  ctx.closePath(); ctx.fill()
  // 躯干3D渐变叠加（左上方光源 → 左侧亮右侧暗）
  const tGrad = ctx.createLinearGradient(shMidX - shHalf * 1.3, waistY, shMidX + shHalf * 1.3, waistY)
  tGrad.addColorStop(0, rgba(trunkC, 0.25))
  tGrad.addColorStop(0.3, rgba(trunkC, 0.1))
  tGrad.addColorStop(0.7, rgba(trunkC, 0))
  tGrad.addColorStop(1, 'rgba(0,0,0,0.2)')
  ctx.fillStyle = tGrad
  ctx.beginPath()
  ctx.moveTo(shL[0], shL[1])
  ctx.quadraticCurveTo(shMidX - shHalf * waistNarrow, waistY, hipL[0], hipL[1])
  ctx.lineTo(hipR[0], hipR[1])
  ctx.quadraticCurveTo(shMidX + shHalf * waistNarrow, waistY, shR[0], shR[1])
  ctx.quadraticCurveTo(shMidX, shL[1] - 2.5 * u, shL[0], shL[1])
  ctx.closePath(); ctx.fill()
  // 肌肉轮廓线：胸肌分隔+腹中线+腹肌线
  const chestY = shL[1] + (waistY - shL[1]) * 0.25
  const abdBottomY = (hipL[1] + hipR[1]) / 2 - 2 * u
  ctx.strokeStyle = rgba(trunkC, 0.15)
  ctx.lineWidth = 1.2
  // 胸肌下缘线（两侧弧向中线）
  ctx.beginPath()
  ctx.moveTo(shL[0] + shHalf * 0.25, chestY)
  ctx.quadraticCurveTo(shMidX, chestY + shHalf * 0.18, shR[0] - shHalf * 0.25, chestY)
  ctx.stroke()
  // 腹中线
  ctx.beginPath()
  ctx.moveTo(shMidX, chestY + shHalf * 0.1)
  ctx.lineTo(shMidX, abdBottomY)
  ctx.stroke()
  // 腹肌横线（2条）
  ctx.lineWidth = 0.8
  ctx.strokeStyle = rgba(trunkC, 0.1)
  const ab1Y = chestY + (abdBottomY - chestY) * 0.35
  const ab2Y = chestY + (abdBottomY - chestY) * 0.65
  ctx.beginPath()
  ctx.moveTo(shMidX - shHalf * 0.3 * waistNarrow, ab1Y)
  ctx.lineTo(shMidX + shHalf * 0.3 * waistNarrow, ab1Y)
  ctx.moveTo(shMidX - shHalf * 0.28 * waistNarrow, ab2Y)
  ctx.lineTo(shMidX + shHalf * 0.28 * waistNarrow, ab2Y)
  ctx.stroke()
  // 肩部三角肌分隔线
  ctx.lineWidth = 1
  ctx.strokeStyle = rgba(trunkC, 0.12)
  ctx.beginPath()
  ctx.moveTo(shL[0] + shHalf * 0.15, shL[1] + 2 * u)
  ctx.lineTo(shL[0] + shHalf * 0.35, chestY - 2 * u)
  ctx.moveTo(shR[0] - shHalf * 0.15, shR[1] + 2 * u)
  ctx.lineTo(shR[0] - shHalf * 0.35, chestY - 2 * u)
  ctx.stroke()
  // 左上肢（肩11→肘13→腕15→手）
  const armC_l = partColor(partDeg['左上肢'])
  const el_L = P(13), wr_L = P(15), hand_L = P(15)
  drawLimbSeg(shL, el_L, 4.5 * u, 3.2 * u, armC_l)   // 上臂
  drawLimbSeg(el_L, wr_L, 3.2 * u, 2.0 * u, armC_l)  // 前臂
  fillEllipse(hand_L[0], hand_L[1], 2.2 * u, 2.8 * u, armC_l)  // 手

  // 右上肢
  const armC_r = partColor(partDeg['右上肢'])
  const el_R = P(14), wr_R = P(16), hand_R = P(16)
  drawLimbSeg(shR, el_R, 4.5 * u, 3.2 * u, armC_r)
  drawLimbSeg(el_R, wr_R, 3.2 * u, 2.0 * u, armC_r)
  fillEllipse(hand_R[0], hand_R[1], 2.2 * u, 2.8 * u, armC_r)

  // 左下肢（髋23→膝25→踝27→脚）
  const legC_l = partColor(partDeg['左下肢'])
  const kn_L = P(25), ank_L = P(27), foot_L = P(31)
  drawLimbSeg(hipL, kn_L, 5.0 * u, 3.5 * u, legC_l)   // 大腿
  drawLimbSeg(kn_L, ank_L, 3.5 * u, 2.2 * u, legC_l)  // 小腿
  fillEllipse(foot_L[0], foot_L[1], 3.5 * u, 2.0 * u, legC_l)  // 脚

  // 右下肢
  const legC_r = partColor(partDeg['右下肢'])
  const kn_R = P(26), ank_R = P(28), foot_R = P(32)
  drawLimbSeg(hipR, kn_R, 5.0 * u, 3.5 * u, legC_r)
  drawLimbSeg(kn_R, ank_R, 3.5 * u, 2.2 * u, legC_r)
  fillEllipse(foot_R[0], foot_R[1], 3.5 * u, 2.0 * u, legC_r)

  // ---- 关节点高光（保留小光点提示关节位置） ----
  for (let i = 0; i < 33; i++) {
    const parts = JOINT_PARTS[i]
    if (!parts) continue
    let d = null
    for (const p of parts) {
      const v = partDeg[p]
      if (v != null && (d == null || v > d)) d = v
    }
    const c = partColor(d)
    const [x, y] = P(i)
    ctx.fillStyle = rgba(c, 0.35)
    ctx.beginPath(); ctx.arc(x, y, 1.8 * u, 0, Math.PI * 2); ctx.fill()
  }
}

watch(() => props.currentTime, draw)
watch(() => props.frames.length, draw)

let ro = null
onMounted(() => {
  draw()
  if (window.ResizeObserver && wrapRef.value) {
    ro = new ResizeObserver(draw)
    ro.observe(wrapRef.value)
  } else {
    window.addEventListener('resize', draw)
  }
})

onBeforeUnmount(() => {
  if (ro) ro.disconnect()
  else window.removeEventListener('resize', draw)
})
</script>

<style scoped>
.jh-panel { width: 100%; height: 100%; display: flex; flex-direction: column; min-height: 0; }
.jh-canvas-wrap {
  position: relative;
  flex: 1;
  min-height: 260px;
  background: linear-gradient(135deg, #0d1117 0%, #161b22 100%);
  border: 1px solid #30363d;
  border-radius: 8px;
  overflow: hidden;
}
.jh-canvas { width: 100%; height: 100%; display: block; }
.jh-tip {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  color: #8b949e; font-size: 13px;
  background: rgba(13, 17, 23, 0.55);
}
.jh-legend {
  display: flex; flex-wrap: wrap; gap: 4px 12px;
  margin-top: 8px; font-size: 11px; color: #6b5a4a; align-items: center;
}
.lg { display: inline-flex; align-items: center; gap: 5px; }
.dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.dot.pulse { background: #a89880; }
.dot.pulse.on { background: #2c9678; animation: jhpulse 1.2s infinite; }
@keyframes jhpulse { 0%,100% { opacity: 1; } 50% { opacity: 0.35; } }
.lg.live { margin-left: auto; color: #6b5a4a; }
</style>
