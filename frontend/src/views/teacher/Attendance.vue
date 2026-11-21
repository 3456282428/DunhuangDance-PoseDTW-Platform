<template>
  <div>
    <PageHeader title="教学考勤管理" subtitle="按排课点名，记录出勤 / 迟到 / 缺勤 / 请假" />

    <el-card shadow="never" style="margin-bottom:16px">
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
          @change="fetchData"
        />
        <el-select v-model="classFilter" placeholder="全部班级" clearable style="width:170px">
          <el-option v-for="c in classOptions" :key="c.classId" :label="c.className" :value="c.classId" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <el-tag type="info" effect="plain">共 {{ filteredSchedules.length }} 节</el-tag>
      </div>
    </el-card>

    <el-table :data="filteredSchedules" stripe v-loading="loading">
      <el-table-column prop="scheduleId" label="编号" width="55" />
      <el-table-column prop="className" label="班级" min-width="130" show-overflow-tooltip />
      <el-table-column prop="courseDate" label="日期" width="110" />
      <el-table-column label="时间" width="120">
        <template #default="{ row }">{{ fmt(row.startTime) }} - {{ fmt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="classroomName" label="教室" width="100" show-overflow-tooltip />
      <el-table-column label="状态" width="85">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openRollCall(row)">点名</el-button>
          <el-button size="small" @click="viewHistory(row)">历史</el-button>
        </template>
      </el-table-column>
      <template #empty>该条件下暂无排课</template>
    </el-table>

    <!-- 点名对话框 -->
    <el-dialog v-model="rollVisible" :title="'点名 - ' + (currentSchedule?.className || '')" width="640px">
      <el-alert type="info" :closable="false" style="margin-bottom:14px"
        :title="`${currentSchedule?.courseDate || ''} ${fmt(currentSchedule?.startTime)}-${fmt(currentSchedule?.endTime)} · ${currentSchedule?.classroomName || ''} · 应到 ${studentList.length} 人`" />
      <div class="roll-tools">
        <el-button size="small" type="success" plain @click="allPresent">一键全勤</el-button>
        <span class="roll-summary">
          出勤 {{ countBy('出勤') }} · 迟到 {{ countBy('迟到') }} · 缺勤 {{ countBy('缺勤') }} · 请假 {{ countBy('请假') }}
        </span>
      </div>
      <el-table :data="studentList" stripe v-loading="rollLoading" max-height="360">
        <el-table-column prop="studentName" label="学员" min-width="120" />
        <el-table-column label="出勤状态" width="180">
          <template #default="{ row }">
            <el-select v-model="row.status" size="small" style="width:120px">
              <el-option label="出勤" value="出勤" />
              <el-option label="迟到" value="迟到" />
              <el-option label="缺勤" value="缺勤" />
              <el-option label="请假" value="请假" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="备注">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="如：病假 / 事假" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="rollVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存点名</el-button>
      </template>
    </el-dialog>

    <!-- 历史考勤对话框 -->
    <el-dialog v-model="historyVisible" :title="`考勤历史 - ${historyDate || '全部日期'}`" width="820px">
      <div class="his-filter">
        <el-select v-model="historyQuery.classId" placeholder="全部班级" clearable size="small" style="width:150px" @change="loadHistory(1)">
          <el-option v-for="c in classOptions" :key="c.classId" :label="c.className" :value="c.classId" />
        </el-select>
        <el-select v-model="historyQuery.status" placeholder="全部状态" clearable size="small" style="width:110px" @change="loadHistory(1)">
          <el-option label="出勤" value="出勤" />
          <el-option label="迟到" value="迟到" />
          <el-option label="缺勤" value="缺勤" />
          <el-option label="请假" value="请假" />
        </el-select>
        <el-input v-model="historyQuery.keyword" size="small" placeholder="学员姓名" clearable style="width:140px"
          @keyup.enter="loadHistory(1)" @clear="loadHistory(1)" />
        <el-button size="small" type="primary" @click="loadHistory(1)">查询</el-button>
      </div>
      <el-table :data="historyList" stripe v-loading="historyLoading" max-height="420">
        <el-table-column prop="courseDate" label="日期" width="105" />
        <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
        <el-table-column label="时间" width="105">
          <template #default="{ row }">{{ fmt(row.startTime) }}-{{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="studentName" label="学员" width="90" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="attStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <template #empty>暂无历史考勤记录</template>
      </el-table>
      <div class="his-pager">
        <el-pagination
          v-model:current-page="historyQuery.pageNum"
          v-model:page-size="historyQuery.pageSize"
          :total="historyTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @current-change="loadHistory()"
          @size-change="loadHistory(1)"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getTeacherSchedule, getAttendanceStudents, batchUpdateAttendance, getAttendanceHistory, getTeacherClasses } from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const dateRange = ref([])
