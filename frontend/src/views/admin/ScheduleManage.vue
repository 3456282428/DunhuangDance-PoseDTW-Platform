<template>
  <div>
    <PageHeader title="课程排班管理" subtitle="每周课表与教室安排">
      <template #actions>
        <el-button type="primary" @click="openDialog()">新增排课</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
    <el-tab-pane label="排课列表" name="schedule">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="weekRange" type="daterange" value-format="YYYY-MM-DD"
          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
          @change="fetchData"
        />
        <el-select v-model="filterTeacher" placeholder="教师" clearable style="width:130px" @change="fetchData">
          <el-option v-for="t in teachers" :key="t.teacherId" :label="t.name" :value="t.teacherId" />
        </el-select>
        <el-select v-model="filterRoom" placeholder="教室" clearable style="width:130px" @change="fetchData">
          <el-option v-for="r in classrooms" :key="r.classroomId" :label="r.classroomName" :value="r.classroomId" />
        </el-select>
      </div>

      <el-table :data="scheduleList" stripe v-loading="loading">
        <el-table-column prop="scheduleId" label="编号" width="70" />
        <el-table-column prop="className" label="班级" min-width="150">
          <template #default="{ row }">{{ row.className || '—' }}</template>
        </el-table-column>
        <el-table-column prop="teacherName" label="教师" width="100">
          <template #default="{ row }">{{ row.teacherName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="classroomName" label="教室" width="110">
          <template #default="{ row }">{{ row.classroomName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="courseDate" label="日期" width="115" />
        <el-table-column label="时间" width="130">
          <template #default="{ row }">{{ row.startTime?.slice(0,5) }} - {{ row.endTime?.slice(0,5) }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="课时" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    </el-tab-pane>

    <el-tab-pane label="调课审批" name="reschedule">
      <el-card shadow="never">
        <div class="toolbar">
          <el-radio-group v-model="rrFilter" @change="loadReschedule">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="待审核">待审核</el-radio-button>
            <el-radio-button value="已通过">已通过</el-radio-button>
            <el-radio-button value="已拒绝">已拒绝</el-radio-button>
          </el-radio-group>
          <el-button @click="loadReschedule">刷新</el-button>
        </div>
        <el-table :data="rrList" stripe v-loading="rrLoading" :row-class-name="rrRowClass">
          <el-table-column prop="requestId" label="编号" width="70" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.requestType === '交换' ? 'warning' : ''">{{ row.requestType || '调课' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="className" label="班级" min-width="130" />
          <el-table-column prop="teacherName" label="教师" width="90" />
          <el-table-column label="原安排" width="190">
            <template #default="{ row }">
              {{ row.originalDate }} {{ row.originalStartTime?.slice(0,5) }}-{{ row.originalEndTime?.slice(0,5) }}
            </template>
          </el-table-column>
          <el-table-column label="申请调整为" min-width="210">
            <template #default="{ row }">
              <div>{{ row.newDate }} {{ row.newStartTime?.slice(0,5) }}-{{ row.newEndTime?.slice(0,5) }}</div>
              <div v-if="row.requestType === '交换'" style="color:#c89a2e;font-size:12px;line-height:1.6">
                ⇄ 与【{{ row.targetClassName || '另一课程' }}{{ row.targetTeacherName ? ' · ' + row.targetTeacherName : '' }}】交换，
                对方调至 {{ row.targetDate }} {{ row.targetStartTime?.slice(0,5) }}-{{ row.targetEndTime?.slice(0,5) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="调课原因" min-width="130" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="rrStatusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status==='待审核'">
                <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
              </template>
              <span v-else style="color:#999;font-size:12px">{{ row.handleRemark || '已处理' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="form.scheduleId ? '编辑排课' : '新增排课'" width="540px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" filterable style="width:100%" @change="onClassChange">
            <el-option v-for="c in classList" :key="c.classId"
              :label="`${c.className}（${c.currentStudents||0}/${c.maxStudents||'—'}人）`" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择教师" filterable style="width:100%">
            <el-option v-for="t in teachers" :key="t.teacherId" :label="t.name" :value="t.teacherId" />
          </el-select>
        </el-form-item>
        <el-form-item label="教室" prop="classroomId">
          <el-select v-model="form.classroomId" placeholder="请选择可容纳班级人数的教室" style="width:100%">
            <el-option v-for="r in availableRooms" :key="r.classroomId"
              :label="`${r.classroomName}（容纳${r.capacity}人${r.roomType?' · '+r.roomType:''}）`"
              :disabled="roomRequired>0 && r.capacity < roomRequired" :value="r.classroomId" />
          </el-select>
          <div v-if="roomRequired>0" class="room-hint">班级约 {{ roomRequired }} 人，仅可选择容量不小于该人数的教室</div>
        </el-form-item>

        <el-form-item v-if="!form.scheduleId" label="排课方式">
          <el-radio-group v-model="mode">
            <el-radio-button value="single">单次</el-radio-button>
            <el-radio-button value="weekly">按周期</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 单次 -->
        <el-form-item v-if="mode==='single' || form.scheduleId" label="上课日期" prop="courseDate">
          <el-date-picker v-model="form.courseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>

        <!-- 周期 -->
        <template v-if="mode==='weekly' && !form.scheduleId">
          <el-form-item label="周期范围" prop="rangeArr">
            <el-date-picker v-model="form.rangeArr" type="daterange" value-format="YYYY-MM-DD"
              range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%" />
          </el-form-item>
          <el-form-item label="上课星期" prop="weekdays">
            <el-checkbox-group v-model="form.weekdays">
              <el-checkbox v-for="w in weekOptions" :key="w.value" :value="w.value">{{ w.label }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>

        <el-form-item label="节次/时间" prop="slot">
          <el-select v-model="form.slot" placeholder="选择标准节次" style="width:100%" @change="onSlotChange">
            <el-option-group v-for="g in slotGroups" :key="g.label" :label="g.label">
              <el-option v-for="s in g.options" :key="s.start" :label="`${s.start.slice(0,5)}-${s.end.slice(0,5)}（${g.label}）`" :value="s.start" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm:ss" placeholder="开始" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm:ss" placeholder="结束" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getScheduleList, createSchedule, updateSchedule, deleteSchedule,
  getClassList, getTeacherOptions, getClassroomList,
  getRescheduleList, approveReschedule, rejectReschedule
} from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 标准节次：45分钟/节，课间10分钟；午休/晚餐间隔
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
const weekOptions = [
  { label: '周一', value: 1 }, { label: '周二', value: 2 }, { label: '周三', value: 3 },
  { label: '周四', value: 4 }, { label: '周五', value: 5 }, { label: '周六', value: 6 }, { label: '周日', value: 7 }
]

const scheduleList = ref([])
const classList = ref([])
const teachers = ref([])
const classrooms = ref([])
// 班级人数：以班级最大人数为准（招满也能容纳）
const roomRequired = computed(() => {
  const c = classList.value.find(x => x.classId === form.value.classId)
  if (!c) return 0
  return Number(c.maxStudents > 0 ? c.maxStudents : (c.currentStudents || 0))
})
// 可容纳该班级人数的教室优先，容量不足的置灰
const availableRooms = computed(() => {
  const list = [...classrooms.value]
  const need = roomRequired.value
  list.sort((a,b)=>{
    const ao = need>0 && a.capacity < need ? 1:0
    const bo = need>0 && b.capacity < need ? 1:0
    return ao-bo || (a.capacity-b.capacity)
  })
  return list
})
function onClassChange(){
  const c = classList.value.find(x=>x.classId===form.value.classId)
  const need = c ? Number(c.maxStudents>0?c.maxStudents:(c.currentStudents||0)) : 0
  const room = classrooms.value.find(r=>r.classroomId===form.value.classroomId)
  if (room && need>0 && Number(room.capacity)<need) form.value.classroomId = null
}
const loading = ref(false)
const weekRange = ref(defaultWeek())
const filterTeacher = ref(null)
const filterRoom = ref(null)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const mode = ref('single')
const emptyForm = () => ({
  scheduleId: null, classId: null, teacherId: null, classroomId: null,
  courseDate: '', startTime: '08:00:00', endTime: '08:45:00', slot: '08:00:00',
  status: '待上课', remark: '', rangeArr: [], weekdays: []
})
const form = ref(emptyForm())
const rules = {
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  teacherId: [{ required: true, message: '请选择教师', trigger: 'change' }],
  classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
  courseDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  rangeArr: [{ required: true, type: 'array', min: 2, message: '请选择周期范围', trigger: 'change' }],
  weekdays: [{ required: true, type: 'array', min: 1, message: '请选择上课星期', trigger: 'change' }],
  slot: [{ required: true, message: '请选择节次', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

function defaultWeek() {
  const now = new Date()
  const day = now.getDay() || 7
  const monday = new Date(now); monday.setDate(now.getDate() - day + 1)
  const sunday = new Date(monday); sunday.setDate(monday.getDate() + 6)
  const fmt = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
  return [fmt(monday), fmt(sunday)]
}
const statusType = s => ({ '已完成': 'success', '已取消': 'info', '待上课': 'warning' }[s] || '')

const onSlotChange = (start) => {
  const s = allSlots.find(x => x.start === start)
  if (s) { form.value.startTime = s.start; form.value.endTime = s.end }
}

const fetchData = async () => {
  loading.value = true
  try {
    const [start, end] = weekRange.value && weekRange.value.length === 2 ? weekRange.value : defaultWeek()
    const res = await getScheduleList({ startDate: start, endDate: end, teacherId: filterTeacher.value, classroomId: filterRoom.value })
    scheduleList.value = res.data || []
  } finally { loading.value = false }
}

const loadOptions = async () => {
  const [c, t, r] = await Promise.all([getClassList({ pageNum:1, pageSize:200 }), getTeacherOptions(), getClassroomList()])
  classList.value = c.data.rows || c.data || []
  teachers.value = t.data || []
  classrooms.value = r.data || []
}

const openDialog = (row) => {
  mode.value = 'single'
  form.value = row
    ? { ...emptyForm(), scheduleId: row.scheduleId, classId: row.classId, teacherId: row.teacherId,
        classroomId: row.classroomId, courseDate: row.courseDate, startTime: row.startTime,
        endTime: row.endTime, slot: row.startTime, status: row.status, remark: row.remark }
    : emptyForm()
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form.value }
    if (mode.value === 'weekly' && !form.value.scheduleId) {
      payload.rangeStart = form.value.rangeArr[0]
      payload.rangeEnd = form.value.rangeArr[1]
      payload.courseDate = form.value.rangeArr[0] // 满足后端非空校验
    }
    if (form.value.scheduleId) {
      await updateSchedule(form.value.scheduleId, payload)
      ElMessage.success('排课已更新')
    } else {
      const res = await createSchedule(payload)
      ElMessage.success(res.message || '排课已创建')
    }
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认取消该排课吗？`, '提示', { type: 'warning' })
    await deleteSchedule(row.scheduleId)
    ElMessage.success('已取消')
    fetchData()
  } catch (e) {}
}

// ============ 调课审批 ============
const activeTab = ref('schedule')
const rrList = ref([])
const rrLoading = ref(false)
const rrFilter = ref('待审核')
const rrStatusType = s => ({ '已通过': 'success', '已拒绝': 'danger', '待审核': 'warning' }[s] || 'info')
const rrRowClass = ({ row }) => row.requestType === '交换' ? 'rr-swap-row' : ''
const loadReschedule = async () => {
  rrLoading.value = true
  try {
    const res = await getRescheduleList(rrFilter.value ? { status: rrFilter.value } : {})
    rrList.value = res.data || []
  } finally { rrLoading.value = false }
}
const onTabChange = (name) => { if (name === 'reschedule') loadReschedule() }
const handleApprove = async (row) => {
  let remark = ''
  const tip = row.requestType === '交换'
    ? `这是【交换申请】：通过后「${row.className}」与「${row.targetClassName || '对方课程'}」将互换时间段，确认通过？`
    : '可填写审批意见（选填）'
  try {
    const r = await ElMessageBox.prompt(tip, row.requestType === '交换' ? '通过交换申请' : '通过调课申请', {
      confirmButtonText: '确认通过', cancelButtonText: '取消', inputValue: '',
      type: row.requestType === '交换' ? 'warning' : ''
    })
    remark = r.value || ''
  } catch (e) { return }
  await approveReschedule(row.requestId, { remark })
  ElMessage.success('已通过，排课时间已同步调整')
  loadReschedule(); fetchData()
}
const handleReject = async (row) => {
  let remark = ''
  try {
    const r = await ElMessageBox.prompt('请填写拒绝原因', '拒绝调课申请', {
      confirmButtonText: '确认拒绝', cancelButtonText: '取消', inputValue: ''
    })
    remark = r.value || ''
  } catch (e) { return }
  await rejectReschedule(row.requestId, { remark })
  ElMessage.success('已拒绝')
  loadReschedule()
}

onMounted(() => { fetchData(); loadOptions() })
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.room-hint{font-size:12px;color:#c89a2e;line-height:1.6;margin-top:2px}
:deep(.rr-swap-row) { background-color: #fdf6ec !important; }
:deep(.rr-swap-row:hover > td) { background-color: #faecd8 !important; }
</style>
