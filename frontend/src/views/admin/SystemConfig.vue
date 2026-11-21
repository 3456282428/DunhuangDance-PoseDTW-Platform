<template>
  <div>
    <PageHeader title="系统基础设置" subtitle="教室、学期、时间模板、机构信息、支付与通知" />

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ============ 教室管理 ============ -->
        <el-tab-pane label="教室管理" name="room">
          <div class="toolbar">
            <el-button type="primary" @click="openRoom()">新增教室</el-button>
          </div>
          <el-table :data="rooms" stripe v-loading="loading.room">
            <el-table-column prop="classroomId" label="编号" width="70" />
            <el-table-column prop="classroomName" label="教室名称" min-width="140" />
            <el-table-column prop="roomType" label="类型" width="140">
              <template #default="{ row }">{{ row.roomType || '—' }}</template>
            </el-table-column>
            <el-table-column prop="capacity" label="容量" width="90">
              <template #default="{ row }">{{ row.capacity || '—' }} 人</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '可用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" @click="openRoom(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeRoom(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ 学期管理 ============ -->
        <el-tab-pane label="学期管理" name="semester">
          <div class="toolbar">
            <el-button type="primary" @click="addSemester">新增学期</el-button>
            <el-button type="success" :loading="saving.semester" @click="saveSemesters">保存学期</el-button>
          </div>
          <el-table :data="semesters" stripe>
            <el-table-column label="学期名称" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="如：2026秋季" />
              </template>
            </el-table-column>
            <el-table-column label="开始日期" width="200">
              <template #default="{ row }">
                <el-date-picker v-model="row.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="结束日期" width="200">
              <template #default="{ row }">
                <el-date-picker v-model="row.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ $index }">
                <el-button size="small" type="danger" @click="semesters.splice($index,1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="tip">学期信息用于班级管理与课程安排。</p>
        </el-tab-pane>

        <!-- ============ 课节时间模板 ============ -->
        <el-tab-pane label="课节时间模板" name="slot">
          <div class="toolbar">
            <el-button type="primary" @click="addSlot">新增时间段</el-button>
            <el-button type="success" :loading="saving.slot" @click="saveSlots">保存模板</el-button>
          </div>
          <el-table :data="slots" stripe>
            <el-table-column label="模板名称" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="如：上午第一节" />
              </template>
            </el-table-column>
            <el-table-column label="开始时间" width="180">
              <template #default="{ row }">
                <el-time-picker v-model="row.startTime" value-format="HH:mm:ss" format="HH:mm" placeholder="开始" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="180">
              <template #default="{ row }">
                <el-time-picker v-model="row.endTime" value-format="HH:mm:ss" format="HH:mm" placeholder="结束" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ $index }">
                <el-button size="small" type="danger" @click="slots.splice($index,1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="tip">预设常用上课时间段，方便排课时快速选择。</p>
        </el-tab-pane>

        <!-- ============ 机构信息 ============ -->
        <el-tab-pane label="机构信息" name="org">
          <el-form :model="org" label-width="110px" style="max-width:680px">
            <el-form-item label="机构名称"><el-input v-model="org.name" /></el-form-item>
            <el-form-item label="品牌简称">
              <el-input v-model="org.shortName" placeholder="如：星海" maxlength="4" show-word-limit style="width:240px" />
              <span class="tip" style="margin-left:12px">未上传 Logo 时显示的占位字与侧边栏短名</span>
            </el-form-item>
            <el-form-item label="机构Logo">
              <el-upload :show-file-list="false" :http-request="uploadLogo" accept="image/*">
                <img v-if="org.logoUrl" :src="org.logoUrl" class="logo-preview" alt="logo" />
                <div v-else class="logo-upload">点击上传</div>
              </el-upload>
              <el-button v-if="org.logoUrl" link type="danger" @click="org.logoUrl=''">移除</el-button>
            </el-form-item>
            <el-form-item label="联系电话"><el-input v-model="org.phone" /></el-form-item>
            <el-form-item label="微信号"><el-input v-model="org.wechat" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="org.email" /></el-form-item>
            <el-form-item label="机构地址"><el-input v-model="org.address" /></el-form-item>
            <el-form-item label="机构简介">
              <el-input v-model="org.introduction" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving.org" @click="saveOrg">保存机构信息</el-button>
              <span class="tip" style="margin-left:12px">保存后同步到游客门户"关于我们"与学员端机构咨询。</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ============ 支付参数 ============ -->
        <el-tab-pane label="支付参数" name="pay">
          <el-form :model="pay" label-width="140px" style="max-width:680px">
            <el-form-item label="默认支付方式">
              <el-select v-model="pay.defaultMethod" style="width:240px">
                <el-option label="微信支付" value="微信" />
                <el-option label="支付宝" value="支付宝" />
                <el-option label="现金" value="现金" />
                <el-option label="刷卡" value="刷卡" />
              </el-select>
            </el-form-item>
            <el-form-item label="微信收款账户"><el-input v-model="pay.wechatAccount" placeholder="微信商户号/收款账号" /></el-form-item>
            <el-form-item label="支付宝账户"><el-input v-model="pay.alipayAccount" placeholder="支付宝商户号/收款账号" /></el-form-item>
            <el-form-item label="退款规则">
              <el-input v-model="pay.refundRule" type="textarea" :rows="3" placeholder="如：开课后按剩余课时比例退款" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving.pay" @click="savePay">保存支付参数</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ============ 通知设置 ============ -->
        <el-tab-pane label="通知设置" name="notify">
          <el-form :model="notify" label-width="160px" style="max-width:680px">
            <el-form-item label="上课提醒">
              <el-switch v-model="notify.classReminder" active-text="开课前自动提醒" />
            </el-form-item>
            <el-form-item label="续费提醒">
              <el-switch v-model="notify.renewalReminder" active-text="课时不足时提醒续费" />
            </el-form-item>
            <el-form-item label="考勤通知">
              <el-switch v-model="notify.attendanceNotify" active-text="考勤结果通知家长" />
            </el-form-item>
            <el-form-item label="短信渠道"><el-switch v-model="notify.smsChannel" active-text="启用短信" /></el-form-item>
            <el-form-item label="微信渠道"><el-switch v-model="notify.wechatChannel" active-text="启用微信模板消息" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving.notify" @click="saveNotify">保存通知设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ============ 主页内容 ============ -->
        <el-tab-pane label="主页内容" name="portal">
          <div style="max-width:820px">
            <h4>首页轮播图</h4>
            <p class="tip">上传多张图片，游客主页顶部自动轮播；不上传则显示默认渐变背景。建议尺寸 1920×600。</p>
            <div class="banner-list">
              <div v-for="(b,i) in portal.banners" :key="i" class="banner-item">
                <img :src="b" alt="banner" />
                <el-button size="small" type="danger" @click="portal.banners.splice(i,1)">删除</el-button>
              </div>
              <el-upload :show-file-list="false" :http-request="uploadBanner" accept="image/*">
                <div class="banner-add">+ 添加图片</div>
              </el-upload>
            </div>

            <el-divider />
            <h4>主视觉文案</h4>
            <el-form label-width="100px" style="margin-top:12px">
              <el-form-item label="主标题">
                <el-input v-model="portal.heroTitle" placeholder="如：发现孩子的艺术天赋" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="portal.heroSubtitle" placeholder="如：钢琴 · 舞蹈 · 美术 一站式艺术启蒙" />
              </el-form-item>
              <el-form-item label="主按钮文案">
                <el-input v-model="portal.btnBrowse" placeholder="如：浏览课程" style="width:220px" />
                <span class="tip" style="margin-left:12px">点击跳转到课程中心</span>
              </el-form-item>
              <el-form-item label="次按钮文案">
                <el-input v-model="portal.btnTrial" placeholder="如：免费试听" style="width:220px" />
                <span class="tip" style="margin-left:12px">点击打开预约试听</span>
              </el-form-item>
            </el-form>

            <el-divider />
            <h4>主视觉统计数字</h4>
            <p class="tip">主页大标题下方的三项数据，如 15+ 资深教师、500+ 在读学员。</p>
            <div v-for="(s,i) in portal.stats" :key="i" class="feature-row">
              <el-input v-model="s.num" placeholder="数字，如 15+" style="width:160px" />
              <el-input v-model="s.label" placeholder="标签，如：资深教师" style="width:220px" />
              <el-button type="danger" @click="portal.stats.splice(i,1)">删除</el-button>
            </div>
            <el-button size="small" @click="portal.stats.push({num:'',label:''})">新增一项</el-button>

            <el-divider />
            <h4>"关于我们"特色卡片</h4>
            <div v-for="(f,i) in portal.features" :key="i" class="feature-row">
              <el-input v-model="f.icon" placeholder="图标" style="width:90px" />
              <el-input v-model="f.title" placeholder="标题，如：专业师资" style="width:180px" />
              <el-input v-model="f.desc" placeholder="描述" />
              <el-button type="danger" @click="portal.features.splice(i,1)">删除</el-button>
            </div>
            <el-button size="small" @click="portal.features.push({icon:'✨',title:'',desc:''})">新增卡片</el-button>

            <div style="margin-top:20px">
              <el-button type="primary" :loading="saving.portal" @click="savePortal">保存主页内容</el-button>
              <span class="tip" style="margin-left:12px">保存后同步到游客浏览主页。</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 教室弹窗 -->
    <el-dialog v-model="roomDialog" :title="roomForm.classroomId ? '编辑教室' : '新增教室'" width="460px">
      <el-form :model="roomForm" label-width="90px">
        <el-form-item label="教室名称" required>
          <el-input v-model="roomForm.classroomName" placeholder="如：钢琴教室1" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="roomForm.roomType" placeholder="选择类型" allow-create filterable style="width:100%">
            <el-option label="钢琴教室" value="钢琴教室" />
            <el-option label="舞蹈教室" value="舞蹈教室" />
            <el-option label="美术教室" value="美术教室" />
            <el-option label="声乐教室" value="声乐教室" />
            <el-option label="综合教室" value="综合教室" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="roomForm.capacity" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roomForm.status" :active-value="1" :inactive-value="0" active-text="可用" inactive-text="停用" inline-prompt />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRoom">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getClassroomList, addClassroom, updateClassroom, deleteClassroom,
  getSystemConfig, saveSystemConfig
} from '@/api/admin'
import { uploadFiles } from '@/api/upload'
import { useBrandStore } from '@/store/brand'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const brandStore = useBrandStore()
const activeTab = ref('room')
const loading = reactive({ room: false })
const saving = reactive({ semester: false, slot: false, org: false, pay: false, notify: false, portal: false })

