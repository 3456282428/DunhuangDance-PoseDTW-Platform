<template>
  <el-popover placement="bottom-end" :width="360" trigger="click" @show="loadList">
    <template #reference>
      <el-badge :value="unread" :hidden="unread === 0" :max="99" class="bell-badge">
        <el-button :icon="Bell" circle size="small" />
      </el-badge>
    </template>
    <div class="msg-panel">
      <div class="msg-head">
        <span>站内消息</span>
        <el-button link type="primary" size="small" @click="readAll">全部已读</el-button>
      </div>
      <el-empty v-if="list.length === 0" description="暂无消息" :image-size="60" />
      <el-scrollbar v-else max-height="320px">
        <div
          v-for="m in list"
          :key="m.messageId"
          class="msg-item"
          :class="{ unread: m.isRead === 0 }"
          @click="readOne(m)"
        >
          <div class="msg-title">
            <el-tag size="small" effect="plain">{{ m.msgType }}</el-tag>
            <span>{{ m.title }}</span>
          </div>
          <div class="msg-content">{{ m.content }}</div>
          <div class="msg-time">{{ formatTime(m.createTime) }}</div>
        </div>
      </el-scrollbar>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMessageList, getUnreadCount, markMessageRead, markAllMessageRead } from '@/api/message'

const unread = ref(0)
const list = ref([])
let timer = null

const loadUnread = async () => {
  try { unread.value = (await getUnreadCount()).data ?? 0 } catch (e) { /* 未登录时忽略 */ }
}
const loadList = async () => {
  try { list.value = (await getMessageList()).data ?? [] } catch (e) { list.value = [] }
}
const readOne = async (m) => {
  if (m.isRead === 0) {
    await markMessageRead(m.messageId)
    m.isRead = 1
    loadUnread()
  }
}
const readAll = async () => {
  await markAllMessageRead()
  list.value.forEach(m => (m.isRead = 1))
  ElMessage.success('已全部标为已读')
  loadUnread()
}
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

onMounted(() => {
  loadUnread()
  timer = setInterval(loadUnread, 30000)
})
onBeforeUnmount(() => timer && clearInterval(timer))
</script>

<style scoped>
.bell-badge { margin-right: 12px; }
.msg-head {
  display: flex; justify-content: space-between; align-items: center;
  font-weight: 600; margin-bottom: 8px;
}
.msg-item { padding: 8px 6px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.msg-item.unread { background-color: #f5f9ff; }
.msg-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; }
.msg-content { font-size: 12px; color: #666; margin: 4px 0; line-height: 1.5; }
.msg-time { font-size: 11px; color: #999; }
</style>
