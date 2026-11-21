<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题/内容/标签" clearable style="width:220px" @keyup.enter="fetchData" />
      <el-select v-model="query.category" placeholder="全部分类" clearable style="width:150px" @change="fetchData">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-if="manage" v-model="query.status" placeholder="全部状态" clearable style="width:120px" @change="fetchData">
        <el-option label="已启用" :value="1" />
        <el-option label="已停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button v-if="manage" type="success" @click="openCreate">新增条目</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading" border>
      <el-table-column prop="kbId" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" @click="viewDoc(row)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceType" label="来源" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="sourceMeta(row.sourceType).type">{{ sourceMeta(row.sourceType).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ownerName" label="归属人" width="100">
        <template #default="{ row }">{{ row.ownerName || '机构共享' }}</template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
      <el-table-column prop="viewCount" label="浏览" width="70" />
      <el-table-column prop="createTime" label="创建时间" width="165">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column v-if="manage" label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="isAuto(row)">
            <el-tooltip content="业务系统自动归集条目，仅支持查看" placement="top">
              <el-tag size="small" type="info" effect="plain">自动归集</el-tag>
            </el-tooltip>
          </template>
          <template v-else>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top:14px;justify-content:flex-end"
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <!-- 查看 -->
    <el-dialog v-model="viewVisible" :title="current?.title" width="680px">
      <template v-if="current">
        <div class="meta-row">
          <el-tag size="small" effect="plain">{{ current.category }}</el-tag>
          <el-tag size="small" :type="sourceMeta(current.sourceType).type">{{ sourceMeta(current.sourceType).label }}</el-tag>
          <span class="meta-text">归属：{{ current.ownerName || '机构共享' }}</span>
          <span class="meta-text">浏览：{{ current.viewCount }}</span>
          <span class="meta-text">{{ (current.createTime || '').replace('T', ' ').slice(0, 16) }}</span>
        </div>
        <div v-if="current.tags" class="tag-row">
          <el-tag v-for="t in current.tags.split(',')" :key="t" size="small" type="info" effect="plain">{{ t }}</el-tag>
        </div>
        <el-divider style="margin:10px 0" />
        <div class="doc-content">{{ current.content || current.summary || '（无正文）' }}</div>
        <div v-if="current.fileUrl" style="margin-top:10px">
          附件：<el-link type="primary" :href="current.fileUrl" target="_blank">{{ current.fileName || current.fileUrl }}</el-link>
        </div>

        <!-- 跨知识库关联内容联动查询（关联类型/维度/强度可追溯） -->
        <template v-if="canRelate">
          <el-divider style="margin:14px 0 10px" />
          <div class="rel-head">
            <span class="rel-title">关联内容（{{ related.length }}）</span>
            <el-button size="small" plain :disabled="!current.kbId" @click="openGraph">关联图谱</el-button>
          </div>
          <div v-loading="relatedLoading" class="rel-list">
            <div
              v-for="r in related"
              :key="r.scope + '-' + r.kbId"
              class="rel-item"
              @click="jumpRelated(r)"
            >
              <el-tag size="small" effect="plain" type="warning">{{ r.relText }}</el-tag>
              <span class="rel-name">{{ r.title }}</span>
              <span v-if="r.dimensionText" class="rel-dim">{{ r.dimensionText }}</span>
              <span v-if="r.strength != null" class="rel-str">{{ Math.round(r.strength * 100) }}%</span>
              <span class="rel-ref">{{ r.label }}</span>
            </div>
            <el-empty v-if="!related.length && !relatedLoading" description="暂无关联内容" :image-size="46" />
          </div>
        </template>

        <!-- 条目适配3D模型推荐（变量：随知识库内容自动更新） -->
        <template v-if="canRelate">
          <el-divider style="margin:14px 0 10px" />
          <div class="rel-head">
            <span class="rel-title">适配3D模型推荐</span>
            <span class="dyn-badge">随知识库内容自动更新</span>
          </div>
          <div v-loading="modelLoading" class="model-card">
            <template v-if="model3d && model3d.matched">
              <div class="model-stage">
                <div class="model-cube">
                  <div class="cube-face f-front" />
                  <div class="cube-face f-back" />
                  <div class="cube-face f-right" />
                  <div class="cube-face f-left" />
                  <div class="cube-face f-top" />
                  <div class="cube-face f-bottom" />
                </div>
              </div>
              <div class="model-info">
                <div class="model-name">
                  {{ model3d.model.modelName }}
                  <el-tag size="small" effect="plain" type="success">{{ model3d.model.categoryText }}</el-tag>
                </div>
                <div class="model-rule">匹配规则：{{ model3d.matchRule }}</div>
                <div class="model-meta">
                  <span>匹配分 <b>{{ Math.round(model3d.matchScore * 100) }}%</b></span>
                  <span v-if="model3d.matchTags">命中标签：{{ model3d.matchTags }}</span>
                  <span>格式 {{ model3d.model.format }}</span>
                </div>
                <div class="model-desc">{{ model3d.model.description }}</div>
                <div class="model-note">{{ model3d.note }}</div>
              </div>
            </template>
            <el-empty v-else-if="!modelLoading" description="暂无匹配的3D模型" :image-size="46" />
          </div>
        </template>
      </template>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 关联关系图谱（可视化） -->
    <el-dialog v-model="graphVisible" title="关联关系图谱" width="760px" @opened="renderGraph" @closed="disposeGraph">
      <div v-loading="graphLoading" ref="graphRef" class="graph-canvas" />
      <template #footer>
        <el-button @click="graphVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑 -->
    <el-dialog v-model="editVisible" :title="form.kbId ? '编辑知识条目' : '新增知识条目'" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="知识条目标题" maxlength="200" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：制度,运营" />
        </el-form-item>
        <el-form-item label="正文内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="知识正文（AI检索主体）" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload :show-file-list="false" :http-request="doUpload" accept=".pdf,.doc,.docx,.txt,.md,.jpg,.png,.mp4">
            <el-button size="small">上传附件</el-button>
          </el-upload>
          <div v-if="form.fileUrl" class="file-chip">
            {{ form.fileName }}
            <el-button link type="danger" size="small" @click="form.fileUrl = ''; form.fileName = ''">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用（AI可检索）" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { uploadFiles } from '@/api/upload'
