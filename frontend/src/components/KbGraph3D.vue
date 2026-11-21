<template>
  <div class="g3d-wrap">
    <!-- 工具栏：维度筛选 + 统计 + 布局操作 -->
    <div class="g3d-toolbar">
      <el-checkbox-group v-model="activeDims" size="small" class="g3d-dims" @change="refilter">
        <el-checkbox-button v-for="d in DIMENSIONS" :key="d.key" :value="d.key">
          <span class="g3d-dot" :style="{ background: d.color }" />{{ d.text }}
        </el-checkbox-button>
      </el-checkbox-group>
      <span class="g3d-stats">{{ statText }}</span>
      <div class="g3d-actions">
        <el-button size="small" plain @click="reheat">重新布局</el-button>
        <el-button size="small" plain @click="resetView">复位视角</el-button>
      </div>
    </div>

    <!-- 实体类别图例 -->
    <div class="g3d-legend">
      <span v-for="c in categoryList" :key="c.key" class="g3d-legend-item">
        <span class="g3d-dot" :style="{ background: c.color }" />{{ c.text }}
      </span>
    </div>

    <!-- 3D画布 -->
    <div ref="boxRef" class="g3d-canvas-box">
      <canvas ref="canvasRef" class="g3d-canvas" />
      <div v-if="hoverTip" class="g3d-tip" :style="{ left: tipPos.x + 'px', top: tipPos.y + 'px' }" v-html="hoverTip" />
      <div v-if="selected" class="g3d-info">
        <div class="g3d-info-head">
          <b>{{ selected.name }}</b>
          <el-tag size="small" effect="plain">{{ selected.categoryText }}</el-tag>
          <el-button link size="small" @click="selected = null">关闭</el-button>
        </div>
        <div class="g3d-info-sub">关联度 {{ selected.degree }} · 点击空白处取消聚焦</div>
        <div class="g3d-info-rels">
          <div v-for="(r, i) in selectedRels" :key="i" class="g3d-info-rel">
            <span class="g3d-rel-type" :style="{ color: dimColor(r.dimension) }">{{ r.relText }}</span>
            <span class="g3d-rel-name">{{ r.otherName }}</span>
            <span class="g3d-rel-strength">{{ Math.round((r.strength || 0) * 100) }}%</span>
            <span class="g3d-rel-dim">{{ r.dimensionText }}</span>
          </div>
        </div>
      </div>
      <div class="g3d-hint">左键拖拽旋转 · 滚轮缩放 · 点击节点聚焦关联</div>
      <div v-if="loading" class="g3d-mask" v-loading="true" element-loading-text="正在计算3D力导向布局…" />
    </div>
  </div>
</template>

<script setup>
/**
 * 知识库关联 3D 建模图谱（零依赖自研渲染）：
 * - 3D 力导向布局（库仑斥力 + 弹簧引力 + 中心重力，alpha 降温收敛）
 * - Canvas 透视投影渲染（yaw/pitch 旋转矩阵 + 焦距缩放 + 深度排序）
 * - 交互：拖拽旋转 / 滚轮缩放 / 悬停提示 / 点击聚焦 / 维度筛选 / 重布局
 * 节点=业务实体（kb_relation 规范化 ref），边=关联（颜色=维度，粗细=强度，虚线=内容相似）。
 */
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getKbRelationGraph } from '@/api/knowledge'

/* 关联维度 → 颜色/文案（与后端 rel_dimension 对应） */
const DIMENSIONS = [
  { key: 'ORG', text: '组织', color: '#1f6feb' },
  { key: 'TEACH', text: '教学', color: '#8250df' },
  { key: 'FIN', text: '财务', color: '#d29922' },
  { key: 'ASSET', text: '资产', color: '#2da44e' },
  { key: 'CONTENT', text: '内容', color: '#db61a2' }
]
const DIM_COLOR = Object.fromEntries(DIMENSIONS.map(d => [d.key, d.color]))
const dimColor = k => DIM_COLOR[k] || '#8c959f'
/* 实体类别色板（循环取色） */
const CAT_PALETTE = ['#1f6feb', '#2da44e', '#bf8700', '#8250df', '#db61a2', '#d29922', '#f85149', '#3fb950', '#54aeff', '#a371f7', '#eac54f', '#6e7781', '#0969da', '#953800']