const classFilter = ref(null)
const scheduleList = ref([])
const classOptions = ref([])
const loading = ref(false)

const rollVisible = ref(false)
const currentSchedule = ref(null)
const studentList = ref([])
const rollLoading = ref(false)
const saving = ref(false)

const historyVisible = ref(false)
const historyList = ref([])
const historyDate = ref('')
const historyLoading = ref(false)
const historyTotal = ref(0)
const historyQuery = reactive({ classId: null, status: '', keyword: '', pageNum: 1, pageSize: 10 })

const fmt = (t) => t ? String(t).slice(0, 5) : ''
const statusType = s => ({ '已完成': 'success', '已取消': 'info', '待上课': 'warning' }[s] || '')
const attStatusType = s => ({ '出勤': 'success', '迟到': 'warning', '缺勤': 'danger', '请假': 'info' }[s] || '')
const allPresent = () => studentList.value.forEach(s => { s.status = '出勤'; s.remark = '' })
const countBy = (st) => studentList.value.filter(s => s.status === st).length

const filteredSchedules = computed(() =>
  classFilter.value ? scheduleList.value.filter(s => s.classId === classFilter.value) : scheduleList.value
)

const loadClasses = async () => {
  try {
    const res = await getTeacherClasses()
    classOptions.value = res.data || []
  } catch (e) {}
}

const fetchData = async () => {
  loading.value = true
  try {
    let params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params = { startDate: dateRange.value[0], endDate: dateRange.value[1] }
    }
    const res = await getTeacherSchedule(params)
    scheduleList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const resetFilter = () => { dateRange.value = []; classFilter.value = null; fetchData() }

const openRollCall = async (row) => {
  currentSchedule.value = row
  rollVisible.value = true
  rollLoading.value = true
  try {
    const res = await getAttendanceStudents(row.scheduleId)
    studentList.value = (res.data || []).map(s => ({
      ...s,
      status: (s.status && s.status !== '未点名') ? s.status : '出勤'
    }))
  } finally {
    rollLoading.value = false
  }
}

const doSave = async () => {
  if (!studentList.value.length) {
    ElMessage.warning('该课程暂无学员')
    return
  }
  saving.value = true
  try {
    const items = studentList.value.map(s => ({ studentId: s.studentId, status: s.status, remark: s.remark }))
    await batchUpdateAttendance(currentSchedule.value.scheduleId, { items })
    ElMessage.success('点名保存成功')
    rollVisible.value = false
  } finally {
    saving.value = false
  }
}

const viewHistory = (row) => {
  historyDate.value = row.courseDate
  historyQuery.classId = row.classId
  historyQuery.status = ''
  historyQuery.keyword = ''
  historyQuery.pageNum = 1
  historyVisible.value = true
  loadHistory(1)
}

const loadHistory = async (p) => {
  if (p) historyQuery.pageNum = p
  historyLoading.value = true
  try {
    const res = await getAttendanceHistory({
      pageNum: historyQuery.pageNum,
      pageSize: historyQuery.pageSize,
      date: historyDate.value || undefined,
      classId: historyQuery.classId || undefined,
      status: historyQuery.status || undefined,
      keyword: historyQuery.keyword || undefined
    })
    historyList.value = res.data?.rows || []
    historyTotal.value = res.data?.total || 0
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  fetchData()
  loadClasses()
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.roll-tools { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; }
.roll-summary { color: #6b5a4a; font-size: 13px; }
.his-filter { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.his-pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
