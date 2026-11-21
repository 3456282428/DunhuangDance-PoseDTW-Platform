<template>
  <div class="dp-root" :class="{ 'dp-reduced': reducedMotion }">
    <!-- 收起后的恢复按钮 -->
    <button v-if="hidden" class="dp-restore" title="唤出桌宠" @click="restore">
      <img src="/deskpet/feitian/avatar.png" alt="唤出飞天桌宠" draggable="false" />
    </button>

    <template v-if="!hidden">
      <!-- 对话气泡（白底不透明，任意背景都清晰） -->
      <div v-show="bubble.show" class="dp-bubble" :style="bubbleStyle">{{ bubble.text }}</div>

      <!-- 桌宠本体：分部件SVG机器人，程序化骨骼动画 -->
      <div
        ref="petEl"
        class="dp-pet"
        :class="[`s-${state}`, { 'dp-dragging': dragging, 'dp-moving': moving }]"
        :style="petStyle"
        @pointerdown="onDown"
        @pointermove="onMove"
        @pointerup="onUp"
        @pointercancel="onUp"
        @contextmenu.prevent="openMenu"
      >
        <div class="dp-sprite" aria-hidden="true">
          <img class="dp-sprite-img" :src="spriteSrc" alt="飞天桌宠" draggable="false" />
          <div class="dp-shadow"></div>
        </div>
      </div>

      <!-- 右键菜单 -->
      <div v-if="menu.show" class="dp-menu" :style="menuStyle">
        <div v-for="m in MENU_ITEMS" :key="m.act" class="dp-menu-item" @click="menuAct(m.act)">
          <span class="dp-menu-ico">{{ m.icon }}</span>{{ m.label }}
        </div>
      </div>

      <!-- AI 聊天面板 -->
      <div v-if="chatOpen" class="dp-chat" :style="chatStyle">
        <div class="dp-chat-head">
          <span class="dp-chat-avatar">
            <img src="/deskpet/feitian/avatar.png" alt="飞天助手" draggable="false" />
          </span>
          <div class="dp-chat-title">
            <b>{{ persona.name }} · AI助手</b>
            <i>{{ sessionId ? (currentTitle || '当前对话') : '新对话' }}</i>
          </div>
          <span class="dp-chat-ico" title="新建对话" :class="{ disabled: pending }" @click.stop="newChat">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 5v14M5 12h14" />
            </svg>
          </span>
          <span class="dp-chat-ico" title="历史对话" :class="{ active: histOpen }" @click.stop="toggleHist">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 4v4h4" /><path d="M12 8v4l3 2" />
            </svg>
          </span>
          <span class="dp-chat-ico" title="收起" @click.stop="chatOpen = false">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </span>
        </div>

        <!-- 历史会话列表面板 -->
        <div v-if="histOpen" class="dp-hist">
          <div class="dp-hist-head">
            <span>历史对话（{{ sessions.length }}）</span>
          </div>
          <div class="dp-hist-list">
            <div v-if="!sessions.length" class="dp-hist-empty">暂无历史对话，发出第一条消息后自动保存</div>
            <div v-for="s in sessions" :key="s.sessionId"
                 class="dp-hist-item" :class="{ on: s.sessionId === sessionId }"
                 @click="switchSession(s)">
              <div class="dp-hist-info">
                <span class="dp-hist-title">{{ s.title || '未命名对话' }}</span>
                <span class="dp-hist-meta">{{ s.msgCount || 0 }} 条 · {{ histTime(s.updateTime) }}</span>
              </div>
              <span class="dp-hist-del" :class="{ armed: armedDelId === s.sessionId }"
                    :title="armedDelId === s.sessionId ? '再点一次确认删除' : '删除对话'"
                    @click.stop="removeSession(s)">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" />
                </svg>
              </span>
            </div>
          </div>
        </div>
        <div ref="msgListEl" class="dp-chat-msgs">
          <div class="dp-chat-welcome">
            <span>{{ persona.chatWelcome }}</span>
            <div v-if="!messages.length" class="dp-chips">
              <button v-for="q in persona.examples" :key="q" class="dp-chip" @click="quickAsk(q)">{{ q }}</button>
            </div>
          </div>
          <div v-for="(msg, i) in messages" :key="i" class="dp-row" :class="msg.role === 'user' ? 'is-user' : 'is-pet'">
            <span class="dp-ava" :class="msg.role === 'user' ? 'is-user' : 'is-pet'">
              <img v-if="msg.role !== 'user'" src="/deskpet/feitian/avatar.png" alt="飞天" draggable="false" />
              <svg v-else viewBox="0 0 24 24" width="13" height="13" fill="currentColor">
                <circle cx="12" cy="8" r="4" />
                <path d="M4 20c0-4 3.6-6 8-6s8 2 8 6v1H4z" />
              </svg>
            </span>
            <div class="dp-col">
              <div class="dp-msg" :class="msg.role === 'user' ? 'is-user' : 'is-pet'">
                <span v-if="msg.role === 'user'">{{ msg.text }}</span>
                <span v-else class="dp-md" v-html="mdLite(msg.text)"></span>
                <span v-if="msg.typing" class="dp-caret"></span>
              </div>
              <div v-if="msg.steps && msg.steps.length" class="dp-steps">
                <span v-for="(st, si) in msg.steps" :key="si" class="dp-step" :class="st.success ? 'ok' : 'fail'">
                  🔧 {{ st.tool }} {{ st.success ? '✓' : '✗' }}
                </span>
              </div>
              <div v-if="msg.confirm" class="dp-confirm">
                <button class="dp-cf-btn primary" :disabled="pending" @click="doConfirm(true, msg)">确认执行</button>
                <button class="dp-cf-btn" :disabled="pending" @click="doConfirm(false, msg)">取消</button>
              </div>
              <i v-if="msg.time" class="dp-time">{{ msg.time }}</i>
            </div>
          </div>
          <div v-if="thinking" class="dp-row is-pet">
            <span class="dp-ava is-pet"><img src="/deskpet/feitian/avatar.png" alt="飞天" draggable="false" /></span>
            <div class="dp-msg is-pet dp-typing"><i></i><i></i><i></i></div>
          </div>
        </div>
        <div class="dp-chat-foot">
          <input
            ref="chatInputEl"
            v-model="chatInput"
            class="dp-chat-input"
            :placeholder="`说点什么，或输入动作（如「跳舞」「挥手」）`"
            maxlength="500"
            @keyup.enter="sendChat"
          />
          <button class="dp-chat-send" :disabled="pending" @click="sendChat">{{ pending ? '···' : '发送' }}</button>
        </div>
        <div class="dp-chat-hint">Enter 发送 · 回答优先依据知识库，仅供参考</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  agentChat, agentConfirm, listAgentSessions,
  getAgentMessages, deleteAgentSession
} from '@/api/agent'

