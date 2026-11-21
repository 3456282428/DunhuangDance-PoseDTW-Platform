<template>
  <div class="admin-page">
    <PageHeader title="知识库与AI模型管理">
      <template #actions>
        <el-button type="primary" plain @click="backupDialog = true">备份管理</el-button>
        <el-button type="warning" plain @click="$router.push('/admin/ai-model')">AI模型管理</el-button>
      </template>
    </PageHeader>

    <!-- ================= GitHub 风格可视化洞察区 ================= -->
    <el-alert v-if="error" type="error" show-icon :closable="false" class="mb">
      <template #title>
        <span>可视化数据加载失败：{{ error }}</span>
        <el-button link type="primary" @click="loadDashboard">点击重试</el-button>
      </template>
    </el-alert>

    <el-skeleton v-if="loading && !dash" :rows="10" animated class="mb" />

    <template v-if="dash">
      <!-- 贡献热力图 -->
      <el-card shadow="never" class="viz-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">知识库贡献热力图（近一年）</span>
            <div class="card-head-actions">
              <el-radio-group v-model="heatScope" size="small" @change="renderHeat">
                <el-radio-button value="ALL">全部</el-radio-button>
                <el-radio-button v-for="s in dash.kbStorage.scopes" :key="s.scope" :value="s.scope">{{ s.scope }}</el-radio-button>
              </el-radio-group>
              <span class="heat-sum">一年新增 <b>{{ heatTotal }}</b> 条</span>
            </div>
          </div>
        </template>
        <div ref="heatRef" class="chart h200" />
        <div class="heat-hint">每个方格 = 一天，颜色越深表示当日新增知识条目越多（含手动维护与业务自动归集）</div>
      </el-card>

      <!-- 资源占比语言条 + 动态流 -->
      <div class="eq-grid">
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">知识资源构成</span>
                <el-radio-group v-model="compDim" size="small" @change="computeComposition">
                  <el-radio-button value="category">按分类</el-radio-button>
                  <el-radio-button value="source">按来源</el-radio-button>
                  <el-radio-button value="scope">按端别</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div class="lang-bar">
              <div v-for="seg in compSegments" :key="seg.name" class="lang-seg"
                   :style="{ width: seg.pct + '%', background: seg.color }" :title="seg.name + ' ' + seg.pct + '%'" />
            </div>
            <div class="lang-legend">
              <div v-for="seg in compSegments" :key="seg.name" class="lang-item">
                <span class="dot" :style="{ background: seg.color }" />
                <span class="lang-name">{{ seg.name }}</span>
                <span class="lang-pct">{{ seg.pct }}%</span>
                <span class="lang-cnt">{{ seg.value }} 条</span>
              </div>
              <div v-if="!compSegments.length" class="dim">暂无数据</div>
            </div>
          </el-card>
        </div>
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">最近知识动态</span>
                <el-button link type="primary" size="small" @click="activeTab = 'LOGS'">查看全部</el-button>
              </div>
            </template>
            <div class="feed">
              <div v-for="(l, idx) in feed" :key="idx" class="feed-item">
                <span class="feed-dot" :style="{ background: opColor(l.opType) }" />
                <div class="feed-body">
                  <div class="feed-text">{{ l.opDetail }}</div>
                  <div class="feed-meta">
                    <el-tag size="small" effect="plain" :type="scopeTag(l.kbScope)">{{ l.kbScope }}</el-tag>
                    <span>{{ opTypeLabel(l.opType) }}</span>
                    <span class="dim">{{ l.opUsername || 'SYSTEM' }} · {{ fmtTime(l.opTime) }}</span>
                  </div>
                </div>
              </div>
              <el-empty v-if="!feed.length" description="暂无动态" :image-size="56" />
            </div>
          </el-card>
        </div>
      </div>

      <!-- 知识库关联3D建模图谱（内嵌，无二级入口） -->
      <el-card shadow="never" class="viz-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">知识库关联3D建模图谱</span>
            <span class="dim">3D力导向建模呈现各数据之间的关联</span>
          </div>
        </template>
        <KbGraph3D />
      </el-card>

      <!-- 存储使用量（仓库式卡片） -->
      <div class="section-title"><span class="bar" />知识库存储使用量</div>
      <el-row :gutter="16">
        <el-col v-for="s in dash.kbStorage.scopes" :key="s.scope" :xs="24" :sm="12" :md="12" :lg="6">
          <div class="repo-card">
            <div class="repo-top">
              <span class="repo-name">{{ s.label }}</span>
              <el-tag size="small" :type="scopeTag(s.scope)" effect="plain">{{ s.scope }}</el-tag>
            </div>
            <div class="repo-desc"><b>{{ fmt(s.docCount) }}</b> 条 · 自动归集 {{ fmt(s.autoCount) }} / 手动 {{ fmt(s.manualCount) }}</div>
            <div class="repo-bar"><div class="repo-bar-fill" :style="{ width: s.contentSharePercent + '%' }" /></div>
            <div class="repo-meta">
              <span>内容 {{ fmt(s.contentChars) }} 字符（{{ s.contentSharePercent }}%）</span>
            </div>
            <div class="repo-meta dim">
              <span>占用 {{ s.physicalReadable }}</span>
              <span>启用 {{ fmt(s.activeCount) }}</span>
              <span>浏览 {{ fmt(s.viewTotal) }}</span>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="12" :lg="6">
          <div class="repo-card total">
            <div class="repo-top"><span class="repo-name">三端合计</span></div>
            <div class="repo-desc"><b>{{ fmt(dash.kbStorage.totalDocs) }}</b> 条</div>
            <div class="repo-bar"><div class="repo-bar-fill fill-all" style="width:100%" /></div>
            <div class="repo-meta"><span>内容 {{ fmt(dash.kbStorage.totalContentChars) }} 字符</span></div>
            <div class="repo-meta dim">
              <span>表占用 {{ dash.kbStorage.totalPhysicalReadable }}</span>
              <span>库总占用 {{ dash.kbStorage.schemaReadable }}</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="eq-grid">
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header><span class="card-title">内容量占比（按知识字符数）</span></template>
            <div ref="ringRef" class="chart h260" />
          </el-card>
        </div>
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header><span class="card-title">存储明细（物理表信息）</span></template>
            <el-table :data="dash.kbStorage.scopes" size="small" border stripe>
              <el-table-column prop="label" label="端别" min-width="130" />
              <el-table-column prop="table" label="物理表" min-width="130" />
              <el-table-column prop="docCount" label="条目数" align="right" width="90" />
              <el-table-column prop="contentChars" label="内容字符" align="right" width="110" />
              <el-table-column prop="physicalReadable" label="表占用" align="right" width="100" />
              <el-table-column prop="contentSharePercent" label="占比" align="right" width="90">
                <template #default="{ row }">{{ row.contentSharePercent }}%</template>
              </el-table-column>
              <el-table-column prop="viewTotal" label="浏览总量" align="right" width="100" />
            </el-table>
          </el-card>
        </div>
      </div>

      <!-- 文档数量趋势 + 资源筛选 -->
      <div class="section-title"><span class="bar" />文档数量趋势与知识资源筛选</div>
      <el-card shadow="never" class="viz-card">
        <div class="filter-bar">
          <span class="filter-label">趋势区间</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadDashboard">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
            <el-radio-button :value="90">近90天</el-radio-button>
          </el-radio-group>
          <span class="filter-label">资源筛选</span>
          <el-select v-model="filterScope" size="small" class="w150" @change="renderDist">
            <el-option v-for="s in dash.kbStorage.scopes" :key="s.scope" :label="s.label" :value="s.scope" />
          </el-select>
          <el-select v-model="filterDim" size="small" class="w130" @change="renderDist">
            <el-option label="按来源类型" value="source" />
            <el-option label="按分类" value="category" />
          </el-select>
          <el-tag v-if="distTotal" size="small" type="info">共 {{ distTotal }} 条</el-tag>
        </div>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <div ref="trendRef" class="chart h300" />
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12" class="dist-col">
            <div class="dist-title">「{{ scopeLabel(filterScope) }}」资源分布</div>
            <div ref="distRef" class="chart h260" />
          </el-col>
        </el-row>
      </el-card>
    </template>

    <!-- ================= 三端知识库管理 + 操作日志 ================= -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="管理员知识库" name="ADMIN">
          <KbDocPanel v-if="activeTab === 'ADMIN'" scope="ADMIN" manage base="admin" />
        </el-tab-pane>
        <el-tab-pane label="教师知识库" name="TEACHER">
          <KbDocPanel v-if="activeTab === 'TEACHER'" scope="TEACHER" manage base="admin" />
        </el-tab-pane>
        <el-tab-pane label="学生知识库" name="STUDENT">
          <KbDocPanel v-if="activeTab === 'STUDENT'" scope="STUDENT" manage base="admin" />
        </el-tab-pane>
        <el-tab-pane label="操作日志" name="LOGS">
          <el-table :data="logs" stripe border v-loading="logsLoading" max-height="560">
            <el-table-column prop="logId" label="ID" width="80" />
            <el-table-column prop="kbScope" label="端别" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="scopeTag(row.kbScope)">{{ row.kbScope }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="opType" label="操作类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="row.opType === 'AUTO_INGEST' ? 'warning' : row.opType === 'DELETE' ? 'danger' : 'info'">{{ opTypeLabel(row.opType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="opDetail" label="操作详情" min-width="260" show-overflow-tooltip />
            <el-table-column prop="opUsername" label="操作人" width="120">
              <template #default="{ row }">{{ row.opUsername || 'SYSTEM' }}</template>
            </el-table-column>
            <el-table-column prop="opTime" label="操作时间" width="165">
              <template #default="{ row }">{{ fmtTime(row.opTime) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="logPageNum"
            :total="logTotal"
            :page-size="10"
            layout="total, prev, pager, next"
            class="pager"
            @current-change="fetchLogs"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 备份管理对话框 -->
    <el-dialog v-model="backupDialog" title="知识库备份与恢复" width="760px">
      <div class="backup-toolbar">
        <el-button type="primary" :loading="backingUp" @click="doBackup('ALL')">立即备份（全部三端）</el-button>
        <el-button @click="doBackup('ADMIN')">仅管理员端</el-button>
        <el-button @click="doBackup('TEACHER')">仅教师端</el-button>
        <el-button @click="doBackup('STUDENT')">仅学生端</el-button>
        <span class="backup-tip">每日 02:30 自动全量备份</span>
      </div>
      <el-table :data="backups" stripe border v-loading="backupLoading" max-height="420">
        <el-table-column prop="backupId" label="ID" width="60" />
        <el-table-column prop="backupName" label="备份名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="backupType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="{ MANUAL: 'primary', AUTO: 'success', RESTORE: 'warning' }[row.backupType] || 'info'">
              {{ { MANUAL: '手动', AUTO: '定时', RESTORE: '恢复前' }[row.backupType] || row.backupType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tableScope" label="范围" width="90" />
        <el-table-column prop="itemCount" label="条目数" width="80" />
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="createTime" label="备份时间" width="165">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="doRestore(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import KbDocPanel from '@/components/KbDocPanel.vue'
import KbGraph3D from '@/components/KbGraph3D.vue'
import { getKbLogs, createKbBackup, getKbBackups, restoreKbBackup } from '@/api/knowledge'
import { getDashboardOverview } from '@/api/techDashboard'

const activeTab = ref('ADMIN')

// ================= 可视化洞察数据 =================
const dash = ref(null)
const loading = ref(false)
const error = ref('')
const heatScope = ref('ALL')
const compDim = ref('category')
const trendDays = ref(30)
const filterScope = ref('ADMIN')
const filterDim = ref('source')

const charts = {}
const heatRef = ref(null)
const ringRef = ref(null)
const trendRef = ref(null)
const distRef = ref(null)

const PALETTE = ['#3178c6', '#f1e05a', '#e34c26', '#563d7c', '#41b883', '#dea584', '#3572A5', '#b07219']
const SCOPE_TAG = { ADMIN: 'danger', TEACHER: 'warning', STUDENT: 'success', SYSTEM: 'info' }
const SOURCE_LABEL = {
  MANUAL: '手动维护', AUTO_ASSIGN: '布置作业', AUTO_GRADE: '批改点评',
  AUTO_SUBMIT: '作业提交', AUTO_SCHEDULE: '定时归集', AUTO_SYNC: '全量同步'
}

const fmt = (n) => (n ?? 0).toLocaleString('zh-CN')
const scopeTag = (s) => SCOPE_TAG[s] || 'info'
const scopeLabel = (s) => dash.value?.kbStorage?.scopes?.find(x => x.scope === s)?.label || s
const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 16)
const opTypeLabel = (t) => ({
  CREATE: '新增', UPDATE: '修改', DELETE: '删除', AUTO_INGEST: '自动归集',
  BACKUP: '备份', RESTORE: '恢复', AI_QUERY: 'AI检索'
}[t] || t)
const opColor = (t) => ({
  CREATE: '#2da44e', UPDATE: '#1f6feb', DELETE: '#cf222e',
  AUTO_INGEST: '#bf8700', BACKUP: '#6e7781', RESTORE: '#8250df', AI_QUERY: '#0969da'
}[t] || '#6e7781')

// ---- 贡献热力图 ----
const heatTotal = computed(() => {
  const a = dash.value?.kbActivity
  if (!a) return 0
  const arr = heatScope.value === 'ALL'
    ? a.totalSeries
    : (a.series.find(s => s.scope === heatScope.value)?.data || [])
  return (arr || []).reduce((x, y) => x + y, 0)
})

function renderHeat() {
  defineChart('heat', heatRef.value, buildHeatOption())
}
function buildHeatOption() {
  const a = dash.value?.kbActivity
  if (!a || !a.dates?.length) return {}
  const values = heatScope.value === 'ALL'
    ? a.totalSeries
    : (a.series.find(s => s.scope === heatScope.value)?.data || [])
  const data = a.dates.map((d, i) => [d, values[i] || 0])
  const maxV = Math.max(1, ...data.map(d => d[1]))
  return {
    tooltip: {
      formatter: p => `${p.data[0]}<br/>新增知识 <b>${p.data[1]}</b> 条`
    },
    visualMap: {
      min: 0, max: maxV, calculable: false, orient: 'horizontal',
      left: 'center', bottom: 0, itemWidth: 11, itemHeight: 11, itemGap: 3,
      inRange: { color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'] },
      textStyle: { fontSize: 10, color: '#8c8c8c' }
    },
    calendar: {
      top: 34, left: 30, right: 20, bottom: 44,
      range: [a.dates[0], a.dates[a.dates.length - 1]],
      cellSize: [13, 13],
      splitLine: { show: false },
      itemStyle: { color: '#ebedf0', borderWidth: 2, borderColor: '#fff' },
      dayLabel: { firstDay: 1, fontSize: 10, color: '#8c8c8c' },
      monthLabel: { fontSize: 10, color: '#8c8c8c' },
      yearLabel: { show: false }
    },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data }]
  }
}

// ---- 资源构成语言条（CSS 渲染，聚合各端）----
const compSegments = ref([])
function computeComposition() {
  const scopes = dash.value?.kbStorage?.scopes || []
  const src = dash.value?.docTrend
  const agg = {}
  if (compDim.value === 'scope') {
    scopes.forEach(s => { agg[s.label] = s.docCount })
  } else {
    const key = compDim.value === 'source' ? 'sourceTypeFilters' : 'categoryFilters'
    const filters = src?.[key] || {}
    scopes.forEach(s => {
      ;(filters[s.scope] || []).forEach(it => {
        const name = compDim.value === 'source' ? (SOURCE_LABEL[it.name] || it.name) : it.name
        agg[name] = (agg[name] || 0) + Number(it.value || 0)
      })
    })
  }
  const total = Object.values(agg).reduce((a, b) => a + b, 0)
  compSegments.value = Object.entries(agg)
    .sort((a, b) => b[1] - a[1])
    .map(([name, value], i) => ({
      name, value,
      pct: total ? Math.round(value * 1000 / total) / 10 : 0,
      color: PALETTE[i % PALETTE.length]
    }))
}

// ---- 内容量占比环形图 ----
function renderRing() {
  const scopes = dash.value?.kbStorage?.scopes || []
  defineChart('ring', ringRef.value, {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 字符（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    color: ['#cf222e', '#bf8700', '#2da44e'],
    series: [{
      type: 'pie', radius: ['52%', '74%'], center: ['50%', '44%'],
      avoidLabelOverlap: true, label: { show: false }, labelLine: { show: false },
      data: scopes.map(s => ({ name: s.label, value: s.contentChars }))
    }]
  })
}

// ---- 文档数量趋势 ----
function renderTrend() {
  const t = dash.value?.docTrend
  if (!t) return
  const colors = { ADMIN: '#cf222e', TEACHER: '#bf8700', STUDENT: '#2da44e' }
  defineChart('trend', trendRef.value, {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    grid: { left: 40, right: 16, top: 34, bottom: 30 },
    xAxis: { type: 'category', data: t.dates, axisLabel: { fontSize: 10, formatter: v => v.slice(5) } },
    yAxis: { type: 'value', name: '新增', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 } },
    series: t.series.map(s => ({
      name: s.label, type: 'line', smooth: true, showSymbol: false,
      lineStyle: { width: 2 }, areaStyle: { opacity: 0.06 },
      itemStyle: { color: colors[s.scope] }, data: s.data
    }))
  })
}

