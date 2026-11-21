<template>
  <div>
    <PageHeader title="我的教学知识库" subtitle="布置作业与批改点评自动归集 · 教学资料手动维护 · AI问答优先依据">
      <template #actions>
        <el-button plain @click="tryAi">AI知识问答</el-button>
      </template>
    </PageHeader>

    <el-alert type="success" :closable="false" style="margin-bottom:14px">
      <template #title>
        知识库自动增长：您<b>布置的作业</b>与<b>批改点评</b>会自动归集为本端知识条目（自动归集条目仅可查看），
        也可手动维护教案课件与教学方法资料；AI问答将优先检索本知识库数据生成回答。
      </template>
    </el-alert>

    <el-card shadow="never">
      <KbDocPanel scope="TEACHER" manage />
    </el-card>

    <el-dialog v-model="aiVisible" title="AI 知识问答" width="620px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="回答由 AI 基于知识库检索内容生成，仅供参考" />
      <el-input v-model="aiQuestion" type="textarea" :rows="3" placeholder="输入问题，如：这学期布置过哪些舞蹈作业？" maxlength="1000" show-word-limit />
      <el-button type="primary" style="margin-top:10px" :loading="aiLoading" @click="askAi">提交问答</el-button>
      <div v-if="aiResult" style="margin-top:14px">
        <template v-if="aiResult.answer">
          <el-divider content-position="left">AI 回答</el-divider>
          <div class="ai-answer">{{ aiResult.answer }}</div>
        </template>
        <el-alert v-else-if="aiResult.status === 'FAILED'" type="error" :closable="false" :title="aiResult.message" style="margin-bottom:12px" />
        <el-divider content-position="left">知识库检索命中（{{ aiResult.hitCount }} 条，回答依据）</el-divider>
        <div v-for="k in aiResult.retrievedKnowledge || []" :key="k.kbId" class="hit-item">
          <el-tag size="small" effect="plain">{{ k.category }}</el-tag>
          <b>{{ k.title }}</b>
          <div class="hit-summary">{{ k.summary }}</div>
        </div>
        <el-empty v-if="!aiResult.hitCount" description="未命中知识库条目" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import KbDocPanel from '@/components/KbDocPanel.vue'
import { aiInvoke } from '@/api/knowledge'

const aiVisible = ref(false)
const aiQuestion = ref('')
const aiLoading = ref(false)
const aiResult = ref(null)

const tryAi = () => {
  aiResult.value = null
  aiQuestion.value = ''
  aiVisible.value = true
}
const askAi = async () => {
  if (!aiQuestion.value.trim()) { ElMessage.warning('请输入问题'); return }
  aiLoading.value = true
  try {
    const res = await aiInvoke({ scope: 'TEACHER', question: aiQuestion.value, topK: 5 })
    aiResult.value = res.data
    ElMessage.info(res.data?.message || '已提交')
  } finally { aiLoading.value = false }
}
</script>

<style scoped>
.ai-answer { white-space: pre-wrap; line-height: 1.7; font-size: 13px; color: #24292f; background: #f6f8fa; border: 1px solid #eaeef2; border-radius: 6px; padding: 10px 12px; }
.hit-item { padding: 8px 0; border-bottom: 1px dashed #e4d9c3; }
.hit-summary { color: #6b5a4a; font-size: 12px; margin-top: 2px; }
</style>
