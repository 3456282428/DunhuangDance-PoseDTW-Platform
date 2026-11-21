<template>
  <div class="ch-page">
    <!-- 目标姿态选择 -->
    <div class="pose-picker">
      <div v-for="p in poseList" :key="p.id" class="pose-mini"
        :class="{ active: p.id === targetId }" @click="targetId = p.id">
        <b>{{ p.name }}</b>
        <span class="pm-desc">{{ p.description }}</span>
      </div>
      <el-skeleton v-if="!poseList.length" :rows="2" animated class="pose-skel" />
    </div>

    <div class="ch-grid">
      <!-- 左：拍摄区 -->
      <el-card shadow="never">
        <template #header><span class="col-title">挑战拍摄</span></template>
        <div class="cam-box">
          <video v-if="cameraOn && !frameUrl" ref="videoRef" class="cam-video" autoplay playsinline muted />
          <canvas v-if="frameUrl" ref="canvasRef" class="cam-canvas" />
          <div v-if="!cameraOn && !frameUrl" class="cam-placeholder">
            <el-icon :size="36"><Camera /></el-icon>
            <span>开启摄像头或上传一张全身照开始挑战</span>
          </div>
          <div v-if="analyzing" class="cam-mask" v-loading="true" element-loading-text="正在识别姿态…首次加载模型约需30秒" />
        </div>
        <div class="cam-actions">
          <el-button v-if="!cameraOn" type="primary" @click="startCamera">
            <el-icon><VideoCamera /></el-icon>&nbsp;开启摄像头
          </el-button>
          <el-button v-else @click="stopCamera">关闭摄像头</el-button>
          <el-button type="primary" plain :disabled="!cameraOn || analyzing" @click="capture">拍照挑战</el-button>
          <el-button plain :disabled="analyzing" @click="triggerFile">
            <el-icon><Upload /></el-icon>&nbsp;上传照片
          </el-button>
          <el-button v-if="frameUrl" link type="primary" @click="retake">重新拍摄</el-button>
          <input ref="fileRef" type="file" accept="image/*" class="hidden-file" @change="onFileChange" />
        </div>
        <p class="cam-tip">提示：请保证全身完整入镜、正对镜头、光线充足，动作定格1秒后再拍照。</p>
      </el-card>

      <!-- 右：评分结果 -->
      <el-card shadow="never">
        <template #header>
          <span class="col-title">挑战结果{{ targetPose ? ` · ${targetPose.name}` : '' }}</span>
        </template>

        <el-empty v-if="!result" description="完成拍摄后这里会显示姿态匹配度" :image-size="70" />

        <div v-else-if="!result.detected" class="result-fail">
          <el-result icon="warning" :title="result.message || '未检测到人体'" sub-title="请调整站位后重新拍摄">
            <template #extra>
              <el-button type="primary" plain @click="retake">重新拍摄</el-button>
            </template>
          </el-result>
        </div>

        <div v-else class="result-body">
          <div class="score-row">
            <el-progress type="circle" :width="110" :percentage="targetScore"
              :color="scoreColor" :stroke-width="9" />
            <div class="score-meta">
              <b class="tier" :style="{ color: scoreColor }">{{ tierText }}</b>
              <span class="score-sub">{{ targetDetail.passed }}/{{ targetDetail.total }} 项角度特征达标</span>
            </div>
          </div>

          <el-alert v-if="bestMatch && bestMatch.pose !== targetId" type="info" :closable="false" class="best-alert"
            :title="`系统判定你当前最接近「${bestMatch.name}」（${bestMatch.score}分），继续向目标姿态调整吧`" />

          <div class="feat-list">
            <div v-for="f in targetDetail.features" :key="f.joint" class="feat-row">
              <span class="feat-joint">{{ jointName(f.joint) }}</span>
              <span class="feat-target">{{ featTargetText(f) }}</span>
              <span class="feat-status">
                <template v-if="f.qualitative">
                  <el-tag size="small" type="info" effect="plain">定性</el-tag>
                </template>
                <template v-else-if="f.passed">
                  <el-icon class="ok"><CircleCheckFilled /></el-icon>
                </template>
                <template v-else>
                  <el-icon class="bad"><CircleCloseFilled /></el-icon>
                </template>
              </span>
            </div>
          </div>

          <el-button type="primary" plain class="retry-btn" @click="retake">再挑战一次</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
