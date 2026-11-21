<template>
  <div class="dh-page">
    <PageHeader title="敦煌舞数字展馆" subtitle="Chang-E动捕数据集 · 8大敦煌舞类别 · MediaPipe 33关键点参考骨架" />

    <el-tabs v-model="activeTab" class="dh-tabs">
      <el-tab-pane label="数字展馆" name="gallery">
        <!-- 展馆说明 -->
        <el-card shadow="never" class="mb">
          <div class="intro">
            <p>
              本展馆基于 <b>Chang-E 敦煌舞动捕数据集</b>构建，收录飞天、莲花步、三十六式、参禅、胡旋、反弹琵琶、雷公鼓等
              <b>8 大类敦煌舞标准动作</b>。每个类别提供 <b>MediaPipe 33 关键点</b>参考骨架序列，
              与AI评分系统使用同一套姿态表示——在展馆中观摩的标准骨架，正是评分时教师参考视频被提取后的形态。
            </p>
            <p class="intro-sub">点击左侧类别卡片加载参考骨架动画，可拖拽旋转、滚轮缩放，逐帧观摩敦煌舞标志性姿态。</p>
          </div>
        </el-card>

        <div class="dh-grid">
      <!-- 左列：8大类别卡 -->
      <div class="dh-cats">
        <div v-for="c in categories" :key="c.id" class="cat-card"
          :class="{ active: c.id === activeId, disabled: c.status !== '已加载' }"
          @click="selectCategory(c)">
          <div class="cat-head">
            <b class="cat-name">{{ c.name_cn }}</b>
            <el-tag size="small" :type="c.status === '已加载' ? 'success' : 'info'" effect="plain">
              {{ c.status }}
            </el-tag>
          </div>
          <div class="cat-en">{{ c.name_en }}</div>
          <div class="cat-meta">{{ c.duration }} · {{ c.performer }}</div>
          <div class="cat-feats">
            <el-tag v-for="f in c.key_features" :key="f" size="small" effect="plain" class="feat-tag">{{ f }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 右列：骨架播放器 + 类别详情 -->
      <div class="dh-main">
        <el-card shadow="never">
          <template #header>
            <span class="col-title">{{ activeCat ? `参考骨架 · ${activeCat.name_cn}` : '参考骨架' }}</span>
            <span v-if="refData.metadata && refData.metadata.synthetic" class="syn-tag">合成示例数据</span>
          </template>
          <div v-loading="loadingRef" element-loading-text="正在加载参考骨架…" class="player-box">
            <template v-if="refData.keypoints">
              <DunhuangSkeletonPlayer :frames="refData.keypoints" :fps="refFps" :confidences="refData.confidences" />
            </template>
            <el-empty v-else-if="loadError" description="敦煌参考数据服务暂不可用，请稍后重试" :image-size="80">
              <el-button type="primary" plain @click="loadReference(activeId)">重新加载</el-button>
            </el-empty>
            <el-empty v-else-if="!loadingRef" description="请选择左侧已加载的动作类别" :image-size="80" />
          </div>
        </el-card>

        <!-- 选中类别详情：标志性姿态特征 -->
        <el-card v-if="activeCat" shadow="never" class="mt">
          <template #header><span class="col-title">动作详情 · {{ activeCat.name_cn }}</span></template>
          <div class="detail-grid">
            <div class="detail-item"><span class="d-label">类别</span><span>{{ activeCat.name_cn }}（{{ activeCat.name_en }}）</span></div>
            <div class="detail-item"><span class="d-label">时长</span><span>{{ activeCat.duration }}</span></div>
            <div class="detail-item"><span class="d-label">表演者</span><span>{{ activeCat.performer }}</span></div>
            <div class="detail-item"><span class="d-label">参考帧数</span><span>{{ refData.num_frames || '--' }} 帧 @ {{ refFps }}fps</span></div>
          </div>
          <template v-if="activePose">
            <el-divider />
            <div class="pose-block">
              <div class="pose-head">
                <b>标志性姿态 · {{ activePose.name }}</b>
                <span class="pose-desc">{{ activePose.description }}</span>
              </div>
              <div class="pose-feats">
                <div v-for="(txt, joint) in poseFeatures" :key="joint" class="pose-feat">
                  <span class="pf-joint">{{ jointName(joint) }}</span>
                  <span class="pf-val">{{ txt }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-card>
        </div>
      </div>
      </el-tab-pane>

      <el-tab-pane label="关键姿态挑战" name="challenge">
        <el-card shadow="never" class="mb">
          <div class="intro">
            <p>
              <b>化身敦煌舞者</b>：选择一个标志性姿态，用摄像头抓拍或上传全身照，
              AI 将提取你的 <b>MediaPipe 33 关键点</b>，按关节角度特征逐项比对打分，并在照片上叠加识别骨架。
            </p>
            <p class="intro-sub">评分角度与AI作业评分、展馆参考骨架完全同源——学得像不像，一看便知。</p>
          </div>
        </el-card>
        <DunhuangPoseChallenge />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
/**
 * 敦煌舞数字展馆（学生端）：
 * 左列8大类别卡 → 右列骨架动画播放器 + 类别详情/标志性姿态特征。
 * 数据经Java代理自Python评分服务（只读），服务不可用时友好降级提示。
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DunhuangSkeletonPlayer from '@/components/DunhuangSkeletonPlayer.vue'
import DunhuangPoseChallenge from '@/components/DunhuangPoseChallenge.vue'
import { getDunhuangCategories, getDunhuangReference } from '@/api/dunhuang'

const activeTab = ref('gallery')

/* 类别 → 标志性关键姿态映射（与后端 DUNHUANG_KEY_POSES 对应） */
const CATEGORY_POSE = {
  pipa_behind_back: 'pipa_behind_back',
  flying_apsaras: 'flying_apsaras_pose',
  sogdian_whirl: 'sogdian_whirl_pose',
  lotus_steps: 'lotus_step_pose',
}
/* 关节特征key → 中文名 */
const JOINT_CN = {
  left_elbow: '左肘关节', right_elbow: '右肘关节',
  left_shoulder: '左肩关节', right_shoulder: '右肩关节',
  left_knee: '左膝关节', right_knee: '右膝关节',
  trunk_inclination: '躯干倾角', neck: '颈部',
  left_arm_horizontal: '左臂水平伸展',
}

const categories = ref([])
const keyPoses = ref({})
const activeId = ref('')
const loadingRef = ref(false)
const loadError = ref(false)
const refData = ref({})

const activeCat = computed(() => categories.value.find(c => c.id === activeId.value) || null)
const activePose = computed(() => {
  const pid = CATEGORY_POSE[activeId.value]
  return pid ? keyPoses.value[pid] : null
})
const refFps = computed(() => refData.value.metadata?.fps || 30)
/* 姿态特征可读化：[min,max] → 角度区间；true → 特征文字本身 */
const poseFeatures = computed(() => {
  if (!activePose.value) return {}
  const out = {}
  for (const [joint, val] of Object.entries(activePose.value.angle_features || {})) {
    out[joint] = Array.isArray(val) ? `角度 ${val[0]}° – ${val[1]}°` : '保持该姿态特征'
  }
  return out
})
const jointName = (k) => JOINT_CN[k] || k

const selectCategory = (c) => {
  if (c.status !== '已加载' || c.id === activeId.value) return
  activeId.value = c.id
  loadReference(c.id)
}

const loadReference = async (category) => {
  if (!category) return
  loadingRef.value = true
  loadError.value = false
  refData.value = {}
  try {
    const res = await getDunhuangReference(category)
    refData.value = res.data || {}
  } catch (e) {
    loadError.value = true
  } finally {
    loadingRef.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getDunhuangCategories()
    categories.value = res.data?.categories || []
    keyPoses.value = res.data?.key_poses || {}
    // 默认选中第一个已加载类别并自动加载骨架
    const first = categories.value.find(c => c.status === '已加载')
    if (first) {
      activeId.value = first.id
      loadReference(first.id)
    }
  } catch (e) {
    ElMessage.error('敦煌展馆数据加载失败：参考数据服务暂不可用')
  }
})
</script>

<style scoped>
.mb { margin-bottom: 16px; }
.mt { margin-top: 16px; }
.intro p { margin: 0 0 6px; font-size: 13px; line-height: 1.8; color: #444; }
.intro-sub { color: #8c959f !important; font-size: 12px !important; }
.dh-grid { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 16px; align-items: start; }
@media (max-width: 992px) { .dh-grid { grid-template-columns: 1fr; } }
.dh-cats { display: flex; flex-direction: column; gap: 10px; }
.cat-card { background: #fff; border: 1px solid #e4d9c3; border-radius: 8px; padding: 12px 14px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s; }
.cat-card:hover { border-color: #8250df; }
.cat-card.active { border-color: #8250df; box-shadow: 0 0 0 2px rgba(130, 80, 223, .15); }
.cat-card.disabled { opacity: .55; cursor: not-allowed; }
.cat-card.disabled:hover { border-color: #e4d9c3; }
.cat-head { display: flex; align-items: center; justify-content: space-between; }
.cat-name { font-size: 14px; }
.cat-en { font-size: 12px; color: #8c959f; margin-top: 2px; }
.cat-meta { font-size: 12px; color: #57606a; margin-top: 6px; }
.cat-feats { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.feat-tag { font-size: 11px; }
.col-title { font-weight: 600; font-size: 14px; }
.syn-tag { margin-left: 10px; font-size: 12px; color: #bf8700; background: #fff8e6;
  border: 1px solid #f0d9a8; border-radius: 4px; padding: 1px 8px; }
.player-box { min-height: 320px; }
.detail-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px 20px; }
.detail-item { font-size: 13px; display: flex; gap: 8px; }
.d-label { color: #8c959f; flex-shrink: 0; }
.pose-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.pose-desc { font-size: 12px; color: #57606a; }
.pose-feats { margin-top: 12px; display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 8px; }
.pose-feat { display: flex; align-items: center; gap: 8px; font-size: 12px;
  background: #f6f8fa; border: 1px solid #eaeef2; border-radius: 6px; padding: 8px 10px; }
.pf-joint { color: #8250df; font-weight: 600; flex-shrink: 0; }
.pf-val { color: #444; }
</style>
