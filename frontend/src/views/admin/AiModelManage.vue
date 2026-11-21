<template>
  <div class="admin-page">
    <PageHeader title="AI模型管理">
      <template #actions>
        <el-tag type="success" effect="dark" size="large">AI能力已接入 · DeepSeek</el-tag>
        <el-button size="small" :loading="exporting" @click="doExport">导出统计报表</el-button>
      </template>
    </PageHeader>

    <el-alert type="info" :closable="false" class="mb">
      <template #title>
        <b>对接约定：</b>AI模型调用时将<b>优先检索并使用知识库中的已有数据作为回答依据</b>——检索范围由下方「模型-知识库关联约定」配置；
        统一调用入口 <code>POST /api/ai/invoke</code> 已接入 DeepSeek 大模型，回答基于知识库检索内容生成并全量留痕。
      </template>
    </el-alert>

    <el-alert v-if="error" type="error" show-icon :closable="false" class="mb">
      <template #title>
        <span>可视化数据加载失败：{{ error }}</span>
        <el-button link type="primary" @click="loadDashboard">点击重试</el-button>
      </template>
    </el-alert>

    <el-skeleton v-if="dashLoading && !dash" :rows="8" animated class="mb" />

    <template v-if="dash">
      <div class="section-title"><span class="bar" />AI模型仓库（运行状态 · 版本 · 性能）</div>
      <div class="eq-grid cols-3">
        <div v-for="m in dash.models" :key="m.modelId">
          <div class="m-repo-card">
            <div class="m-repo-top">
              <span class="m-repo-name">{{ m.modelName }}</span>
              <el-tag size="small" :type="statusTag(m.status)" effect="light">{{ statusText(m.status) }}</el-tag>
            </div>
            <div class="m-repo-desc">{{ m.description || '暂无描述' }}</div>
            <div class="m-repo-badge">
              <span class="badge"><span class="dot" :style="{ background: typeColor(m.modelType) }" />{{ m.modelType }}</span>
              <span class="badge">{{ m.provider || '未指定提供方' }}</span>
              <span class="badge" :class="m.updateStatus === 'UPDATE_AVAILABLE' ? 'warn' : 'ok'">{{ updText(m.updateStatus) }}</span>
            </div>
            <div class="m-repo-ver">
              <span class="ver-tag">{{ m.currentVersion || '—' }}</span>
              <span class="arrow">→</span>
              <span class="ver-tag latest">{{ m.latestVersion || '—' }}</span>
              <span v-if="m.lastUpgradeTime" class="ver-time">登记 {{ fmtTime(m.lastUpgradeTime) }}</span>
            </div>
            <div class="m-repo-metrics">
              <span>延迟 {{ m.metricSummary?.latest?.latencyMs ?? '—' }} ms</span>
              <span>准确率 {{ pct(m.metricSummary?.avgAccuracy) }}</span>
              <span>成功率 {{ pct(m.metricSummary?.avgSuccessRate) }}</span>
              <span>样本 {{ m.metricSummary?.count ?? 0 }}</span>
            </div>
            <div class="m-repo-bind" v-if="(m.bindings || []).length">
              知识库绑定：
              <el-tag v-for="b in m.bindings" :key="b.bindingId" size="small" effect="plain" class="bind-tag">
                {{ b.kbScope }}{{ b.kbCategory ? ' / ' + b.kbCategory : '' }}
              </el-tag>
            </div>
            <div class="m-repo-actions">
              <el-button link type="primary" size="small" @click="openTimeline(m)">版本迭代记录（{{ (m.versions || []).length }}）</el-button>
              <el-button link type="primary" size="small" @click="focusMetrics(m)">性能实时曲线</el-button>
              <el-button v-if="canUpgrade" link type="warning" size="small" @click="openUpgrade(m)">触发更新</el-button>
            </div>
          </div>
        </div>
        <div v-if="!dash.models.length" class="span-all">
          <el-empty description="暂无AI模型，请在下方登记" :image-size="70" />
        </div>
      </div>

      <div class="eq-grid">
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">版本迭代记录</span>
                <el-select v-model="timelineModelId" size="small" class="w150" @change="renderTimeline">
                  <el-option v-for="m in dash.models" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
                </el-select>
              </div>
            </template>
            <el-timeline v-if="timelineVersions.length">
              <el-timeline-item
                v-for="v in timelineVersions" :key="v.versionId"
                :timestamp="v.releaseDate" placement="top"
                :type="v.isCurrent === 1 ? 'success' : v.upgradeStatus === 'PENDING' ? 'warning' : 'primary'"
              >
                <div class="tl-row">
                  <el-tag size="small" :type="v.isCurrent === 1 ? 'success' : 'info'" effect="dark">{{ v.version }}</el-tag>
                  <el-tag size="small" :type="v.upgradeStatus === 'PENDING' ? 'warning' : 'success'" effect="plain">
                    {{ v.upgradeStatus === 'PENDING' ? '待应用升级' : '已应用' }}
                  </el-tag>
                  <el-tag v-if="v.isCurrent === 1" size="small" type="success">运行中</el-tag>
                </div>
                <div class="tl-log">{{ v.changelog || '—' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无版本记录" :image-size="60" />
          </el-card>
        </div>
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">模型性能指标实时曲线</span>
                <div class="head-actions">
                  <el-select v-model="metricModelId" size="small" class="w150" @change="loadMetrics">
                    <el-option v-for="m in dash.models" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
                  </el-select>
                  <el-checkbox-group v-model="metricKeys" size="small" @change="renderMetrics">
                    <el-checkbox-button v-for="k in METRIC_DEFS" :key="k.key" :value="k.key">{{ k.label }}</el-checkbox-button>
                  </el-checkbox-group>
                  <el-switch v-model="autoRefresh" size="small" active-text="自动刷新(30s)" @change="toggleAuto" />
                  <el-button size="small" :loading="collecting" @click="doCollect">立即采样</el-button>
                </div>
              </div>
            </template>
            <div ref="metricRef" class="chart h280" />
            <div class="metric-cards">
              <div class="metric-card"><span class="mc-label">采样点</span><b>{{ metricData?.summary?.count ?? '—' }}</b></div>
              <div class="metric-card"><span class="mc-label">平均延迟</span><b>{{ metricData?.summary?.avgLatencyMs ?? '—' }} ms</b></div>
              <div class="metric-card"><span class="mc-label">平均准确率</span><b>{{ pct(metricData?.summary?.avgAccuracy) }}</b></div>
              <div class="metric-card"><span class="mc-label">平均成功率</span><b>{{ pct(metricData?.summary?.avgSuccessRate) }}</b></div>
              <div class="metric-card"><span class="mc-label">知识库命中</span><b>{{ metricData?.summary?.avgKbHit ?? '—' }}</b></div>
              <div class="metric-card"><span class="mc-label">调用总量</span><b>{{ fmt(metricData?.summary?.totalCalls) }}</b></div>
              <div class="metric-card"><span class="mc-label">最新延迟</span><b>{{ metricData?.summary?.latest?.latencyMs ?? '—' }} ms</b></div>
              <div class="metric-card"><span class="mc-label">最近采样</span><b>{{ fmtTime(metricData?.summary?.latest?.metricTime) || '—' }}</b></div>
            </div>
          </el-card>
        </div>
      </div>
    </template>

    <div class="section-title"><span class="bar" />API 调用与用量（接口访问真实留痕）</div>

    <el-alert v-if="apiError" type="error" show-icon :closable="false" class="mb">
      <template #title>
        <span>API用量数据加载失败：{{ apiError }}</span>
        <el-button link type="primary" @click="loadApiUsage">点击重试</el-button>
      </template>
    </el-alert>
    <el-skeleton v-if="apiLoading && !api" :rows="6" animated class="mb" />

    <template v-if="api">
      <div class="api-sum-grid">
        <div class="api-sum-card"><span class="mc-label">窗口内总调用</span><b>{{ fmt(api.summary.totalCalls) }}</b></div>
        <div class="api-sum-card"><span class="mc-label">今日调用</span><b>{{ fmt(api.summary.todayCalls) }}</b></div>
        <div class="api-sum-card"><span class="mc-label">平均耗时</span><b>{{ api.summary.avgDurationMs }} ms</b></div>
        <div class="api-sum-card"><span class="mc-label">成功率</span><b>{{ api.summary.successRate }}%</b></div>
        <div class="api-sum-card"><span class="mc-label">覆盖端点</span><b>{{ fmt(api.summary.endpointCount) }}</b></div>
        <div class="api-sum-card"><span class="mc-label">调用用户</span><b>{{ fmt(api.summary.userCount) }}</b></div>
        <div class="api-sum-card"><span class="mc-label">高峰时段</span><b>{{ api.summary.peakLabel }}</b></div>
      </div>

      <div class="eq-grid">
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">接口调用流量与平均耗时</span>
                <el-radio-group v-model="apiDays" size="small" @change="loadApiUsage">
                  <el-radio-button :value="7">7 天</el-radio-button>
                  <el-radio-button :value="14">14 天</el-radio-button>
                  <el-radio-button :value="30">30 天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div ref="trafficRef" class="chart h260" />
            <div class="heat-hint">数据来自后端接口访问留痕（每次 /api/** 真实请求自动落库），无访问的日期计 0</div>
          </el-card>
        </div>
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header><span class="card-title">调用构成占比</span></template>
            <div class="lang-label">按接口业务分组</div>
            <div class="lang-bar">
              <div v-for="seg in groupSegments" :key="seg.name" class="lang-seg"
                   :style="{ width: seg.percent + '%', background: seg.color }" :title="seg.name + ' ' + seg.percent + '%'" />
            </div>
            <div class="lang-legend">
              <div v-for="seg in groupSegments" :key="seg.name" class="lang-item">
                <span class="dot" :style="{ background: seg.color }" />
                <span class="lang-name">{{ seg.name }}</span>
                <span class="lang-pct">{{ seg.percent }}%</span>
                <span class="lang-cnt">{{ seg.value }} 次</span>
              </div>
              <div v-if="!groupSegments.length" class="dim">暂无调用数据</div>
            </div>
            <div class="lang-label mt">按调用角色</div>
            <div class="lang-bar">
              <div v-for="seg in roleSegments" :key="seg.name" class="lang-seg"
                   :style="{ width: seg.percent + '%', background: seg.color }" :title="seg.name + ' ' + seg.percent + '%'" />
            </div>
            <div class="lang-legend">
              <div v-for="seg in roleSegments" :key="seg.name" class="lang-item">
                <span class="dot" :style="{ background: seg.color }" />
                <span class="lang-name">{{ seg.label }}</span>
                <span class="lang-pct">{{ seg.percent }}%</span>
                <span class="lang-cnt">{{ seg.value }} 次</span>
              </div>
              <div v-if="!roleSegments.length" class="dim">暂无调用数据</div>
            </div>
          </el-card>
        </div>
      </div>

      <div class="eq-grid">
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">接口调用量排行 Top {{ api.topEndpoints.length }}</span>
                <span class="dim">数字路径段归一为 {id} 后聚合</span>
              </div>
            </template>
            <div ref="topRef" class="chart h300" />
          </el-card>
        </div>
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">调用时段分布（星期 × 小时）</span>
                <span class="dim">峰值 {{ fmt(api.punchMax) }} 次</span>
              </div>
            </template>
            <div ref="punchRef" class="chart h300" />
          </el-card>
        </div>
      </div>

      <div class="eq-grid">
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header><span class="card-title">接口调用明细（按调用量降序）</span></template>
            <el-table :data="api.topEndpoints" stripe border size="small">
              <el-table-column prop="apiGroup" label="业务分组" width="120" />
              <el-table-column prop="method" label="方法" width="80">
                <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.method }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="endpoint" label="接口端点" min-width="240" show-overflow-tooltip />
              <el-table-column prop="calls" label="调用次数" width="100" align="right">
                <template #default="{ row }">{{ fmt(row.calls) }}</template>
              </el-table-column>
              <el-table-column prop="avgDurationMs" label="平均耗时" width="110" align="right">
                <template #default="{ row }">{{ row.avgDurationMs }} ms</template>
              </el-table-column>
              <el-table-column prop="failCalls" label="失败" width="80" align="right">
                <template #default="{ row }"><span :class="{ bad: row.failCalls > 0 }">{{ row.failCalls }}</span></template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
        <div>
          <el-card shadow="never" class="viz-card">
            <template #header><span class="card-title">AI统一入口调用与知识库检索</span></template>
            <div class="api-sum-grid mini">
              <div class="api-sum-card"><span class="mc-label">调用总量</span><b>{{ fmt(api.aiInvoke.total) }}</b></div>
              <div class="api-sum-card"><span class="mc-label">命中知识库</span><b>{{ fmt(api.aiInvoke.hitCalls) }}</b></div>
              <div class="api-sum-card"><span class="mc-label">命中率</span><b>{{ api.aiInvoke.hitRate }}%</b></div>
              <div class="api-sum-card"><span class="mc-label">平均命中条数</span><b>{{ api.aiInvoke.avgHit }}</b></div>
            </div>
            <div class="lang-label mt">按调用端别</div>
            <div class="lang-bar">
              <div v-for="seg in invokeScopeSegments" :key="seg.name" class="lang-seg"
                   :style="{ width: seg.percent + '%', background: seg.color }" :title="seg.name + ' ' + seg.percent + '%'" />
            </div>
            <div class="lang-legend">
              <div v-for="seg in invokeScopeSegments" :key="seg.name" class="lang-item">
                <span class="dot" :style="{ background: seg.color }" />
                <span class="lang-name">{{ seg.label }}</span>
                <span class="lang-pct">{{ seg.percent }}%</span>
                <span class="lang-cnt">{{ seg.value }} 次</span>
              </div>
              <div v-if="!invokeScopeSegments.length" class="dim">暂无AI调用</div>
            </div>
            <div class="heat-hint">统一入口 <code>POST /api/ai/invoke</code>：调用时优先检索知识库已有数据，全量留痕</div>
          </el-card>
        </div>
      </div>
    </template>

    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-header">
          <span class="card-title">模型注册（{{ models.length }}）</span>
          <el-button type="primary" size="small" @click="openModelDialog()">登记模型</el-button>
        </div>
      </template>
      <el-table :data="models" stripe border v-loading="loading">
        <el-table-column prop="modelId" label="ID" width="60" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="modelType" label="类型" width="100">
          <template #default="{ row }"><el-tag size="small">{{ row.modelType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="provider" label="提供方" width="120" />
        <el-table-column prop="endpoint" label="服务地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openModelDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeModel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mb">
      <template #header>
        <div class="card-header">
          <span class="card-title">模型-知识库关联约定（AI检索依据配置）</span>
          <el-button type="primary" size="small" @click="openBindingDialog()">新增约定</el-button>
        </div>
      </template>
      <el-table :data="bindings" stripe border v-loading="loading">
        <el-table-column prop="bindingId" label="ID" width="60" />
        <el-table-column prop="modelName" label="AI模型" min-width="160" />
        <el-table-column prop="kbScope" label="知识库范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.kbScope === 'ALL' ? 'danger' : 'success'" effect="plain">{{ row.kbScope }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="kbCategory" label="限定分类" width="120">
          <template #default="{ row }">{{ row.kbCategory || '不限' }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" />
        <el-table-column prop="enabled" label="生效" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === 1" @change="v => toggleBinding(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeBinding(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mb">
      <template #header><span class="card-title">AI调用日志（统一入口全量留痕）</span></template>
      <el-table :data="invokeLogs" stripe border v-loading="loading" max-height="360">
        <el-table-column prop="invokeId" label="ID" width="70" />
        <el-table-column prop="callerScope" label="调用端" width="100">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.callerScope }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="question" label="提问内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="kbScope" label="检索范围" width="100" />
        <el-table-column prop="hitCount" label="命中条数" width="90" />
        <el-table-column prop="invokeStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="{ PENDING: 'info', SUCCESS: 'success', FAILED: 'danger' }[row.invokeStatus] || 'warning'">{{ { PENDING: '未配置模型', SUCCESS: '成功', FAILED: '失败' }[row.invokeStatus] || row.invokeStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invokeTime" label="调用时间" width="165">
          <template #default="{ row }">{{ fmtTime(row.invokeTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="modelDialog" :title="modelForm.modelId ? '编辑模型' : '登记模型'" width="560px">
      <el-form :model="modelForm" label-width="90px">
        <el-form-item label="模型名称" required><el-input v-model="modelForm.modelName" placeholder="如 platform-knowledge-llm" /></el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="modelForm.modelType" style="width:100%">
            <el-option label="LLM 大语言模型" value="LLM" />
            <el-option label="EMBEDDING 向量模型" value="EMBEDDING" />
            <el-option label="RERANK 重排模型" value="RERANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="提供方"><el-input v-model="modelForm.provider" placeholder="OpenAI / 本地部署 等" /></el-form-item>
        <el-form-item label="服务地址"><el-input v-model="modelForm.endpoint" placeholder="https://..." /></el-form-item>
        <el-form-item label="API密钥"><el-input v-model="modelForm.apiKey" placeholder="脱敏存储" show-password /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="modelForm.status" style="width:100%">
            <el-option label="未配置" value="UNCONFIGURED" />
            <el-option label="已启用" value="ENABLED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="modelForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialog = false">取消</el-button>
        <el-button type="primary" @click="submitModel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindingDialog" title="新增模型-知识库关联约定" width="520px">
      <el-form :model="bindingForm" label-width="100px">
        <el-form-item label="AI模型" required>
          <el-select v-model="bindingForm.modelId" style="width:100%">
            <el-option v-for="m in models" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库范围" required>
          <el-select v-model="bindingForm.kbScope" style="width:100%">
            <el-option label="全部三端（ALL）" value="ALL" />
            <el-option label="管理员端" value="ADMIN" />
            <el-option label="教师端" value="TEACHER" />
            <el-option label="学生端" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="限定分类"><el-input v-model="bindingForm.kbCategory" placeholder="留空=不限" /></el-form-item>
        <el-form-item label="检索优先级"><el-input-number v-model="bindingForm.priority" :min="0" :max="99" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBinding">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="upgradeDialog" title="触发模型更新" width="520px">
      <el-alert type="warning" :closable="false" class="mb-sm"
        title="将登记升级意向并全量留痕，确认无误后可执行版本切换" />
      <el-form :model="upgradeForm" label-width="100px">
        <el-form-item label="模型">{{ upgradeForm.modelName }}</el-form-item>
        <el-form-item label="当前版本"><el-tag>{{ upgradeForm.currentVersion || '—' }}</el-tag></el-form-item>
        <el-form-item label="目标版本" required><el-input v-model="upgradeForm.targetVersion" placeholder="如 1.2.0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="upgradeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitUpgrade">登记升级意向</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import {
  getAiModelList, createAiModel, updateAiModel, deleteAiModel,
  getAiBindingList, createAiBinding, updateAiBindingEnabled, deleteAiBinding,
  getAiInvokeLogs
} from '@/api/knowledge'
import {
  getDashboardOverview, getModelMetrics, upgradeModel, collectMetrics,
  getApiUsage, exportDashboardReport
} from '@/api/techDashboard'

const loading = ref(false)
const models = ref([])
const bindings = ref([])
const invokeLogs = ref([])

const modelDialog = ref(false)
const modelForm = ref({})
const bindingDialog = ref(false)
const bindingForm = ref({ modelId: null, kbScope: 'ALL', kbCategory: '', priority: 0 })

const STATUS_TEXT = { UNCONFIGURED: '未配置', ENABLED: '已启用', DISABLED: '已停用' }
const STATUS_TAG = { UNCONFIGURED: 'info', ENABLED: 'success', DISABLED: 'danger' }
const UPD_TEXT = { UP_TO_DATE: '已是最新', UPDATE_AVAILABLE: '有可用更新', SYNC_FAILED: '同步失败', UNKNOWN: '未知' }
const TYPE_COLOR = { LLM: '#3178c6', EMBEDDING: '#2da44e', RERANK: '#bf8700' }
const ROLE_LABEL = { ADMIN: '系统管理员', TEACHER: '教师', STUDENT: '学员', ANON: '未登录' }
const ROLE_COLOR = { ADMIN: '#cf222e', TEACHER: '#bf8700', STUDENT: '#2da44e', ANON: '#6e7781' }
const PALETTE = ['#3178c6', '#2da44e', '#bf8700', '#8250df', '#1f6feb', '#0969da', '#cf222e', '#6e7781']
const METRIC_DEFS = [
  { key: 'latencyMs', label: '延迟(ms)', axis: 0, color: '#cf222e' },
  { key: 'accuracy', label: '准确率', axis: 1, color: '#2da44e' },
  { key: 'successRate', label: '成功率', axis: 1, color: '#1f6feb' },
  { key: 'kbHitAvg', label: '知识库命中', axis: 2, color: '#8250df' },
  { key: 'callCount', label: '调用量', axis: 2, color: '#bf8700' },
  { key: 'throughput', label: '吞吐(QPS)', axis: 2, color: '#0969da' }
]

const fmt = (n) => (n ?? 0).toLocaleString('zh-CN')
const pct = (v) => (v == null ? '—' : (Number(v) * 100).toFixed(1) + '%')
const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 16)
const statusText = (s) => STATUS_TEXT[s] || s
const statusTag = (s) => STATUS_TAG[s] || 'info'
const updText = (s) => UPD_TEXT[s] || s
const typeColor = (t) => TYPE_COLOR[t] || '#6e7781'
const canUpgrade = true

const fetchAll = async () => {
  loading.value = true
  try {
    const [m, b, l] = await Promise.all([getAiModelList(), getAiBindingList(), getAiInvokeLogs()])
    models.value = m.data || []
    bindings.value = b.data || []
    invokeLogs.value = l.data || []
  } finally { loading.value = false }
}

const openModelDialog = (row) => {
  modelForm.value = row ? { ...row } : { modelName: '', modelType: 'LLM', provider: '', endpoint: '', apiKey: '', status: 'UNCONFIGURED', description: '' }
  modelDialog.value = true
}
const submitModel = async () => {
  if (!modelForm.value.modelName?.trim()) { ElMessage.warning('请输入模型名称'); return }
  if (modelForm.value.modelId) await updateAiModel(modelForm.value.modelId, modelForm.value)
  else await createAiModel(modelForm.value)
  ElMessage.success('保存成功')
  modelDialog.value = false
  fetchAll(); loadDashboard()
}
const removeModel = async (row) => {
  await ElMessageBox.confirm(`确认删除模型「${row.modelName}」？其关联约定将一并删除。`, '提示', { type: 'warning' })
  await deleteAiModel(row.modelId)
  ElMessage.success('已删除')
  fetchAll(); loadDashboard()
}

const openBindingDialog = () => {
  if (!models.value.length) { ElMessage.warning('请先登记AI模型'); return }
  bindingForm.value = { modelId: models.value[0].modelId, kbScope: 'ALL', kbCategory: '', priority: 0 }
  bindingDialog.value = true
}
const submitBinding = async () => {
  if (!bindingForm.value.modelId) { ElMessage.warning('请选择AI模型'); return }
  await createAiBinding(bindingForm.value)
  ElMessage.success('已创建关联约定')
  bindingDialog.value = false
  fetchAll(); loadDashboard()
}
const toggleBinding = async (row, v) => {
  await updateAiBindingEnabled(row.bindingId, v ? 1 : 0)
  ElMessage.success(v ? '已生效' : '已停用')
  fetchAll(); loadDashboard()
}
const removeBinding = async (row) => {
  await ElMessageBox.confirm('确认删除该关联约定？', '提示', { type: 'warning' })
  await deleteAiBinding(row.bindingId)
  ElMessage.success('已删除')
  fetchAll(); loadDashboard()
}

const dash = ref(null)
const dashLoading = ref(false)
const error = ref('')
const timelineModelId = ref(null)
const timelineVersions = ref([])
const metricModelId = ref(null)
const metricKeys = ref(['latencyMs', 'accuracy', 'successRate'])
const metricData = ref(null)
const collecting = ref(false)
const autoRefresh = ref(false)
const charts = {}
const metricRef = ref(null)
let autoTimer = null

async function loadDashboard() {
  dashLoading.value = true
  error.value = ''
  try {
    const res = await getDashboardOverview(30)
    dash.value = res.data
    const ms = dash.value.models || []
    if (!timelineModelId.value && ms.length) timelineModelId.value = ms[0].modelId
    if (!metricModelId.value && ms.length) metricModelId.value = ms[0].modelId
    renderTimeline()
  } catch (e) {
    error.value = e.message || '未知错误'
  } finally { dashLoading.value = false }
  await nextTick()
  if (metricModelId.value) loadMetrics()
  setTimeout(renderMetrics, 240)
}

function renderTimeline() {
  const m = (dash.value?.models || []).find(x => x.modelId === timelineModelId.value)
  const vs = (m?.versions || []).slice().sort((a, b) => String(b.version).localeCompare(String(a.version)))
  timelineVersions.value = vs
}

async function loadMetrics() {
  if (!metricModelId.value) return
  const res = await getModelMetrics(metricModelId.value, 60)
  metricData.value = res.data
  renderMetrics()
}
function renderMetrics() {
  const d = metricData.value
  if (!d) return
  const axisDefs = [
    { name: '延迟(ms)', color: '#cf222e' },
    { name: '比率', color: '#2da44e' },
    { name: '次数', color: '#bf8700' }
  ]
  const series = METRIC_DEFS.filter(k => metricKeys.value.includes(k.key)).map(k => ({
    name: k.label, type: 'line', smooth: true, showSymbol: false, yAxisIndex: k.axis,
    lineStyle: { width: 2 }, itemStyle: { color: k.color }, data: d[k.key] || []
  }))
  drawChart('metric', metricRef.value, {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    grid: { left: 46, right: 46, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: d.times || [], axisLabel: { fontSize: 10 } },
    yAxis: axisDefs.map((a, i) => ({
      type: 'value', name: a.name, nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 },
      position: i === 0 ? 'left' : 'right', offset: i === 2 ? 34 : 0
    })),
    series
  })
}
const doCollect = async () => {
  collecting.value = true
  try {
    await collectMetrics()
    ElMessage.success('已触发一次指标采样')
    loadMetrics()
  } finally { collecting.value = false }
}
function toggleAuto(v) {
  if (autoTimer) { clearInterval(autoTimer); autoTimer = null }
  if (v) autoTimer = setInterval(loadMetrics, 30000)
}
function focusMetrics(m) {
  metricModelId.value = m.modelId
  loadMetrics()
  ElMessage.info(`已切换到「${m.modelName}」性能曲线`)
}
function openTimeline(m) {
  timelineModelId.value = m.modelId
  renderTimeline()
  ElMessage.info(`已切换到「${m.modelName}」版本记录`)
}

const upgradeDialog = ref(false)
const upgradeForm = ref({})
const openUpgrade = (m) => {
  upgradeForm.value = { modelId: m.modelId, modelName: m.modelName, currentVersion: m.currentVersion, targetVersion: m.latestVersion || '' }
  upgradeDialog.value = true
}
const submitUpgrade = async () => {
  if (!upgradeForm.value.targetVersion?.trim()) { ElMessage.warning('请输入目标版本'); return }
  const res = await upgradeModel(upgradeForm.value.modelId, upgradeForm.value.targetVersion)
  ElMessage.success(res.data?.message || '升级意向已登记')
  upgradeDialog.value = false
  loadDashboard()
}

const exporting = ref(false)
const api = ref(null)
const apiLoading = ref(false)
const apiError = ref('')
const apiDays = ref(30)
const trafficRef = ref(null)
const topRef = ref(null)
const punchRef = ref(null)

const doExport = async () => {
  exporting.value = true
  try {
    const res = await exportDashboardReport()
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `仪表盘统计报表_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('报表已导出')
  } finally { exporting.value = false }
}
async function loadApiUsage() {
  apiLoading.value = true
  apiError.value = ''
  try {
    const res = await getApiUsage(apiDays.value)
    api.value = res.data
  } catch (e) {
    apiError.value = e.message || '未知错误'
  } finally { apiLoading.value = false }
  await nextTick()
  renderAllApi()
  setTimeout(renderAllApi, 240)
}
function renderAllApi() { renderTraffic(); renderTop(); renderPunch() }

const groupSegments = computed(() => {
  const rows = api.value?.groupShare || []
  return rows.map((r, i) => ({ ...r, color: PALETTE[i % PALETTE.length] }))
})
const roleSegments = computed(() => {
  const rows = api.value?.roleShare || []
  return rows.map(r => ({ ...r, label: ROLE_LABEL[r.name] || r.name, color: ROLE_COLOR[r.name] || '#6e7781' }))
})
const invokeScopeSegments = computed(() => {
  const rows = api.value?.aiInvoke?.scopeShare || []
  return rows.map(r => ({ ...r, label: ROLE_LABEL[r.name] || r.name, color: ROLE_COLOR[r.name] || '#6e7781' }))
})

function renderTraffic() {
  const d = api.value
  if (!d || !d.dates?.length) return
  drawChart('apiTraffic', trafficRef.value, {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    grid: { left: 52, right: 56, top: 36, bottom: 30 },
    xAxis: {
      type: 'category', data: d.dates,
      axisLabel: { fontSize: 10, formatter: v => v.slice(5) }
    },
    yAxis: [
      { type: 'value', name: '调用次数', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 }, minInterval: 1 },
      { type: 'value', name: '平均耗时(ms)', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 }, splitLine: { show: false } }
    ],
    series: [
      { name: '调用次数', type: 'bar', barMaxWidth: 16, itemStyle: { color: '#1f6feb', borderRadius: [3, 3, 0, 0] }, data: d.calls },
      { name: '失败次数', type: 'bar', barMaxWidth: 16, itemStyle: { color: '#cf222e', borderRadius: [3, 3, 0, 0] }, data: d.failCalls },
      { name: '平均耗时(ms)', type: 'line', smooth: true, showSymbol: false, yAxisIndex: 1, lineStyle: { width: 2 }, itemStyle: { color: '#2da44e' }, data: d.avgDuration }
    ]
  })
}

function renderTop() {
  const list = (api.value?.topEndpoints || []).slice().reverse()
  if (!list.length) return
  drawChart('apiTop', topRef.value, {
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: p => {
        const x = list[p[0].dataIndex]
        return `${x.method} ${x.endpoint}<br/>分组：${x.apiGroup}<br/>调用 <b>${x.calls}</b> 次 · 平均 ${x.avgDurationMs} ms · 失败 ${x.failCalls}`
      }
    },
    grid: { left: 8, right: 46, top: 12, bottom: 8, containLabel: true },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 }, minInterval: 1 },
    yAxis: {
      type: 'category', data: list.map(x => `${x.method} ${x.endpoint}`),
      axisLabel: { fontSize: 10, width: 200, overflow: 'truncate' }
    },
    series: [{
      type: 'bar', barMaxWidth: 14,
      itemStyle: { color: '#1f6feb', borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', fontSize: 10, color: '#57606a' },
      data: list.map(x => x.calls)
    }]
  })
}

function renderPunch() {
  const d = api.value
  if (!d || !d.punchCard) return
  const data = []
  for (let wd = 0; wd < 7; wd++) {
    for (let h = 0; h < 24; h++) data.push([h, wd, d.punchCard[wd][h] || 0])
  }
  drawChart('apiPunch', punchRef.value, {
    tooltip: {
      formatter: p => `周${'一二三四五六日'.charAt(p.data[1])} ${String(p.data[0]).padStart(2, '0')}:00<br/>调用 <b>${p.data[2]}</b> 次`
    },
    grid: { left: 46, right: 16, top: 12, bottom: 46 },
    visualMap: {
      min: 0, max: Math.max(1, d.punchMax), calculable: false, orient: 'horizontal',
      left: 'center', bottom: 0, itemWidth: 11, itemHeight: 11, itemGap: 3,
      inRange: { color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'] },
      textStyle: { fontSize: 10, color: '#8c8c8c' }
    },
    xAxis: {
      type: 'category', data: Array.from({ length: 24 }, (_, h) => h),
      axisLabel: { fontSize: 10, interval: 1 }, splitArea: { show: true }
    },
    yAxis: {
      type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLabel: { fontSize: 10 }, splitArea: { show: true }
    },
    series: [{
      type: 'heatmap', data,
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis: { itemStyle: { shadowBlur: 6, shadowColor: 'rgba(0,0,0,.35)' } }
    }]
  })
}

function drawChart(key, el, option) {
  if (!el) return
  let inst = echarts.getInstanceByDom(el)
  if (!inst) inst = echarts.init(el)
  inst.setOption(option, true)
  if (el.clientWidth > 0 && el.clientHeight > 0) inst.resize()
  charts[key] = inst
}
function handleResize() { Object.values(charts).forEach(c => c && c.resize()) }

onMounted(() => {
  fetchAll()
  loadDashboard()
  loadApiUsage()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (autoTimer) clearInterval(autoTimer)
  Object.values(charts).forEach(c => c && c.dispose())
})
</script>

<style scoped>
.admin-page {
  --sp-xs: 4px; --sp-sm: 8px; --sp-md: 12px; --sp-lg: 16px; --sp-xl: 24px;
  --radius: 8px; --radius-sm: 6px;
  --border: #d0d7de; --border-light: #eaeef2; --bg-soft: #f6f8fa;
  --text-1: #24292f; --text-2: #57606a; --text-3: #8c8c8c;
}

.mb { margin-bottom: var(--sp-lg); }
.mb-sm { margin-bottom: var(--sp-md); }
.mt { margin-top: var(--sp-lg); }
.w150 { width: 150px; }
.dim { color: var(--text-3); }

.viz-card { margin-bottom: var(--sp-lg); }
:deep(.el-card__header) { padding: var(--sp-md) var(--sp-lg); }
:deep(.el-card__body) { padding: var(--sp-lg); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: var(--sp-md); flex-wrap: wrap; }
.card-title { font-size: 14px; font-weight: 600; color: var(--text-1); }
.section-title { display: flex; align-items: center; gap: var(--sp-sm); font-size: 15px; font-weight: 600; color: var(--text-1); margin: var(--sp-xs) 0 var(--sp-md); }
.section-title .bar { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, #1f6feb, #2da44e); }
.head-actions { display: flex; align-items: center; gap: var(--sp-md); flex-wrap: wrap; }

.chart { width: 100%; }
.h200 { height: 200px; }
.h260 { height: 260px; }
.h280 { height: 280px; }
.h300 { height: 300px; }
.heat-hint { font-size: 12px; color: var(--text-3); margin-top: var(--sp-sm); line-height: 1.5; }

.m-repo-card { border: 1px solid var(--border); border-radius: var(--radius); padding: var(--sp-md) var(--sp-lg); background: #fff; margin-bottom: var(--sp-lg); }
.m-repo-top { display: flex; justify-content: space-between; align-items: center; gap: var(--sp-sm); }
.m-repo-name { font-size: 14px; font-weight: 600; color: #0969da; }
.m-repo-desc { font-size: 12px; color: var(--text-2); margin: var(--sp-sm) 0; line-height: 1.5; min-height: 18px; }
.m-repo-badge { display: flex; flex-wrap: wrap; gap: var(--sp-sm); margin-bottom: var(--sp-sm); }
.m-repo-badge .badge { display: inline-flex; align-items: center; gap: var(--sp-xs); font-size: 12px; color: var(--text-2);
  border: 1px solid var(--border); border-radius: 10px; padding: 1px var(--sp-sm); }
.m-repo-badge .badge .dot { width: 8px; height: 8px; border-radius: 50%; }
.m-repo-badge .badge.warn { color: #9a6700; border-color: #d4a72c; }
.m-repo-badge .badge.ok { color: #1a7f37; border-color: #4ac26b; }
.m-repo-ver { display: flex; align-items: center; gap: var(--sp-sm); margin-bottom: var(--sp-sm); flex-wrap: wrap; }
.ver-tag { background: var(--bg-soft); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 1px var(--sp-sm); font-size: 12px; font-family: monospace; }
.ver-tag.latest { background: #ddf4ff; border-color: #54aeff; color: #0969da; }
.ver-time { font-size: 12px; color: var(--text-3); }
.m-repo-metrics { display: flex; flex-wrap: wrap; gap: var(--sp-xs) var(--sp-lg); font-size: 12px; color: var(--text-2); margin-bottom: var(--sp-sm); }
.m-repo-bind { font-size: 12px; color: var(--text-2); margin-bottom: var(--sp-sm); }
.bind-tag { margin-right: var(--sp-xs); }
.m-repo-actions { display: flex; flex-wrap: wrap; gap: var(--sp-xs); border-top: 1px solid var(--border-light); padding-top: var(--sp-sm); }

.tl-row { display: flex; align-items: center; gap: var(--sp-sm); flex-wrap: wrap; }
.tl-log { font-size: 12px; color: var(--text-2); margin-top: var(--sp-xs); line-height: 1.5; }

.metric-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--sp-sm); margin-top: var(--sp-md); }
.metric-card { background: var(--bg-soft); border: 1px solid var(--border-light); border-radius: var(--radius-sm); padding: var(--sp-sm) var(--sp-md); }
.metric-card .mc-label { display: block; font-size: 11px; color: var(--text-3); margin-bottom: 2px; }
.metric-card b { font-size: 14px; color: var(--text-1); font-variant-numeric: tabular-nums; }

.api-sum-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: var(--sp-md); margin-bottom: var(--sp-lg); }
.api-sum-grid.mini { grid-template-columns: repeat(2, 1fr); margin-bottom: 0; }
.api-sum-card { background: var(--bg-soft); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: var(--sp-sm) var(--sp-md); }
.api-sum-card .mc-label { display: block; font-size: 11px; color: var(--text-3); margin-bottom: 2px; white-space: nowrap; }
.api-sum-card b { font-size: 16px; color: var(--text-1); font-variant-numeric: tabular-nums; }
.bad { color: #cf222e; font-weight: 600; }
.lang-label { font-size: 12px; color: #6b5a4a; margin-bottom: var(--sp-xs); font-weight: 600; }

.lang-bar { display: flex; height: 12px; border-radius: var(--radius-sm); overflow: hidden; margin: var(--sp-xs) 0 var(--sp-md); background: #ebedf0; }
.lang-seg { height: 100%; transition: width .3s; }
.lang-legend { display: flex; flex-wrap: wrap; gap: var(--sp-sm) 18px; }
.lang-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.lang-item .dot { width: 10px; height: 10px; border-radius: 50%; }
.lang-name { color: #2c1810; }
.lang-pct { color: #2c1810; font-weight: 600; }
.lang-cnt { color: var(--text-3); }

@media (max-width: 768px) {
  .metric-cards { grid-template-columns: repeat(2, 1fr); }
}

/* ===== 并排卡片等高网格（CSS Grid item 默认 stretch，包装div拉满后卡片flex填满） ===== */
.eq-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--sp-lg); margin-bottom: var(--sp-lg); }
.eq-grid.cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.eq-grid .span-all { grid-column: 1 / -1; }
.eq-grid > div { display: flex; flex-direction: column; min-width: 0; }
.eq-grid .viz-card { flex: 1; margin-bottom: 0; display: flex; flex-direction: column; }
.eq-grid .viz-card :deep(.el-card__body) { flex: 1; }
.eq-grid .m-repo-card { flex: 1; box-sizing: border-box; margin-bottom: 0; }
@media (max-width: 1200px) {
  .eq-grid.cols-3 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 992px) {
  .eq-grid, .eq-grid.cols-3 { grid-template-columns: minmax(0, 1fr); }
}
</style>
