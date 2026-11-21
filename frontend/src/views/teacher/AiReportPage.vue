<template>
  <div class="report-page">
    <PageHeader title="AI评分报告" :subtitle="subtitle">
      <template #actions>
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" plain :loading="rescoring" @click="reScore">重新评分</el-button>
      </template>
    </PageHeader>

    <!-- 实时评分进度条（评分服务上报的阶段进度） -->
    <el-card v-if="rescoring || data.aiStatus === '评分中'" shadow="never" class="ai-progress-card">
      <el-progress :percentage="aiProgress" :stroke-width="12" striped striped-flow />
      <p class="ai-progress-stage">{{ aiStage || '正在处理…' }}</p>
    </el-card>

    <template v-if="report">
      <!-- 大屏三栏布局：左=实时动态分析 / 中=学生视频 / 右=实时热力图+时序对齐 -->
      <div class="report-grid">
        <!-- 左侧栏：实时动态分析（随视频联动） -->
        <div class="grid-col grid-left">
          <el-card shadow="never" class="side-card grow">
            <template #header><span class="col-title">实时动态分析</span></template>
            <AiLiveCharts :frames="framesData" :current-time="nowTime" :playing="playing" @seek="seekAll" />
          </el-card>
        </div>

        <!-- 中间核心区：学生练习视频（视觉重心）+ 实时节奏对齐曲线（与视频两侧对齐） -->
        <div class="grid-col grid-center">
          <el-card shadow="never" class="side-card grow center-card">
            <template #header>
              <span class="col-title student">学生练习视频</span>
              <span class="hero-tag">{{ centerIsOverlay ? '骨架叠加' : '原视频' }}</span>
            </template>
            <template v-if="centerVideoUrl">
              <video ref="centerRef" :src="centerVideoUrl" controls
                preload="metadata" class="hero-video" @play="onPlay" @pause="onPause"
                @seeked="onSeeked" />
              <AiRhythmLive
                v-if="report.rhythm && report.rhythm.available && report.rhythm.chart"
                :rhythm="report.rhythm" :current-time="nowTime" :playing="playing"
                @seek="seekAll" />
            </template>
            <el-empty v-else description="无学生练习视频" :image-size="80" />
          </el-card>
        </div>

        <!-- 右侧栏：实时关节热力图 + DTW时序对齐分析 -->
        <div class="grid-col grid-right">
          <el-card shadow="never" class="side-card grow">
            <template #header><span class="col-title">关节偏差热力图</span></template>
            <AiJointHeatmap :frames="framesData" :current-time="nowTime" :playing="playing" />
          </el-card>

          <el-card shadow="never" class="side-card">
            <template #header><span class="col-title">时序对齐分析（DTW）</span></template>
            <img v-if="report.dtwVisUrl" :src="report.dtwVisUrl" class="dtw-img" alt="DTW对齐可视化" />
            <el-empty v-else description="重新评分后生成DTW对齐分析图" :image-size="60" />
            <div class="dtw-stats">
              <div class="dtw-stat">
                <span class="ds-label">DTW距离</span>
                <b class="ds-val">{{ report.dtwDistance != null ? report.dtwDistance.toFixed(4) : '--' }}</b>
              </div>
              <div class="dtw-stat">
                <span class="ds-label">节奏弯曲成本</span>
                <b class="ds-val">{{ report.rhythmCost != null ? report.rhythmCost.toFixed(4) : '--' }}</b>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 总览统计：横向排布（全宽） -->
      <el-card shadow="never" class="mt14">
        <template #header><span class="col-title">总览统计</span></template>
        <AiScoreOverview :report="report" :frames="framesData" :worst-frames="report.worstFrames || []" @seek="seekAll" />
      </el-card>

      <!-- 节奏对齐分析：音乐节拍 × 动作节拍（视觉+听觉融合） -->
      <el-card shadow="never" class="mt14">
        <template #header><span class="col-title">节奏对齐分析（音乐节拍 × 动作节拍）</span></template>
        <AiRhythmChart :rhythm="report.rhythm || null" @seek="seekAll" />
      </el-card>

      <!-- AI教练点评：豆包多模态语义增强（通道二） -->
      <el-card shadow="never" class="mt14">
        <template #header><span class="col-title">AI教练点评（豆包语义增强）</span></template>
        <AiSemanticCoach
          :semantic="report.semantic || null"
          :target-id="submissionId"
          :is-student="isStudent"
          @analyzed="loadData"
        />
      </el-card>

      <!-- 动作综合分析：总体评价 / 优点 / 不足 / 训练建议 -->
      <el-card shadow="never" class="mt14">
        <template #header><span class="col-title">动作综合分析</span></template>
        <AiAnalysis :analysis="report.analysis || null" @seek="seekAll" />
      </el-card>

      <!-- 动作偏差最大的对比截图 -->
      <el-card v-if="report.worstFrames && report.worstFrames.length" shadow="never" class="mt14">
        <template #header><span class="col-title">动作问题最大的瞬间</span></template>
        <div class="worst-grid">
          <div v-for="wf in report.worstFrames" :key="wf.rank" class="worst-item" @click="seekAll(wf.teacher_time)">
            <img :src="bustUrl(wf.url)" class="worst-img" :alt="'动作偏差对比截图' + wf.rank" />
            <div class="worst-caption">
              <el-tag type="danger" size="small">问题帧 #{{ wf.rank }}</el-tag>
              <span>时间点 {{ formatTime(wf.teacher_time) }}</span>
              <span>问题部位：{{ wf.worst_part }}</span>
              <span>平均偏差约 {{ Math.round(wf.deviation * 180) }}°</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 最下方：合成对比视频（全宽）+ 四组视频（师生 × 原视频/骨架叠加，全部同步） -->
      <el-card shadow="never" class="mt14">
        <template #header><span class="col-title">对比视频（五路同步）</span></template>
        <div class="video-block cmp-hero">
          <p class="video-label">师生并排对比 · 合成视频（骨架叠加 + 实时记分）</p>
          <video v-if="report.comparisonVideo" ref="comparisonRef" :src="report.comparisonVideo" controls
            preload="metadata" class="cmp-video cmp-video-wide" @play="onPlay" @pause="onPause"
            @seeked="onSeeked" />
          <el-empty v-else description="暂无合成对比视频，重新评分后生成" :image-size="60" />
        </div>
        <div class="quad-grid">
          <div class="video-block">
            <p class="video-label teacher">教师示范 · 原视频</p>
            <video v-if="data.refVideoUrl" ref="teacherOrigRef" :src="data.refVideoUrl" controls
              preload="metadata" class="cmp-video" @play="onPlay" @pause="onPause"
              @seeked="onSeeked" />
            <el-empty v-else description="无教师参考视频" :image-size="60" />
          </div>
          <div class="video-block">
            <p class="video-label teacher">教师示范 · 骨架叠加</p>
            <video v-if="report.processedTeacherVideo" ref="teacherProcRef"
              :src="report.processedTeacherVideo" controls preload="metadata" class="cmp-video"
              @play="onPlay" @pause="onPause" @seeked="onSeeked" />
            <el-empty v-else description="暂无预处理视频，重新评分后生成" :image-size="60" />
          </div>
          <div class="video-block">
            <p class="video-label student">学生练习 · 原视频</p>
            <video v-if="data.studentVideoUrl" ref="studentOrigRef" :src="data.studentVideoUrl" controls
              preload="metadata" class="cmp-video" @play="onPlay" @pause="onPause"
              @seeked="onSeeked" />
            <el-empty v-else description="无学生练习视频" :image-size="60" />
          </div>
          <div class="video-block">
            <p class="video-label student">学生练习 · 骨架叠加</p>
            <video v-if="report.processedStudentVideo" ref="studentProcRef"
              :src="report.processedStudentVideo" controls preload="metadata" class="cmp-video"
              @play="onPlay" @pause="onPause" @seeked="onSeeked" />
            <el-empty v-else description="暂无预处理视频，重新评分后生成" :image-size="60" />
          </div>
        </div>
      </el-card>
    </template>

    <el-card v-else shadow="never">
      <el-empty description="暂无AI评分报告，点击右上角「重新评分」生成" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import * as teacherApi from '@/api/teacher'
