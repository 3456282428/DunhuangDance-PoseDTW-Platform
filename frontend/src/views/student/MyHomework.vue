<template>
  <div>
    <PageHeader title="我的作业" subtitle="查看作业、提交作业（支持图片/视频/PDF）" />

    <el-table :data="homeworkList" stripe v-loading="loading">
      <el-table-column prop="homeworkId" label="编号" width="60" />
      <el-table-column prop="title" label="作业标题" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="得分" width="80" />
      <el-table-column prop="submitTime" label="提交时间" width="170" :formatter="(r)=>fmt(r.submitTime)" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" type="primary" :disabled="row.status === '已提交' || row.status === '已批改'" @click="openSubmit(row)">提交</el-button>
          <el-button size="small" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @pagination="fetchData"
    />

    <!-- 提交作业对话框 -->
    <el-dialog v-model="submitVisible" title="提交作业" width="560px">
      <el-alert v-if="currentHomework?.refVideoUrl" type="info" :closable="false" style="margin-bottom:12px"
        title="本作业支持AI智能评分：请上传横屏、全身可见、光线充足的练习视频" />
      <el-descriptions :column="1" border style="margin-bottom:16px">
        <el-descriptions-item label="作业标题">{{ currentHomework?.title }}</el-descriptions-item>
        <el-descriptions-item label="作业要求">{{ currentHomework?.content }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="作答内容">
          <el-input v-model="submitForm.content" type="textarea" :rows="4" placeholder="填写作业内容" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            :http-request="handleUpload"
            :on-remove="handleRemove"
            multiple
            accept="image/*,video/*,.pdf"
            :file-list="fileList"
          >
            <el-button type="primary" plain>选择文件（图片/视频/PDF）</el-button>
            <template #tip>
              <div class="el-upload__tip">支持图片、视频、PDF，单个不超过 100MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">提交作业</el-button>
      </template>
    </el-dialog>

    <!-- 作业详情对话框 -->
    <el-dialog v-model="detailVisible" title="作业详情" width="620px" @close="stopAiPolling">
      <template v-if="detail">
        <el-descriptions :column="1" border style="margin-bottom:16px">
          <el-descriptions-item label="作业标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="作业要求">{{ detail.content }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="我的作答">{{ detail.submitContent || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detail.submitTime ? fmt(detail.submitTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ detail.score ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="教师评语">{{ detail.teacherComment || '暂无' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="attachments.length" class="attachments">
          <h4>附件（{{ attachments.length }}）</h4>
          <div v-for="(att, i) in attachments" :key="i" class="att-item">
            <img v-if="isImage(att.url)" :src="att.url" class="att-img" @click="imgPreview = att.url" />
            <video v-else-if="isVideo(att.url)" :src="att.url" controls class="att-video"></video>
            <a v-else :href="att.url" target="_blank" class="att-link">{{ att.name }}</a>
          </div>
        </div>
        <!-- AI智能评分区块 -->
        <div v-if="detail.refVideoUrl" class="ai-section">
          <h4>AI智能评分</h4>
          <p class="ai-video-label">教师标准动作视频</p>
          <video :src="detail.refVideoUrl" controls style="width:100%;max-height:300px;border-radius:6px"></video>
          <div class="ai-actions">
            <el-button type="primary" size="small" :loading="aiScoring" :disabled="aiStatus === '评分中'"
              @click="startAiScore">开始AI评分</el-button>
            <el-button v-if="aiStatus === '已完成'" type="success" size="small" plain
              @click="goAiReport">查看AI报告</el-button>
            <el-tag v-if="aiStatus === '评分中'" type="primary" size="small">
              <el-icon is-loading style="vertical-align:-2px"><Loading /></el-icon>
              评分中
            </el-tag>
          </div>
          <!-- 实时进度条：评分服务上报的阶段进度 -->
          <div v-if="aiStatus === '评分中'" class="ai-progress">
            <el-progress :percentage="aiProgress" :stroke-width="10" striped striped-flow />
            <p class="ai-stage">{{ aiStage || '正在处理…' }}</p>
          </div>
          <AiScoreReport v-if="aiReportData || aiStatus" :report="aiReportData" :status="aiStatus" style="margin-top:12px" />
        </div>
      </template>
    </el-dialog>

    <el-image-viewer v-if="imgPreview" :url-list="[imgPreview]" @close="imgPreview = ''" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getMyHomework, submitHomework, getHomeworkDetail, triggerAiScore, getAiScoreResult } from '@/api/student'