/**
 * 关键姿态挑战（学生端）：
 * 选择敦煌标志姿态 → 摄像头抓拍/上传全身照 → Python MediaPipe 提取关键点，
 * 按角度特征逐项评分；前端在原图上叠加33关键点骨架。照片服务端即时删除不留存。
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getDunhuangCategories, challengePose } from '@/api/dunhuang'

const JOINT_CN = {
  left_elbow: '左肘', right_elbow: '右肘',
  left_shoulder: '左肩', right_shoulder: '右肩',
  left_knee: '左膝', right_knee: '右膝',
  left_hip: '左髋', right_hip: '右髋',
  trunk_inclination: '躯干倾角', neck: '颈部',
  left_arm_horizontal: '左臂伸展',
}
/* 定性特征的可读要求 */
const QUAL_TEXT = { left_arm_horizontal: '左臂保持水平伸展' }

/* MediaPipe骨架连线（与播放器一致的六部位配色） */
const BONES = [
  [11, 12, 'torso'], [11, 23, 'torso'], [12, 24, 'torso'], [23, 24, 'torso'],
  [11, 13, 'larm'], [13, 15, 'larm'],
  [12, 14, 'rarm'], [14, 16, 'rarm'],
  [23, 25, 'lleg'], [25, 27, 'lleg'],
  [24, 26, 'rleg'], [26, 28, 'rleg'],
]
const PART_COLOR = {
  torso: '#8c959f', larm: '#3fb950', rarm: '#54aeff',
  lleg: '#d29922', rleg: '#a371f7', head: '#f85149',
}

const poseList = ref([])
const targetId = ref('')
const cameraOn = ref(false)
const analyzing = ref(false)
const frameUrl = ref('')
const result = ref(null)

const videoRef = ref(null)
const canvasRef = ref(null)
const fileRef = ref(null)
let stream = null

const targetPose = computed(() => poseList.value.find(p => p.id === targetId.value) || null)
const targetDetail = computed(() => result.value?.detected && targetId.value
  ? result.value.details[targetId.value] : null)
const targetScore = computed(() => targetDetail.value?.score ?? 0)
const bestMatch = computed(() => result.value?.detected ? result.value.best : null)
const scoreColor = computed(() =>
  targetScore.value >= 85 ? '#2da44e' : targetScore.value >= 60 ? '#bf8700' : '#cf222e')
const tierText = computed(() => {
  const s = targetScore.value
  if (s >= 85) return '神韵兼备'
  if (s >= 70) return '有模有样'
  if (s >= 50) return '初具雏形'
  return '继续练习'
})
const jointName = (k) => JOINT_CN[k] || k
const featTargetText = (f) => {
  if (f.qualitative) return QUAL_TEXT[f.joint] || '保持该姿态特征'
  if (!f.visible) return `目标 ${f.min}°–${f.max}° · 关键点不可见`
  return `目标 ${f.min}°–${f.max}° · 实际 ${f.actual}°`
}

/* ================= 摄像头 ================= */
const startCamera = async () => {
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false,
    })
    cameraOn.value = true
    await new Promise(r => setTimeout(r, 60))
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play().catch(() => {})
    }
  } catch (e) {
    ElMessage.warning('无法启动摄像头（需授权或HTTPS/localhost环境），可改用「上传照片」挑战')
  }
}
const stopCamera = () => {
  stream?.getTracks().forEach(t => t.stop())
  stream = null
  cameraOn.value = false
}