// ---- 资源分布（按筛选）----
const filterDist = ref([])
const distTotal = computed(() => filterDist.value.reduce((a, b) => a + b, 0))
const SOURCE_DIST = {
  MANUAL: '手动维护', AUTO_ASSIGN: '布置作业', AUTO_GRADE: '批改点评',
  AUTO_SUBMIT: '作业提交', AUTO_SCHEDULE: '定时归集', AUTO_SYNC: '全量同步'
}
function renderDist() {
  const t = dash.value?.docTrend
  if (!t) return
  const key = filterDim.value === 'source' ? 'sourceTypeFilters' : 'categoryFilters'
  const rows = (t[key] || {})[filterScope.value] || []
  filterDist.value = rows.map(r => Number(r.value || 0))
  defineChart('dist', distRef.value, {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 条（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%', '66%'], center: ['50%', '42%'],
      label: { fontSize: 11, formatter: '{b}\n{d}%' },
      data: rows.map(r => ({
        name: filterDim.value === 'source' ? (SOURCE_DIST[r.name] || r.name) : r.name,
        value: r.value
      }))
    }]
  })
}

// ---- 图表实例统一管理 ----
function defineChart(key, el, option) {
  if (!el) return
  let inst = echarts.getInstanceByDom(el)
  if (!inst) inst = echarts.init(el)
  inst.setOption(option, true)
  if (el.clientWidth > 0 && el.clientHeight > 0) inst.resize()
  charts[key] = inst
}