// ---------- 教室 ----------
const rooms = ref([])
const roomDialog = ref(false)
const emptyRoom = () => ({ classroomId: null, classroomName: '', roomType: '', capacity: 20, status: 1 })
const roomForm = ref(emptyRoom())

const fetchRooms = async () => {
  loading.room = true
  try { const res = await getClassroomList(); rooms.value = res.data || [] }
  finally { loading.room = false }
}
const openRoom = (row) => { roomForm.value = row ? { ...row } : emptyRoom(); roomDialog.value = true }
const submitRoom = async () => {
  if (!roomForm.value.classroomName) return ElMessage.warning('请输入教室名称')
  if (roomForm.value.classroomId) await updateClassroom(roomForm.value.classroomId, roomForm.value)
  else await addClassroom(roomForm.value)
  ElMessage.success('保存成功'); roomDialog.value = false; fetchRooms()
}
const removeRoom = async (row) => {
  await ElMessageBox.confirm(`确认删除教室「${row.classroomName}」吗？`, '提示', { type: 'warning' })
  await deleteClassroom(row.classroomId); ElMessage.success('删除成功'); fetchRooms()
}

// ---------- 学期 ----------
const semesters = ref([])
const addSemester = () => semesters.value.push({ name: '', startDate: '', endDate: '' })
const saveSemesters = async () => {
  if (semesters.value.some(s => !s.name)) return ElMessage.warning('学期名称不能为空')
  saving.semester = true
  try {
    await saveSystemConfig('semesters', semesters.value)
    ElMessage.success('学期已保存')
  } finally { saving.semester = false }
}

