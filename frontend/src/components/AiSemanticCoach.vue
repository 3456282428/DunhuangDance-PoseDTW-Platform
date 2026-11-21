<template>
  <div class="semantic-root">
    <!-- 加载中 -->
    <div v-if="loading" class="semantic-loading">
      <el-icon class="is-loading" :size="22"><Loading /></el-icon>
      <span>豆包教练正在观看教学视频并生成点评，约需10~30秒…</span>
    </div>

    <!-- 未分析：触发入口 -->
    <div v-else-if="!semantic || !semantic.available" class="semantic-empty">
      <p class="empty-tip">
        基于通道一量化评分与视频关键帧，由豆包多模态模型生成
        <b>动作解析 · 发力点标注 · 视听一致性判断 · 改进建议</b>
      </p>
      <el-button type="primary" :disabled="!canTrigger" @click="analyze(false)">生成AI教练点评</el-button>
    </div>

    <!-- 已分析：点评内容 -->
    <div v-else class="semantic-body">
      <div class="meta-row">
        <el-tag size="small" type="success" effect="plain">AI教练点评</el-tag>
        <span class="meta">{{ semantic.model }}</span>
        <span class="meta">生成于 {{ semantic.analyzedAt }}</span>
        <el-button link type="primary" size="small" class="refresh-btn" @click="analyze(true)">重新生成</el-button>
      </div>

      <!-- 降级：原始文本 -->
      <p v-if="semantic.rawText" class="raw-text">{{ semantic.rawText }}</p>

      <template v-else>
        <div class="sec">
          <h4 class="sec-title">教师示范解析</h4>
          <p class="sec-text">{{ semantic.teacherAnalysis }}</p>
        </div>
        <div class="sec">
          <h4 class="sec-title">学生动作解析</h4>
          <p class="sec-text">{{ semantic.studentAnalysis }}</p>
        </div>
        <div class="sec" v-if="semantic.rhythmConsistency">
          <h4 class="sec-title">节奏视听一致性判断</h4>
          <p class="sec-text">{{ semantic.rhythmConsistency }}</p>
        </div>
        <div class="sec" v-if="suggestions.length">
          <h4 class="sec-title">改进建议</h4>
          <div class="sug-grid">
            <div v-for="(s, i) in suggestions" :key="i" class="sug-card">
              <span class="sug-idx">{{ i + 1 }}</span>
              <div>
                <b class="sug-title">{{ s.title }}</b>
                <p class="sug-detail">{{ s.detail }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { triggerAiSemantics as teacherTrigger } from '@/api/teacher'
import { triggerAiSemantics as studentTrigger } from '@/api/student'

const props = defineProps({
  semantic: { type: Object, default: null },
  // 教师端为submissionId，学生端为homeworkId（与ai-score端点口径一致）
  targetId: { type: [String, Number], required: true },
  isStudent: { type: Boolean, default: false },
  // 自动分析：挂载时无已有点评则自动触发（提交/评分完成后打开报告页即自动生成，无需手动点击）
  auto: { type: Boolean, default: true }
})
const emit = defineEmits(['analyzed'])

const loading = ref(false)

const canTrigger = computed(() => props.targetId != null && props.targetId !== '')
const suggestions = computed(() =>
  Array.isArray(props.semantic?.suggestions) ? props.semantic.suggestions : [])

const analyze = async (force) => {
  loading.value = true
  try {
    const fn = props.isStudent ? studentTrigger : teacherTrigger
    const res = await fn(props.targetId, force)
    const data = res?.data ?? res
    if (data?.status === 'SUCCESS') {
      ElMessage.success(force ? 'AI教练点评已更新' : 'AI教练点评已生成')
      emit('analyzed', data.semantic)
    } else {
      ElMessage.error(data?.message || '生成失败，请稍后重试')
    }
  } catch (e) {
    ElMessage.error(e?.message || '生成失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 已有结果时默认不自动重算；挂载时无点评且允许自动分析则自动触发（服务端有缓存，仅首次生成时真正调用）
onMounted(() => {
  if (props.auto && !props.semantic?.available && canTrigger.value) {
    analyze(false)
  }
})
</script>

<style scoped>
.semantic-root { width: 100%; }
.semantic-loading { display: flex; align-items: center; gap: 10px; justify-content: center; padding: 26px 0; color: #6b5a4a; font-size: 13px; }
.semantic-empty { text-align: center; padding: 18px 0 8px; }
.empty-tip { margin: 0 0 12px; font-size: 13px; color: #6b5a4a; }
.empty-tip b { color: #1685a9; }

.meta-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.meta { font-size: 12px; color: #6b5a4a; }
.refresh-btn { margin-left: auto; }

.sec { margin-bottom: 12px; }
.sec-title { margin: 0 0 4px; font-size: 13px; font-weight: 600; color: #2c1810; }
.sec-text { margin: 0; font-size: 13px; line-height: 1.7; color: #6b5a4a; }
.raw-text { margin: 0; font-size: 13px; line-height: 1.7; color: #6b5a4a; white-space: pre-wrap; }

.sug-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.sug-card { display: flex; gap: 8px; border: 1px solid #e4d9c3; border-radius: 8px; padding: 10px 12px; background: #fafbfc; }
.sug-idx { flex: 0 0 auto; width: 20px; height: 20px; border-radius: 50%; background: #1685a9; color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; }
.sug-title { font-size: 13px; color: #2c1810; display: block; }
.sug-detail { margin: 3px 0 0; font-size: 12px; line-height: 1.6; color: #6b5a4a; }

@media (max-width: 900px) { .sug-grid { grid-template-columns: 1fr; } }
</style>