/* ================= 拍照 / 上传 ================= */
const triggerFile = () => fileRef.value?.click()

/** 将画面缩放到宽度≤720后导出JPEG，并绘制到结果画布 */
const buildFrame = async (source, sourceW, sourceH) => {
  const maxW = 720
  const scale = Math.min(1, maxW / sourceW)
  const w = Math.round(sourceW * scale), h = Math.round(sourceH * scale)
  const c = document.createElement('canvas')
  c.width = w; c.height = h
  c.getContext('2d').drawImage(source, 0, 0, w, h)
  const blob = await new Promise(r => c.toBlob(r, 'image/jpeg', 0.85))
  if (frameUrl.value) URL.revokeObjectURL(frameUrl.value)
  frameUrl.value = URL.createObjectURL(blob)
  await new Promise(r => setTimeout(r, 30))
  drawFrame(blob)
  return blob
}

const drawFrame = (blob) => {
  const canvas = canvasRef.value
  if (!canvas) return
  const img = new Image()
  const url = URL.createObjectURL(blob)
  img.onload = () => {
    canvas.width = img.width; canvas.height = img.height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    if (result.value?.detected) drawSkeleton(ctx, img.width, img.height)
    URL.revokeObjectURL(url)
  }
  img.onerror = () => URL.revokeObjectURL(url)
  img.src = url
}

