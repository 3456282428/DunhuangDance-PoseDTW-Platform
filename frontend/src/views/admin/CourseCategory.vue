<template>
  <div>
    <PageHeader title="课程产品配置" subtitle="类别与具体课程整合管理，课程名称唯一">
      <template #actions>
        <el-button type="primary" @click="openDialog()">新增课程</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.courseName" placeholder="课程名称" clearable style="width:200px" @keyup.enter="fetchData" />
        <el-select v-model="query.category" placeholder="全部类别" clearable style="width:150px" @change="fetchData">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px" @change="fetchData">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>

      <el-table :data="list" stripe v-loading="loading" border>
        <el-table-column prop="courseId" label="编号" width="60" />
        <el-table-column prop="courseNo" label="课程编号" width="90">
          <template #default="{ row }">
            <el-tag size="small" type="success" effect="plain">{{ row.courseNo || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="category" label="类别" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ageGroup" label="适用年龄" width="100" />
        <el-table-column prop="level" label="难度" width="80" />
        <el-table-column label="课时" width="110">
          <template #default="{ row }">{{ row.totalHours }}课时 / {{ row.lessonDuration }}分钟</template>
        </el-table-column>
        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <span style="color:#c13a2a;font-weight:600">¥{{ row.price }}</span>
            <span v-if="row.originalPrice && Number(row.originalPrice)>Number(row.price)" style="color:#bbb;text-decoration:line-through;margin-left:6px;font-size:12px">¥{{ row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status===1 ? 'success' : 'info'" size="small">{{ row.status===1 ? '已上架' : '已下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.courseId ? '编辑课程' : '新增课程'" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="名称唯一，不可重复" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类别" prop="category">
              <el-select v-model="form.category" placeholder="如：乐器" filterable allow-create style="width:100%">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用年龄">
              <el-input v-model="form.ageGroup" placeholder="如：5-12岁" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度级别">
              <el-select v-model="form.level" placeholder="请选择" clearable style="width:100%">
                <el-option label="启蒙" value="启蒙" />
                <el-option label="初级" value="初级" />
                <el-option label="中级" value="中级" />
                <el-option label="高级" value="高级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总课时">
              <el-input-number v-model="form.totalHours" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单次时长">
              <el-input-number v-model="form.lessonDuration" :min="15" :step="5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优惠价">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程简介">
              <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="教学目标、内容介绍" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上架状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
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
import { ref, reactive, onMounted } from 'vue'
import { getCourseList, createCourse, updateCourse, deleteCourse } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ['乐器', '舞蹈', '美术', '声乐']
const list = ref([])
const loading = ref(false)
const query = reactive({ courseName: '', category: '', status: null })
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ courseId: null, courseName: '', category: '', ageGroup: '', level: '', totalHours: 32, lessonDuration: 45, originalPrice: 0, price: 0, intro: '', status: 1, sortOrder: 0 })
const form = ref(emptyForm())
const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCourseList({ courseName: query.courseName || undefined, category: query.category || undefined, status: query.status ?? undefined })
    list.value = res.data || []
  } finally { loading.value = false }
}

const openDialog = (row) => {
  form.value = row ? { ...row } : emptyForm()
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.courseId) await updateCourse(form.value.courseId, form.value)
    else await createCourse(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除课程「${row.courseName}」？若该课程下已有班级将无法删除。`, '提示', { type: 'warning' })
  await deleteCourse(row.courseId)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
