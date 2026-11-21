<template>
  <div>
    <PageHeader title="我的订单" subtitle="报名缴费与续费记录" />
    <el-table :data="orders" stripe v-loading="loading">
      <el-table-column prop="paymentId" label="订单号" width="100" />
      <el-table-column prop="className" label="课程/班级" />
      <el-table-column prop="amount" label="金额" />
      <el-table-column prop="payType" label="类型" />
      <el-table-column prop="payMethod" label="支付方式" />
      <el-table-column prop="payDate" label="时间" :formatter="fmtTime" width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status || '已支付' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            size="small"
            type="warning"
            :disabled="row.status === '退款中' || row.status === '已退款'"
            @click="handleRefund(row)"
          >申请退款</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @pagination="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyOrders, applyRefund } from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMyOrders({ pageNum: pageNum.value, pageSize: pageSize.value })
    orders.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const statusType = (s) => {
  if (s === '退款中') return 'warning'
  if (s === '已退款') return 'info'
  return 'success'
}

const handleRefund = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入退款原因', '申请退款', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写退款原因',
      inputValidator: v => (v && v.trim() ? true : '请输入退款原因')
    })
    await applyRefund(row.paymentId, value)
    ElMessage.success('退款申请已提交，等待处理')
    fetchData()
  } catch (e) {}
}

const fmtTime = (_row, _col, val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(fetchData)
</script>