const props = defineProps({
  /** 挂载端别：ADMIN / TEACHER / STUDENT，决定人设与AI检索范围 */
  scope: { type: String, required: true }
})

/* ---------------- 人设（借鉴参考项目"按性格差异化反应"） ---------------- */
const PERSONAS = {
  ADMIN: {
    name: '程程',
    greetings: [
      '您好，我是运营管家·程程，系统运行正常。',
      '数据看板已就绪，运营问题随时问我。',
      '嗨，我是程程，今天的数据我来帮您盯。'
    ],
    clickLines: {
      jump: ['收到指令，活力充满！', '突发情况？程程已就位。'],
      wave: ['您好，程程随时为您服务。', '敬礼！系统一切正常。'],
      dance: ['庄重归庄重，偶尔也要活动一下。', '年会彩排，先跳为敬。'],
      spin: ['原地盘点一圈，井然有序。']
    },
    actionReply: {
      dance: '好，为本周业绩跳一支庆功舞！',
      wave: '挥手致意，各位同事辛苦了~',
      jump: '业绩长虹，一跃冲天！',
      walk: '程程去巡场了，稍候回来。',
      spin: '转一圈，检查各模块运转正常。'
    },
    chatWelcome: '您好，我是运营管家·程程。机构运营、系统使用的问题都可以问我，我会优先依据知识库回答。',
    examples: ['本月新增了多少学员', '本月营收情况怎么样', '学员请假需要什么手续', '系统近期接口调用情况']
  },
  TEACHER: {
    name: '助助',
    greetings: [
      '老师好呀！我是教学助手·助助~',
      '批改作业辛苦啦，AI评分进度我来帮您盯！',
      '今天也要元气满满地教学哦~'
    ],
    clickLines: {
      jump: ['耶！助助能量满格！', '跳高高~为课堂打气！'],
      wave: ['老师好~挥手挥手！', '欢迎回来，想您啦！'],
      dance: ['助助最擅长课间操啦！', '来一段热身舞吧！'],
      spin: ['转个圈圈，把好运转给您~']
    },
    actionReply: {
      dance: '看好了，助助的课间操来啦！',
      wave: '老师好呀，挥手致意~',
      jump: '蹦蹦跳跳，课堂气氛搞起来！',
      walk: '助助去教室转一圈，马上回来~',
      spin: '转圈圈~晕乎乎但很开心！'
    },
    chatWelcome: '老师好，我是助教·助助~ 作业、排班、学员的问题都可以问我，我会优先查教学知识库回答。',
    examples: ['我本周有哪些课', '有哪些作业需要批改', '学员作业提交情况', '舞蹈教学法要点']
  },
  STUDENT: {
    name: '舞舞',
    greetings: [
      '嗨嗨！我是你的学舞伙伴·舞舞！',
      '今天也要开开心心练舞哦！',
      '想你啦！我们一起来跳舞吧~'
    ],
    clickLines: {
      jump: ['看我的空中转体！', '蹦跶蹦跶，练舞去咯！'],
      wave: ['嗨~要一起练舞吗？', '挥挥小手，向你问好！'],
      dance: ['舞舞的拿手绝活来啦！', '跟着节奏摇摆~'],
      spin: ['单脚旋转~像芭蕾那样！']
    },
    actionReply: {
      dance: '跟上节奏，一二三四！',
      wave: '嗨嗨~舞舞向你挥手！',
      jump: '跳起来！像跳完一支舞那样开心！',
      walk: '舞舞先走两步热热身~',
      spin: '转圈圈~晕乎乎但很开心！'
    },
    chatWelcome: '嗨，我是舞舞！作业、课表、舞蹈练习的问题都可以问我哦，我会优先翻学习知识库~',
    examples: ['我有哪些作业还没交', '我本周的课程安排', '舞蹈基本功怎么练', '我的出勤情况']
  }
}
const persona = PERSONAS[props.scope] || PERSONAS.STUDENT

/* ---------------- 动作菜单 ---------------- */
const MENU_ITEMS = [
  { act: 'chat', icon: '💬', label: 'AI 聊天' },
  { act: 'walk', icon: '🚶', label: '走一走' },
  { act: 'wave', icon: '👋', label: '挥手' },
  { act: 'dance', icon: '💃', label: '跳舞' },
  { act: 'jump', icon: '🤸', label: '跳跃' },
  { act: 'spin', icon: '🌀', label: '转个圈' },
  { act: 'hide', icon: '🫥', label: '收起桌宠' }
]

/* ---------------- 状态机与定位 ---------------- */
const ACTION_DUR = { wave: 2000, dance: 2600, jump: 850, spin: 1050 }
const state = ref('idle')          // idle/walk/wave/dance/jump/spin/think/talk/drag
const dragging = ref(false)
const moving = ref(false)
const hidden = ref(false)
const reducedMotion = typeof window !== 'undefined' &&
  window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches

const pos = reactive({ x: window.innerWidth - 210, y: window.innerHeight - 230 })
const petEl = ref(null)

/* ---------------- 飞天精灵帧映射（AI 生成敦煌飞天序列） ---------------- */
const SPRITE_BASE = '/deskpet/feitian/'
const stateSpriteMap = {
  idle: 'idle_1.png',
  walk: 'idle_1.png',      // 行走用 idle + CSS 位移动画
  wave: 'happy_1.png',     // 挥手 → 开心举手
  dance: 'happy_1.png',    // 跳舞 → 开心
  jump: 'happy_1.png',     // 跳跃 → 开心
  spin: 'happy_1.png',     // 转圈 → 开心
  think: 'work_1.png',     // 思考 → 工作书写
  talk: 'love_1.png',      // 说话 → 爱心互动
  drag: 'sad_1.png',       // 被拖拽 → 委屈
  sleep: 'sleep_1.png',    // 睡觉
}
const spriteSrc = computed(() => SPRITE_BASE + (stateSpriteMap[state.value] || stateSpriteMap.idle))

const lsKey = (suffix) => `deskpet.${props.scope}.${suffix}`
const lsGet = (k, fallback) => { try { const v = localStorage.getItem(k); return v ? JSON.parse(v) : fallback } catch { return fallback } }
const lsSet = (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)) } catch { /* ignore */ } }

const clampPos = (x, y) => ({
  x: Math.min(Math.max(x, 8), window.innerWidth - 148),
  y: Math.min(Math.max(y, 8), window.innerHeight - 188)
})

const petStyle = computed(() => ({
  left: pos.x + 'px',
  top: pos.y + 'px',
  transition: moving.value ? 'left 1.5s linear, top 1.5s linear' : 'none'
}))

/* ---------------- 计时器统一管理（卸载全清理，页面隐藏暂停调度） ---------------- */
const timers = new Set()
const later = (fn, ms) => {
  const id = setTimeout(() => { timers.delete(id); fn() }, ms)
  timers.add(id)
  return id
}
const clearT = (id) => { clearTimeout(id); clearInterval(id); timers.delete(id) }

/* ---------------- 气泡（自动关闭，同参考项目 _bubble_timer） ---------------- */
const bubble = reactive({ show: false, text: '' })
let bubbleTimer = 0
const showBubble = (text, ms = 3600) => {
  bubble.text = text
  bubble.show = true
  clearT(bubbleTimer)
  bubbleTimer = later(() => { bubble.show = false }, ms)
}
const bubbleStyle = computed(() => {
  const x = Math.min(Math.max(pos.x + 62 - 110, 8), window.innerWidth - 236)
  return { left: x + 'px', top: (pos.y - 14) + 'px', transform: 'translateY(-100%)' }
})

/* ---------------- 动作触发 ---------------- */
let actionTimer = 0
const doAction = (name) => {
  if (!ACTION_DUR[name]) return
  stopWalk()
  clearT(actionTimer)
  state.value = name
  actionTimer = later(() => { state.value = 'idle'; scheduleIdle() }, ACTION_DUR[name])
}

/* 单击差异化反应（同人设绑定） */
const pick = (arr) => arr[Math.floor(Math.random() * arr.length)]
const clickReact = () => {
  const r = Math.random()
  const act = r < 0.35 ? 'jump' : r < 0.6 ? 'wave' : r < 0.8 ? 'spin' : 'dance'
  const line = persona.clickLines[act]
  showBubble(line ? pick(line) : persona.name + '很高兴被你点到~')
  doAction(act)
}

/* ---------------- 走动（CSS transition 平滑位移） ---------------- */
let walkTimer = 0
const walkTo = (tx, ty) => {
  stopWalk()
  const t = clampPos(tx ?? (8 + Math.random() * (window.innerWidth - 160)), ty ?? pos.y)
  const dist = Math.abs(t.x - pos.x)
  if (dist < 40) { scheduleIdle(); return }
  moving.value = true
  state.value = 'walk'
  pos.x = t.x
  pos.y = t.y
  walkTimer = later(() => {
    moving.value = false
    if (state.value === 'walk') state.value = 'idle'
    lsSet(lsKey('pos'), { x: pos.x, y: pos.y })
    scheduleIdle()
  }, 1550)
}
const stopWalk = () => {
  clearT(walkTimer)
  moving.value = false
  if (state.value === 'walk') state.value = 'idle'
}

/* ---------------- 空闲活动调度（随机走动/做动作，同参考项目活动调度器） ---------------- */
let idleTimer = 0
const scheduleIdle = () => {
  clearT(idleTimer)
  idleTimer = later(() => {
    if (document.hidden || chatOpen.value || hidden.value) { scheduleIdle(); return }
    const r = Math.random()
    if (r < 0.5) walkTo()
    else if (r < 0.72) { showBubble(pick(persona.greetings), 3000); doAction('wave') }
    else if (r < 0.86) doAction('dance')
    else if (r < 0.94) doAction('spin')
    else doAction('jump')
  }, 12000 + Math.random() * 9000)
}