import {
  getKbPage, getKbDoc, createKbDoc, updateKbDoc, deleteKbDoc,
  getTeacherKbPage, createTeacherKbDoc, updateTeacherKbDoc, deleteTeacherKbDoc,
  getStudentKbPage,
  getKbRelated, getKbGraph, getKbModel3d
} from '@/api/knowledge'

const props = defineProps({
  scope: { type: String, required: true }, // ADMIN / TEACHER / STUDENT
  manage: { type: Boolean, default: false },
  base: { type: String, default: '' } // 调用端：admin=管理员统一管控接口；空=按scope自动匹配
})

const CATEGORY_MAP = {
  ADMIN: ['综合', '制度规范', '公告通知', '运营经验', '日报快照'],
  TEACHER: ['教学资料', '教案课件', '教学方法', '作业布置', '批改点评'],
  STUDENT: ['学习资料', '练习指导', '作业成果']
}

// base=admin 时走管理员统一管控接口（可跨端管理），否则按 scope 走对应角色自己的接口
const effectiveBase = props.base || (props.scope === 'ADMIN' ? 'admin' : props.scope.toLowerCase())
const api = effectiveBase === 'admin'
  ? {
      page: p => getKbPage(props.scope, p),
      create: d => createKbDoc(props.scope, d),
      update: (id, d) => updateKbDoc(props.scope, id, d),
      del: id => deleteKbDoc(props.scope, id)
    }
  : effectiveBase === 'teacher'
    ? { page: getTeacherKbPage, create: createTeacherKbDoc, update: updateTeacherKbDoc, del: deleteTeacherKbDoc }
    : { page: getStudentKbPage, create: null, update: null, del: null }

const categories = CATEGORY_MAP[props.scope] || ['综合']

const list = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const query = reactive({ keyword: '', category: '', status: null })