import * as studentApi from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'
import AiScoreOverview from '@/components/AiScoreOverview.vue'
import AiLiveCharts from '@/components/AiLiveCharts.vue'
import AiJointHeatmap from '@/components/AiJointHeatmap.vue'
import AiAnalysis from '@/components/AiAnalysis.vue'
import AiRhythmChart from '@/components/AiRhythmChart.vue'
import AiRhythmLive from '@/components/AiRhythmLive.vue'
import AiSemanticCoach from '@/components/AiSemanticCoach.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
// 教师路由用 :submissionId，学生路由用 :homeworkId
const submissionId = route.params.submissionId || route.params.homeworkId
// 根据路由前缀自动切换教师/学生API
const isStudent = route.path.startsWith('/student')
const api = isStudent ? studentApi : teacherApi

const data = ref({})
const report = ref(null)
const rescoring = ref(false)
// AI评分阶段进度（评分服务上报，0-100 + 当前阶段文案）
const aiProgress = ref(0)
const aiStage = ref('')
// 逐帧偏差序列（实时图表/热力图/总览共用数据源）
const framesData = ref([])
// 共享播放时钟（教师预处理视频时间轴，秒）与播放状态
const nowTime = ref(0)
const playing = ref(false)

const subtitle = computed(() => {
  const parts = [`提交编号 #${submissionId}`]
  if (data.value.aiScore != null) parts.push(`AI得分 ${data.value.aiScore}`)
  if (data.value.aiScoredAt) parts.push(`评分时间 ${String(data.value.aiScoredAt).replace('T', ' ').slice(0, 19)}`)
  return parts.join(' · ')
})

