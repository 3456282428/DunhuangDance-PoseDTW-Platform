<template>
  <div>
    <PageHeader title="反馈回复处理" subtitle="学员/家长反馈" />
    <el-table :data="feedbackList" stripe>
      <el-table-column prop="feedbackId" label="编号" width="60" />
      <el-table-column prop="studentName" label="学员" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" @click="reply(row)">回复</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTeacherFeedbackList, replyFeedback } from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const feedbackList = ref([])

const fetchData = async () => {
  const res = await getTeacherFeedbackList({})
  feedbackList.value = res.data || []
}

const reply = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入回复内容', '回复', { type: 'info' })
    await replyFeedback(row.feedbackId, value)
    ElMessage.success('回复成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
