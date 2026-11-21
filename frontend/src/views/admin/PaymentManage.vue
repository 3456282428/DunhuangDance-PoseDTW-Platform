<template>
  <div>
    <PageHeader title="订单与财务管理" subtitle="订单、欠费续费与收入报表" />

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ============ 订单管理 ============ -->
      <el-tab-pane label="订单管理" name="orders">
        <el-row :gutter="16" class="balance-row">
          <el-col :span="6">
            <el-card shadow="hover" :body-style="{padding:'18px 20px'}">
              <div class="bal-label">总收入（学员缴费）</div>
              <div class="bal-value green">¥{{ Number(balance.income||0).toLocaleString() }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" :body-style="{padding:'18px 20px'}">
              <div class="bal-label">退款支出</div>
              <div class="bal-value red">¥{{ Number(balance.expense||0).toLocaleString() }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" :body-style="{padding:'18px 20px'}">
              <div class="bal-label">教师工资（支出）</div>
              <div class="bal-value red">¥{{ Number(balance.salary||0).toLocaleString() }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" :body-style="{padding:'18px 20px'}">
              <div class="bal-label">账户余额</div>
              <div class="bal-value blue">¥{{ Number(balance.balance||0).toLocaleString() }}</div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never">
          <el-form :inline="true" class="filter-bar">
            <el-form-item label="学员">
              <el-select v-model="query.studentId" placeholder="全部学员" clearable filterable style="width:160px">
                <el-option v-for="s in students" :key="s.studentId" :label="s.name" :value="s.studentId" />
              </el-select>
            </el-form-item>
            <el-form-item label="日期">
              <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="query.payType" placeholder="全部" clearable style="width:110px">
                <el-option v-for="t in payTypes.filter(x=>x!=='退款')" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="方式">
              <el-select v-model="query.payMethod" placeholder="全部" clearable style="width:110px">
                <el-option v-for="m in payMethods" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
                <el-option v-for="s in payStatusList" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="search">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button type="success" @click="openPayDialog()">新建缴费</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="paymentList" stripe v-loading="loading">
            <el-table-column prop="paymentId" label="单号" width="60" />
            <el-table-column prop="studentName" label="学员" width="80" />
            <el-table-column prop="className" label="班级" min-width="110" show-overflow-tooltip />
            <el-table-column label="金额" width="95">
              <template #default="{ row }">
                <span :style="{color: row.amount < 0 ? '#c13a2a' : '#2c9678', fontWeight:600}">
                  {{ row.amount < 0 ? '-' : '' }}¥{{ Math.abs(row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="payType" label="类型" width="70">
              <template #default="{ row }">
                <el-tag size="small" :type="row.payType==='退款'?'danger':''">{{ row.payType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="payMethod" label="方式" width="75" />
            <el-table-column prop="payDate" label="时间" width="145">
              <template #default="{ row }">{{ fmt(row.payDate) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="payStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="warning" :disabled="row.status!=='已支付' || row.amount<0"
                  @click="handleRefund(row)">退款</el-button>
              </template>
            </el-table-column>
          </el-table>

          <Pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
            :total="total" @pagination="fetchPayments" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 财务管理 ============ -->
      <el-tab-pane label="财务管理" name="finance">
        <el-row :gutter="20">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>
                <div class="card-head">
                  <span>学员账户</span>
                  <el-select v-model="accountStudentId" placeholder="选择学员查看账户" filterable clearable
                    style="width:220px" @change="fetchAccount">
                    <el-option v-for="s in students" :key="s.studentId" :label="s.name" :value="s.studentId" />
                  </el-select>
                </div>
              </template>
              <div v-if="account">
                <el-row :gutter="12" class="acct-cards">
                  <el-col :span="6"><div class="acct-box"><div class="t">累计缴费</div><div class="v green">¥{{ account.totalPaid }}</div></div></el-col>
                  <el-col :span="6"><div class="acct-box"><div class="t">累计退款</div><div class="v red">¥{{ account.totalRefund }}</div></div></el-col>
                  <el-col :span="6"><div class="acct-box"><div class="t">净缴金额</div><div class="v blue">¥{{ account.netPaid }}</div></div></el-col>
                  <el-col :span="6"><div class="acct-box"><div class="t">欠费</div><div class="v" :class="account.arrears>0?'red':'green'">¥{{ account.arrears }}</div></div></el-col>
                </el-row>
                <el-divider content-position="left">报名班级</el-divider>
                <el-table :data="account.enrollments" size="small" stripe>
                  <el-table-column prop="className" label="班级" />
                  <el-table-column prop="status" label="状态" width="80" />
                  <el-table-column label="学费" width="100"><template #default="{row}">¥{{ row.tuitionAmount }}</template></el-table-column>
                  <el-table-column label="已缴" width="100"><template #default="{row}">¥{{ row.paid }}</template></el-table-column>
                  <el-table-column label="欠缴" width="100"><template #default="{row}"><span :style="{color:row.arrears>0?'#c13a2a':''}">¥{{ row.arrears }}</span></template></el-table-column>
                </el-table>
                <el-divider content-position="left">缴费历史</el-divider>
                <el-table :data="account.payments" size="small" stripe max-height="260">
                  <el-table-column prop="payDate" label="时间" width="160"><template #default="{row}">{{ fmt(row.payDate) }}</template></el-table-column>
                  <el-table-column prop="payType" label="类型" width="80" />
                  <el-table-column prop="payMethod" label="方式" width="80" />
                  <el-table-column label="金额" width="100"><template #default="{row}">¥{{ row.amount }}</template></el-table-column>
                  <el-table-column prop="remark" label="备注" show-overflow-tooltip />
                </el-table>
              </div>
              <el-empty v-else description="请选择学员查看账户明细" />
            </el-card>
          </el-col>

          <el-col :span="10">
            <el-card shadow="never">
              <template #header><div class="card-head"><span>欠费提醒</span><el-button size="small" @click="fetchArrears">刷新</el-button></div></template>
              <el-table :data="arrears" size="small" stripe max-height="520">
                <el-table-column prop="studentName" label="学员" width="90" />
                <el-table-column prop="className" label="班级" show-overflow-tooltip />
                <el-table-column label="欠费" width="90">
                  <template #default="{row}"><span style="color:#c13a2a;font-weight:600">¥{{ row.arrears }}</span></template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{row}">
                    <el-button size="small" type="primary" @click="openRenew(row)">续费</el-button>
                    <el-button size="small" @click="remind(row)">催缴</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!arrears.length" description="暂无欠费学员" />
            </el-card>
          </el-col>
        </el-row>

        <!-- 教师工资账户 -->
        <el-card shadow="never" style="margin-top:20px">
          <template #header>
            <div class="card-head">
              <span>教师账户（工资）</span>
              <el-select v-model="accountTeacherId" placeholder="选择教师查看工资账户" filterable clearable
                style="width:240px" @change="fetchTeacherAccount">
                <el-option v-for="t in teachers" :key="t.teacherId"
                  :label="`${t.teacherNo} ${t.name}`" :value="t.teacherId" />
              </el-select>
            </div>
          </template>
          <div v-if="tAccount">
            <el-row :gutter="12" class="acct-cards">
              <el-col :span="5"><div class="acct-box"><div class="t">基本底薪</div><div class="v">¥{{ tAccount.baseSalary }}</div></div></el-col>
              <el-col :span="5"><div class="acct-box"><div class="t">课时费(元/时)</div><div class="v">¥{{ tAccount.hourlyRate }}</div></div></el-col>
              <el-col :span="5"><div class="acct-box"><div class="t">已完成课时</div><div class="v blue">{{ tAccount.completedHours }} 时</div></div></el-col>
              <el-col :span="5"><div class="acct-box"><div class="t">课时工资</div><div class="v">¥{{ tAccount.classFee }}</div></div></el-col>
              <el-col :span="4"><div class="acct-box"><div class="t">应发合计</div><div class="v red">¥{{ tAccount.amount }}</div></div></el-col>
            </el-row>
            <el-divider content-position="left">授课班级课时明细（{{ tAccount.employType || '—' }}）</el-divider>
            <el-table :data="tAccount.classHours" size="small" stripe>
              <el-table-column prop="className" label="授课班级" />
              <el-table-column prop="lessonCount" label="已授课次" width="120" />
              <el-table-column label="累计课时" width="140">
                <template #default="{row}">{{ row.hours }} 时</template>
              </el-table-column>
              <el-table-column label="课时工资">
                <template #default="{row}">¥{{ (Number(row.hours) * Number(tAccount.hourlyRate)).toFixed(2) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!tAccount.classHours || !tAccount.classHours.length" description="该教师暂无已完成课时" />
          </div>
          <el-empty v-else description="请选择教师查看工资账户明细" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 统计报表 ============ -->
      <el-tab-pane label="统计报表" name="reports">
        <el-row :gutter="16">
          <el-col :span="6" v-for="c in refundCards" :key="c.t">
            <el-card shadow="hover" :body-style="{padding:'18px'}">
              <div class="rp-title">{{ c.t }}</div>
              <div class="rp-value" :style="{color:c.color}">{{ c.v }}</div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top:16px">
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>
                <div class="card-head"><span>收入构成</span>
                  <el-radio-group v-model="incomeDim" size="small" @change="renderIncome">
                    <el-radio-button label="type">按类型</el-radio-button>
                    <el-radio-button label="method">按方式</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              <div ref="pieRef" style="height:320px"></div>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never">
              <template #header><span>月度收入趋势</span></template>
              <div ref="barRef" style="height:320px"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" style="margin-top:16px">
          <template #header><span>学员缴费排行 TOP10</span></template>
          <div ref="topRef" style="height:340px"></div>
        </el-card>
        <el-card shadow="never" style="margin-top:16px">
          <template #header>
            <span>教师工资支出（与学员缴费收入区分）</span>
            <span style="margin-left:12px;color:#c13a2a;font-weight:600">合计 ¥{{ Number(balance.salary||0).toLocaleString() }}</span>
          </template>
          <el-table :data="salaryRows" stripe size="small">
            <el-table-column prop="teacherNo" label="工号" width="90" />
            <el-table-column prop="teacherName" label="教师" width="110" />
            <el-table-column prop="employType" label="用工类型" width="100">
              <template #default="{row}"><el-tag size="small">{{ row.employType }}</el-tag></template>
            </el-table-column>
            <el-table-column label="底薪" width="110">
              <template #default="{row}">¥{{ row.baseSalary }}</template>
            </el-table-column>
            <el-table-column label="课时费(元/时)" width="120">
              <template #default="{row}">¥{{ row.hourlyRate }}</template>
            </el-table-column>
            <el-table-column label="已完成课时" width="100">
              <template #default="{row}">{{ row.completedHours }} 时</template>
            </el-table-column>
            <el-table-column label="应发工资">
              <template #default="{row}"><span style="color:#c13a2a;font-weight:600">¥{{ row.amount }}</span></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建缴费弹窗 -->
    <el-dialog v-model="payVisible" title="新建缴费" width="460px">
      <el-form :model="payForm" :rules="payRules" ref="payFormRef" label-width="90px">
        <el-form-item label="班级" prop="classId">
          <el-select v-model="payForm.classId" filterable placeholder="请选择班级（必选）" style="width:100%">
            <el-option v-for="c in classList" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="学员">
          <el-select v-model="payForm.studentId" filterable clearable placeholder="可选（班级类缴费可留空）" style="width:100%">
            <el-option v-for="s in students" :key="s.studentId" :label="s.name" :value="s.studentId" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="payForm.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="类型" prop="payType">
              <el-select v-model="payForm.payType" style="width:100%">
                <el-option v-for="t in payTypes.filter(x=>x!=='退款')" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式" prop="payMethod">
              <el-select v-model="payForm.payMethod" style="width:100%">
                <el-option v-for="m in payMethods" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="payForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPay">保存</el-button>
      </template>
    </el-dialog>

    <!-- 续费弹窗 -->
    <el-dialog v-model="renewVisible" title="学员续费" width="420px">
      <el-form :model="renewForm" label-width="90px">
        <el-form-item label="学员">{{ renewForm.studentName }}</el-form-item>
        <el-form-item label="续费金额">
          <el-input-number v-model="renewForm.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="renewForm.payMethod" style="width:100%">
            <el-option v-for="m in payMethods" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="renewForm.remark" placeholder="如：延长有效期 / 购买课时包" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRenew">确认续费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getPaymentList, createPayment, refundPayment, getStudentOptions, getClassList,
  getStudentAccount, getArrearsList, renewPayment, getIncomeStat, getRefundStat, getTopStudents,
  getTeacherSalary, getTeacherAccount, getStaffList
} from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const payTypes = ['报名', '续费', '考级', '比赛', '其他', '退款']
const payMethods = ['微信', '支付宝', '现金', '刷卡']
const payStatusList = ['待支付', '已支付', '退款中', '已退款', '已取消']
const payStatusType = s => ({ '已支付': 'success', '退款中': 'warning', '已退款': 'info', '已取消': 'info', '待支付': 'danger' }[s] || '')
const fmt = d => d ? String(d).replace('T', ' ').slice(0, 19) : ''

const activeTab = ref('orders')
const students = ref([])
const classList = ref([])

// 账户余额
const balance = ref({ income: 0, expense: 0, salary: 0, balance: 0 })
const salaryRows = ref([])
const fetchBalance = async () => {
  try {
    const [refundRes, salaryRes] = await Promise.all([getRefundStat(), getTeacherSalary()])
    const d = refundRes.data || {}
    const salary = Number(salaryRes.data?.total || 0)
    salaryRows.value = salaryRes.data?.rows || []
    balance.value = {
      income: d.totalIncome || 0,
      expense: d.totalRefund || 0,
      salary,
      balance: (Number(d.totalIncome || 0) - Number(d.totalRefund || 0) - salary).toFixed(2)
    }
  } catch (e) {}
}

// ---- 订单 ----
const loading = ref(false)
const paymentList = ref([])
const total = ref(0)
const query = reactive({ studentId: null, dateRange: [], payType: '', payMethod: '', status: '', pageNum: 1, pageSize: 10 })

const fetchPayments = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum, pageSize: query.pageSize,
      studentId: query.studentId || undefined,
      payType: query.payType || undefined,
      payMethod: query.payMethod || undefined,
      status: query.status || undefined
    }
    if (query.dateRange && query.dateRange.length === 2) {
      params.startTime = query.dateRange[0] + ' 00:00:00'
      params.endTime = query.dateRange[1] + ' 23:59:59'
    }
    const res = await getPaymentList(params)
    paymentList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
const search = () => { query.pageNum = 1; fetchPayments() }
const resetQuery = () => {
  Object.assign(query, { studentId: null, dateRange: [], payType: '', payMethod: '', status: '', pageNum: 1 })
  fetchPayments()
}

const payVisible = ref(false)
const saving = ref(false)
const payFormRef = ref(null)
const emptyPay = () => ({ studentId: null, classId: null, amount: 1000, payType: '报名', payMethod: '微信', remark: '' })
const payForm = ref(emptyPay())
const payRules = {
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'change' }],
  payType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  payMethod: [{ required: true, message: '请选择付款方式', trigger: 'change' }]
}
const openPayDialog = () => { payForm.value = emptyPay(); payVisible.value = true }
const submitPay = async () => {
  await payFormRef.value.validate()
  saving.value = true
  try {
    await createPayment(payForm.value)
    ElMessage.success('缴费已录入')
    payVisible.value = false
    fetchPayments()
    fetchBalance()
  } finally { saving.value = false }
}
const handleRefund = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `确认对 ${row.studentName} 的 ¥${Math.abs(row.amount)} 订单退款？可填写备注`, '退款处理',
      { confirmButtonText: '确认退款', inputValue: '全额退款' })
    await refundPayment(row.paymentId, value || '全额退款')
    ElMessage.success('退款成功')
    fetchPayments()
    fetchBalance()
  } catch (e) {}
}

// ---- 财务 ----
const accountStudentId = ref(null)
const account = ref(null)
const arrears = ref([])
const fetchAccount = async () => {
  if (!accountStudentId.value) { account.value = null; return }
  const res = await getStudentAccount(accountStudentId.value)
  account.value = res.data
}
const fetchArrears = async () => { const res = await getArrearsList(); arrears.value = res.data || [] }

// 教师工资账户
const teachers = ref([])
const accountTeacherId = ref(null)
const tAccount = ref(null)
const fetchTeacherAccount = async () => {
  if (!accountTeacherId.value) { tAccount.value = null; return }
  const res = await getTeacherAccount(accountTeacherId.value)
  tAccount.value = res.data
}
const fetchTeachers = async () => {
  const res = await getStaffList({})
  teachers.value = (res.data || []).filter(t => t.status === 1)
}
const remind = (row) => ElMessage.success(`已向 ${row.studentName} 发送催缴通知（¥${row.arrears}）`)

const renewVisible = ref(false)
const renewForm = ref({ studentId: null, studentName: '', classId: null, amount: 1000, payMethod: '微信', remark: '' })
const openRenew = (row) => {
  renewForm.value = { studentId: row.studentId, studentName: row.studentName, classId: row.classId, amount: row.arrears, payMethod: '微信', remark: '' }
  renewVisible.value = true
}
const submitRenew = async () => {
  saving.value = true
  try {
    await renewPayment(renewForm.value)
    ElMessage.success('续费办理成功')
    renewVisible.value = false
    fetchArrears()
    if (accountStudentId.value === renewForm.value.studentId) fetchAccount()
  } finally { saving.value = false }
}

// ---- 报表 ----
const pieRef = ref(null), barRef = ref(null), topRef = ref(null)
let pieChart, barChart, topChart
const incomeDim = ref('type')
const refundCards = ref([
  { t: '总收入', v: '¥0', color: '#2c9678' },
  { t: '退款总额', v: '¥0', color: '#c13a2a' },
  { t: '退款笔数', v: '0 笔', color: '#c89a2e' },
  { t: '退款率', v: '0%', color: '#6b5a4a' }
])
const palette = ['#1685a9', '#2c9678', '#c89a2e', '#c13a2a', '#9B59B6', '#1ABC9C']

const renderIncome = async () => {
  const res = await getIncomeStat(incomeDim.value)
  await nextTick()
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    color: palette,
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '52%'],
      data: (res.data.categories || []).map((c, i) => ({ name: c, value: res.data.values[i] })),
      label: { formatter: '{b}\n¥{c}' }
    }]
  }, true)
}
const renderMonth = async () => {
  const res = await getIncomeStat('month')
  await nextTick()
  if (!barChart) barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>收入：¥${p[0].value}` },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: res.data.categories },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: res.data.values, itemStyle: { color: '#1685a9', borderRadius: [4,4,0,0] }, barMaxWidth: 40 }]
  }, true)
}
const renderTop = async () => {
  const res = await getTopStudents(10)
  const list = (res.data || []).slice().reverse()
  await nextTick()
  if (!topChart) topChart = echarts.init(topRef.value)
  topChart.setOption({
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}：¥${p[0].value}` },
    grid: { left: 80, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: list.map(x => x.studentName) },
    series: [{ type: 'bar', data: list.map(x => Number(x.total)), itemStyle: { color: '#2c9678', borderRadius: [0,4,4,0] }, barMaxWidth: 22 }]
  }, true)
}
const loadReports = async () => {
  const r = await getRefundStat()
  const d = r.data
  refundCards.value = [
    { t: '总收入', v: `¥${Number(d.totalIncome||0).toLocaleString()}`, color: '#2c9678' },
    { t: '退款总额', v: `¥${Number(d.totalRefund||0).toLocaleString()}`, color: '#c13a2a' },
    { t: '退款笔数', v: `${d.refundCount||0} 笔`, color: '#c89a2e' },
    { t: '退款率', v: `${d.refundRate||0}%`, color: '#6b5a4a' }
  ]
  renderIncome(); renderMonth(); renderTop()
}
let reportsLoaded = false
const onTabChange = (name) => {
  if (name === 'finance') { fetchArrears(); fetchTeachers() }
  if (name === 'reports' && !reportsLoaded) { reportsLoaded = true; nextTick(loadReports) }
}

const loadOptions = async () => {
  const [s, c] = await Promise.all([getStudentOptions(), getClassList({ pageNum:1, pageSize:200 })])
  students.value = s.data || []
  classList.value = c.data.rows || c.data || []
}

onMounted(() => { loadOptions(); fetchPayments(); fetchBalance() })
</script>

<style scoped>
.balance-row { margin-bottom: 16px; }
.bal-label { color:#6b5a4a; font-size:13px; }
.bal-value { font-size:26px; font-weight:700; margin-top:6px; }
.filter-bar { margin-bottom: 4px; }
.card-head { display:flex; justify-content:space-between; align-items:center; }
.acct-cards { margin-bottom: 4px; }
.acct-box { background:#f7f9fc; border-radius:8px; padding:12px; text-align:center; }
.acct-box .t { color:#6b5a4a; font-size:12px; }
.acct-box .v { font-size:18px; font-weight:700; margin-top:6px; }
.green { color:#2c9678; } .red { color:#c13a2a; } .blue { color:#1685a9; }
.rp-title { color:#6b5a4a; font-size:13px; }
.rp-value { font-size:24px; font-weight:700; margin-top:8px; }
</style>