const parseReport = (str) => {
  if (!str) return null
  try { return typeof str === 'object' ? str : JSON.parse(str) } catch (e) { return null }
}

// 秒 → mm:ss
const formatTime = (sec) => {
  if (sec == null || isNaN(sec)) return '--'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 视频URL加时间戳防缓存（评分刚完成时浏览器可能缓存了旧响应）
const bustUrl = (url) => {
  if (!url) return ''
  const ts = data.value.aiScoredAt ? new Date(data.value.aiScoredAt).getTime() : Date.now()
  return url + (url.includes('?') ? '&' : '?') + 't=' + ts
}

// 中间核心区视频：优先骨架叠加预处理视频，无则回退原视频
const centerVideoUrl = computed(() => {
  const r = report.value
  if (!r) return ''
  return r.processedStudentVideo || data.value.studentVideoUrl || ''
})
const centerIsOverlay = computed(() => !!(report.value && report.value.processedStudentVideo))

const loadData = async () => {
  const res = await api.getAiScoreResult(submissionId)
  data.value = res.data || {}
  // 师生原视频与产物视频一样加时间戳防缓存：原视频文件可能被同名替换（大视频转码压缩），
  // 否则浏览器会混用旧缓存分段与新内容，导致播放暂停/回退
  if (data.value.refVideoUrl) data.value.refVideoUrl = bustUrl(data.value.refVideoUrl)
  if (data.value.studentVideoUrl) data.value.studentVideoUrl = bustUrl(data.value.studentVideoUrl)
  const r = parseReport(data.value.aiReport)
  if (r) {
    r.processedTeacherVideo = bustUrl(r.processedTeacherVideo)
    r.processedStudentVideo = bustUrl(r.processedStudentVideo)
    if (r.comparisonVideo) r.comparisonVideo = bustUrl(r.comparisonVideo)
    if (r.dtwVisUrl) r.dtwVisUrl = bustUrl(r.dtwVisUrl)
    if (r.jointHeatmapUrl) r.jointHeatmapUrl = bustUrl(r.jointHeatmapUrl)
  }
  report.value = r
  // 拉取逐帧偏差序列（实时/总览/热力图共用，同一数据口径）
  framesData.value = []
  if (r && r.framesDataUrl) {
    try {
      const resp = await fetch(bustUrl(r.framesDataUrl))
      const j = await resp.json()
      framesData.value = j.frames || []
    } catch (e) {
      framesData.value = []
    }
  }
}

// ============ 重新评分（显式强制，覆盖留存结果） ============
let pollTimer = null
let pollCount = 0
const reScore = async () => {
  try {
    await ElMessageBox.confirm('将重新执行AI评分并覆盖当前结果，确定继续？', '重新评分', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) { return }
  rescoring.value = true
  try {
    await api.triggerAiScore(submissionId, true)
    data.value = { ...data.value, aiStatus: '评分中' }
    startPolling()
  } catch (e) {
    rescoring.value = false
  }
}

const startPolling = () => {
  stopPolling()
  pollCount = 0
  pollTimer = setInterval(async () => {
    pollCount++
    try {
      const res = await api.getAiScoreResult(submissionId)
      const d = res.data || {}
      data.value = { ...data.value, ...d }
      if (d.aiProgress != null) aiProgress.value = d.aiProgress
      if (d.aiStage != null) aiStage.value = d.aiStage
      if (d.aiStatus === '已完成') {
        stopPolling()
        rescoring.value = false
        await loadData()
        ElMessage.success('AI评分完成')
      } else if (d.aiStatus === '失败') {
        stopPolling()
        rescoring.value = false
        ElMessage.error('AI评分失败，请稍后重试')
      }
    } catch (e) {
      // 查询失败继续轮询
    } finally {
      if (pollCount >= 200) {
        stopPolling()
        rescoring.value = false
        ElMessage.warning('AI评分超时，请稍后查看结果')
      }
    }
  }, 3000)
}
const stopPolling = () => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

// ============ 六路视频同步播放（中区学生视频 + 合成对比 + 底部四路） ============
const centerRef = ref(null)
const comparisonRef = ref(null)
const teacherOrigRef = ref(null)
const teacherProcRef = ref(null)
const studentOrigRef = ref(null)
const studentProcRef = ref(null)

const videos = () => [centerRef.value, comparisonRef.value, teacherOrigRef.value, teacherProcRef.value,
  studentOrigRef.value, studentProcRef.value]
  .filter(v => v)

// 主时钟视频：教师族优先。framesData 时间轴即教师预处理视频时间轴，
// 全部派生关系都以「主时钟 → 其它视频」单向计算，杜绝多源互推造成往复 seek。
const masterVideo = () =>
  teacherProcRef.value || teacherOrigRef.value || centerRef.value ||
  comparisonRef.value || studentProcRef.value || studentOrigRef.value

let syncing = false

// 程序化 seek 守卫：被本组件写入过 currentTime 的元素，在其后 400ms 内
// 到达的 seeked 视为程序化写入而忽略，切断「写入 → seeked → 再广播」的
// 反馈回路；用时间戳自愈，避免计数法在连续写入下残留导致用户拖动失效
const progSeek = new WeakMap()
const writeTime = (v, t) => {
  if (!v || !v.duration || isNaN(v.duration)) return
  const clamped = Math.max(0, Math.min(t, v.duration - 0.05))
  if (Math.abs(v.currentTime - clamped) < 0.02) return
  progSeek.set(v, Date.now() + 400)
  v.currentTime = clamped
}

const withOthers = (src, fn) => {
  if (syncing) return
  syncing = true
  videos().forEach(v => { if (v !== src) fn(v) })
  setTimeout(() => { syncing = false }, 200)
}

// ============ DTW 内容级播放对齐 ============
// 评分管线沿DTW对齐路径导出逐帧锚点 {t: 教师秒, st: 对齐学生秒}，
// 播放时按锚点插值映射，等效无限精度的"抽帧/变速"对齐，且零转码零画质损失。
// 旧任务frames.json无st字段时自动回退线性比例映射。
const dtwAnchors = computed(() => framesData.value.filter(f => f.st !== undefined))

// 锚点插值：anchors按key递增，二分查找+线性插值，越界取端点
const interpAnchors = (anchors, key, val, outKey) => {
  if (!anchors.length) return null
  if (val <= anchors[0][key]) return anchors[0][outKey]
  const last = anchors[anchors.length - 1]
  if (val >= last[key]) return last[outKey]
  let lo = 0, hi = anchors.length - 1
  while (hi - lo > 1) {
    const mid = (lo + hi) >> 1
    if (anchors[mid][key] <= val) lo = mid; else hi = mid
  }
  const a = anchors[lo], b = anchors[hi]
  const span = b[key] - a[key]
  return span > 0 ? a[outKey] + (b[outKey] - a[outKey]) * (val - a[key]) / span : a[outKey]
}

const teacherTimeToStudent = (t) =>
  dtwAnchors.value.length ? interpAnchors(dtwAnchors.value, 't', t, 'st') : null
const studentTimeToTeacher = (s) =>
  dtwAnchors.value.length ? interpAnchors(dtwAnchors.value, 'st', s, 't') : null

const isTeacherVideo = (v) =>
  v === teacherOrigRef.value || v === teacherProcRef.value || v === centerRef.value
const isStudentVideo = (v) => v === studentOrigRef.value || v === studentProcRef.value

// 播放进度映射：师生跨族优先DTW内容对齐；同族视频时间轴本就一致、
// 对比视频为服务端DTW渲染产物，均按比例映射
const mappedTime = (src, target) => {
  if (!src.duration || !target.duration || isNaN(src.duration) || isNaN(target.duration)) {
    return src.currentTime
  }
  if (isTeacherVideo(src) && isStudentVideo(target)) {
    const s = teacherTimeToStudent(src.currentTime)
    if (s != null) return Math.max(0, Math.min(s, target.duration - 0.05))
  } else if (isStudentVideo(src) && isTeacherVideo(target)) {
    const t = studentTimeToTeacher(src.currentTime)
    if (t != null) return Math.max(0, Math.min(t, target.duration - 0.05))
  }
  return src.currentTime * (target.duration / src.duration)
}

// 以 src 为基准单向派生其它视频；threshold>0 时只纠正偏差超过阈值的视频
const deriveFrom = (src, threshold = 0) => {
  if (!src) return
  videos().forEach(v => {
    if (v === src) return
    const target = mappedTime(src, v)
    if (threshold > 0 && Math.abs(v.currentTime - target) <= threshold) return
    writeTime(v, target)
  })
}

const onPlay = (e) => withOthers(e.target, v => { if (v.paused) v.play().catch(() => {}) })
// 暂停只同步暂停状态，不做位置纠偏：师生视频时长不同（本项目实测 73.9s vs
// 125.5s），DTW 映射要求在播放中以约 0.59× 速率前进，强行按映射“对齐”必然
// 把视频拉回——这正是用户反馈的进度回退/跳跃。对齐只发生在用户主动寻址时。
const onPause = (e) => withOthers(e.target, v => { if (!v.paused) v.pause() })
// 仅"用户拖动"才更换时间基准；程序化写入产生的 seeked 直接忽略、不再级联广播
const onSeeked = (e) => {
  const el = e.target
  if (Date.now() < (progSeek.get(el) || 0)) return
  const m = masterVideo()
  if (m && el !== m) writeTime(m, mappedTime(el, m))
  deriveFrom(masterVideo())
}

// ============ 共享播放时钟 + 跨模块联动寻址 ============
// framesData时间轴 = 教师预处理视频（30fps）；学生视频经DTW锚点映射寻址
const framesDuration = () =>
  framesData.value.length ? framesData.value[framesData.value.length - 1].t : 0

const seekAll = (t) => {
  const fd = framesDuration()
  videos().forEach(v => {
    if (!v.duration) return
    let target
    if (isStudentVideo(v)) {
      const s = teacherTimeToStudent(t)
      target = s != null ? s : t * (v.duration / (fd || v.duration))
    } else {
      target = fd > 0 ? t * (v.duration / fd) : t
    }
    writeTime(v, target)
  })
  nowTime.value = t
}

let clockTimer = null
onMounted(() => {
  loadData()
  // 100ms共享时钟：驱动实时模块刷新（延迟≤100ms，满足≤200ms要求）
  clockTimer = setInterval(() => {
    const v = masterVideo()
    if (!v) return
    const fd = framesDuration()
    let tf
    if (fd > 0 && v.duration) {
      if (isStudentVideo(v)) {
        // 时钟源是学生视频时，经DTW锚点反查教师时间轴
        const t = studentTimeToTeacher(v.currentTime)
        tf = t != null ? t : v.currentTime * (fd / v.duration)
      } else {
        tf = v.currentTime * (fd / v.duration)
      }
    } else {
      tf = v.currentTime
    }
    nowTime.value = tf
    playing.value = !v.paused && !v.ended
    // 播放中不做任何纠偏：师生视频时长不同，DTW 映射要求在播放中按比例差速
    // 前进，任何按映射写回 currentTime 都会表现为可见的进度回退/跳跃。
    // 同步只在用户主动播放/暂停/拖动时发生。
  }, 100)
})

onBeforeUnmount(() => {
  stopPolling()
  if (clockTimer) { clearInterval(clockTimer); clockTimer = null }
})
</script>

<style scoped>
/* ============ 敦煌主题：朱砂红 + 石青 + 金箔 + 米纸 ============ */
.report-page { background: var(--dh-bg, #f5f0e6); min-height: 100vh; }

/* 紧凑化：收紧全部卡片内外边距 */
.report-page :deep(.el-card__header) { padding: 8px 12px; }
.report-page :deep(.el-card__body) { padding: 10px 12px; }

/* 卡片敦煌化：米纸底 + 金箔边框 */
.report-page :deep(.el-card) {
  background: var(--dh-card-bg, #faf7f0);
  border: 1px solid var(--dh-border, #d4c5a9) !important;
  border-radius: 8px;
}
.report-page :deep(.el-card__header) {
  border-bottom: 1px solid var(--dh-border, #d4c5a9);
  background: linear-gradient(135deg, rgba(166, 42, 32, 0.04) 0%, rgba(201, 168, 76, 0.04) 100%);
}

/* CSS Grid 三栏布局：gap=0 消除全部缝隙，卡片边框充当分隔线 */
.report-grid {
  display: grid;
  grid-template-columns: 1fr 1.8fr 1fr;
  gap: 0;
  align-items: stretch;
}
.grid-col { display: flex; flex-direction: column; min-width: 0; }
.grid-col :deep(.el-card) { border-radius: 0; box-shadow: none !important; width: 100% !important; }
.grid-left :deep(.el-card) { border-right: 0; }
.grid-center :deep(.el-card) { border-left: 0; border-right: 0; }
.grid-right :deep(.el-card) { border-left: 0; }

.side-card { margin-bottom: 0; }
.side-card + .side-card { margin-bottom: 0; margin-top: 4px; }
/* grow卡片拉伸填满栏高（等高三栏，消除留白） */
.grow { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.grow :deep(.el-card__body) { flex: 1; min-height: 0; overflow: hidden; }
.mt14 { margin-top: 10px; }
.ai-progress-card { margin-bottom: 10px; }
.ai-progress-stage { margin: 6px 0 0; font-size: 12px; color: var(--dh-text-light, #6b5a4a); }
.col-title { font-weight: 600; font-size: 14px; color: var(--dh-text, #2c1810); }
.col-title.teacher { color: var(--dh-secondary, #1a6b5c); }
.col-title.student { color: var(--dh-primary, #a62a20); }
.hero-tag { margin-left: 8px; font-size: 12px; color: var(--dh-text-light, #6b5a4a); font-weight: 400; }
.center-card :deep(.el-card__body) { display: flex; flex-direction: column; align-items: stretch; justify-content: flex-start; }
/* 固定高度盒：元素始终占满卡片宽度×380px，竖版视频由object-fit居中留黑边，宽度与下方曲线严格一致 */
.hero-video { width: 100%; height: 380px; object-fit: contain; background: #000; border-radius: 6px; display: block; }
.dtw-img { width: 100%; height: 150px; object-fit: contain; display: block; background: #0d1117; border-radius: 6px; }
.dtw-stats { display: flex; gap: 16px; margin-top: 10px; padding-top: 10px; border-top: 1px dashed var(--dh-border, #d4c5a9); }
.dtw-stat { flex: 1; }
.ds-label { display: block; font-size: 11px; color: var(--dh-text-light, #6b5a4a); letter-spacing: 0.5px; }
.ds-val { display: block; font-size: 18px; color: var(--dh-primary, #a62a20); font-variant-numeric: tabular-nums; margin-top: 2px; }
.quad-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.cmp-hero { margin-bottom: 12px; }
.cmp-video-wide { max-height: 460px; }
.video-block { margin: 0; }
.video-label { margin: 0 0 4px; font-size: 13px; font-weight: 600; }
.video-label.teacher { color: var(--dh-secondary, #1a6b5c); }
.video-label.student { color: var(--dh-primary, #a62a20); }
.cmp-video { width: 100%; max-height: 250px; background: #000; border-radius: 6px; }
.worst-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 12px; }
.worst-item { cursor: pointer; }
.worst-img { width: 100%; background: #000; border-radius: 6px; display: block; }
.worst-caption { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 4px 12px; align-items: center; font-size: 13px; color: var(--dh-text-light, #6b5a4a); }
@media (max-width: 1100px) { .report-grid { grid-template-columns: 1fr; gap: 6px; } .quad-grid { grid-template-columns: 1fr; } .grow { min-height: 320px; } }
</style>
