<template>
  <div>
    <PageHeader title="考级与比赛" subtitle="浏览考级/比赛项目，在线报名、缴费与成绩查询" />

    <el-tabs v-model="tab" class="exam-tabs">
      <!-- 项目浏览 -->
      <el-tab-pane label="考级比赛项目" name="projects">
        <div class="filter-bar">
          <el-select v-model="fType" placeholder="类型" clearable style="width:120px" @change="loadProjects">
            <el-option label="考级" value="考级" /><el-option label="比赛" value="比赛" />
          </el-select>
          <el-select v-model="fCat" placeholder="类别" clearable style="width:120px" @change="loadProjects">
            <el-option v-for="c in cats" :key="c" :label="c" :value="c" />
          </el-select>
        </div>
        <el-row :gutter="16">
          <el-col :span="8" v-for="p in projects" :key="p.projectId" class="proj-col">
            <el-card shadow="hover" class="proj-card">
              <div class="proj-head">
                <el-tag size="small" :type="p.activity_type==='考级'?'warning':'danger'">{{ p.activity_type }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ p.category }}</el-tag>
                <el-tag size="small" :type="statusType(p.status)">{{ p.status }}</el-tag>
              </div>
              <h3 class="proj-name">{{ p.name }}</h3>
              <div class="proj-meta">主办：{{ p.organizer || '—' }}</div>
              <div class="proj-meta">考试：{{ p.exam_date }} · {{ p.location || '待定' }}</div>
              <div class="proj-meta">报名截止：{{ p.signup_end }}</div>
              <div class="proj-foot">
                <span class="fee">¥{{ p.fee }}</span>
                <el-button type="primary" size="small" :disabled="p.status==='已结束'" @click="openRegister(p)">立即报名</el-button>
                <el-button size="small" @click="detail=p; dVisible=true">详情</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 我的报名 -->
      <el-tab-pane label="我的报名" name="my">
        <el-table :data="myList" border stripe>
          <el-table-column prop="projectName" label="项目" min-width="160" show-overflow-tooltip />
          <el-table-column prop="activityType" label="类型" width="70" />
          <el-table-column prop="fee" label="费用" width="90"><template #default="{row}">¥{{ row.fee }}</template></el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{row}"><el-tag size="small" :type="regStatusType(row.status)">{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="create_time" label="报名时间" width="160"><template #default="{row}">{{ fmt(row.createTime) }}</template></el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{row}">
              <el-button size="small" @click="openTicket(row)">准考证/成绩</el-button>
              <el-button v-if="row.status==='已确认'" size="small" type="success" @click="doPay(row)">去缴费</el-button>
              <el-button v-if="['待审核','已确认','已驳回'].includes(row.status)" size="small" type="danger" @click="doCancel(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 项目详情 -->
    <el-dialog v-model="dVisible" :title="detail?.name" width="560px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="主办单位">{{ detail.organizer }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ detail.signup_start }} 至 {{ detail.signup_end }}</el-descriptions-item>
        <el-descriptions-item label="考试时间/地点">{{ detail.exam_date }} · {{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="报名条件/要求">{{ detail.requirement }}</el-descriptions-item>
        <el-descriptions-item label="奖项设置">{{ detail.awardDesc }}</el-descriptions-item>
        <el-descriptions-item label="所需材料">{{ detail.materials }}</el-descriptions-item>
        <el-descriptions-item label="费用">¥{{ detail.fee }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 报名表单 -->
    <el-dialog v-model="rVisible" title="在线报名" width="520px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="项目"><el-input :model-value="form.projectName" disabled /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender"><el-radio value="男">男</el-radio><el-radio value="女">女</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" /></el-form-item>
        <el-form-item label="材料附件">
          <el-upload :http-request="up" :show-file-list="true" action="#">
            <el-button size="small">上传照片/证件/作品</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="报名费用"><span class="fee">¥{{ form.fee }}</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="rVisible=false">取消</el-button><el-button type="primary" @click="submitReg">提交报名</el-button></template>
    </el-dialog>

    <!-- 准考证/成绩 -->
    <el-dialog v-model="tVisible" title="准考证与成绩" width="520px">
      <el-descriptions :column="1" border v-if="ticket">
        <el-descriptions-item label="项目">{{ ticket.projectName }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag size="small" :type="regStatusType(ticket.status)">{{ ticket.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="审核留言">{{ ticket.auditRemark || '—' }}</el-descriptions-item>
        <el-descriptions-item label="考试时间">{{ ticket.examTime || '待安排' }}</el-descriptions-item>
        <el-descriptions-item label="考场/座位">{{ ticket.examRoom || '待安排' }} {{ ticket.seatNo ? '· '+ticket.seatNo : '' }}</el-descriptions-item>
        <el-descriptions-item label="成绩/结果">{{ ticket.result || '待公布' }}</el-descriptions-item>
        <el-descriptions-item label="获奖情况">{{ ticket.award || '—' }}</el-descriptions-item>
        <el-descriptions-item label="证书编号">{{ ticket.certificateNo || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFiles } from '@/api/upload'

const cats = ['乐器','舞蹈','美术','声乐']
const tab = ref('projects')
const fType = ref(''), fCat = ref('')
const projects = ref([]), myList = ref([])
const dVisible = ref(false), detail = ref(null)
const rVisible = ref(false), tVisible = ref(false), ticket = ref(null)
const form = ref({})

const fmt = (t) => { if(!t) return ''; const d=new Date(t); if(isNaN(d)) return t; const p=n=>String(n).padStart(2,'0'); return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}` }
const statusType = (s) => s==='报名中'?'success':s==='即将截止'?'warning':'info'
const regStatusType = (s) => ({'待审核':'warning','已确认':'primary','已缴费':'success','已完成':'success','已驳回':'danger','已取消':'info'}[s]||'info')

async function loadProjects(){
  const res = await request.get('/student/exam/projects',{params:{activityType:fType.value,category:fCat.value}})
  projects.value = res.data || []
}
async function loadMy(){ const res = await request.get('/student/exam/my'); myList.value = res.data || [] }
function openRegister(p){ form.value = { projectId:p.project_id, projectName:p.name, fee:p.fee, gender:'男' }; rVisible.value=true }
async function up(opt){
  const r = await uploadFiles([opt.file]); form.value.materialUrl = r.data?.[0]?.url
  ElMessage.success('上传成功')
}
async function submitReg(){
  if (form.value.phone && !/^1[3-9]\d{9}$/.test(form.value.phone)) { ElMessage.warning('联系电话必须为11位有效手机号'); return }
  await request.post('/student/exam/register', form.value)
  ElMessage.success('报名成功，等待审核'); rVisible.value=false; loadMy()
}
function openTicket(row){ ticket.value=row; tVisible.value=true }
async function doPay(row){
  await ElMessageBox.confirm(`确认支付 ¥${row.fee}？`,'在线缴费',{type:'warning'})
  await request.post(`/student/exam/${row.regId}/pay`, null, {params:{payMethod:'微信'}})
  ElMessage.success('缴费成功'); loadMy()
}
async function doCancel(row){
  await ElMessageBox.confirm('确认取消该报名？','提示',{type:'warning'})
  await request.post(`/student/exam/${row.regId}/cancel`)
  ElMessage.success('已取消'); loadMy()
}
onMounted(()=>{ loadProjects(); loadMy() })
</script>

<style scoped>
.filter-bar{margin-bottom:14px;display:flex;gap:10px}
.proj-col{margin-bottom:16px}
.proj-head{display:flex;gap:6px;margin-bottom:8px}
.proj-name{margin:6px 0;font-size:16px}
.proj-meta{color:#6b5a4a;font-size:13px;line-height:1.9}
.proj-foot{display:flex;align-items:center;justify-content:space-between;margin-top:10px}
.fee{color:#c13a2a;font-weight:700;font-size:16px}
</style>