const sourceMeta = (t) => ({
  MANUAL: { label: '手动维护', type: 'success' },
  AUTO_ASSIGN: { label: '布置作业', type: 'warning' },
  AUTO_GRADE: { label: '批改点评', type: 'warning' },
  AUTO_SUBMIT: { label: '作业提交', type: 'warning' },
  AUTO_SCHEDULE: { label: '定时归集', type: 'warning' },
  AUTO_SYNC: { label: '全量同步', type: 'warning' }
}[t] || { label: t || '手动', type: 'info' })

const isAuto = (row) => row.sourceType && row.sourceType.startsWith('AUTO')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.page({
      pageNum: pageNum.value, pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      status: props.manage ? (query.status ?? undefined) : undefined
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

// ---- 查看 ----
const viewVisible = ref(false)
const current = ref(null)
const canRelate = computed(() => effectiveBase === 'admin')

const related = ref([])
const relatedLoading = ref(false)
const loadRelated = async (kbId) => {
  relatedLoading.value = true
  try {
    const res = await getKbRelated(props.scope, kbId)
    related.value = res.data || []
  } catch { related.value = [] } finally { relatedLoading.value = false }
}

// ---- 条目适配3D模型推荐（随知识库内容自动更新） ----
const model3d = ref(null)
const modelLoading = ref(false)
const loadModel3d = async (kbId) => {
  modelLoading.value = true
  try {
    const res = await getKbModel3d(props.scope, kbId)
    model3d.value = res.data || null
  } catch { model3d.value = null } finally { modelLoading.value = false }
}

const viewDoc = (row) => {
  current.value = row
  viewVisible.value = true
  related.value = []
  model3d.value = null
  if (canRelate.value) {
    loadRelated(row.kbId)
    loadModel3d(row.kbId)
  }
}

/** 联动查询：点击关联条目 → 直接查看该条目并刷新其关联与模型推荐 */
const jumpRelated = async (r) => {
  if (!r.kbId) return
  try {
    const res = await getKbDoc(props.scope, r.kbId)
    current.value = res.data || current.value
    model3d.value = null
    loadRelated(r.kbId)
    loadModel3d(r.kbId)
  } catch { /* ignore */ }
}

// ---- 关联关系图谱（可视化） ----
const graphVisible = ref(false)
const graphLoading = ref(false)
const graphRef = ref(null)
let graphInst = null

/** 打开图谱弹窗：实际渲染交给 el-dialog 的 @opened 回调（弹窗内容是懒挂载的，仅有 ref 还不存在） */
const openGraph = () => {
  if (!current.value?.kbId) return
  graphVisible.value = true
}

const renderGraph = async () => {
  if (!current.value?.kbId || !graphRef.value) return
  graphLoading.value = true
  graphInst = echarts.getInstanceByDom(graphRef.value) || echarts.init(graphRef.value)
  graphInst.setOption({ series: [] }, true)
  try {
    const res = await getKbGraph(props.scope, current.value.kbId)
    graphInst.setOption(buildGraphOption(res.data || {}), true)
  } catch (e) {
    console.warn('[知识库] 关联图谱数据加载失败', e)
  } finally {
    graphLoading.value = false
  }
}

const buildGraphOption = (g) => {
  const nodes = (g.nodes || []).map((n) => ({
    id: n.id,
    name: n.title || n.sourceRef || '节点',
    category: n.center ? 0 : (n.kbId ? 1 : 2),
    symbolSize: n.center ? 56 : 40,
    label: { show: true, fontSize: 11 }
  }))
  const links = (g.edges || []).map((e) => ({
    source: e.source,
    target: e.target,
    value: e.label,
    label: { show: true, formatter: e.label, fontSize: 10, color: '#8c8c8c' },
    lineStyle: { color: '#b6c2cf', curveness: 0.08, width: 1.4 }
  }))
  return {
    tooltip: {
      formatter: (p) => (p.dataType === 'edge' ? p.data.value : p.data.name)
    },
    legend: [{
      data: ['当前条目', '关联条目', '关联业务实体'],
      bottom: 0, icon: 'circle', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 }
    }],
    color: ['#1f6feb', '#2da44e', '#bf8700'],
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      focusNodeAdjacency: true,
      force: { repulsion: 900, edgeLength: [90, 160], gravity: 0.08 },
      categories: [{ name: '当前条目' }, { name: '关联条目' }, { name: '关联业务实体' }],
      label: { position: 'bottom', color: '#24292f' },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 6,
      emphasis: { focus: 'adjacency' },
      data: nodes,
      links
    }]
  }
}

