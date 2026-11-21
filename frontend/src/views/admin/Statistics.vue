<template>
  <div v-loading="loading">
    <PageHeader title="运营数据统计" subtitle="续课率 · 出勤率 · 教室利用率 · 教师课时">
      <template #actions>
        <el-radio-group v-model="quickRange" size="default" @change="onQuickRange" style="margin-right:12px">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="month">近30天</el-radio-button>
          <el-radio-button value="term">近90天</el-radio-button>
          <el-radio-button value="custom">自定义</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :disabled="quickRange !== 'custom'"
          @change="fetchAll"
        />
      </template>
    </PageHeader>

    <!-- 核心指标卡 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-head">
            <span>续课率</span>
            <el-tooltip placement="top">
              <template #content>
                <div style="max-width:260px;line-height:1.7">
                  口径：区间内课程到期（班级最后一课）的学员为“应续”，<br/>
                  到期前{{ renew.beforeDays || 30 }}天至到期后{{ renew.afterDays || 60 }}天内再次报名或有“续费”支付的为“已续”。<br/>
                  续课率 = 已续学员 ÷ 应续学员（按学员去重）。
                </div>
              </template>
              <el-icon class="tip-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="big-num" style="color:#2c9678">{{ renew.due === 0 ? '—' : fmt(renew.rate) + '%' }}</div>
          <div class="sub-info" v-if="renew.due === 0">所选区间暂无课程到期学员，暂无法计算续课率</div>
          <div class="sub-info" v-else>已续 {{ renew.renewed }} 人 / 应续 {{ renew.due }} 人</div>
          <div class="sub-info">多课学员占比：{{ fmt(renew.multiRate) }}%（{{ renew.multi }}/{{ renew.totalEnrolled }}人）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-head"><span>出勤率</span></div>
          <div class="big-num" style="color:#1685a9">{{ fmt(attend.rate) }}%</div>
          <div class="sub-info">出勤（含迟到）{{ attend.present }} / 考勤记录 {{ attend.total }}</div>
          <div class="sub-info">迟到 {{ attend.late }} · 请假 {{ attend.leave }} · 缺勤 {{ attend.absent }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-head"><span>区间排课</span></div>
          <div class="big-num" style="color:#c89a2e">{{ overview.schedules }} <small>节</small></div>
          <div class="sub-info">累计授课 {{ fmt1(overview.hours) }} 课时</div>
          <div class="sub-info">授课教师 {{ teachers.length }} 人</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-head"><span>缺勤率</span></div>
          <div class="big-num" style="color:#c13a2a">{{ fmt(absentRate) }}%</div>
          <div class="sub-info">缺勤 {{ attend.absent }} 人次</div>
          <div class="sub-info">请假率 {{ fmt(leaveRate) }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:16px">
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <div class="card-head">出勤分布<span class="card-sub">数据来源：教师端考勤记录</span></div>
          </template>
          <EChart v-if="attend.total > 0" :option="attendancePieOption" height="300px" />
          <el-empty v-else description="所选区间暂无考勤记录" :image-size="90" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-head">
              教师课时排行<span class="card-sub">区间合计 {{ fmt1(overview.hours) }} 课时（已取消排课不计）</span>
            </div>
          </template>
          <EChart v-if="teachers.length" :option="teacherHoursBarOption" height="300px" />
          <el-empty v-else description="所选区间暂无授课记录" :image-size="90" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-head">
              教室利用情况<span class="card-sub">按区间排课节数统计，括号为占比</span>
            </div>
          </template>
          <EChart v-if="classrooms.some(c=>c.scheduleCount>0)" :option="classroomBarOption" height="300px" />
          <el-empty v-else description="所选区间暂无排课" :image-size="90" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计口径说明 -->
    <el-card shadow="never" style="margin-top:16px">
      <template #header>统计口径说明</template>
      <ul class="rules">
        <li><b>续课率：</b>区间内课程到期学员为应续，到期前30天至到期后60天内再次报名或续费为已续；续课率＝已续÷应续。</li>
        <li><b>出勤率：</b>出勤（含迟到）人次 ÷ 全部考勤人次，迟到单独展示。</li>
        <li><b>教师课时 / 教室利用：</b>按区间实际排课统计，已取消排课不计。</li>
        <li>不选区间时按全部历史数据统计。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getRenewalRate, getAttendanceRate, getClassroomUtilization, getTeacherHours } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'

const loading = ref(false)
const quickRange = ref('all')
const dateRange = ref([])

const renew = reactive({ rate: 0, due: 0, renewed: 0, multi: 0, multiRate: 0, totalEnrolled: 0, beforeDays: 30, afterDays: 60 })
const attend = reactive({ rate: 0, total: 0, present: 0, late: 0, absent: 0, leave: 0 })
const overview = reactive({ schedules: 0, hours: 0 })
const classrooms = ref([])
const teachers = ref([])

const fmt = (v) => Number(v || 0).toFixed(1)
const fmt1 = (v) => Number(v || 0).toFixed(1)
const absentRate = computed(() => attend.total ? (attend.absent * 100 / attend.total) : 0)
const leaveRate = computed(() => attend.total ? (attend.leave * 100 / attend.total) : 0)

const onQuickRange = (v) => {
  if (v === 'custom') return
  const end = new Date()
  const start = new Date()
  if (v === 'month') start.setDate(end.getDate() - 29)
  if (v === 'term') start.setDate(end.getDate() - 89)
  const f = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  dateRange.value = v === 'all' ? [] : [f(start), f(end)]
  fetchAll()
}

const buildParams = () => {
  if (dateRange.value && dateRange.value.length === 2) return { startDate: dateRange.value[0], endDate: dateRange.value[1] }
  return {}
}

const fetchAll = async () => {
  loading.value = true
  try {
    const params = buildParams()
    const [r1, r2, r3, r4] = await Promise.all([
      getRenewalRate(params), getAttendanceRate(params),
      getClassroomUtilization(params), getTeacherHours(params)
    ])
    const d1 = r1.data || {}
    renew.rate = d1.renewalRate || 0
    renew.due = d1.dueStudents || 0
    renew.renewed = d1.renewedStudents || 0
    renew.multi = d1.multiCourseStudents || 0
    renew.multiRate = d1.multiCourseRate || 0
    renew.totalEnrolled = d1.totalEnrolledStudents || 0
    renew.beforeDays = d1.renewWindowBeforeDays || 30
    renew.afterDays = d1.renewWindowAfterDays || 60

    const d2 = r2.data || {}
    attend.rate = d2.attendanceRate || 0
    attend.total = d2.totalRecords || 0
    attend.present = d2.presentRecords || 0
    attend.late = d2.lateRecords || 0
    attend.absent = d2.absentRecords || 0
    attend.leave = d2.leaveRecords || 0

    overview.schedules = r4.data?.totalSchedules || r3.data?.totalSchedules || 0
    overview.hours = r4.data?.totalHours || 0
    classrooms.value = r3.data?.rooms || []
    teachers.value = r4.data?.teachers || []
  } finally {
    loading.value = false
  }
}

const PIE_COLORS = ['#2c9678', '#c89a2e', '#c13a2a', '#6b5a4a']
const attendancePieOption = computed(() => ({
  color: PIE_COLORS,
  tooltip: { trigger: 'item', formatter: '{b}: {c} 人次 ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie', radius: ['42%', '68%'], center: ['50%', '46%'],
    label: { formatter: '{b}\n{c} 人次' },
    data: [
      { name: '准时出勤', value: Math.max(attend.present - attend.late, 0) },
      { name: '迟到', value: attend.late },
      { name: '缺勤', value: attend.absent },
      { name: '请假', value: attend.leave }
    ].filter(x => x.value > 0)
  }]
}))

const teacherHoursBarOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: v => Number(v).toFixed(1) + ' 课时' },
  grid: { left: 60, right: 30, top: 24, bottom: 40 },
  xAxis: { type: 'category', data: teachers.value.map(t => t.displayName || t.teacherName), axisLabel: { rotate: 20 } },
  yAxis: { type: 'value', name: '课时' },
  series: [{
    type: 'bar', barWidth: 34,
    data: teachers.value.map(t => Number(Number(t.totalHours).toFixed(1))),
    itemStyle: { color: '#1685a9', borderRadius: [4, 4, 0, 0] },
    label: { show: true, position: 'top', formatter: p => Number(p.value).toFixed(1) }
  }]
}))

const classroomBarOption = computed(() => ({
  tooltip: {
    trigger: 'axis', axisPointer: { type: 'shadow' },
    formatter: (ps) => {
      const p = ps[0]
      const c = classrooms.value[p.dataIndex]
      return `${c.classroomName}<br/>排课 ${c.scheduleCount} 节（占比 ${c.share}%）<br/>课时 ${c.totalHours}`
    }
  },
  grid: { left: 60, right: 30, top: 24, bottom: 40 },
  xAxis: { type: 'category', data: classrooms.value.map(c => c.classroomName), axisLabel: { rotate: 20 } },
  yAxis: { type: 'value', name: '排课节数' },
  series: [{
    type: 'bar', barWidth: 34,
    data: classrooms.value.map(c => c.scheduleCount),
    itemStyle: { color: '#2c9678', borderRadius: [4, 4, 0, 0] },
    label: { show: true, position: 'top', formatter: (p) => `${p.value}节 ${classrooms.value[p.dataIndex].share}%` }
  }]
}))

onMounted(fetchAll)
</script>

<style scoped>
.kpi-card { min-height: 168px; }
.kpi-head { display: flex; align-items: center; gap: 6px; color: #6b5a4a; font-weight: 600; margin-bottom: 8px; }
.tip-icon { color: #6b5a4a; cursor: help; }
.big-num { font-size: 32px; font-weight: bold; line-height: 1.2; }
.big-num small { font-size: 15px; font-weight: normal; color: #6b5a4a; }
.sub-info { color: #6b5a4a; font-size: 13px; margin-top: 6px; }
.card-head { display: flex; align-items: center; gap: 10px; font-weight: 600; }
.card-sub { font-size: 12px; font-weight: normal; color: #6b5a4a; }
.rules { margin: 0; padding-left: 18px; color: #6b5a4a; font-size: 13px; line-height: 2; }
</style>