function renderAll() {
  renderHeat(); renderRing(); renderTrend(); renderDist()
}

function handleResize() {
  Object.values(charts).forEach(c => c && c.resize())
}

// ---- 加载总览 ----
async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const res = await getDashboardOverview(trendDays.value)
    dash.value = res.data
    if (!dash.value.kbStorage?.scopes?.some(s => s.scope === filterScope.value)) {
      filterScope.value = dash.value.kbStorage?.scopes?.[0]?.scope || 'ADMIN'
    }
    computeComposition()
  } catch (e) {
    error.value = e.message || '未知错误'
  } finally {
    loading.value = false
  }
  await nextTick()
  renderAll()
  setTimeout(() => { renderAll(); handleResize() }, 240)
}

// ================= 操作日志 / 动态流 =================
const logs = ref([])
const logsLoading = ref(false)
const logTotal = ref(0)
const logPageNum = ref(1)
const feed = ref([])

const fetchLogs = async () => {
  logsLoading.value = true
  try {
    const res = await getKbLogs({ pageNum: logPageNum.value, pageSize: 10 })
    logs.value = res.data?.rows || []
    logTotal.value = res.data?.total || 0
  } finally { logsLoading.value = false }
}
const fetchFeed = async () => {
  const res = await getKbLogs({ pageNum: 1, pageSize: 8 })
  feed.value = res.data?.rows || []
}