// ---------- 时间模板 ----------
const slots = ref([])
const addSlot = () => slots.value.push({ name: '', startTime: '08:00:00', endTime: '08:45:00' })
const saveSlots = async () => {
  if (slots.value.some(s => !s.name || !s.startTime)) return ElMessage.warning('请完善模板名称与时间')
  saving.slot = true
  try { await saveSystemConfig('time_template', { slots: slots.value }); ElMessage.success('时间模板已保存') }
  finally { saving.slot = false }
}

// ---------- 机构信息 ----------
const org = reactive({ name: '', shortName: '', logoUrl: '', phone: '', wechat: '', email: '', address: '', introduction: '' })
const saveOrg = async () => {
  saving.org = true
  try {
    await saveSystemConfig('organization', { ...org })
    await brandStore.refresh()
    ElMessage.success('机构信息已保存并同步到全站')
  }
  finally { saving.org = false }
}

// ---------- 支付参数 ----------
const pay = reactive({ defaultMethod: '微信', wechatAccount: '', alipayAccount: '', refundRule: '' })
const savePay = async () => {
  saving.pay = true
  try { await saveSystemConfig('payment', { ...pay }); ElMessage.success('支付参数已保存') }
  finally { saving.pay = false }
}

// ---------- 通知 ----------
const notify = reactive({ classReminder: true, renewalReminder: true, attendanceNotify: false, smsChannel: true, wechatChannel: false })
const saveNotify = async () => {
  saving.notify = true
  try { await saveSystemConfig('notify', { ...notify }); ElMessage.success('通知设置已保存') }
  finally { saving.notify = false }
}

