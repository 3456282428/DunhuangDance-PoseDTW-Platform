<template>
  <div>
    <PageHeader title="考级与比赛" subtitle="查看项目、推荐学员报名、跟踪本班学员成绩" />

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>报名总数</span><b>{{ stats.totalReg||0 }}</b></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>已完成</span><b>{{ stats.finished||0 }}</b></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>通过人数</span><b>{{ stats.passCount||0 }}</b></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>通过率</span><b>{{ stats.passRate||0 }}%</b></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="tab">
      <el-tab-pane label="考级比赛项目" name="projects">
        <el-table :data="projects" border stripe>
          <el-table-column prop="name" label="项目名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="activity_type" label="类型" width="70" />
          <el-table-column prop="category" label="类别" width="80" />
          <el-table-column prop="organizer" label="主办单位" min-width="140" show-overflow-tooltip />
          <el-table-column prop="exam_date" label="考试日期" width="120" />
          <el-table-column prop="fee" label="费用" width="90"><template #default="{row}">¥{{ row.fee }}</template></el-table-column>
          <el-table-column prop="regCount" label="报名数" width="80" />
          <el-table-column label="状态" width="100"><template #default="{row}"><el-tag size="small">{{ row.status }}</el-tag></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="学员报名与成绩" name="regs">
        <div class="filter-bar">
          <el-select v-model="fType" placeholder="类型" clearable style="width:120px" @change="loadRegs">
            <el-option label="考级" value="考级" /><el-option label="比赛" value="比赛" />
          </el-select>
          <el-select v-model="fStatus" placeholder="状态" clearable style="width:130px" @change="loadRegs">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button type="primary" @click="openRecommend">推荐学员报名</el-button>
        </div>
        <el-table :data="regs" border stripe>
          <el-table-column prop="studentName" label="学员" width="100" />
          <el-table-column prop="projectName" label="项目" min-width="150" show-overflow-tooltip />
          <el-table-column prop="activityType" label="类型" width="70" />
          <el-table-column prop="teacherComment" label="教师推荐意见" min-width="130" show-overflow-tooltip />
          <el-table-column label="状态" width="90"><template #default="{row}"><el-tag size="small">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column prop="result" label="成绩" width="100"><template #default="{row}">{{ row.result||'—' }}</template></el-table-column>
          <el-table-column prop="award" label="获奖" width="100"><template #default="{row}">{{ row.award||'—' }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="recVisible" title="推荐/代为报名" width="480px">
      <el-form :model="rec" label-width="92px">
        <el-form-item label="学号">
          <el-input v-model="rec.studentNo" placeholder="输入学号，如 20240101" style="width:calc(100% - 86px)" @keyup.enter="lookup">
            <template #append><el-button @click="lookup">查询</el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item label="姓名">
          <el-tag v-if="rec.name" type="success" size="large">{{ rec.name }}</el-tag>
          <span v-else style="color:#6b5a4a;font-size:13px">输入学号后自动比对数据库</span>
        </el-form-item>
        <el-form-item label="在读班级">
          <el-select v-model="rec.classId" :disabled="!rec.classes?.length" placeholder="选择关联班级" style="width:100%">
            <el-option v-for="c in (rec.classes||[])" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="rec.projectId" :disabled="!rec.name" placeholder="选择项目" style="width:100%">
            <el-option v-for="p in projects" :key="p.project_id" :label="`${p.name}（¥${p.fee}）`" :value="p.project_id" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐意见"><el-input v-model="rec.comment" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="recVisible=false">取消</el-button><el-button type="primary" @click="submitRec">提交推荐</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const tab = ref('projects')
const projects = ref([]), regs = ref([]), stats = ref({})
const fType = ref(''), fStatus = ref('')
const statuses = ['待审核','已确认','已缴费','已完成','已驳回','已取消']
const recVisible = ref(false), rec = ref({})

async function loadAll(){
  projects.value = (await request.get('/teacher/exam/projects')).data || []
  stats.value = (await request.get('/teacher/exam/stats')).data || {}
}
async function loadRegs(){
  regs.value = (await request.get('/teacher/exam/registrations',{params:{activityType:fType.value,status:fStatus.value}})).data || []
}
function openRecommend(){ rec.value={studentNo:'',name:'',classes:[],classId:null,projectId:null,comment:''}; recVisible.value=true }
async function lookup(){
  if(!rec.value.studentNo) return ElMessage.warning('请输入学号')
  try{
    const r = await request.get('/teacher/exam/student-lookup',{params:{studentNo:rec.value.studentNo}})
    rec.value.name = r.data.name
    rec.value.classes = r.data.classes||[]
    rec.value.classId = rec.value.classes[0]?.classId||null
    ElMessage.success(`已匹配：${r.data.name}`)
  }catch(e){ rec.value.name=''; rec.value.classes=[] }
}
async function submitRec(){
  if(!rec.value.studentNo||!rec.value.projectId) return ElMessage.warning('请先查询学号并选择项目')
  await request.post('/teacher/exam/recommend', {studentNo:rec.value.studentNo,classId:rec.value.classId,projectId:rec.value.projectId,comment:rec.value.comment})
  ElMessage.success('推荐成功，已进入待审核'); recVisible.value=false; loadRegs()
}
onMounted(()=>{ loadAll(); loadRegs() })
</script>

<style scoped>
.stat-row{margin-bottom:14px}
.stat{display:flex;flex-direction:column;gap:6px}
.stat span{color:#6b5a4a;font-size:13px}
.stat b{font-size:24px;color:#1685a9}
.filter-bar{display:flex;gap:10px;margin-bottom:14px}
</style>