/* ---------------- 拖拽（Pointer事件，含单击判定） ---------------- */
let dragStart = null
let moved = false
let downTime = 0
const onDown = (e) => {
  if (e.button !== 0) return
  closeMenu()
  dragStart = { px: e.clientX, py: e.clientY, ox: pos.x, oy: pos.y }
  moved = false
  downTime = Date.now()
  dragging.value = true
  try { petEl.value?.setPointerCapture?.(e.pointerId) } catch { /* 合成事件/无效指针时忽略，拖拽仍可进行 */ }
}
const onMove = (e) => {
  if (!dragging.value || !dragStart) return
  const dx = e.clientX - dragStart.px
  const dy = e.clientY - dragStart.py
  if (!moved && Math.hypot(dx, dy) > 5) { moved = true; state.value = 'drag' }
  if (moved) {
    const t = clampPos(dragStart.ox + dx, dragStart.oy + dy)
    pos.x = t.x
    pos.y = t.y
  }
}
const onUp = () => {
  if (!dragging.value) return
  dragging.value = false
  dragStart = null
  if (moved) {
    if (state.value === 'drag') state.value = 'idle'
    lsSet(lsKey('pos'), { x: pos.x, y: pos.y })
  } else if (Date.now() - downTime < 500 && state.value !== 'think' && state.value !== 'talk') {
    clickReact()
  }
}

/* ---------------- 右键菜单 ---------------- */
const menu = reactive({ show: false, x: 0, y: 0 })
const menuStyle = computed(() => ({
  left: Math.min(menu.x, window.innerWidth - 150) + 'px',
  top: Math.min(menu.y, window.innerHeight - 250) + 'px'
}))
const openMenu = (e) => { menu.x = e.clientX; menu.y = e.clientY; menu.show = true }
const closeMenu = () => { menu.show = false }
const onDocClick = () => closeMenu()
const menuAct = (act) => {
  closeMenu()
  if (act === 'chat') return openChat()
  if (act === 'hide') {
    hidden.value = true
    lsSet(lsKey('hidden'), true)
    return
  }
  if (act === 'walk') return walkTo()
  doAction(act)
}
const restore = () => {
  hidden.value = false
  lsSet(lsKey('hidden'), false)
  showBubble(pick(persona.greetings), 3600)
}

/* ---------------- AI 聊天（Agent 多会话：/api/agent/chat，DeepSeek + 工具闭环） ---------------- */
const chatOpen = ref(false)
const chatInput = ref('')
const chatInputEl = ref(null)
const messages = ref([])
const pending = ref(false)
const thinking = ref(false)
const msgListEl = ref(null)
let typeTimer = 0

/* 多会话状态：当前会话ID、会话列表、历史面板、待确认删除项 */
const sessionId = ref(null)
const sessions = ref([])
const histOpen = ref(false)
const armedDelId = ref(null)
const chatInited = ref(false)
let delArmedTimer = 0
const currentTitle = computed(() => {
  const cur = sessions.value.find((s) => s.sessionId === sessionId.value)
  return cur ? (cur.title || '未命名对话') : ''
})

const chatStyle = computed(() => {
  const w = 340
  const h = 500
  const x = Math.min(Math.max(pos.x + 62 - w / 2, 8), window.innerWidth - w - 8)
  let y = pos.y - h - 16
  if (y < 8) y = Math.min(pos.y + 150, window.innerHeight - h - 8)
  return { left: x + 'px', top: Math.max(y, 8) + 'px' }
})

const scrollBottom = () => nextTick(() => {
  if (msgListEl.value) msgListEl.value.scrollTop = msgListEl.value.scrollHeight
})

/* 消息时间（HH:mm） */
const fmtTime = (d) => {
  const p = (n) => String(n).padStart(2, '0')
  return p(d.getHours()) + ':' + p(d.getMinutes())
}

/* 后端 LocalDateTime 容错解析（ISO串/数组均可），返回 HH:mm 或 MM-dd HH:mm */
const parseParts = (v) => {
  if (!v) return null
  const m = String(v).match(/(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/)
  if (m) return m
  if (Array.isArray(v) && v.length >= 5) return [null, v[0], v[1], v[2], v[3], v[4]]
  return null
}
const msgTime = (v) => {
  const m = parseParts(v)
  return m ? `${String(m[4]).padStart(2, '0')}:${String(m[5]).padStart(2, '0')}` : ''
}
const histTime = (v) => {
  const m = parseParts(v)
  return m ? `${String(m[2]).padStart(2, '0')}-${String(m[3]).padStart(2, '0')} ${String(m[4]).padStart(2, '0')}:${String(m[5]).padStart(2, '0')}` : ''
}

/* 轻量 Markdown：先转义再行内加粗/代码，行级识别序号列表与空行；
   打字机逐字推进时未闭合的 ** 不匹配替换，按原文显示，天然容错 */
const mdLite = (raw) => {
  if (!raw) return ''
  return String(raw).split('\n').map((line) => {
    const esc = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    const body = esc.replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>').replace(/`([^`]+)`/g, '<code>$1</code>')
    if (!line.trim()) return '<div class="md-gap"></div>'
    const li = /^(\s*)(\d+[.、)]|[·•\-]|[一二三四五六七八九十]+[、.])/.test(line) ? ' md-li' : ''
    return `<div class="md-ln${li}">${body || '&nbsp;'}</div>`
  }).join('')
}

const openChat = async () => {
  chatOpen.value = true
  await loadSessions()
  // 首次打开自动回填最近一次会话；新建对话后不再自动覆盖
  if (!chatInited.value) {
    chatInited.value = true
    const latest = sessions.value[0]
    if (latest) {
      try {
        const res = await getAgentMessages(latest.sessionId)
        sessionId.value = latest.sessionId
        messages.value = (res.data || []).map((r) => ({
          role: r.role === 'user' ? 'user' : 'pet',
          text: r.content || '',
          time: msgTime(r.createTime)
        }))
      } catch { /* 未登录或加载失败保持空白草稿态 */ }
    }
  }
  scrollBottom()
  nextTick(() => { if (chatInputEl.value) chatInputEl.value.focus() })
}

/* ---------------- 多会话管理（新建 / 列表 / 切换 / 删除，后端持久化） ---------------- */

const loadSessions = async () => {
  try {
    const res = await listAgentSessions()
    sessions.value = res.data || []
  } catch { /* 列表加载失败静默，不阻断聊天 */ }
}