const disposeGraph = () => {
  if (graphInst) { graphInst.dispose(); graphInst = null }
}

// ---- 新增/编辑 ----
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ kbId: null, title: '', category: categories[0], tags: '', content: '', fileUrl: '', fileName: '', status: 1 })
const form = ref(emptyForm())
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const openCreate = () => {
  form.value = emptyForm()
  editVisible.value = true
}
const openEdit = (row) => {
  form.value = { ...row }
  editVisible.value = true
}

const doUpload = async ({ file }) => {
  const res = await uploadFiles([file])
  const item = (res.data || [])[0]
  if (item) { form.value.fileUrl = item.url; form.value.fileName = item.name }
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.kbId) await api.update(form.value.kbId, form.value)
    else await api.create(form.value)
    ElMessage.success('保存成功')
    editVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除知识条目「${row.title}」？`, '提示', { type: 'warning' })
  await api.del(row.kbId)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
onBeforeUnmount(disposeGraph)
defineExpose({ fetchData })
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.meta-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.meta-text { color: #6b5a4a; font-size: 12px; }
.tag-row { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.doc-content { white-space: pre-wrap; line-height: 1.8; max-height: 420px; overflow: auto; }
.file-chip { margin-top: 6px; font-size: 12px; color: #6b5a4a; }

.rel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.rel-title { font-size: 13px; font-weight: 600; color: #24292f; }
.rel-list { min-height: 40px; }
.rel-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: 6px; cursor: pointer;
  border: 1px solid #eaeef2;
  margin-bottom: 4px;
}
.rel-item:hover { background: #f6f8fa; border-color: #d0d7de; }
.rel-name { flex: 1; min-width: 0; font-size: 13px; color: #0969da; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rel-dim { font-size: 12px; color: #6f42c1; flex-shrink: 0; }
.rel-str { font-size: 12px; color: #2da44e; font-weight: 600; flex-shrink: 0; }
.rel-ref { font-size: 12px; color: #8c8c8c; flex-shrink: 0; }

/* ---- 3D模型推荐卡片 ---- */
.dyn-badge {
  font-size: 12px; color: #1f6feb; background: rgba(31,111,235,.08);
  border: 1px solid rgba(31,111,235,.25); padding: 2px 10px; border-radius: 10px;
}
.model-card {
  display: flex; gap: 16px; min-height: 120px; align-items: center;
  border: 1px solid #eaeef2; border-radius: 8px; padding: 14px;
}
.model-stage { width: 90px; height: 90px; perspective: 260px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.model-cube { width: 56px; height: 56px; position: relative; transform-style: preserve-3d; animation: model-rotate 9s linear infinite; }
.cube-face {
  position: absolute; inset: 0; border-radius: 4px;
  border: 1px solid rgba(31,111,235,.45);
  background: linear-gradient(135deg, rgba(31,111,235,.16), rgba(45,164,78,.16));
  box-shadow: inset 0 0 12px rgba(31,111,235,.12);
}
.f-front { transform: translateZ(28px); }
.f-back { transform: rotateY(180deg) translateZ(28px); }
.f-right { transform: rotateY(90deg) translateZ(28px); }
.f-left { transform: rotateY(-90deg) translateZ(28px); }
.f-top { transform: rotateX(90deg) translateZ(28px); }
.f-bottom { transform: rotateX(-90deg) translateZ(28px); }
@keyframes model-rotate {
  from { transform: rotateX(-22deg) rotateY(0deg); }
  to { transform: rotateX(-22deg) rotateY(360deg); }
}
.model-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.model-name { font-size: 14px; font-weight: 600; color: #24292f; display: flex; align-items: center; gap: 8px; }
.model-rule { font-size: 12px; color: #0969da; }
.model-meta { font-size: 12px; color: #8c8c8c; display: flex; gap: 12px; flex-wrap: wrap; }
.model-meta b { color: #24292f; }
.model-desc { font-size: 12px; color: #6b5a4a; }
.model-note { font-size: 12px; color: #bf8700; }
.graph-canvas { width: 100%; height: 440px; }
</style>