const loading = ref(true)
const activeDims = ref(DIMENSIONS.map(d => d.key))
const categoryList = ref([])
const dimStats = ref({})
const hoverTip = ref('')
const tipPos = ref({ x: 0, y: 0 })
const selected = ref(null)
const selectedRels = ref([])

const statText = computed(() => {
  const dims = DIMENSIONS.filter(d => dimStats.value[d.key]).map(d => `${d.text}${dimStats.value[d.key]}`).join(' / ')
  return `实体 ${nodeCount.value} · 可见关联 ${visibleEdgeCount.value}/${edgeCount.value}${dims ? ' · ' + dims : ''}`
})
const nodeCount = ref(0)
const edgeCount = ref(0)
const visibleEdgeCount = ref(0)

/* ================= 数据与图结构 ================= */
let nodes = []            // {id,name,category,categoryText,degree,x,y,z,vx,vy,vz,color,r0}
let edges = []            // {src,dst,relType,relText,dimension,strength,relSource,visible}
let nodeById = new Map()
let adjacency = new Map() // id -> [{otherName,relText,dimension,dimensionText,strength}]

const buildGraph = (data) => {
  const colorMap = {}
  categoryList.value = Object.entries(data.categories || {}).map(([key, text], i) => {
    colorMap[key] = CAT_PALETTE[i % CAT_PALETTE.length]
    return { key, text, color: colorMap[key] }
  })
  dimStats.value = data.dimensionStats || {}
  nodeCount.value = data.nodeCount || 0
  edgeCount.value = data.edgeCount || 0

  nodes = (data.nodes || []).map(n => {
    // 初始位置：均匀球壳分布，加速布局收敛
    const t = Math.random() * Math.PI * 2, p = Math.acos(2 * Math.random() - 1), R = 150
    return {
      id: n.id, name: n.name, category: n.category, categoryText: n.categoryText, degree: n.degree || 0,
      x: R * Math.sin(p) * Math.cos(t), y: R * Math.sin(p) * Math.sin(t) * 0.7, z: R * Math.cos(p),
      vx: 0, vy: 0, vz: 0, color: colorMap[n.category] || '#6e7781',
      r0: 3.2 + Math.min(9, Math.sqrt(n.degree || 0) * 1.6)
    }
  })
  nodeById = new Map(nodes.map(n => [n.id, n]))
  adjacency = new Map()
  edges = (data.edges || []).map(e => {
    // 后端边字段为 source/target（节点字段为 id）
    const rec = { src: e.source, dst: e.target, relType: e.relType, relText: e.relText, dimension: e.dimension, dimensionText: e.dimensionText, strength: e.strength, relSource: e.relSource, visible: true }
    if (nodeById.has(rec.src) && nodeById.has(rec.dst)) {
      if (!adjacency.has(rec.src)) adjacency.set(rec.src, [])
      if (!adjacency.has(rec.dst)) adjacency.set(rec.dst, [])
      adjacency.get(rec.src).push({ otherName: nodeById.get(rec.dst)?.name, relText: rec.relText, dimension: rec.dimension, dimensionText: rec.dimensionText, strength: rec.strength })
      adjacency.get(rec.dst).push({ otherName: nodeById.get(rec.src)?.name, relText: rec.relText, dimension: rec.dimension, dimensionText: rec.dimensionText, strength: rec.strength })
    }
    return rec
  })
  refilter()
}

/* 维度筛选：边按维度过滤，仅渲染可见边的端点；统计同步刷新可见关联数 */
const refilter = () => {
  const set = new Set(activeDims.value)
  let vis = 0
  for (const e of edges) { e.visible = set.has(e.dimension); if (e.visible) vis++ }
  visibleEdgeCount.value = vis
  // 若筛选后选中节点不在可见图中则取消聚焦
  if (selected.value && !nodes.some(n => n.id === selected.value.id && visibleNodeIds.has(n.id))) selected.value = null
}
const visibleNodeIds = new Set()
const computeVisibleNodes = () => {
  visibleNodeIds.clear()
  for (const e of edges) {
    if (!e.visible) continue
    visibleNodeIds.add(e.src); visibleNodeIds.add(e.dst)
  }
}

