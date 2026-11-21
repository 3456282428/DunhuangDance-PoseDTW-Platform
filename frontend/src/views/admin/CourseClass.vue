<template>
  <div>
    <PageHeader title="班级管理" subtitle="一门课程可开设多个班级，一个班级只对应一门课程">
      <template #actions>
        <el-button type="primary" @click="openDialog()">新增班级</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="班级名称" clearable style="width:200px" @keyup.enter="fetchData" />
        <el-select v-model="filterCourseId" placeholder="按课程筛选" clearable filterable style="width:220px" @change="fetchData">
          <el-option v-for="c in courses" :key="c.courseId" :label="c.courseName" :value="c.courseId" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>

      <el-table :data="list" stripe v-loading="loading" border>
        <el-table-column prop="classId" label="编号" width="60" />
        <el-table-column prop="className" label="班级名称" min-width="130" />
        <el-table-column prop="courseName" label="所属课程" min-width="140">
          <template #default="{ row }">
            {{ row.courseName || '—' }}
            <el-tag size="small" effect="plain" style="margin-left:6px">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="classroomName" label="教室" width="110" />
        <el-table-column label="人数" width="90">
          <template #default="{ row }">{{ row.currentStudents || 0 }}/{{ row.maxStudents || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status===1 ? 'success' : 'info'" size="small">{{ row.status===1 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @pagination="fetchData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.classId ? '编辑班级' : '新增班级'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择课程（一门课程可开多个班）" filterable style="width:100%" @change="onCourseChange">
            <el-option v-for="c in courses" :key="c.courseId"
              :label="`${c.courseName}（${c.category || '未分类'}）`" :value="c.courseId" />
          </el-select>
        </el-form-item>

        <el-alert v-if="selectedCourse" type="info" :closable="false" style="margin-bottom:14px">
          <template #title>
            {{ selectedCourse.courseName }}：{{ selectedCourse.totalHours }}课时 / 每节{{ selectedCourse.lessonDuration }}分钟 /
            优惠价 ¥{{ selectedCourse.price }}<span v-if="selectedCourse.ageGroup"> · 适用{{ selectedCourse.ageGroup }}</span><span v-if="selectedCourse.level"> · {{ selectedCourse.level }}</span>
          </template>
        </el-alert>

        <el-form-item label="班级名称">
          <el-input v-model="form.className" placeholder="不填则默认使用课程名称" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="教室">
              <el-select v-model="form.classroomId" placeholder="请选择（默认教室，排课时可调整）" clearable filterable style="width:100%">
                <el-option v-for="r in classrooms" :key="r.classroomId" :label="`${r.classroomName}（容纳${r.capacity}人）`" :value="r.classroomId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学期">
              <el-input v-model="form.semester" placeholder="如：2026秋季" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大人数" prop="maxStudents">
              <el-input-number v-model="form.maxStudents" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getClassList, createClass, updateClass, deleteClass, getCourseList, getClassroomList } from '@/api/admin'
import { getTeacherOptions } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const courses = ref([])
const teachers = ref([])
const classrooms = ref([])
const loading = ref(false)
const keyword = ref('')
const filterCourseId = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ classId: null, courseId: null, className: '', teacherId: null, classroomId: null, semester: '', startDate: '', endDate: '', maxStudents: 20, status: 1 })
const form = ref(emptyForm())
const rules = {
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  maxStudents: [{ required: true, message: '请设置最大人数', trigger: 'blur' }]
}
const selectedCourse = computed(() => courses.value.find(c => c.courseId === form.value.courseId))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getClassList({ pageNum: pageNum.value, pageSize: pageSize.value, className: keyword.value || undefined })
    let rows = res.data.rows || res.data || []
    if (filterCourseId.value) rows = rows.filter(r => r.courseId === filterCourseId.value)
    list.value = rows
    total.value = res.data.total || rows.length
  } finally { loading.value = false }
}

const loadOptions = async () => {
  const [c, t, r] = await Promise.all([
    getCourseList({ status: 1 }),
    getTeacherOptions().catch(() => ({ data: [] })),
    getClassroomList().catch(() => ({ data: [] }))
  ])
  courses.value = c.data || []
  teachers.value = t.data || []
  classrooms.value = r.data || []
}

const onCourseChange = () => {
  const c = selectedCourse.value
  if (c && !form.value.className) form.value.className = c.courseName
}

const openDialog = (row) => {
  form.value = row ? { ...row } : emptyForm()
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.classId) await updateClass(form.value.classId, form.value)
    else await createClass(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除班级「${row.className}」？`, '提示', { type: 'warning' })
  await deleteClass(row.classId)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(() => { fetchData(); loadOptions() })
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
