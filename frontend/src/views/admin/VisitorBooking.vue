<template>
  <div>
    <PageHeader title="游客试听预约" subtitle="访客线索管理与转化">
      <template #actions>
        <el-radio-group v-model="statusFilter" size="default" @change="fetchData">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="待确认">待确认</el-radio-button>
          <el-radio-button value="已确认">已确认</el-radio-button>
          <el-radio-button value="已取消">已取消</el-radio-button>
        </el-radio-group>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <div style="margin-bottom:16px;display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="按姓名/手机号搜索" clearable style="width:240px" @keyup.enter="fetchData" @clear="fetchData">
          <template #append>
            <el-button @click="fetchData">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="bookingId" label="编号" width="60" />
        <el-table-column prop="name" label="访客姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="className" label="试听课程" min-width="140" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="preferredTime" label="期望时间" width="160" />
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <template v-if="row.status==='待确认'">
              <el-button size="small" type="success" @click="handle(row,'已确认')">确认</el-button>
              <el-button size="small" type="danger" @click="handle(row,'已取消')">取消</el-button>
            </template>
            <el-button v-else size="small" text @click="handle(row,'待确认')">重置为待确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getVisitorBookingList, handleVisitorBooking } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const statusFilter = ref('')
const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getVisitorBookingList({
      status: statusFilter.value || undefined,
      keyword: keyword.value || undefined
    })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handle(row, status) {
  await handleVisitorBooking(row.bookingId, status)
  ElMessage.success(status === '已确认' ? '已确认该试听预约' : status === '已取消' ? '已取消该预约' : '已重置')
  fetchData()
}

function statusType(s) {
  if (s === '已确认') return 'success'
  if (s === '已取消') return 'info'
  return 'warning'
}

onMounted(fetchData)
</script>
