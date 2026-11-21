<template>
  <div>
    <PageHeader title="互动反馈" subtitle="提交建议、投诉或咨询" />
    <el-form label-width="80px" style="max-width:600px" @submit.prevent="submit">
      <el-form-item label="类型">
        <el-select v-model="form.type" placeholder="请选择">
          <el-option label="建议" value="建议" />
          <el-option label="投诉" value="投诉" />
          <el-option label="咨询" value="咨询" />
        </el-select>
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="form.content" type="textarea" rows="4" />
      </el-form-item>
      <el-button type="primary" native-type="submit">提交</el-button>
    </el-form>

    <el-divider />

    <h4>我的反馈记录</h4>
    <el-table :data="feedbackList" stripe>
      <el-table-column prop="feedbackId" label="编号" width="60" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="replyContent" label="回复" />
    </el-table>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { submitFeedback, getMyFeedback } from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const form = reactive({ type: '建议', content: '' })
const feedbackList = ref([])

const fetchData = async () => {
  const res = await getMyFeedback()
  feedbackList.value = res.data || []
}

const submit = async () => {
  if (!form.content) {
    ElMessage.warning('请输入内容')
    return
  }
  await submitFeedback(form)
  ElMessage.success('提交成功')
  form.content = ''
  fetchData()
}

onMounted(fetchData)
</script>
