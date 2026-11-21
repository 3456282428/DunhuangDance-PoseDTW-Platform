<template>
  <div>
    <PageHeader title="课程排班管理" subtitle="我的课表与调课申请">
      <template #actions>
        <el-button @click="openHistory">调课申请记录</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="fetchData"
        />
      </div>

      <el-table :data="scheduleList" stripe v-loading="loading">
        <el-table-column prop="scheduleId" label="编号" width="55" />
        <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
        <el-table-column prop="courseDate" label="日期" width="105" />
        <el-table-column label="时间" width="120">
          <template #default="{ row }">{{ row.startTime?.slice(0,5) }} - {{ row.endTime?.slice(0,5) }}</template>
        </el-table-column>
        <el-table-column prop="classroomName" label="教室" width="100" />
        <el-table-column label="状态" width="85">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调课状态" width="95">
          <template #default="{ row }">
            <el-tag v-if="reqMap[row.scheduleId]" size="small" :type="reqStatusType(reqMap[row.scheduleId].status)">
              {{ reqMap[row.scheduleId].status }}
            </el-tag>
            <span v-else style="color:#bbb;font-size:12px">未申请</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button
              size="small"
              :type="applyBtn(row).type"
              :disabled="row.status === '已取消' || applyBtn(row).disabled"
              @click="openApply(row)"
            >{{ applyBtn(row).text }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 申请调课弹窗 -->
    <el-dialog v-model="applyVisible" title="申请调课" width="500px">
      <el-alert
        :title="`原安排：${current?.courseDate || ''} ${current?.startTime?.slice(0,5) || ''}-${current?.endTime?.slice(0,5) || ''}（${current?.className || ''}）`"
        type="info" :closable="false" style="margin-bottom:12px"
      />
      <el-alert
        v-if="current && reqMap[current.scheduleId]"
        :title="`上一次调课申请：${reqMap[current.scheduleId].status}${reqMap[current.scheduleId].handleRemark ? '（' + reqMap[current.scheduleId].handleRemark + '）' : ''}`"
        :type="reqMap[current.scheduleId].status === '已拒绝' ? 'error' : 'success'"
        :closable="false" style="margin-bottom:12px"
      />
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="新日期" prop="newDate">
          <el-date-picker v-model="form.newDate" type="date" value-format="YYYY-MM-DD" placeholder="选择新上课日期" style="width:100%" @change="detectConflict" />
        </el-form-item>
        <el-form-item label="新时间段" prop="slot">
          <el-select v-model="form.slot" placeholder="按课程节次选择时间段" style="width:100%" @change="onSlotChange">
            <el-option-group v-for="g in slotGroups" :key="g.label" :label="g.label">
              <el-option
                v-for="s in g.options" :key="s.start"
                :label="`${s.start.slice(0,5)}-${s.end.slice(0,5)}（${g.label}）`"
                :value="s.start"
              />
            </el-option-group>
          </el-select>
        </el-form-item>

        <!-- 冲突检测结果 -->
        <el-alert
          v-if="conflictChecking" title="正在检测该时段是否有其他课程..." type="info" :closable="false" style="margin-bottom:12px"
        />
        <template v-else-if="conflicts.length">
          <el-alert
            v-for="c in conflicts" :key="c.scheduleId" type="warning" :closable="false" style="margin-bottom:8px"
            :title="`该时段已有课程：${conflictText(c)}（${c.reasons.join('、')}）`"
          />
          <el-alert
            v-if="conflicts.length === 1" type="success" :closable="false" style="margin-bottom:12px"
            title="可选择与该课程交换时间段（审批通过后两节课时间互换）"
          >
            <div style="margin-top:6px">
              <el-radio-group v-model="swapMode" size="small">
                <el-radio-button value="swap">与该课程交换</el-radio-button>
                <el-radio-button value="reselect">我再换个时段</el-radio-button>
              </el-radio-group>
            </div>
          </el-alert>
          <el-alert
            v-else type="error" :closable="false" style="margin-bottom:12px"
            title="该时段存在多节冲突课程，无法两两交换，请更换日期或时间段"
          />
        </template>
        <el-alert
          v-else-if="conflictChecked" type="success" :closable="false" style="margin-bottom:12px"
          title="该时段无其他课程，可正常申请调课"
        />

        <el-form-item label="调课原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明调课原因，便于教务审核" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 调课记录弹窗 -->
    <el-dialog v-model="historyVisible" title="我的调课申请记录" width="680px">
      <el-table :data="historyList" stripe max-height="420">
        <el-table-column prop="scheduleId" label="排课ID" width="80" />
        <el-table-column label="类型" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.requestType === '交换' ? 'warning' : ''">{{ row.requestType || '调课' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原时间" min-width="150">
          <template #default="{ row }">{{ row.originalDate }} {{ row.originalStartTime?.slice(0,5) }}</template>
        </el-table-column>
        <el-table-column label="新时间" min-width="150">
          <template #default="{ row }">{{ row.newDate }} {{ row.newStartTime?.slice(0,5) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="reqStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyList.length" description="暂无调课申请" />
    </el-dialog>

    <!-- 课程详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`课程详情 - ${detail?.className || ''}`" width="560px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="日期">{{ detail.courseDate }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.startTime?.slice(0,5) }} - {{ detail.endTime?.slice(0,5) }}</el-descriptions-item>
        <el-descriptions-item label="教师">{{ detail.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="教室">{{ detail.classroomName }}</el-descriptions-item>
        <el-descriptions-item label="课时">{{ detail.hours }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>学员名单（{{ detail?.studentList?.length || 0 }}人）</el-divider>
      <el-table :data="detail?.studentList || []" stripe max-height="260" size="small">
        <el-table-column prop="studentName" label="学员" />
        <el-table-column prop="status" label="考勤状态" width="110" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getTeacherSchedule, getScheduleDetail,
  requestReschedule, getMyRescheduleRequests, checkScheduleConflict
} from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const dateRange = ref(defaultWeek())
const scheduleList = ref([])
const loading = ref(false)
// 每节课最近一次调课申请：scheduleId -> 申请记录
const reqMap = ref({})

const applyVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const current = ref(null)
// 标准节次：45分钟/节，课间10分钟；与管理端排课节次保持一致
const slotGroups = [
  { label: '上午', options: [
    { start: '08:00:00', end: '08:45:00' }, { start: '08:55:00', end: '09:40:00' },
    { start: '09:50:00', end: '10:35:00' }, { start: '10:45:00', end: '11:30:00' } ] },
  { label: '下午', options: [
    { start: '14:30:00', end: '15:15:00' }, { start: '15:25:00', end: '16:10:00' },
    { start: '16:20:00', end: '17:05:00' } ] },
  { label: '晚上', options: [
    { start: '19:30:00', end: '20:15:00' }, { start: '20:25:00', end: '21:10:00' },
    { start: '21:15:00', end: '22:00:00' } ] }
]
const allSlots = slotGroups.flatMap(g => g.options)
const form = ref({ newDate: '', slot: '', newStartTime: '', newEndTime: '', reason: '' })
const rules = {
  newDate: [{ required: true, message: '请选择新日期', trigger: 'change' }],
  slot: [{ required: true, message: '请选择时间段', trigger: 'change' }],
  reason: [{ required: true, message: '请填写调课原因', trigger: 'blur' }]
}
const onSlotChange = (start) => {
  const s = allSlots.find(x => x.start === start)
  if (s) { form.value.newStartTime = s.start; form.value.newEndTime = s.end }
  detectConflict()
}

// 目标时段冲突检测
const conflicts = ref([])
const conflictChecking = ref(false)
const conflictChecked = ref(false)
const swapMode = ref('reselect')
let conflictSeq = 0
const conflictText = (c) =>
  `${c.courseDate} ${c.startTime?.slice(0,5)}-${c.endTime?.slice(0,5)} ${c.className || ''}（${c.teacherName || ''}${c.classroomName ? ' · ' + c.classroomName : ''}）`
const detectConflict = async () => {
  if (!current.value || !form.value.newDate || !form.value.newStartTime || !form.value.newEndTime) {
    conflicts.value = []; conflictChecked.value = false; return
  }
  const seq = ++conflictSeq
  conflictChecking.value = true
  try {
    const res = await checkScheduleConflict(current.value.scheduleId, {
      date: form.value.newDate,
      startTime: form.value.newStartTime,
      endTime: form.value.newEndTime
    })
    if (seq !== conflictSeq) return
    conflicts.value = res.data || []
    swapMode.value = conflicts.value.length === 1 ? 'swap' : 'reselect'
    conflictChecked.value = true
  } finally {
    if (seq === conflictSeq) conflictChecking.value = false
  }
}

const historyVisible = ref(false)
const historyList = ref([])
const detailVisible = ref(false)
const detail = ref(null)

function defaultWeek() {
  const now = new Date()
  const day = now.getDay() || 7
  const monday = new Date(now); monday.setDate(now.getDate() - day + 1)
  const sunday = new Date(monday); sunday.setDate(monday.getDate() + 6)
  const fmt = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
  return [fmt(monday), fmt(sunday)]
}
const statusType = s => ({ '已完成': 'success', '已取消': 'info', '待上课': 'warning' }[s] || '')
const reqStatusType = s => ({ '已通过': 'success', '已拒绝': 'danger', '待审核': 'warning' }[s] || 'info')

// 调课按钮按申请状态分类：待审核禁用防重复提交；已通过/已拒绝可再次申请
const applyBtn = (row) => {
  const req = reqMap.value[row.scheduleId]
  if (!req) return { text: '调课', type: 'warning', disabled: false }
  if (req.status === '待审核') return { text: '审核中', type: 'info', disabled: true }
  if (req.status === '已通过') return { text: '再次调课', type: 'warning', disabled: false }
  if (req.status === '已拒绝') return { text: '重新调课', type: 'danger', disabled: false }
  return { text: '调课', type: 'warning', disabled: false }
}

const loadMyRequests = async () => {
  const res = await getMyRescheduleRequests()
  const map = {}
  for (const r of (res.data || [])) {
    const prev = map[r.scheduleId]
    if (!prev || r.requestId > prev.requestId) map[r.scheduleId] = r
  }
  reqMap.value = map
}

const fetchData = async () => {
  loading.value = true
  try {
    const [start, end] = dateRange.value && dateRange.value.length === 2 ? dateRange.value : defaultWeek()
    const res = await getTeacherSchedule({ startDate: start, endDate: end })
    scheduleList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openApply = (row) => {
  current.value = row
  const matched = allSlots.find(s => s.start === row.startTime)
  form.value = {
    newDate: row.courseDate || '',
    slot: matched ? matched.start : '',
    newStartTime: matched ? matched.start : '',
    newEndTime: matched ? matched.end : '',
    reason: ''
  }
  conflicts.value = []; conflictChecked.value = false; swapMode.value = 'reselect'
  applyVisible.value = true
  detectConflict()
}

const submit = async () => {
  await formRef.value.validate()
  // 冲突处理校验
  if (conflictChecking.value) return ElMessage.warning('正在检测时段冲突，请稍候')
  if (conflicts.value.length > 1) return ElMessage.error('该时段有多节冲突课程，无法交换，请更换时段')
  const swap = conflicts.value.length === 1 && swapMode.value === 'swap'
  if (conflicts.value.length === 1 && !swap) return ElMessage.error('该时段已有其他课程，请选择"与该课程交换"或更换时段')
  saving.value = true
  try {
    const payload = { ...form.value }
    if (swap) {
      payload.requestType = '交换'
      payload.targetScheduleId = conflicts.value[0].scheduleId
    } else {
      payload.requestType = '调课'
    }
    await requestReschedule(current.value.scheduleId, payload)
    ElMessage.success(swap ? '交换申请已提交，等待教务审核' : '调课申请已提交，等待教务审核')
    applyVisible.value = false
    loadMyRequests()
  } finally {
    saving.value = false
  }
}

const openHistory = async () => {
  historyVisible.value = true
  const res = await getMyRescheduleRequests()
  historyList.value = res.data || []
}

const viewDetail = async (row) => {
  const res = await getScheduleDetail(row.scheduleId)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(() => { fetchData(); loadMyRequests() })
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
</style>
