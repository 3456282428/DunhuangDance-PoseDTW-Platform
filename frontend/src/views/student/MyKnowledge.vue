<template>
  <div>
    <PageHeader title="我的学习知识库" subtitle="学习资料浏览 · 作业成果自动归集 · AI问答优先依据" />

    <el-alert type="success" :closable="false" style="margin-bottom:14px">
      <template #title>
        这里汇集了机构共享的学习资料与您<b>提交作业自动归集的学习成果</b>；后续AI学习助手回答将优先检索这些知识数据。
      </template>
    </el-alert>

    <el-card shadow="never">
      <KbDocPanel scope="STUDENT" :manage="false" />
    </el-card>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue'
import KbDocPanel from '@/components/KbDocPanel.vue'
</script>