// ================= 备份 =================
const backupDialog = ref(false)
const backups = ref([])
const backupLoading = ref(false)
const backingUp = ref(false)
const fetchBackups = async () => {
  backupLoading.value = true
  try {
    const res = await getKbBackups()
    backups.value = res.data || []
  } finally { backupLoading.value = false }
}
const doBackup = async (scope) => {
  backingUp.value = true
  try {
    await createKbBackup(scope)
    ElMessage.success('备份完成')
    fetchBackups()
    loadDashboard()
  } finally { backingUp.value = false }
}
const doRestore = async (row) => {
  await ElMessageBox.confirm(
    `确认从备份「${row.backupName}」恢复？恢复前会自动创建当前数据的安全备份。`,
    '恢复确认', { type: 'warning' }
  )
  const res = await restoreKbBackup(row.backupId)
  ElMessage.success(`恢复完成：共 ${res.data?.restoredCount ?? 0} 条（安全备份ID：${res.data?.safetyBackupId ?? '-'}）`)
  fetchBackups()
  loadDashboard()
  fetchFeed()
}

watch(backupDialog, (v) => { if (v) fetchBackups() })

onMounted(() => {
  loadDashboard()
  fetchLogs()
  fetchFeed()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(charts).forEach(c => c && c.dispose())
})
</script>