/** 新建对话：本地立即切空白草稿，首条消息发送时后端自动建会话并生成标题 */
const newChat = () => {
  if (pending.value) return
  sessionId.value = null
  messages.value = []
  histOpen.value = false
  nextTick(() => { if (chatInputEl.value) chatInputEl.value.focus() })
}

const toggleHist = () => {
  histOpen.value = !histOpen.value
  if (histOpen.value) loadSessions()
}

/** 切换历史会话：拉取消息记录回填 */
const switchSession = async (s) => {
  if (pending.value || s.sessionId === sessionId.value) { histOpen.value = false; return }
  try {
    const res = await getAgentMessages(s.sessionId)
    const rows = res.data || []
    messages.value = rows.map((r) => ({
      role: r.role === 'user' ? 'user' : 'pet',
      text: r.content || '',
      time: msgTime(r.createTime)
    }))
    sessionId.value = s.sessionId
    histOpen.value = false
    scrollBottom()
  } catch {
    ElMessage.warning('历史消息加载失败，请稍后重试')
  }
}

/** 删除对话：内联二次点击确认（3秒内再点生效，无阻塞弹窗） */
const removeSession = (s) => {
  if (pending.value) return
  if (armedDelId.value !== s.sessionId) {
    armedDelId.value = s.sessionId
    clearT(delArmedTimer)
    delArmedTimer = later(() => { armedDelId.value = null }, 3000)
    return
  }
  clearT(delArmedTimer)
  armedDelId.value = null
  deleteAgentSession(s.sessionId).then(() => {
    sessions.value = sessions.value.filter((x) => x.sessionId !== s.sessionId)
    if (s.sessionId === sessionId.value) {
      sessionId.value = null
      messages.value = []
    }
    ElMessage.success('对话已删除')
  }).catch(() => ElMessage.error('删除失败，请稍后重试'))
}

/* 示例问题一键提问 */
const quickAsk = (q) => {
  if (pending.value) return
  chatInput.value = q
  sendChat()
}

/* 动作短词识别（借鉴参考项目 _looks_like_action：短词+关键词直接本地触发动画） */
const ACTION_WORDS = [
  { act: 'dance', re: /跳舞|舞一段|跳支舞|扭一扭|摇摆|蹦迪/ },
  { act: 'wave', re: /挥手|招手|打招呼|敬礼|再见/ },
  { act: 'walk', re: /走一走|散步|走两步|巡逻|走动/ },
  { act: 'spin', re: /转圈|旋转|转一圈/ },
  { act: 'jump', re: /跳跃|跳高|蹦一蹦|蹦跳/ }
]
const matchAction = (text) => {
  for (const w of ACTION_WORDS) if (w.re.test(text)) return w.act
  return null
}

/** 渲染 AI 回复：打字机输出 + 工具步骤 + 敏感操作确认门控 */
const renderPetReply = (content, opts = {}) => {
  thinking.value = false
  const reply = {
    role: 'pet', text: '', typing: true, time: fmtTime(new Date()),
    steps: opts.steps && opts.steps.length ? opts.steps : undefined,
    confirm: opts.confirm || undefined
  }
  messages.value.push(reply)
  const idx = messages.value.length - 1
  state.value = 'talk'
  clearT(typeTimer)
  let i = 0
  typeTimer = later(function tick() {
    i = Math.min(i + 2, content.length)
    messages.value[idx].text = content.slice(0, i)
    scrollBottom()
    if (i < content.length) {
      typeTimer = later(tick, 24)
    } else {
      delete messages.value[idx].typing
      state.value = 'idle'
      pending.value = false
      const replyAct = matchAction(content)
      if (replyAct) {
        if (replyAct === 'walk') walkTo()
        else doAction(replyAct)
      } else scheduleIdle()
    }
  }, 24)
  if (!chatOpen.value) showBubble(content.slice(0, 42) + (content.length > 42 ? '…' : ''), 5000)
}

const sendChat = async () => {
  const text = chatInput.value.trim()
  if (!text || pending.value) return
  chatInput.value = ''
  closeMenu()
  messages.value.push({ role: 'user', text, time: fmtTime(new Date()) })
  scrollBottom()

  // 动作短词：本地触发动画，不调用AI（同参考项目交互约定）
  const act = text.length <= 10 ? matchAction(text) : null
  if (act) {
    messages.value.push({ role: 'pet', text: persona.actionReply[act] || '看我的！', time: fmtTime(new Date()) })
    scrollBottom()
    if (act === 'walk') walkTo()
    else doAction(act)
    return
  }

  pending.value = true
  thinking.value = true
  state.value = 'think'
  try {
    const res = await agentChat({ sessionId: sessionId.value, message: text })
    const d = res.data || {}
    if (d.sessionId && d.sessionId !== sessionId.value) {
      sessionId.value = d.sessionId
      loadSessions()
    }
    const content = d.content || '（我没有组织好语言，请换个问法试试~）'
    renderPetReply(content, {
      steps: d.steps,
      confirm: d.type === 'confirm' && d.pendingAction ? d.pendingAction : null
    })
  } catch {
    thinking.value = false
    state.value = 'idle'
    pending.value = false
    messages.value.push({ role: 'pet', text: '（网络好像开小差了，稍后再试一次吧~）', time: fmtTime(new Date()) })
    scrollBottom()
  }
}

/** 敏感操作确认/拒绝（后端二次校验归属与权限） */
const doConfirm = async (approve, msg) => {
  if (pending.value || !msg.confirm) return
  pending.value = true
  try {
    const res = await agentConfirm({ sessionId: sessionId.value, actionId: msg.confirm.actionId, approve })
    delete msg.confirm
    const d = res.data || {}
    renderPetReply(d.content || (approve ? '已执行完成。' : '已取消操作。'), { steps: d.steps })
    loadSessions()
  } catch {
    pending.value = false
    ElMessage.error('确认请求失败，请稍后重试')
  }
}

