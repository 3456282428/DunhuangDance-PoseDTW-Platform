<template>
  <div>
    <PageHeader title="作业管理" subtitle="查看学员提交情况与批改">
      <template #actions>
        <el-button type="primary" @click="assignVisible = true">布置作业</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" style="margin-bottom:14px">
      <div class="filter">
        <el-select v-model="filters.classId" placeholder="按班级" clearable style="width:170px" @change="search">
          <el-option v-for="c in classOptions" :key="c.classId" :label="c.className" :value="c.classId" />
        </el-select>
        <el-select v-model="filters.status" placeholder="按状态" clearable style="width:130px" @change="search">
          <el-option label="未截止" value="未截止" />
          <el-option label="已截止" value="已截止" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </el-card>

    <el-table :data="homeworkList" stripe v-loading="loading">
      <el-table-column prop="homeworkId" label="编号" width="55" />
      <el-table-column prop="title" label="作业标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="className" label="班级" width="120" />
      <el-table-column label="截止时间" width="165">
        <template #default="{ row }">{{ fmt(row.deadline) || '不限' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="85">
        <template #default="{ row }">
          <el-tag :type="row.deadlineStatus === '已截止' ? 'info' : 'success'" size="small">{{ row.deadlineStatus || '未截止' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交情况" width="110">
        <template #default="{ row }">{{ row.submittedCount || 0 }} / {{ row.totalStudents || 0 }}</template>
      </el-table-column>
      <el-table-column label="平均分" width="80">
        <template #default="{ row }">{{ row.avgScore != null ? row.avgScore : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openSubmissions(row)">查看提交</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @pagination="fetchData"
    />

    <!-- 布置作业对话框 -->
    <el-dialog v-model="assignVisible" title="布置作业" width="520px">
      <el-form label-width="80px">
        <el-form-item label="班级">
          <el-select v-model="assignForm.classId" placeholder="请选择班级" style="width:100%">
            <el-option v-for="c in classOptions" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题">
          <el-input v-model="assignForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        <el-form-item label="作业要求">
          <el-input v-model="assignForm.content" type="textarea" :rows="4" placeholder="请输入作业要求" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="assignForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择截止时间（可选）" style="width:100%" />
        </el-form-item>
        <el-form-item label="标准视频">
          <div style="width:100%">
            <el-upload v-if="!assignForm.refVideoUrl" ref="refVideoUploadRef" accept="video/*" :limit="1"
              :auto-upload="false" :show-file-list="false" :on-change="handleRefVideoChange">
              <el-button size="small" type="primary" plain :loading="refVideoUploading">
                {{ refVideoUploading ? '上传中…' : '上传标准动作视频（可选）' }}
              </el-button>
            </el-upload>
            <div v-else class="ref-video-info">
              <span class="ref-video-name">{{ assignForm.refVideoName || '标准动作视频' }}</span>
              <el-button link type="danger" size="small" @click="removeRefVideo">删除</el-button>
            </div>
            <div class="upload-tip">上传标准动作视频后，学生提交练习视频即可获得AI智能评分。拍摄建议：横屏、全身可见、光线充足</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="doAssign">发布</el-button>
      </template>
    </el-dialog>

    <!-- 查看提交对话框 -->
    <el-dialog v-model="submissionVisible" title="学员提交情况" width="860px" @close="stopAiPolling">
      <el-alert type="success" :closable="false" style="margin-bottom:12px"
        :title="`应交 ${submissions.length} 人 · 已交 ${subStats.submitted} 人 · 待批改 ${subStats.pending} 人 · 已批改 ${subStats.graded} 人 · 提交率 ${subStats.rate}% · 平均分 ${subStats.avg}`" />
      <div class="sub-filter">
        <el-radio-group v-model="subFilter" size="small">
          <el-radio-button label="全部">全部</el-radio-button>
          <el-radio-button label="待批改">待批改</el-radio-button>
          <el-radio-button label="已批改">已批改</el-radio-button>
          <el-radio-button label="未提交">未提交</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="filteredSubmissions" stripe max-height="360">
        <el-table-column prop="studentName" label="学员" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="subTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.submitTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="得分" width="90">
          <template #default="{ row }">
            <span v-if="row.score != null">
              {{ row.score }}
              <el-tag v-if="Number(row.score) >= 90" type="warning" size="small" effect="dark">优秀</el-tag>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="AI分" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.aiStatus === '评分中'" type="primary" size="small">
              <el-icon is-loading style="vertical-align:-2px"><Loading /></el-icon>
              评分中
            </el-tag>
            <el-tag v-else-if="row.aiStatus === '失败'" type="danger" size="small">失败</el-tag>
            <span v-else-if="row.aiScore != null" class="ai-score-link"
              :style="{ color: aiScoreColor(row.aiScore), fontWeight: 600 }"
              @click="goAiReport(row)">{{ row.aiScore }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="70">
          <template #default="{ row }">
            <el-tag v-if="hasAttach(row)" size="small">有</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button size="small" @click="openGrade(row)">批改</el-button>
            <el-button v-if="row.aiScore != null && row.aiStatus === '已完成'" size="small" type="success" plain
              @click="goAiReport(row)">查看报告</el-button>
            <el-button v-else size="small" type="primary" plain @click="handleAiScore(row)">AI评分</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="selectedRow" class="sub-detail">
        <p><b>作答内容：</b>{{ selectedRow.content || '（未填写）' }}</p>
        <div v-if="selectedAttachments.length">
          <p><b>附件：</b></p>
          <div v-for="(att, i) in selectedAttachments" :key="i" class="att-item">
            <img v-if="isImage(att.url)" :src="att.url" class="att-img" @click="imgPreview = att.url" />
            <video v-else-if="isVideo(att.url)" :src="att.url" controls class="att-video"></video>
            <a v-else :href="att.url" target="_blank" class="att-link">{{ att.name }}</a>
          </div>
        </div>
        <p v-if="selectedRow.teacherComment"><b>评语：</b>{{ selectedRow.teacherComment }}</p>
      </div>
    </el-dialog>

    <!-- 批改对话框 -->
    <el-dialog v-model="gradeVisible" title="批改作业" width="460px">
      <el-form label-width="80px">
        <el-form-item label="学员">{{ gradeForm.studentName }}</el-form-item>
        <el-form-item label="得分">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.comment" type="textarea" :rows="3" placeholder="请输入评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="doGrade">保存批改</el-button>
      </template>
    </el-dialog>

    <el-image-viewer v-if="imgPreview" :url-list="[imgPreview]" @close="imgPreview = ''" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherHomeworkList, getHomeworkSubmissions, gradeHomework, assignHomework, getTeacherClasses, triggerAiScore, getAiScoreResult } from '@/api/teacher'
import { uploadFiles } from '@/api/upload'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const homeworkList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filters = reactive({ classId: null, status: '' })
const fmt = (t) => t ? String(t).replace('T', ' ').slice(0, 16) : ''

const assignVisible = ref(false)
const assignForm = reactive({ classId: null, title: '', content: '', deadline: '', refVideoUrl: '', refVideoName: '' })
const assigning = ref(false)
const classOptions = ref([])
const refVideoUploading = ref(false)
const refVideoUploadRef = ref(null)

const submissionVisible = ref(false)
const submissions = ref([])
const selectedRow = ref(null)

const gradeVisible = ref(false)
const gradeForm = reactive({ submissionId: null, studentName: '', score: 0, comment: '' })
const grading = ref(false)
const imgPreview = ref('')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTeacherHomeworkList({
      pageNum: pageNum.value, pageSize: pageSize.value,
      classId: filters.classId || undefined,
      status: filters.status || undefined
    })
    homeworkList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => { pageNum.value = 1; fetchData() }
const resetFilter = () => { filters.classId = null; filters.status = ''; search() }

const doAssign = async () => {
  if (!assignForm.classId) {
    ElMessage.warning('请选择班级')
    return
  }
  if (!assignForm.title || !assignForm.content) {
    ElMessage.warning('请填写标题和要求')
    return
  }
  assigning.value = true
  try {
    await assignHomework({
      classId: assignForm.classId,
      title: assignForm.title,
      content: assignForm.content,
      deadline: assignForm.deadline || null,
      refVideoUrl: assignForm.refVideoUrl || null,
      refVideoName: assignForm.refVideoName || null
    })
    ElMessage.success('布置成功')
    assignVisible.value = false
    assignForm.classId = null
    assignForm.title = ''
    assignForm.content = ''
    assignForm.deadline = ''
    assignForm.refVideoUrl = ''
    assignForm.refVideoName = ''
    fetchData()
  } finally {
    assigning.value = false
  }
}

// 上传标准动作视频（选择文件后立即上传）
const handleRefVideoChange = async (uploadFile) => {
  if (uploadFile.status !== 'ready') return
  if (refVideoUploading.value) return
  const file = uploadFile.raw
  if (!file) return
  refVideoUploading.value = true
  try {
    const res = await uploadFiles([file])
    const item = res.data && res.data[0]
    if (item) {
      assignForm.refVideoUrl = item.url
      assignForm.refVideoName = file.name
    }
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    refVideoUploading.value = false
    refVideoUploadRef.value?.clearFiles()
  }
}

const removeRefVideo = () => {
  assignForm.refVideoUrl = ''
  assignForm.refVideoName = ''
}

const loadClasses = async () => {
  try {
    const res = await getTeacherClasses()
    classOptions.value = res.data || []
  } catch (e) {
    // 错误已由拦截器处理
  }
}

const openSubmissions = async (row) => {
  const res = await getHomeworkSubmissions(row.homeworkId)
  submissions.value = res.data || []
  subFilter.value = '全部'
  selectedRow.value = null
  submissionVisible.value = true
}

const hasAttach = (row) => !!(row.attachmentUrl && row.attachmentUrl.trim())

const openGrade = (row) => {
  selectedRow.value = row
  gradeForm.submissionId = row.submissionId
  gradeForm.studentName = row.studentName
  gradeForm.score = row.score || 0
  gradeForm.comment = row.teacherComment || ''
  gradeVisible.value = true
}

const doGrade = async () => {
  grading.value = true
  try {
    await gradeHomework(gradeForm.submissionId, {
      score: gradeForm.score,
      teacherComment: gradeForm.comment
    })
    ElMessage.success('批改成功')
    gradeVisible.value = false
    // 刷新当前提交列表
    if (selectedRow.value) {
      const res = await getHomeworkSubmissions(selectedRow.value.homeworkId)
      submissions.value = res.data || []
    }
    fetchData()
  } finally {
    grading.value = false
  }
}

const selectedAttachments = computed(() => {
  if (!selectedRow.value) return []
  const urls = (selectedRow.value.attachmentUrl || '').split(',').filter(Boolean)
  const names = (selectedRow.value.attachmentName || '').split(',').filter(Boolean)
  return urls.map((u, i) => ({ url: u, name: names[i] || u }))
})

const isImage = (url) => /\.(png|jpe?g|gif|bmp|webp)$/i.test(url)
const isVideo = (url) => /\.(mp4|webm|ogg|mov)$/i.test(url)
const fmtDateTime = (t) => t ? String(t).replace('T', ' ').slice(0, 19) : ''
const subTagType = (s) => ({ '已批改': 'success', '已提交': 'warning', '未提交': 'info' }[s] || 'info')

const subFilter = ref('全部')
const filteredSubmissions = computed(() => {
  if (subFilter.value === '全部') return submissions.value
  if (subFilter.value === '待批改') return submissions.value.filter(s => s.status === '已提交')
  return submissions.value.filter(s => s.status === subFilter.value)
})

const subStats = computed(() => {
  const total = submissions.value.length
  const submitted = submissions.value.filter(s => s.status === '已提交' || s.status === '已批改').length
  const pending = submissions.value.filter(s => s.status === '已提交').length
  const graded = submissions.value.filter(s => s.status === '已批改').length
  const scored = submissions.value.filter(s => s.score != null)
  const avg = scored.length
    ? (scored.reduce((a, b) => a + Number(b.score), 0) / scored.length).toFixed(1)
    : '—'
  return { submitted, pending, graded, rate: total ? Math.round(submitted * 100 / total) : 0, avg }
})

// ============ 舞蹈AI智能评分 ============
const aiScoreColor = (v) => {
  const n = Number(v)
  if (isNaN(n)) return '#6b5a4a'
  if (n < 60) return '#c13a2a'
  if (n < 80) return '#b3542e'
  if (n < 90) return '#c89a2e'
  return '#2c9678'
}

let aiPollTimer = null
let aiPollCount = 0
const AI_POLL_MAX = 200

// 跳转AI评分报告页（下一级页面，评分结果留存可直接查看）
const goAiReport = (row) => {
  router.push(`/teacher/homework/ai-report/${row.submissionId}`)
}

const handleAiScore = async (row) => {
  if (row.aiStatus === '评分中') {
    ElMessage.info('AI评分进行中')
    return
  }
  const hw = homeworkList.value.find(h => h.homeworkId === row.homeworkId)
  if (hw && 'refVideoUrl' in hw && !hw.refVideoUrl) {
    ElMessage.warning('该作业未设置标准动作视频，无法AI评分')
    return
  }
  try {
    const res = await triggerAiScore(row.submissionId)
    // 已有留存评分结果时后端直接返回已完成，不再重复评分
    if (res.data && res.data.aiStatus === '已完成') {
      row.aiStatus = '已完成'
      if (res.data.aiScore != null) row.aiScore = res.data.aiScore
      goAiReport(row)
      return
    }
    row.aiStatus = '评分中'
    startAiPolling(row)
  } catch (e) {
    // 错误已由拦截器处理
  }
}

const startAiPolling = (row) => {
  stopAiPolling()
  aiPollCount = 0
  aiPollTimer = setInterval(async () => {
    aiPollCount++
    try {
      const res = await getAiScoreResult(row.submissionId)
      const d = res.data || {}
      if (d.aiScore != null) row.aiScore = d.aiScore
      if (d.aiReport != null) row.aiReport = d.aiReport
      if (d.aiStatus != null) row.aiStatus = d.aiStatus
      if (row.aiStatus === '已完成') {
        stopAiPolling()
        goAiReport(row)
      } else if (row.aiStatus === '失败') {
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
  }, 3000)
}

const stopAiPolling = () => {
  if (aiPollTimer) {
    clearInterval(aiPollTimer)
    aiPollTimer = null
  }
}

onMounted(() => {
  fetchData()
  loadClasses()
})

onBeforeUnmount(stopAiPolling)
</script>

<style scoped>
.filter { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.sub-filter { margin-bottom: 10px; }
.sub-detail { margin-top: 16px; padding: 12px; background: #f7f8fa; border-radius: 6px; }
.sub-detail p { margin: 6px 0; }
.att-item { margin-bottom: 10px; }
.att-img { max-width: 100%; max-height: 300px; border-radius: 6px; cursor: zoom-in; border: 1px solid #e4d9c3; }
.att-video { max-width: 100%; max-height: 300px; border-radius: 6px; }
.att-link { color: #1685a9; }
.ref-video-info { display: flex; align-items: center; gap: 8px; width: 100%; }
.ref-video-name { font-size: 13px; color: #6b5a4a; word-break: break-all; }
.upload-tip { margin-top: 4px; font-size: 12px; color: #6b5a4a; line-height: 1.5; }
.ai-score-link { cursor: pointer; }
</style>