<style scoped>
/* ===== 设计令牌：统一间距 / 圆角 / 边框 / 文字层级（与 AI模型管理页一致） ===== */
.admin-page {
  --sp-xs: 4px; --sp-sm: 8px; --sp-md: 12px; --sp-lg: 16px; --sp-xl: 24px;
  --radius: 8px; --radius-sm: 6px;
  --border: #d0d7de; --border-light: #eaeef2; --bg-soft: #f6f8fa;
  --text-1: #24292f; --text-2: #57606a; --text-3: #8c8c8c;
}

/* ===== 通用工具类 ===== */
.mb { margin-bottom: var(--sp-lg); }
.w150 { width: 150px; }
.w130 { width: 130px; }
.dim { color: var(--text-3); }

/* ===== 卡片与区块垂直节奏（统一 16px） ===== */
.viz-card { margin-bottom: var(--sp-lg); }
:deep(.el-card__header) { padding: var(--sp-md) var(--sp-lg); }
:deep(.el-card__body) { padding: var(--sp-lg); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: var(--sp-md); flex-wrap: wrap; }
.card-title { font-size: 14px; font-weight: 600; color: var(--text-1); }
.card-head-actions { display: flex; align-items: center; gap: var(--sp-md); flex-wrap: wrap; }
.section-title { display: flex; align-items: center; gap: var(--sp-sm); font-size: 15px; font-weight: 600; color: var(--text-1); margin: var(--sp-xs) 0 var(--sp-md); }
.section-title .bar { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, #1f6feb, #2da44e); }

/* ===== 图表高度刻度（200 / 260 / 300） ===== */
.chart { width: 100%; }
.h200 { height: 200px; }
.h260 { height: 260px; }
.h300 { height: 300px; }
.heat-hint { font-size: 12px; color: var(--text-3); margin-top: var(--sp-sm); line-height: 1.5; }
.heat-sum { font-size: 12px; color: #6b5a4a; }
.heat-sum b { color: #216e39; font-size: 15px; font-variant-numeric: tabular-nums; }

/* ===== GitHub 语言条 ===== */
.lang-bar { display: flex; height: 12px; border-radius: var(--radius-sm); overflow: hidden; margin: var(--sp-xs) 0 var(--sp-md); background: #ebedf0; }
.lang-seg { height: 100%; transition: width .3s; }
.lang-legend { display: flex; flex-wrap: wrap; gap: var(--sp-sm) 18px; }
.lang-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.lang-item .dot { width: 10px; height: 10px; border-radius: 50%; }
.lang-name { color: #2c1810; }
.lang-pct { color: #2c1810; font-weight: 600; }
.lang-cnt { color: var(--text-3); }

/* ===== 知识动态流 ===== */
.feed { max-height: 236px; overflow: auto; }
.feed-item { display: flex; gap: 10px; padding: var(--sp-sm) 0; border-bottom: 1px dashed var(--border-light); }
.feed-item:last-child { border-bottom: none; }
.feed-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.feed-body { flex: 1; min-width: 0; }
.feed-text { font-size: 13px; color: #2c1810; line-height: 1.5; }
.feed-meta { display: flex; align-items: center; gap: var(--sp-sm); margin-top: 3px; font-size: 12px; color: #6b5a4a; flex-wrap: wrap; }
.feed-meta .dim { color: var(--text-3); }

/* ===== 仓库式存储卡片 ===== */
.repo-card { border: 1px solid var(--border); border-radius: var(--radius); padding: var(--sp-md) var(--sp-lg); background: #fff; margin-bottom: var(--sp-lg); height: calc(100% - var(--sp-lg)); box-sizing: border-box; }
.repo-card.total { border-color: #1f6feb; background: var(--bg-soft); }
.repo-top { display: flex; justify-content: space-between; align-items: center; gap: var(--sp-sm); }
.repo-name { font-size: 14px; font-weight: 600; color: #0969da; }
.repo-desc { font-size: 12px; color: var(--text-2); margin: var(--sp-sm) 0; }
.repo-desc b { font-size: 18px; color: var(--text-1); font-variant-numeric: tabular-nums; }
.repo-bar { height: 6px; border-radius: 3px; background: #ebedf0; overflow: hidden; margin-bottom: var(--sp-sm); }
.repo-bar-fill { height: 100%; background: linear-gradient(90deg, #2da44e, #40c463); }
.repo-bar-fill.fill-all { background: linear-gradient(90deg, #1f6feb, #58a6ff); }
.repo-meta { display: flex; flex-wrap: wrap; gap: var(--sp-xs) var(--sp-lg); font-size: 12px; color: var(--text-2); }
.repo-meta.dim span { color: var(--text-3); }

/* ===== 筛选条 / 分布图 ===== */
.filter-bar { display: flex; align-items: center; gap: var(--sp-md); flex-wrap: wrap; margin-bottom: var(--sp-lg); }
.filter-label { font-size: 13px; color: #6b5a4a; }
.dist-title { font-size: 13px; color: #6b5a4a; margin-bottom: var(--sp-xs); }
.pager { margin-top: var(--sp-lg); justify-content: flex-end; }

/* ===== 备份 ===== */
.backup-toolbar { display: flex; gap: var(--sp-md); align-items: center; margin-bottom: var(--sp-md); flex-wrap: wrap; }
.backup-tip { color: var(--text-3); font-size: 12px; }

/* ===== 响应式：小屏堆叠时分布图与趋势图拉开间距 ===== */
@media (max-width: 992px) {
  .dist-col { margin-top: var(--sp-sm); }
}

/* ===== 并排卡片等高网格（CSS Grid item 默认 stretch，包装div拉满后卡片flex填满） ===== */
.eq-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--sp-lg); margin-bottom: var(--sp-lg); }
.eq-grid > div { display: flex; flex-direction: column; min-width: 0; }
.eq-grid .viz-card { flex: 1; margin-bottom: 0; display: flex; flex-direction: column; }
.eq-grid .viz-card :deep(.el-card__body) { flex: 1; }
@media (max-width: 992px) {
  .eq-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