/* ================= 3D 力导向布局 ================= */
let alpha = 1
const reheat = () => { alpha = 0.85 }
const simTick = () => {
  const n = nodes.length
  if (!n) return
  // 库仑斥力（O(n²)，数据规模≤数百节点）
  for (let i = 0; i < n; i++) {
    const a = nodes[i]
    for (let j = i + 1; j < n; j++) {
      const b = nodes[j]
      let dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z
      let d2 = dx * dx + dy * dy + dz * dz
      if (d2 < 1) { d2 = 1; dx = Math.random() - 0.5; dy = Math.random() - 0.5; dz = Math.random() - 0.5 }
      const f = (2400 / d2) * alpha
      const d = Math.sqrt(d2), ux = dx / d, uy = dy / d, uz = dz / d
      a.vx += ux * f; a.vy += uy * f; a.vz += uz * f
      b.vx -= ux * f; b.vy -= uy * f; b.vz -= uz * f
    }
  }
  // 弹簧引力：关联越强（strength→1）节点越近
  for (const e of edges) {
    if (!e.visible) continue
    const a = nodeById.get(e.src), b = nodeById.get(e.dst)
    if (!a || !b) continue
    const rest = 60 + (1 - (e.strength || 0.5)) * 90
    const dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z
    const d = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
    const f = (d - rest) * 0.02 * alpha
    const ux = dx / d, uy = dy / d, uz = dz / d
    a.vx += ux * f; a.vy += uy * f; a.vz += uz * f
    b.vx -= ux * f; b.vy -= uy * f; b.vz -= uz * f
  }
  // 中心重力 + 阻尼积分
  for (const p of nodes) {
    p.vx -= p.x * 0.012 * alpha; p.vy -= p.y * 0.012 * alpha; p.vz -= p.z * 0.012 * alpha
    p.vx *= 0.86; p.vy *= 0.86; p.vz *= 0.86
    const sp = Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz)
    if (sp > 8) { p.vx *= 8 / sp; p.vy *= 8 / sp; p.vz *= 8 / sp }
    p.x += p.vx; p.y += p.vy; p.z += p.vz
  }
  alpha *= 0.995
  if (alpha < 0.02) alpha = 0
}

/* ================= 透视投影渲染 ================= */
const canvasRef = ref(null)
const boxRef = ref(null)
let ctx = null, W = 0, H = 0, dpr = 1
let yaw = 0.6, pitch = 0.42, zoom = 1
const FOCAL = 720
const resetView = () => { yaw = 0.6; pitch = 0.42; zoom = 1 }

const project = (x, y, z) => {
  const cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch)
  const x1 = x * cy + z * sy
  const z1 = -x * sy + z * cy
  const y2 = y * cp - z1 * sp
  const z2 = y * sp + z1 * cp
  const depth = z2 + 920
  const s = (FOCAL * zoom) / depth
  return { sx: W / 2 + x1 * s, sy: H / 2 + y2 * s, depth, s }
}