/* ---------------- 生命周期 ---------------- */
const onVisChange = () => {
  if (document.hidden) { clearT(idleTimer); clearT(walkTimer) }
  else if (!hidden.value) scheduleIdle()
}
const onWinResize = () => {
  const t = clampPos(pos.x, pos.y)
  pos.x = t.x
  pos.y = t.y
}

onMounted(() => {
  const savedPos = lsGet(lsKey('pos'), null)
  if (savedPos) Object.assign(pos, clampPos(savedPos.x, savedPos.y))
  // 各端默认落位：管理端右下、教师端左下、学生端右下（避开各自高频操作区）
  else if (props.scope === 'TEACHER') Object.assign(pos, { x: 230, y: window.innerHeight - 230 })
  hidden.value = !!lsGet(lsKey('hidden'), false)
  document.addEventListener('click', onDocClick)
  document.addEventListener('visibilitychange', onVisChange)
  window.addEventListener('resize', onWinResize)
  if (!hidden.value) {
    later(() => {
      showBubble(pick(persona.greetings), 4200)
      if (Math.random() < 0.6 && !reducedMotion) doAction('wave')
    }, 900)
    scheduleIdle()
  }
})

onBeforeUnmount(() => {
  timers.forEach((id) => { clearTimeout(id); clearInterval(id) })
  timers.clear()
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('visibilitychange', onVisChange)
  window.removeEventListener('resize', onWinResize)
})
</script>

<style scoped>
/* ============ 容器与本体 ============ */
.dp-restore {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 4000;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 2px solid #d63a2b;
  background: #fff;
  box-shadow: 0 4px 12px rgba(214, 58, 43, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s;
}
.dp-restore:hover { transform: scale(1.1) rotate(-8deg); }
.dp-restore img { width: 100%; height: 100%; object-fit: cover; border-radius: 50%; pointer-events: none; }

.dp-pet {
  position: fixed;
  z-index: 4000;
  width: 138px;
  cursor: grab;
  user-select: none;
  touch-action: none;
  filter: drop-shadow(0 8px 12px rgba(20, 24, 32, 0.22));
  will-change: left, top;
}
.dp-pet.dp-dragging { cursor: grabbing; }

/* ============ 飞天精灵（PNG 序列帧 + CSS 整体动画） ============ */
.dp-sprite { position: relative; display: block; }
.dp-sprite-img {
  display: block;
  width: 100%;
  height: auto;
  pointer-events: none;
  user-select: none;
  filter: drop-shadow(0 6px 16px rgba(166, 42, 32, 0.18));
}
.dp-shadow {
  position: absolute;
  left: 50%;
  bottom: -6px;
  width: 70%;
  height: 10px;
  margin-left: -35%;
  background: radial-gradient(ellipse at center, rgba(20, 24, 32, 0.14) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

/* 空闲：呼吸浮动 */
.s-idle .dp-sprite-img { animation: dp-bob 3.4s ease-in-out infinite; }
.s-idle .dp-shadow { animation: dp-shadow-pulse 3.4s ease-in-out infinite; }

/* 行走：摇摆 */
.s-walk .dp-sprite-img { animation: dp-waddle 0.5s ease-in-out infinite alternate; }

/* 挥手/跳舞/跳跃/转圈：整体变换 */
.s-wave .dp-sprite-img { animation: dp-sprite-wave 2s ease-in-out; }
.s-dance .dp-sprite-img { animation: dp-dance 1.2s ease-in-out 2; }
.s-jump .dp-sprite-img { animation: dp-jump 0.85s cubic-bezier(0.36, 0.07, 0.19, 0.97); }
.s-spin .dp-sprite-img { animation: dp-spin 1.05s ease-in-out; }

/* 思考/说话：轻微浮动 */
.s-think .dp-sprite-img { animation: dp-think 2s ease-in-out infinite; }
.s-talk .dp-sprite-img { animation: dp-bob 2s ease-in-out infinite; }

/* 被拖拽：委屈下垂 */
.s-drag .dp-sprite-img { animation: dp-drag-sag 0.8s ease-in-out infinite alternate; }

/* 睡觉：缓慢呼吸 */
.s-sleep .dp-sprite-img { animation: dp-breathe 3.4s ease-in-out infinite; }
.s-sleep .dp-shadow { animation: dp-shadow-pulse 3.4s ease-in-out infinite; }

@keyframes dp-bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3.5px); }
}
@keyframes dp-breathe {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(1.025); }
}
@keyframes dp-shadow-pulse {
  0%, 100% { transform: scaleX(1); opacity: 0.14; }
  50% { transform: scaleX(0.92); opacity: 0.1; }
}
@keyframes dp-sprite-wave {
  0% { transform: rotate(0); }
  25% { transform: rotate(-8deg) translateY(-4px); }
  50% { transform: rotate(4deg); }
  75% { transform: rotate(-6deg) translateY(-2px); }
  100% { transform: rotate(0); }
}
@keyframes dp-drag-sag {
  from { transform: translateY(0) scaleY(1); opacity: 0.85; }
  to { transform: translateY(3px) scaleY(0.96); opacity: 0.7; }
}
@keyframes dp-blink {
  0%, 91%, 100% { transform: scaleY(1); }
  94% { transform: scaleY(0.08); }
  97% { transform: scaleY(1); }
}
@keyframes dp-waddle {
  from { transform: rotate(-3deg); }
  to { transform: rotate(3deg); }
}
@keyframes dp-roll {
  from { transform: translateX(0); }
  to { transform: translateX(-8px); }
}
@keyframes dp-wave {
  0% { transform: rotate(-12deg); }
  22% { transform: rotate(-150deg); }
  38% { transform: rotate(-122deg); }
  54% { transform: rotate(-150deg); }
  70% { transform: rotate(-126deg); }
  86% { transform: rotate(-150deg); }
  100% { transform: rotate(-12deg); }
}
@keyframes dp-dance {
  0%, 100% { transform: translateY(0) rotate(0); }
  25% { transform: translateY(-9px) rotate(-7deg); }
  50% { transform: translateY(0) rotate(0); }
  75% { transform: translateY(-9px) rotate(7deg); }
}
@keyframes dp-dance-arm-l {
  from { transform: rotate(12deg); }
  to { transform: rotate(-120deg); }
}
@keyframes dp-dance-arm-r {
  from { transform: rotate(-12deg); }
  to { transform: rotate(120deg); }
}
@keyframes dp-jump {
  0% { transform: translateY(0) scale(1, 1); }
  18% { transform: translateY(4px) scale(1.08, 0.9); }
  50% { transform: translateY(-42px) scale(0.96, 1.06); }
  80% { transform: translateY(0) scale(1, 1); }
  90% { transform: translateY(2px) scale(1.06, 0.93); }
  100% { transform: translateY(0) scale(1, 1); }
}
@keyframes dp-spin {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}
@keyframes dp-think {
  0%, 100% { transform: translateY(-2.5px); }
  50% { transform: translateY(-4px); }
}
@keyframes dp-talk {
  from { opacity: 0.2; }
  to { opacity: 0.5; }
}