const drawSkeleton = (ctx, w, h) => {
  const kp = result.value.keypoints.map(k => ({ x: k[0] * w, y: k[1] * h, v: k[2] }))
  ctx.lineCap = 'round'
  for (const [a, b, part] of BONES) {
    const pa = kp[a], pb = kp[b]
    if (!pa || !pb || pa.v < 0.3 || pb.v < 0.3) continue
    ctx.strokeStyle = PART_COLOR[part]
    ctx.lineWidth = Math.max(2.5, w * 0.006)
    ctx.globalAlpha = 0.9
    ctx.beginPath(); ctx.moveTo(pa.x, pa.y); ctx.lineTo(pb.x, pb.y); ctx.stroke()
  }
  // 颈部连线
  const head = kp[0], ls = kp[11], rs = kp[12]
  if (head && ls && rs && head.v >= 0.3 && ls.v >= 0.3 && rs.v >= 0.3) {
    ctx.strokeStyle = PART_COLOR.head
    ctx.lineWidth = Math.max(2.5, w * 0.006)
    ctx.beginPath(); ctx.moveTo((ls.x + rs.x) / 2, (ls.y + rs.y) / 2); ctx.lineTo(head.x, head.y); ctx.stroke()
  }
  for (let i = 0; i < kp.length; i++) {
    const p = kp[i]
    if (!p || p.v < 0.3) continue
    ctx.globalAlpha = 1
    ctx.fillStyle = i === 0 ? PART_COLOR.head : '#ffd33d'
    ctx.beginPath(); ctx.arc(p.x, p.y, Math.max(2.5, w * 0.0045), 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = 'rgba(255,255,255,.85)'
    ctx.lineWidth = 1
    ctx.stroke()
  }
  ctx.globalAlpha = 1
}

const submitFrame = async (blob) => {
  analyzing.value = true
  result.value = null
  try {
    const fd = new FormData()
    fd.append('file', blob, 'challenge.jpg')
    const res = await challengePose(fd)
    result.value = res.data || {}
    drawFrame(blob)
    if (!result.value.detected) ElMessage.warning(result.value.message)
  } catch (e) {
    ElMessage.error('姿态识别失败：服务暂不可用，请稍后重试')
  } finally {
    analyzing.value = false
  }
}

const capture = async () => {
  const video = videoRef.value
  if (!video || !video.videoWidth) return
  const blob = await buildFrame(video, video.videoWidth, video.videoHeight)
  stopCamera()
  submitFrame(blob)
}

const onFileChange = async (ev) => {
  const file = ev.target.files?.[0]
  ev.target.value = ''
  if (!file) return
  const url = URL.createObjectURL(file)
  try {
    const img = await new Promise((res, rej) => {
      const i = new Image()
      i.onload = () => res(i); i.onerror = rej; i.src = url
    })
    if (cameraOn.value) stopCamera()
    const blob = await buildFrame(img, img.naturalWidth, img.naturalHeight)
    submitFrame(blob)
  } catch (e) {
    ElMessage.error('图片读取失败，请更换图片')
  } finally {
    URL.revokeObjectURL(url)
  }
}

const retake = () => {
  result.value = null
  if (frameUrl.value) { URL.revokeObjectURL(frameUrl.value); frameUrl.value = '' }
  startCamera()
}

onMounted(async () => {
  try {
    const res = await getDunhuangCategories()
    poseList.value = Object.entries(res.data?.key_poses || {}).map(([id, p]) => ({ id, ...p }))
    if (poseList.value.length) targetId.value = poseList.value[0].id
  } catch (e) {
    ElMessage.error('挑战姿态数据加载失败：参考数据服务暂不可用')
  }
})

onBeforeUnmount(() => {
  stopCamera()
  if (frameUrl.value) URL.revokeObjectURL(frameUrl.value)
})
</script>

<style scoped>
.pose-picker { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 16px; }
@media (max-width: 992px) { .pose-picker { grid-template-columns: repeat(2, 1fr); } }
.pose-mini { background: #fff; border: 1px solid #e4d9c3; border-radius: 8px; padding: 10px 12px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s; display: flex; flex-direction: column; gap: 4px; }
.pose-mini:hover { border-color: #8250df; }
.pose-mini.active { border-color: #8250df; box-shadow: 0 0 0 2px rgba(130, 80, 223, .15); }
.pose-mini b { font-size: 13px; }
.pm-desc { font-size: 12px; color: #8c959f; line-height: 1.5; }
.pose-skel { grid-column: 1 / -1; }
.ch-grid { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 16px; align-items: start; }
@media (max-width: 992px) { .ch-grid { grid-template-columns: 1fr; } }
.col-title { font-weight: 600; font-size: 14px; }
.cam-box { position: relative; width: 100%; border-radius: 8px; overflow: hidden; background: #0d1117;
  display: flex; align-items: center; justify-content: center; min-height: 320px; max-height: 560px; }
.cam-video { width: 100%; height: auto; max-height: 560px; object-fit: contain; transform: scaleX(-1); }
.cam-canvas { width: 100%; height: auto; max-height: 560px; object-fit: contain; }
.cam-placeholder { color: #8c959f; display: flex; flex-direction: column; gap: 10px; align-items: center; font-size: 13px; }
.cam-mask { position: absolute; inset: 0; background: rgba(255,255,255,.55); z-index: 4; }
.cam-actions { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.hidden-file { display: none; }
.cam-tip { font-size: 12px; color: #8c959f; margin: 10px 0 0; }
.result-body { display: flex; flex-direction: column; gap: 14px; }
.score-row { display: flex; align-items: center; gap: 18px; }
.score-meta { display: flex; flex-direction: column; gap: 6px; }
.tier { font-size: 22px; }
.score-sub { font-size: 12px; color: #57606a; }
.best-alert { margin: 0; }
.feat-list { display: flex; flex-direction: column; gap: 6px; }
.feat-row { display: flex; align-items: center; gap: 8px; font-size: 12px;
  background: #f6f8fa; border: 1px solid #eaeef2; border-radius: 6px; padding: 8px 10px; }
.feat-joint { width: 64px; flex-shrink: 0; color: #8250df; font-weight: 600; }
.feat-target { flex: 1; color: #57606a; }
.feat-status { flex-shrink: 0; }
.ok { color: #2da44e; font-size: 17px; }
.bad { color: #cf222e; font-size: 17px; }
.retry-btn { margin-top: 2px; }
.result-fail :deep(.el-result__icon) { margin-bottom: 4px; }
</style>
