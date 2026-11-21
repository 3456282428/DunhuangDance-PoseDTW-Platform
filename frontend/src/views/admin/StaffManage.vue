<template>
  <div>
    <PageHeader title="师资信息管理" subtitle="教师从入职到离职全生命周期档案">
      <template #actions>
        <el-button type="primary" @click="openDialog()">新增教师</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="姓名/工号/专业" clearable style="width:220px" @keyup.enter="fetchData" />
        <el-select v-model="employType" placeholder="全部用工类型" clearable style="width:150px" @change="fetchData">
          <el-option v-for="t in employTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>

      <el-table :data="list" stripe v-loading="loading" border>
        <el-table-column label="教师" min-width="160">
          <template #default="{ row }">
            <el-avatar :size="38" :src="row.photo" style="vertical-align:middle;margin-right:8px">
              {{ row.name?.charAt(0) }}
            </el-avatar>
            <span style="font-weight:600">{{ row.name }}</span>
            <div style="color:#6b5a4a;font-size:12px">{{ row.teacherNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="major" label="专业" width="100" />
        <el-table-column prop="title" label="职称" width="90" />
        <el-table-column label="用工类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.employType)" size="small">{{ row.employType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teachYears" label="教龄" width="70">
          <template #default="{ row }">{{ row.teachYears || 0 }}年</template>
        </el-table-column>
        <el-table-column label="带班/课时" width="100">
          <template #default="{ row }">{{ row.classCount || 0 }}班 / {{ row.totalHours || 0 }}时</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status===1 ? 'success' : 'info'" size="small">{{ row.status===1 ? '在职' : '离职' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" @click="resetPwd(row)">重置密码</el-button>
            <el-button v-if="row.status===1" size="small" type="warning" @click="leave(row)">离职</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.teacherId ? '编辑教师档案' : '新增教师'" width="760px" top="5vh">
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="base">
          <div class="photo-row">
            <el-upload :show-file-list="false" :http-request="uploadPhoto" accept="image/*">
              <el-avatar :size="72" :src="form.photo">{{ form.name?.charAt(0) || '上传' }}</el-avatar>
            </el-upload>
            <span class="tip">点击上传教师照片</span>
          </div>
          <el-form :model="form" label-width="100px">
            <el-row :gutter="12">
              <el-col :span="8"><el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="性别"><el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男"/><el-option label="女" value="女"/></el-select></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="工号"><el-input v-model="form.teacherNo" placeholder="留空自动生成" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="住址"><el-input v-model="form.address" /></el-form-item></el-col>
              <el-col :span="6"><el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" /></el-form-item></el-col>
              <el-col :span="6"><el-form-item label="紧急电话"><el-input v-model="form.emergencyPhone" maxlength="11" placeholder="11位手机号" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 教育与工作 -->
        <el-tab-pane label="教育与工作" name="work">
          <el-form :model="form" label-width="100px">
            <el-row :gutter="12">
              <el-col :span="8"><el-form-item label="学历"><el-input v-model="form.education" placeholder="本科/硕士等" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="毕业院校"><el-input v-model="form.school" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="专业"><el-input v-model="form.major" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="职称"><el-input v-model="form.title" placeholder="高级教师等" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="教龄(年)"><el-input-number v-model="form.teachYears" :min="0" style="width:100%"/></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="入职日期"><el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item></el-col>
              <el-col :span="8">
                <el-form-item label="用工类型">
                  <el-select v-model="form.employType" style="width:100%">
                    <el-option v-for="t in employTypes" :key="t" :label="t" :value="t" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="24"><el-form-item label="教师简介"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 教学能力 -->
        <el-tab-pane label="教学能力" name="ability">
          <el-form :model="form" label-width="100px">
            <el-form-item label="可授课程">
              <el-select v-model="courseIdArr" multiple filterable placeholder="选择该教师可授课程" style="width:100%">
                <el-option v-for="c in courses" :key="c.courseId" :label="`${c.courseName}（${c.category||''}）`" :value="c.courseId" />
              </el-select>
            </el-form-item>
            <el-form-item label="资质证件">
              <el-upload :show-file-list="false" :http-request="uploadCert" accept="image/*,.pdf">
                <el-button>上传证件/证书</el-button>
              </el-upload>
              <div class="cert-list">
                <el-tag v-for="(u,i) in certArr" :key="i" closable @close="certArr.splice(i,1)" style="margin:4px">
                  <a :href="u" target="_blank">证件{{ i+1 }}</a>
                </el-tag>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 薪资 -->
        <el-tab-pane label="薪资" name="salary">
          <el-form :model="form" label-width="100px">
            <el-form-item label="底薪(元/月)"><el-input-number v-model="form.baseSalary" :min="0" :precision="2" style="width:200px" /></el-form-item>
            <el-form-item label="课时费(元/时)"><el-input-number v-model="form.hourlyRate" :min="0" :precision="2" style="width:200px" /></el-form-item>
            <el-form-item label="预估月课时费">
              <span style="color:#c13a2a;font-weight:600">¥{{ estimatedPay }}</span>
              <span class="tip" style="margin-left:8px">= 课时费 × 累计课时({{ form.totalHours || 0 }})，仅参考</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存档案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getStaffList, createStaff, updateStaff, deleteStaff,
  resetStaffPassword, staffLeave, getCourseList
} from '@/api/admin'
import { uploadFiles } from '@/api/upload'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const employTypes = ['全职', '兼职', '实习', '离职']
const typeColor = t => ({ 全职: 'success', 兼职: 'warning', 实习: '', 离职: 'info' }[t] || 'info')

const list = ref([])
const courses = ref([])
const loading = ref(false)
const keyword = ref('')
const employType = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const activeTab = ref('base')
const emptyForm = () => ({ teacherId: null, name: '', gender: '男', birthDate: '', phone: '', email: '', address: '', emergencyContact: '', emergencyPhone: '', teacherNo: '', education: '', school: '', major: '', title: '', teachYears: 0, hireDate: '', employType: '全职', intro: '', photo: '', certs: '', courseIds: '', baseSalary: 0, hourlyRate: 0, status: 1, totalHours: 0 })
const form = ref(emptyForm())
const courseIdArr = ref([])
const certArr = ref([])
const estimatedPay = computed(() => (((Number(form.value.hourlyRate) || 0) * (Number(form.value.totalHours) || 0))).toFixed(2))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStaffList({ keyword: keyword.value || undefined, employType: employType.value || undefined })
    list.value = res.data || []
  } finally { loading.value = false }
}

const openDialog = (row) => {
  form.value = row ? { ...row } : emptyForm()
  courseIdArr.value = form.value.courseIds ? String(form.value.courseIds).split(',').map(Number).filter(Boolean) : []
  try { certArr.value = form.value.certs ? JSON.parse(form.value.certs) : [] } catch { certArr.value = [] }
  activeTab.value = 'base'
  dialogVisible.value = true
}

const uploadPhoto = async ({ file }) => {
  const res = await uploadFiles([file])
  if (res.data?.[0]?.url) { form.value.photo = res.data[0].url; ElMessage.success('照片已上传') }
}
const uploadCert = async ({ file }) => {
  const res = await uploadFiles([file])
  if (res.data?.[0]?.url) { certArr.value.push(res.data[0].url); ElMessage.success('证件已上传') }
}

const MOBILE = /^1[3-9]\d{9}$/
const submit = async () => {
  if (!form.value.name) { ElMessage.warning('请填写姓名'); activeTab.value = 'base'; return }
  if (form.value.phone && !MOBILE.test(form.value.phone)) { ElMessage.warning('联系电话必须为11位有效手机号'); activeTab.value = 'base'; return }
  if (form.value.emergencyPhone && !MOBILE.test(form.value.emergencyPhone)) { ElMessage.warning('紧急电话必须为11位有效手机号'); activeTab.value = 'base'; return }
  form.value.courseIds = courseIdArr.value.join(',')
  form.value.certs = JSON.stringify(certArr.value)
  saving.value = true
  try {
    if (form.value.teacherId) await updateStaff(form.value.teacherId, form.value)
    else await createStaff(form.value)
    ElMessage.success('档案已保存，教师登录账号默认密码 123456')
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const resetPwd = async (row) => {
  await ElMessageBox.confirm(`将教师「${row.name}」的登录密码重置为 123456？`, '提示', { type: 'warning' })
  await resetStaffPassword(row.teacherId)
  ElMessage.success('密码已重置为 123456')
}
const leave = async (row) => {
  await ElMessageBox.confirm(`确认将「${row.name}」设为离职？离职后将停用其登录账号，其班级与排课请尽快重新分配。`, '离职办理', { type: 'warning' })
  await staffLeave(row.teacherId)
  ElMessage.success('已办理离职')
  fetchData()
}
const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除教师「${row.name}」档案？此操作不可恢复。`, '提示', { type: 'warning' })
  await deleteStaff(row.teacherId)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(async () => {
  fetchData()
  const c = await getCourseList({ status: 1 })
  courses.value = c.data || []
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.photo-row { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.tip { color: #6b5a4a; font-size: 13px; }
.cert-list { margin-top: 6px; }
</style>