const draw = () => {
  ctx.clearRect(0, 0, W, H)
  computeVisibleNodes()
  const selId = selected.value?.id
  const neighbors = selId ? new Set([selId]) : null
  if (selId) for (const e of edges) {
    if (!e.visible) continue
    if (e.src === selId) neighbors.add(e.dst)
    else if (e.dst === selId) neighbors.add(e.src)
  }
  // 边：颜色=维度，粗细=强度，虚线=内容相似
  for (const e of edges) {
    if (!e.visible) continue
    const a = nodeById.get(e.src), b = nodeById.get(e.dst)
    if (!a || !b) continue
    const pa = a.proj, pb = b.proj
    if (!pa || !pb) continue
    const on = !selId || e.src === selId || e.dst === selId
    const sim = e.relType === 'CONTENT_SIM'
    ctx.globalAlpha = (on ? 1 : 0.1) * (sim ? 0.3 : 0.72)
    ctx.strokeStyle = dimColor(e.dimension)
    ctx.lineWidth = Math.max(0.5, (0.6 + (e.strength || 0.5) * 2.4) * Math.min(1.6, (pa.s + pb.s) / 2))
    ctx.setLineDash(sim ? [5, 4] : [])
    ctx.beginPath(); ctx.moveTo(pa.sx, pa.sy); ctx.lineTo(pb.sx, pb.sy); ctx.stroke()
  }
  ctx.setLineDash([])
  // 节点：按深度降序（远→近）绘制，近处覆盖远处
  const drawList = []
  for (const p of nodes) {
    if (!visibleNodeIds.has(p.id) && !selId) continue
    p.proj = project(p.x, p.y, p.z)
    if (p.proj.depth < 40) continue
    drawList.push(p)
  }
  drawList.sort((a, b) => b.proj.depth - a.proj.depth)
  for (const p of drawList) {
    const { sx, sy, s } = p.proj
    const r = Math.max(1.4, Math.min(17, p.r0 * s))
    p.screenR = r
    const dim = selId && !neighbors.has(p.id)
    ctx.globalAlpha = dim ? 0.12 : 1
    if (!dim && p.degree >= 6) { ctx.shadowColor = p.color; ctx.shadowBlur = 10 }
    ctx.fillStyle = p.color
    ctx.beginPath(); ctx.arc(sx, sy, r, 0, Math.PI * 2); ctx.fill()
    ctx.shadowBlur = 0
    if (p.id === selId) { ctx.strokeStyle = '#24292f'; ctx.lineWidth = 2; ctx.beginPath(); ctx.arc(sx, sy, r + 3, 0, Math.PI * 2); ctx.stroke() }
    // 中心枢纽 / 选中 / 悬停节点显示标签
    if (!dim && (p.degree >= 6 || p.id === selId || p === hoverNode)) {
      ctx.globalAlpha = dim ? 0.12 : 0.95
      ctx.font = '11px system-ui, sans-serif'
      ctx.lineWidth = 3; ctx.strokeStyle = 'rgba(255,255,255,.92)'
      ctx.textAlign = 'center'
      ctx.strokeText(p.name, sx, sy - r - 4)
      ctx.fillStyle = '#24292f'
      ctx.fillText(p.name, sx, sy - r - 4)
    }
  }
  ctx.globalAlpha = 1
}

/* ================= 主循环 ================= */
/* 双驱动渲染：RAF 前台 60fps；后台标签页浏览器会暂停 RAF，用定时器兜底保证图谱持续可渲染/可交互 */
let rafId = 0, bgTimer = 0, lastFrame = 0
const tick = () => {
  const now = performance.now()
  if (now - lastFrame < 28) return
  lastFrame = now
  if (alpha > 0) simTick()
  draw()
}
const loop = () => { tick(); rafId = requestAnimationFrame(loop) }