/* 减弱动态偏好：关闭全部动画 */
.dp-reduced .dp-pet *,
.dp-reduced .dp-pet { animation: none !important; }

/* ============ 气泡 ============ */
.dp-bubble {
  position: fixed;
  z-index: 4010;
  max-width: 224px;
  padding: 9px 12px;
  background: #ffffff;
  border: 1px solid #e3e6ec;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(23, 28, 38, 0.14);
  font-size: 12.5px;
  line-height: 1.55;
  color: #24292f;
  pointer-events: none;
}
.dp-bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -5px;
  width: 9px;
  height: 9px;
  margin-left: -5px;
  background: #fff;
  border-right: 1px solid #e3e6ec;
  border-bottom: 1px solid #e3e6ec;
  transform: rotate(45deg);
}

/* ============ 右键菜单 ============ */
.dp-menu {
  position: fixed;
  z-index: 4020;
  min-width: 132px;
  padding: 5px;
  background: #fff;
  border: 1px solid #e3e6ec;
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(23, 28, 38, 0.18);
}
.dp-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: #24292f;
  cursor: pointer;
  white-space: nowrap;
}
.dp-menu-item:hover { background: #f3f6fb; color: #d63a2b; }
.dp-menu-ico { width: 18px; text-align: center; }

/* ============ AI 聊天面板（敦煌风格：朱砂红头 + 米纸底 + 金箔饰边） ============ */
.dp-chat {
  position: fixed;
  z-index: 4015;
  width: 340px;
  height: 500px;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(rgba(250, 247, 240, 0.5), rgba(250, 247, 240, 0.5)),
    url('/deskpet/feitian/bg_msgs.png') center / cover no-repeat;
  border: 1px solid var(--dh-border, #d4c5a9);
  border-radius: 12px;
  box-shadow: 0 14px 40px rgba(44, 24, 16, 0.25);
  overflow: hidden;
}
.dp-chat-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background:
    linear-gradient(135deg, rgba(166, 42, 32, 0.45) 0%, rgba(143, 31, 20, 0.5) 100%),
    url('/deskpet/feitian/bg_header.jpg') center / cover no-repeat;
  border-bottom: 2px solid var(--dh-gold, #c9a84c);
  color: #fff;
  flex-shrink: 0;
}
.dp-chat-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.dp-chat-avatar img { width: 100%; height: 100%; object-fit: cover; }
.dp-chat-title { flex: 1; line-height: 1.25; min-width: 0; }
.dp-chat-title b { display: block; font-size: 13px; color: #fff; text-shadow: 0 1px 3px rgba(0, 0, 0, 0.55); }
.dp-chat-title i { font-style: normal; font-size: 10.5px; color: rgba(255, 255, 255, 0.9); text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45); }
.dp-chat-ico {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.dp-chat-ico:hover { background: rgba(255, 255, 255, 0.15); color: #fff; }
.dp-chat-ico.active { background: rgba(255, 255, 255, 0.2); color: #fff; }
.dp-chat-ico.disabled { opacity: 0.4; pointer-events: none; }

/* ---- 历史会话面板 ---- */
.dp-hist {
  position: absolute;
  top: 48px;
  left: 8px;
  right: 8px;
  z-index: 12;
  max-height: 62%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(23, 28, 38, 0.18);
  overflow: hidden;
}
.dp-hist-head {
  padding: 8px 12px;
  font-size: 11.5px;
  color: #57606a;
  border-bottom: 1px solid #eaeef2;
  background: #f6f8fa;
  flex-shrink: 0;
}
.dp-hist-list { overflow-y: auto; }
.dp-hist-empty { padding: 22px 14px; text-align: center; font-size: 11.5px; color: #8c949e; line-height: 1.7; }
.dp-hist-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f2f4f7;
}
.dp-hist-item:hover { background: #f6f8fa; }
.dp-hist-item.on { background: #ddf4ff; }
.dp-hist-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.dp-hist-title {
  font-size: 12.5px;
  color: #24292f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dp-hist-meta { font-size: 10.5px; color: #8c949e; }
.dp-hist-del {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c949e;
  flex-shrink: 0;
  opacity: 0;
  transition: background 0.15s, color 0.15s, opacity 0.15s;
}
.dp-hist-item:hover .dp-hist-del { opacity: 1; }
.dp-hist-del:hover { background: #eff1f3; color: #cf222e; }
.dp-hist-del.armed { opacity: 1; background: #ffebe9; color: #cf222e; }

/* ---- 工具调用步骤与敏感操作确认 ---- */
.dp-steps { display: flex; flex-wrap: wrap; gap: 4px; padding: 2px 4px; }
.dp-step {
  font-size: 10.5px;
  line-height: 1.6;
  padding: 1px 7px;
  border-radius: 9px;
  background: #f0f3f6;
  color: #57606a;
}
.dp-step.ok { background: #dafbe1; color: #1a7f37; }
.dp-step.fail { background: #ffebe9; color: #cf222e; }
.dp-confirm { display: flex; gap: 8px; padding: 2px 4px; }
.dp-cf-btn {
  border: 1px solid #d0d7de;
  background: #fff;
  color: #57606a;
  border-radius: 7px;
  padding: 4px 14px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dp-cf-btn:hover:not(:disabled) { background: #f6f8fa; }
.dp-cf-btn.primary { background: #1f6feb; border-color: #1f6feb; color: #fff; }
.dp-cf-btn.primary:hover:not(:disabled) { background: #1a5fd0; }
.dp-cf-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.dp-chat-msgs {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dp-chat-msgs::-webkit-scrollbar { width: 6px; }
.dp-chat-msgs::-webkit-scrollbar-track { background: transparent; }
.dp-chat-msgs::-webkit-scrollbar-thumb { background: var(--dh-border, #d4c5a9); border-radius: 3px; }
.dp-chat-msgs::-webkit-scrollbar-thumb:hover { background: var(--dh-gold, #c9a84c); }

.dp-chat-welcome {
  font-size: 12px;
  color: var(--dh-text-light, #6b5a4a);
  background: #fff;
  border: 1px dashed var(--dh-border, #d4c5a9);
  border-radius: 10px;
  padding: 9px 11px;
  line-height: 1.6;
}
.dp-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.dp-chip {
  border: none;
  background: var(--dh-gold-light, #e8d5a3);
  color: var(--dh-primary, #a62a20);
  border-radius: 12px;
  padding: 3px 10px;
  font-size: 11.5px;
  line-height: 1.5;
  cursor: pointer;
  transition: background 0.15s;
}
.dp-chip:hover { background: var(--dh-gold, #c9a84c); color: #fff; }

/* 消息行：头像 + 气泡 + 时间 */
.dp-row { display: flex; gap: 7px; align-items: flex-start; }
.dp-row.is-user { flex-direction: row-reverse; }
.dp-ava {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e7ebf1;
  color: #57606a;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  overflow: hidden;
}
.dp-ava.is-pet {
  background: linear-gradient(135deg, var(--dh-primary, #a62a20), var(--dh-gold, #c9a84c));
  color: #fff;
}
.dp-ava img { width: 100%; height: 100%; object-fit: cover; }
.dp-col { max-width: calc(100% - 30px); display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.dp-row.is-user .dp-col { align-items: flex-end; }
.dp-msg {
  width: fit-content;
  max-width: 100%;
  padding: 8px 11px;
  border-radius: 10px;
  font-size: 12.5px;
  line-height: 1.65;
  word-break: break-word;
}
.dp-msg.is-user {
  background: var(--dh-primary, #a62a20);
  color: #fff;
  border-bottom-right-radius: 3px;
  white-space: pre-wrap;
}
.dp-msg.is-pet { background: #fff; border: 1px solid var(--dh-border, #d4c5a9); color: var(--dh-text, #2c1810); border-bottom-left-radius: 3px; }
.dp-time { font-style: normal; font-size: 10px; color: #9aa4b2; padding: 0 4px; user-select: none; }

/* AI 回复轻量 Markdown */
.dp-md .md-ln { min-height: 1em; }
.dp-md .md-li { padding-left: 3px; }
.dp-md .md-gap { height: 7px; }
.dp-md b { font-weight: 600; }
.dp-md code {
  background: #f3f4f6;
  border: 1px solid #e6e8eb;
  border-radius: 4px;
  padding: 0 4px;
  font-size: 11.5px;
  font-family: Consolas, Monaco, monospace;
}

.dp-caret { display: inline-block; width: 2px; height: 12px; background: var(--dh-primary, #a62a20); margin-left: 2px; vertical-align: -2px; animation: dp-caret 0.8s step-end infinite; }
@keyframes dp-caret { 50% { opacity: 0; } }

.dp-typing { display: flex; gap: 4px; padding: 10px 12px; }
.dp-typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--dh-gold, #c9a84c);
  animation: dp-dot 1.2s ease-in-out infinite;
}
.dp-typing i:nth-child(2) { animation-delay: 0.2s; }
.dp-typing i:nth-child(3) { animation-delay: 0.4s; }
@keyframes dp-dot {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.55; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.dp-chat-foot {
  display: flex;
  gap: 8px;
  padding: 10px 10px 6px;
  border-top: 1px solid var(--dh-border, #d4c5a9);
  flex-shrink: 0;
}
.dp-chat-input {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--dh-border, #d4c5a9);
  border-radius: 8px;
  font-size: 12.5px;
  outline: none;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dp-chat-input:focus { border-color: var(--dh-primary, #a62a20); box-shadow: 0 0 0 2px rgba(166, 42, 32, 0.12); }
.dp-chat-send {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: var(--dh-primary, #a62a20);
  color: #fff;
  font-size: 12.5px;
  cursor: pointer;
  letter-spacing: 2px;
  transition: background 0.15s;
}
.dp-chat-send:disabled { opacity: 0.55; cursor: not-allowed; }
.dp-chat-send:hover:not(:disabled) { background: var(--dh-primary-light, #c94a3a); }
.dp-chat-hint {
  padding: 0 10px 8px;
  font-size: 10.5px;
  color: var(--dh-text-light, #6b5a4a);
  text-align: center;
  flex-shrink: 0;
  user-select: none;
}
</style>
