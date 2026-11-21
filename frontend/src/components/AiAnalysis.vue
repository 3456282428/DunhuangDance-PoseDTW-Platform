<template>
  <div v-if="a" class="ai-analysis">
    <!-- 总体评价 -->
    <div class="aa-head">
      <el-tag :type="levelType(a.level)" effect="dark" size="large" round>{{ a.level }}</el-tag>
      <p class="aa-summary">{{ a.summary }}</p>
    </div>

    <!-- 优点 / 不足 双栏 -->
    <div class="aa-grid">
      <div class="aa-col">
        <p class="aa-col-title good">做得好的地方</p>
        <template v-if="a.strengths && a.strengths.length">
          <div v-for="(s, i) in a.strengths" :key="'g'+i" class="aa-item">
            <span class="aa-icon good">✓</span>
            <div>
              <div class="aa-line">
                <b>{{ s.name }}</b>
                <el-tag size="small" type="success">{{ s.score }}分</el-tag>
                <el-tag size="small" effect="plain">{{ s.type }}</el-tag>
              </div>
              <p class="aa-comment">{{ s.comment }}</p>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无明显优势项，继续练习后可形成稳定优势" :image-size="48" />
      </div>

      <div class="aa-col">
        <p class="aa-col-title bad">需要改进的地方</p>
        <template v-if="a.weaknesses && a.weaknesses.length">
          <div v-for="(w, i) in a.weaknesses" :key="'w'+i" class="aa-item">
            <span class="aa-icon bad">!</span>
            <div>
              <div class="aa-line">
                <b>{{ w.name }}</b>
                <el-tag size="small" :type="w.score >= 75 ? 'warning' : 'danger'">{{ w.score }}分</el-tag>
                <el-tag size="small" effect="plain">{{ w.type }}</el-tag>
                <span v-if="w.avgDeg != null" class="aa-avg">平均偏差约{{ w.avgDeg }}°</span>
              </div>
              <p class="aa-comment">{{ w.comment }}</p>
              <div v-if="w.timePoints && w.timePoints.length" class="aa-times">
                <span class="aa-times-label">重点回看</span>
                <el-button v-for="(t, k) in w.timePoints" :key="k" size="small" text type="primary"
                  class="aa-chip" @click="$emit('seek', t)">▶ {{ formatTime(t) }}</el-button>
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else description="各维度均达到80分以上，暂无突出短板" :image-size="48" />
      </div>
    </div>

    <!-- 训练建议 -->
    <div class="aa-suggest">
      <p class="aa-col-title">训练建议</p>
      <div v-for="(sg, i) in a.suggestions" :key="'s'+i" class="sg-item">
        <span class="sg-no">{{ i + 1 }}</span>
        <div>
          <el-tag size="small" type="warning" effect="plain" class="sg-target">{{ sg.target }}</el-tag>
          <span class="sg-action">{{ sg.action }}</span>
          <div v-if="sg.timePoints && sg.timePoints.length" class="aa-times">
            <span class="aa-times-label">对应时间</span>
            <el-button v-for="(t, k) in sg.timePoints" :key="k" size="small" text type="primary"
              class="aa-chip" @click="$emit('seek', t)">▶ {{ formatTime(t) }}</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <el-empty v-else description="暂无综合分析数据，重新评分后生成" :image-size="60" />
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ analysis: { type: Object, default: null } })
defineEmits(['seek'])
const a = computed(() => props.analysis)

const levelType = (lv) => ({
  '优秀': 'success', '良好': 'primary', '合格': 'info',
  '待提高': 'warning', '需重点练习': 'danger'
}[lv] || 'info')

const formatTime = (sec) => {
  if (sec == null || isNaN(sec)) return '--:--'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
</script>

<style scoped>
.ai-analysis { display: flex; flex-direction: column; gap: 14px; }
.aa-head { display: flex; align-items: center; gap: 14px; }
.aa-summary { margin: 0; font-size: 14px; color: #2c1810; line-height: 1.7; }
.aa-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.aa-col { background: #fafbfc; border-radius: 8px; padding: 12px 14px; }
.aa-col-title { margin: 0 0 10px; font-weight: 600; font-size: 14px; }
.aa-col-title.good { color: #2c9678; }
.aa-col-title.bad { color: #c13a2a; }
.aa-item { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px dashed #e4d9c3; }
.aa-item:last-child { border-bottom: none; }
.aa-icon { flex: none; width: 20px; height: 20px; border-radius: 50%; color: #fff; font-size: 12px;
  display: flex; align-items: center; justify-content: center; margin-top: 2px; font-weight: 700; }
.aa-icon.good { background: #2c9678; }
.aa-icon.bad { background: #c13a2a; }
.aa-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 13px; }
.aa-comment { margin: 4px 0 0; font-size: 13px; color: #6b5a4a; line-height: 1.6; }
.aa-avg { font-size: 12px; color: #6b5a4a; }
.aa-times { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; margin-top: 4px; }
.aa-times-label { font-size: 12px; color: #6b5a4a; }
.aa-chip { padding: 2px 6px; font-size: 12px; height: auto; }
.aa-suggest { background: #fffbeb; border: 1px solid #faecd8; border-radius: 8px; padding: 12px 14px; }
.sg-item { display: flex; gap: 10px; padding: 7px 0; }
.sg-no { flex: none; width: 20px; height: 20px; border-radius: 4px; background: #c89a2e; color: #fff;
  font-size: 12px; display: flex; align-items: center; justify-content: center; font-weight: 700; margin-top: 1px; }
.sg-target { margin-right: 8px; }
.sg-action { font-size: 13px; color: #2c1810; line-height: 1.7; }
@media (max-width: 1100px) { .aa-grid { grid-template-columns: 1fr; } }
</style>
