<template>
  <div>
    <PageHeader title="考级比赛管理" subtitle="项目发布、报名审核、缴费确认、考场安排与成绩录入" />

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>报名总数</span><b>{{ stats.totalReg||0 }}</b></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>已完成</span><b>{{ stats.finished||0 }}</b></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>获奖人数</span><b>{{ stats.awardCount||0 }}</b></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat"><span>通过率</span><b>{{ stats.passRate||0 }}%</b></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="tab">
      <!-- 项目管理 -->
      <el-tab-pane label="项目管理" name="project">
        <div class="filter-bar">
          <el-input v-model="kw" placeholder="项目名称" clearable style="width:180px" @keyup.enter="loadProjects" />
          <el-select v-model="pType" placeholder="类型" clearable style="width:110px" @change="loadProjects">
            <el-option label="考级" value="考级" /><el-option label="比赛" value="比赛" />
          </el-select>
          <el-button type="primary" @click="loadProjects">查询</el-button>
          <el-button type="success" @click="openProject(null)">发布项目</el-button>
        </div>
        <el-table :data="projects" border stripe size="default">
          <el-table-column prop="project_id" label="编号" width="46" />
          <el-table-column prop="name" label="项目名称" min-width="122" show-overflow-tooltip />
          <el-table-column prop="activity_type" label="类型" width="56" />
          <el-table-column prop="category" label="类别" width="60" />
          <el-table-column prop="organizer" label="主办单位" min-width="80" show-overflow-tooltip />
          <el-table-column prop="exam_date" label="考试日期" width="94" />
          <el-table-column prop="fee" label="费用" width="66"><template #default="{row}">¥{{ row.fee }}</template></el-table-column>
          <el-table-column prop="regCount" label="报名" width="44" />
          <el-table-column label="状态" width="92"><template #default="{row}"><el-tag size="small">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="104">
            <template #default="{row}">
              <el-button link type="primary" size="small" @click="openProject(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="delProject(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 报名管理 -->
      <el-tab-pane label="报名管理" name="reg">
        <div class="filter-bar">
          <el-select v-model="rProject" placeholder="全部项目" clearable style="width:200px" @change="loadRegs">
            <el-option v-for="p in projects" :key="p.project_id" :label="p.name" :value="p.project_id" />
          </el-select>
          <el-select v-model="rStatus" placeholder="全部状态" clearable style="width:130px" @change="loadRegs">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button type="primary" @click="loadRegs">查询</el-button>
        </div>
        <el-table :data="regs" border stripe>
          <el-table-column prop="regId" label="编号" width="55" />
          <el-table-column prop="studentName" label="学员" width="90" />
          <el-table-column prop="projectName" label="项目" min-width="140" show-overflow-tooltip />
          <el-table-column prop="activityType" label="类型" width="65" />
          <el-table-column prop="phone" label="电话" width="115" />
          <el-table-column prop="teacherName" label="推荐教师" width="90"><template #default="{row}">{{ row.teacherName||'—' }}</template></el-table-column>
          <el-table-column label="状态" width="85"><template #default="{row}"><el-tag size="small">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{row}">
              <el-button v-if="row.status==='待审核'" size="small" type="success" @click="audit(row,true)">确认</el-button>
              <el-button v-if="row.status==='待审核'" size="small" type="danger" @click="audit(row,false)">驳回</el-button>
              <el-button v-if="row.status==='已确认'" size="small" type="warning" @click="markPaid(row)">确认缴费</el-button>
              <el-button v-if="['已缴费','已完成'].includes(row.status)" size="small" @click="openArrange(row)">考场安排</el-button>
              <el-button v-if="row.status==='已缴费'" size="small" type="primary" @click="openResult(row)">成绩录入</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 项目编辑 -->
    <el-dialog v-model="pVisible" :title="pf.projectId?'编辑项目':'发布项目'" width="640px">
      <el-form :model="pf" label-width="92px">
        <el-form-item label="项目名称"><el-input v-model="pf.name" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="pf.activityType"><el-radio value="考级">考级</el-radio><el-radio value="比赛">比赛</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="pf.category" style="width:100%"><el-option v-for="c in cats" :key="c" :label="c" :value="c" /></el-select>
        </el-form-item>
        <el-form-item label="主办单位"><el-input v-model="pf.organizer" /></el-form-item>
        <el-form-item label="报名起止">
          <el-date-picker v-model="pf.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="截止" />
        </el-form-item>
        <el-form-item label="考试日期"><el-date-picker v-model="pf.examDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="考试时间">
          <el-time-picker v-model="pf.timeRange" is-range range-separator="至" start-placeholder="开始" end-placeholder="结束"
            value-format="HH:mm" format="HH:mm" :minute-step="5" style="width:100%" />
        </el-form-item>
        <el-form-item label="考场"><el-input v-model="pf.examRoom" placeholder="如 第一考场 / 大剧院A厅" /></el-form-item>
        <el-form-item label="地点"><el-input v-model="pf.location" /></el-form-item>
        <el-form-item label="费用(元)"><el-input-number v-model="pf.fee" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item>
        <el-form-item label="报名条件/要求"><el-input v-model="pf.requirement" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="奖项设置"><el-input v-model="pf.awardDesc" /></el-form-item>
        <el-form-item label="所需材料"><el-input v-model="pf.materials" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="pf.status" style="width:100%"><el-option v-for="s in ['报名中','即将截止','已结束']" :key="s" :label="s" :value="s" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="pVisible=false">取消</el-button><el-button type="primary" @click="saveProject">保存</el-button></template>
    </el-dialog>

    <!-- 考场安排（时间/考场继承项目，座位自动分配） -->
    <el-dialog v-model="aVisible" title="考场/赛程安排" width="480px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:14px"
        title="考试时间与考场与项目设置保持一致，座位号按同一时间段、同一考场自动顺序分配。" />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="项目">{{ curReg?.projectName }}</el-descriptions-item>
        <el-descriptions-item label="考试时间">{{ fixedTime }}</el-descriptions-item>
        <el-descriptions-item label="考场">{{ curReg?.projectExamRoom || '—' }}</el-descriptions-item>
        <el-descriptions-item label="当前座位">
          <el-tag v-if="curReg?.seatNo" type="success">{{ curReg.seatNo }} 号</el-tag>
          <span v-else style="color:#6b5a4a">点击确定后自动分配</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="aVisible=false">取消</el-button><el-button type="primary" @click="saveArrange">自动分配并保存</el-button></template>
    </el-dialog>

    <!-- 成绩录入 -->
    <el-dialog v-model="resVisible" title="成绩录入与发布" width="460px">
      <el-form :model="rf" label-width="92px">
        <el-form-item label="成绩/结果"><el-input v-model="rf.result" placeholder="如 通过/良好/一等奖" /></el-form-item>
        <el-form-item label="获奖情况"><el-input v-model="rf.award" /></el-form-item>
        <el-form-item label="证书编号"><el-input v-model="rf.certificateNo" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="resVisible=false">取消</el-button><el-button type="primary" @click="saveResult">发布成绩</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const cats = ['乐器','舞蹈','美术','声乐']
const statuses = ['待审核','已确认','已缴费','已完成','已驳回','已取消']
const tab = ref('project')
const projects = ref([]), regs = ref([]), stats = ref({})
const kw = ref(''), pType = ref(''), rProject = ref(''), rStatus = ref('')
const pVisible = ref(false), pf = ref({})
const aVisible = ref(false), af = ref({}), curReg = ref(null)
const resVisible = ref(false), rf = ref({})

async function loadProjects(){
  projects.value = (await request.get('/admin/exam-module/projects',{params:{keyword:kw.value,activityType:pType.value}})).data||[]
}
async function loadRegs(){
  regs.value = (await request.get('/admin/exam-module/registrations',{params:{projectId:rProject.value,status:rStatus.value}})).data||[]
}
async function loadStats(){ stats.value = (await request.get('/admin/exam-module/stats')).data||{} }

function openProject(row){
  if(row){
    pf.value = {
      projectId: row.project_id,
      name: row.name, activityType: row.activity_type, category: row.category,
      organizer: row.organizer, location: row.location, fee: Number(row.fee)||0,
      requirement: row.requirement, awardDesc: row.award_desc, materials: row.materials,
      examDate: row.exam_date ? String(row.exam_date) : '',
      timeRange:[row.exam_start||'', row.exam_end||''],
      examRoom: row.exam_room||'',
      range:[row.signup_start?String(row.signup_start):'', row.signup_end?String(row.signup_end):''],
      status: row.status
    }
  } else {
    pf.value = {activityType:'考级',category:'乐器',status:'报名中',fee:0,range:[],timeRange:['09:00','10:30']}
  }
  pVisible.value=true
}
async function saveProject(){
  if(!pf.value.name) return ElMessage.warning('请填写项目名称')
  const body={
    projectId: pf.value.projectId,
    name: pf.value.name, activityType: pf.value.activityType, category: pf.value.category,
    organizer: pf.value.organizer, location: pf.value.location, fee: pf.value.fee,
    requirement: pf.value.requirement, awardDesc: pf.value.awardDesc, materials: pf.value.materials,
    examDate: pf.value.examDate||null,
    examStart: pf.value.timeRange?.[0]||null, examEnd: pf.value.timeRange?.[1]||null,
    examRoom: pf.value.examRoom||null,
    status: pf.value.status,
    signupStart: pf.value.range?.[0]||null, signupEnd: pf.value.range?.[1]||null
  }
  if(body.projectId) await request.put(`/admin/exam-module/project/${body.projectId}`,body)
  else await request.post('/admin/exam-module/project',body)
  ElMessage.success('已保存'); pVisible.value=false; loadProjects()
}
async function delProject(row){
  await ElMessageBox.confirm(`确认删除项目「${row.name}」？`,'提示',{type:'warning'})
  await request.delete(`/admin/exam-module/project/${row.project_id}`)
  ElMessage.success('已删除'); loadProjects()
}
async function audit(row,pass){
  let remark=''
  if(!pass){ ({value:remark} = await ElMessageBox.prompt('请填写驳回原因','驳回',{inputType:'textarea'})) }
  await request.post(`/admin/exam-module/registration/${row.regId}/audit`,{pass,remark})
  ElMessage.success(pass?'已确认':'已驳回'); loadRegs(); loadStats()
}
async function markPaid(row){
  await ElMessageBox.confirm('确认该学员已线下缴费？','确认缴费',{type:'warning'})
  await request.post(`/admin/exam-module/registration/${row.regId}/paid`)
  ElMessage.success('已确认缴费并生成财务订单'); loadRegs(); loadStats()
}
const fixedTime = computed(()=>{
  const r=curReg.value; if(!r) return ''
  const d=r.examDate?String(r.examDate):''
  return `${d} ${r.projectExamStart||''}-${r.projectExamEnd||''}`
})
function openArrange(row){ curReg.value=row; aVisible.value=true }
async function saveArrange(){
  await request.post(`/admin/exam-module/registration/${curReg.value.regId}/arrange`)
  ElMessage.success('已按项目时间/考场安排并自动分配座位'); aVisible.value=false; loadRegs()
}
function openResult(row){ curReg.value=row; rf.value={result:row.result||'',award:row.award||'',certificateNo:row.certificate_no||''}; resVisible.value=true }
async function saveResult(){
  await request.post(`/admin/exam-module/registration/${curReg.value.regId}/result`,rf.value)
  ElMessage.success('成绩已发布'); resVisible.value=false; loadRegs(); loadStats()
}
onMounted(()=>{ loadProjects(); loadRegs(); loadStats() })
</script>

<style scoped>
.stat-row{margin-bottom:14px}
.stat{display:flex;flex-direction:column;gap:6px}
.stat span{color:#6b5a4a;font-size:13px}
.stat b{font-size:24px;color:#1685a9}
.filter-bar{display:flex;gap:10px;margin-bottom:14px;flex-wrap:wrap}
</style>