import { uploadFiles } from '@/api/upload'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import AiScoreReport from '@/components/AiScoreReport.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const homeworkList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const submitVisible = ref(false)
const detailVisible = ref(false)
const currentHomework = ref(null)
const submitForm = reactive({ content: '' })
const uploadedFiles = ref([])
const fileList = ref([])
const submitting = ref(false)
const detail = ref(null)
const imgPreview = ref('')

const statusType = (s) => (s === '已批改' ? 'success' : s === '已提交' ? 'primary' : 'info')
const fmt = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d)) return t
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMyHomework({ pageNum: pageNum.value, pageSize: pageSize.value })
    homeworkList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const openSubmit = (row) => {
  currentHomework.value = row
  submitForm.content = ''
  uploadedFiles.value = []
  fileList.value = []
  submitVisible.value = true
}

const handleUpload = async ({ file, onSuccess, onError }) => {
  try {
    const res = await uploadFiles([file])
    const item = res.data && res.data[0]
    if (item) uploadedFiles.value.push(item)
    onSuccess(null)
  } catch (e) {
    onError(e)
  }
}

const handleRemove = (file) => {
  uploadedFiles.value = uploadedFiles.value.filter(f => f.name !== file.name)
}

const doSubmit = async () => {
  if (!submitForm.content.trim() && uploadedFiles.value.length === 0) {
    ElMessage.warning('请填写作答内容或上传附件')
    return
  }
  submitting.value = true
  try {
    await submitHomework(currentHomework.value.homeworkId, {
      content: submitForm.content,
      attachmentUrl: uploadedFiles.value.map(f => f.url).join(','),
      attachmentName: uploadedFiles.value.map(f => f.name).join(',')
    })
    ElMessage.success('提交成功')
    submitVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

const openDetail = async (row) => {
  stopAiPolling()
  aiStatus.value = ''
  aiReportData.value = null
  const res = await getHomeworkDetail(row.homeworkId)
  const d = res.data || {}
  detail.value = {
    ...d,
    homeworkId: d.homeworkId ?? row.homeworkId,
    refVideoUrl: d.refVideoUrl ?? row.refVideoUrl ?? '',
    aiScore: d.aiScore ?? null,
    aiReport: d.aiReport ?? null,
    aiStatus: d.aiStatus ?? null
  }
  detailVisible.value = true
  // 详情接口未返回AI评分字段时，兜底查询一次
  if (detail.value.refVideoUrl && detail.value.aiScore == null && detail.value.aiStatus == null) {
    try {
      const r = await getAiScoreResult(row.homeworkId)
      const s = r.data || {}
      if (s.aiStatus != null) detail.value.aiStatus = s.aiStatus
      if (s.aiScore != null) detail.value.aiScore = s.aiScore
      if (s.aiReport != null) detail.value.aiReport = s.aiReport
    } catch (e) {
      // 错误已由拦截器处理
    }
  }
  aiStatus.value = detail.value.aiStatus || ''
  aiReportData.value = parseReport(detail.value.aiReport)
}

// ============ 舞蹈AI智能评分 ============
const aiStatus = ref('')
const aiReportData = ref(null)
const aiScoring = ref(false)
// AI评分阶段进度（评分服务上报，0-100 + 当前阶段文案）
const aiProgress = ref(0)
const aiStage = ref('')
let aiPollTimer = null
let aiPollCount = 0
const AI_POLL_MAX = 200

const parseReport = (str) => {
  if (!str) return null
  try {
    return typeof str === 'object' ? str : JSON.parse(str)
  } catch (e) {
    return null
  }
}

// 跳转到独立AI评分报告页（与教师端一致的大屏三栏布局）
const goAiReport = () => {
  const hwId = detail.value?.homeworkId
  if (!hwId) return
  detailVisible.value = false
  stopAiPolling()
  router.push(`/student/homework/ai-report/${hwId}`)
}

const startAiScore = async () => {
  const hwId = detail.value?.homeworkId
  if (!hwId) return
  if (aiStatus.value === '评分中') {
    ElMessage.info('AI评分进行中')
    return
  }
  aiScoring.value = true
  try {
    const res = await triggerAiScore(hwId)
    const d = res.data || {}
    if (d.aiStatus === '已完成') {
      // 已有留存评分结果直接展示，不显示假的"评分中"进度条
      aiStatus.value = '已完成'
      aiProgress.value = 100
      if (d.aiScore != null) detail.value.aiScore = d.aiScore
      ElMessage.success('已有AI评分结果，直接展示')
    } else {
      aiStatus.value = d.aiStatus || '评分中'
      aiProgress.value = 0
      aiStage.value = ''
      aiReportData.value = null
      startAiPolling(hwId)
    }
  } catch (e) {
    // 显示后端返回的真实错误原因（如"未上传教师参考视频"等）
    const msg = e?.response?.data?.message || e?.message || 'AI评分触发失败，请稍后重试'
    ElMessage.error(msg)
    aiStatus.value = '失败'
  } finally {
    aiScoring.value = false
  }
}

const startAiPolling = (homeworkId) => {
  stopAiPolling()
  aiPollCount = 0
  const pollOnce = async () => {
    aiPollCount++
    try {
      const res = await getAiScoreResult(homeworkId)
      const d = res.data || {}
      if (d.aiStatus != null) aiStatus.value = d.aiStatus
      if (d.aiProgress != null) aiProgress.value = d.aiProgress
      if (d.aiStage != null) aiStage.value = d.aiStage
      if (d.aiReport) aiReportData.value = parseReport(d.aiReport)
      if (aiStatus.value === '已完成') {
        stopAiPolling()
        ElMessage.success('AI评分完成')
      } else if (aiStatus.value === '失败') {
        stopAiPolling()
        ElMessage.error('AI评分失败，请稍后重试')
      }
    } catch (e) {
      // 查询失败时继续轮询
    } finally {
      if (aiPollCount >= AI_POLL_MAX) {
        stopAiPolling()
        ElMessage.warning('AI评分超时，请稍后查看结果')
      }
    }
  }
  pollOnce() // 立即查询一次，缓存结果/已有进度秒级显示
  aiPollTimer = setInterval(pollOnce, 3000)
}

const stopAiPolling = () => {
  if (aiPollTimer) {
    clearInterval(aiPollTimer)
    aiPollTimer = null
  }
}

onMounted(fetchData)

onBeforeUnmount(stopAiPolling)

const attachments = computed(() => {
  if (!detail.value) return []
  const urls = (detail.value.attachmentUrl || '').split(',').filter(Boolean)
  const names = (detail.value.attachmentName || '').split(',').filter(Boolean)
  return urls.map((u, i) => ({ url: u, name: names[i] || u }))
})

const isImage = (url) => /\.(png|jpe?g|gif|bmp|webp)$/i.test(url)
const isVideo = (url) => /\.(mp4|webm|ogg|mov)$/i.test(url)
</script>

<style scoped>
.attachments h4 { margin: 8px 0; }
.att-item { margin-bottom: 12px; }
.att-img { max-width: 100%; max-height: 300px; border-radius: 6px; cursor: zoom-in; border: 1px solid #e4d9c3; }
.att-video { max-width: 100%; max-height: 300px; border-radius: 6px; }
.att-link { color: #1685a9; }
.ai-section { margin-top: 16px; padding-top: 12px; border-top: 1px solid #e4d9c3; }
.ai-section h4 { margin: 0 0 8px; font-size: 14px; color: #2c1810; }
.ai-video-label { margin: 0 0 6px; font-size: 12px; color: #6b5a4a; }
.ai-actions { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.ai-progress { margin-top: 10px; }
.ai-stage { margin: 6px 0 0; font-size: 12px; color: #6b5a4a; }
</style>