// ---------- 主页内容（轮播/主视觉/特色卡片） ----------
const portal = reactive({
  banners: [],
  heroTitle: '',
  heroSubtitle: '',
  btnBrowse: '浏览课程',
  btnTrial: '免费试听',
  stats: [
    { num: '15+', label: '资深教师' },
    { num: '500+', label: '在读学员' },
    { num: '30+', label: '开设班级' }
  ],
  features: [
    { icon: '🎓', title: '专业师资', desc: '科班出身教师团队，多年一线教学经验' },
    { icon: '🏆', title: '考级指导', desc: '系统对接艺术考级与各类比赛' },
    { icon: '💡', title: '个性化教学', desc: '小班与一对一结合，因材施教' },
    { icon: '🏫', title: '环境优越', desc: '专业教室，安全舒适的学习氛围' }
  ]
})
const savePortal = async () => {
  saving.portal = true
  try { await saveSystemConfig('portal', { ...portal }); ElMessage.success('主页内容已保存并同步') }
  finally { saving.portal = false }
}
const uploadLogo = async ({ file }) => {
  const res = await uploadFiles([file])
  if (res.data?.[0]?.url) { org.logoUrl = res.data[0].url; ElMessage.success('Logo已上传，记得保存机构信息') }
}
const uploadBanner = async ({ file }) => {
  const res = await uploadFiles([file])
  if (res.data?.[0]?.url) { portal.banners.push(res.data[0].url); ElMessage.success('已添加，记得保存主页内容') }
}

onMounted(async () => {
  fetchRooms()
  const [sem, tpl, o, p, n, pc] = await Promise.all([
    getSystemConfig('semesters').catch(() => ({})),
    getSystemConfig('time_template').catch(() => ({})),
    getSystemConfig('organization').catch(() => ({})),
    getSystemConfig('payment').catch(() => ({})),
    getSystemConfig('notify').catch(() => ({})),
    getSystemConfig('portal').catch(() => ({}))
  ])
  // 兼容旧的字符串数组学期
  if (Array.isArray(sem.data)) semesters.value = sem.data.map(x => typeof x === 'string' ? { name: x, startDate: '', endDate: '' } : x)
  if (tpl.data?.slots) slots.value = tpl.data.slots
  if (o.data) Object.assign(org, { wechat: '', ...o.data })
  if (p.data) Object.assign(pay, p.data)
  if (n.data) Object.assign(notify, n.data)
  if (pc.data) Object.assign(portal, pc.data)
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.tip { color: #6b5a4a; font-size: 13px; margin-top: 10px; }
.logo-preview, .logo-upload { width: 80px; height: 80px; border-radius: 10px; border: 1px dashed #d4c5a9; object-fit: cover; }
.logo-upload { display: flex; align-items: center; justify-content: center; color: #6b5a4a; font-size: 13px; cursor: pointer; }
.banner-list { display: flex; flex-wrap: wrap; gap: 14px; }
.banner-item { position: relative; width: 220px; }
.banner-item img { width: 220px; height: 90px; object-fit: cover; border-radius: 8px; border: 1px solid #e4d9c3; }
.banner-item .el-button { position: absolute; top: 4px; right: 4px; }
.banner-add { width: 120px; height: 90px; border: 1px dashed #a89880; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #6b5a4a; cursor: pointer; }
.feature-row { display: flex; gap: 10px; margin-bottom: 10px; align-items: center; }
</style>