/* ================= 交互：拖拽旋转 / 滚轮缩放 / 悬停 / 点击聚焦 ================= */
let hoverNode = null
let dragging = false, moved = 0, lastX = 0, lastY = 0
const pick = (mx, my) => {
  let best = null, bestD = 1e9
  for (const p of nodes) {
    if (!p.proj || p.screenR == null) continue
    const dx = p.proj.sx - mx, dy = p.proj.sy - my
    const d = dx * dx + dy * dy
    const hit = Math.max(p.screenR + 6, 16)
    if (d < hit * hit && d < bestD) { best = p; bestD = d }
  }
  return best
}
const onPointerDown = ev => {
  dragging = true; moved = 0; lastX = ev.clientX; lastY = ev.clientY
  try { canvasRef.value.setPointerCapture(ev.pointerId) } catch (e) { /* 合成事件无活动指针，忽略 */ }
}
const onPointerMove = ev => {
  const rect = canvasRef.value.getBoundingClientRect()
  const mx = ev.clientX - rect.left, my = ev.clientY - rect.top
  if (dragging) {
    const dx = ev.clientX - lastX, dy = ev.clientY - lastY
    moved += Math.abs(dx) + Math.abs(dy)
    yaw += dx * 0.005
    pitch = Math.max(-1.4, Math.min(1.4, pitch + dy * 0.005))
    lastX = ev.clientX; lastY = ev.clientY
    hoverTip.value = ''
    hoverNode = null
    return
  }
  const hit = pick(mx, my)
  hoverNode = hit
  if (hit) {
    hoverTip.value = `<b>${hit.name}</b><br/>${hit.categoryText} · 关联度 ${hit.degree}`
    tipPos.value = { x: mx + 14, y: my + 10 }
  } else hoverTip.value = ''
}
const onPointerUp = ev => {
  dragging = false
  if (moved < 5) {
    const rect = canvasRef.value.getBoundingClientRect()
    const hit = pick(ev.clientX - rect.left, ev.clientY - rect.top)
    selected.value = hit ? { id: hit.id, name: hit.name, categoryText: hit.categoryText, degree: hit.degree } : null
    selectedRels.value = hit ? (adjacency.get(hit.id) || []).sort((a, b) => (b.strength || 0) - (a.strength || 0)).slice(0, 8) : []
  }
}
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
  resize()
  ro = new ResizeObserver(resize)
  ro.observe(boxRef.value)
  try {
    const res = await getKbRelationGraph()
    buildGraph(res.data || {})
  } catch (e) {
    hoverTip.value = ''
  } finally {
    loading.value = false
  }
  // 预热布局：先离线迭代120步，首屏即呈现分散形态
  for (let i = 0; i < 120; i++) simTick()
  // 防御：fetch 期间组件可能已被卸载（对话框 destroy-on-close），避免对 null 挂事件
  if (!canvasRef.value) return
  canvasRef.value.addEventListener('pointerdown', onPointerDown)
  canvasRef.value.addEventListener('pointermove', onPointerMove)
  canvasRef.value.addEventListener('pointerup', onPointerUp)
  canvasRef.value.addEventListener('wheel', onWheel, { passive: false })
  loop()
  bgTimer = setInterval(tick, 300)
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
.g3d-wrap { display: flex; flex-direction: column; gap: 8px; }
.g3d-toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.g3d-dims :deep(.el-checkbox-button__inner) { display: flex; align-items: center; gap: 5px; }
.g3d-stats { font-size: 12px; color: #57606a; }
.g3d-actions { margin-left: auto; display: flex; gap: 8px; }
.g3d-legend { display: flex; flex-wrap: wrap; gap: 4px 14px; }
.g3d-legend-item { font-size: 12px; color: #57606a; display: inline-flex; align-items: center; gap: 5px; }
.g3d-dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.g3d-canvas-box { position: relative; height: 560px; border: 1px solid var(--border, #d0d7de); border-radius: 8px; overflow: hidden;
  background: radial-gradient(ellipse at 50% 40%, #f6f8fa 0%, #eef1f4 70%, #e7ebef 100%); }
.g3d-canvas { position: absolute; inset: 0; cursor: grab; touch-action: none; }
.g3d-canvas:active { cursor: grabbing; }
.g3d-hint { position: absolute; left: 12px; bottom: 10px; font-size: 12px; color: #8c959f; pointer-events: none; }
.g3d-tip { position: absolute; z-index: 5; pointer-events: none; background: rgba(36,41,47,.92); color: #fff;
  font-size: 12px; line-height: 1.5; padding: 6px 10px; border-radius: 6px; max-width: 260px; word-break: break-all; }
.g3d-mask { position: absolute; inset: 0; background: rgba(255,255,255,.6); z-index: 6; }
.g3d-info { position: absolute; right: 12px; top: 12px; z-index: 5; width: 300px; max-height: 70%; overflow: auto;
  background: rgba(255,255,255,.96); border: 1px solid #d0d7de; border-radius: 8px; padding: 10px 12px; box-shadow: 0 4px 16px rgba(31,35,40,.08); }
.g3d-info-head { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.g3d-info-head b { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.g3d-info-sub { font-size: 12px; color: #8c959f; margin: 4px 0 6px; }
.g3d-info-rel { display: flex; align-items: center; gap: 6px; font-size: 12px; padding: 3px 0; border-top: 1px dashed #eaeef2; }
.g3d-rel-type { flex-shrink: 0; font-weight: 600; }
.g3d-rel-name { flex: 1; min-width: 0; color: #0969da; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.g3d-rel-strength { flex-shrink: 0; color: #2da44e; font-weight: 600; }
.g3d-rel-dim { flex-shrink: 0; color: #8c959f; }
</style>
