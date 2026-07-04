<template>
  <div class="agent-panel" :class="{ 'is-fullscreen': fullscreen }">
    <!-- 头部 -->
    <div class="panel-header">
      <div class="header-title">
        <el-icon><ChatDotRound /></el-icon>
        <span>煤炭智能助手</span>
        <el-tag size="small" type="info">Coal-AI</el-tag>
      </div>
      <div class="header-actions">
        <el-button link @click="fullscreen = !fullscreen" :title="fullscreen ? '退出全屏' : '全屏查看'">
          <el-icon :size="18"><FullScreen v-if="!fullscreen" /><CopyDocument v-else /></el-icon>
        </el-button>
        <el-button link @click="clearChat">清空对话</el-button>
        <el-button link @click="$emit('close')">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <ChatMessage
        v-for="(msg, idx) in messages"
        :key="idx"
        :role="msg.role"
        :content="msg.content"
        :tool-calls="msg.toolCalls"
        :thought="msg.thought"
      />
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-indicator">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>思考中...</span>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        placeholder="输入问题，如：煤业公司上个月产销存数据怎么样？"
        @keydown.enter.exact="send"
        :disabled="loading"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        @click="send"
        :loading="loading"
        :disabled="!input.trim()"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { ChatDotRound, Close, Promotion, Loading, FullScreen, CopyDocument } from '@element-plus/icons-vue'
import ChatMessage from './ChatMessage.vue'
import { sendMessage, clearSession } from '../../utils/agent'

defineEmits(['close'])

const input = ref('')
const loading = ref(false)
const fullscreen = ref(false)
const sessionId = ref(null)
const messages = ref([])
const messageListRef = ref(null)

function send() {
  if (!input.value.trim() || loading.value) return
  const userMsg = input.value.trim()
  input.value = ''

  // 添加用户消息
  messages.value.push({ role: 'user', content: userMsg })
  scrollToBottom()

  // 添加 AI 占位
  const aiMsgIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', toolCalls: [], thought: '' })
  loading.value = true

  const callbacks = {
    onThought(thought) {
      messages.value[aiMsgIdx].thought = thought
    },
    onToolCall(toolData) {
      if (!messages.value[aiMsgIdx].toolCalls) {
        messages.value[aiMsgIdx].toolCalls = []
      }
      messages.value[aiMsgIdx].toolCalls.push(toolData)
    },
    onMessage(content) {
      messages.value[aiMsgIdx].content += content
      scrollToBottom()
    },
    onSession(serverSessionId) {
      if (serverSessionId) {
        sessionId.value = serverSessionId
      }
    },
    onDone() {
      loading.value = false
      scrollToBottom()
    },
    onError(err) {
      messages.value[aiMsgIdx].content = '抱歉，处理请求时出错：' + err
      loading.value = false
    }
  }

  const controller = sendMessage(userMsg, callbacks, sessionId.value)
  // 存储 controller 以便需要时取消
  messages.value[aiMsgIdx]._controller = controller
}

function clearChat() {
  if (sessionId.value) {
    clearSession(sessionId.value)
  }
  messages.value = []
  sessionId.value = null
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.agent-panel {
  position: fixed;
  bottom: 100px;
  right: 30px;
  width: 420px;
  max-height: 600px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  z-index: 9999;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  max-height: 400px;
}

.input-area {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.input-area .el-textarea {
  flex: 1;
}
.input-area .el-button {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #909399;
  font-size: 13px;
}

/* 全屏模式 */
.agent-panel.is-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw !important;
  max-height: 100vh !important;
  border-radius: 0;
  z-index: 10000;
}
.agent-panel.is-fullscreen .message-list {
  max-height: none;
  flex: 1;
  padding: 16px 10%;
}
.agent-panel.is-fullscreen .input-area {
  padding: 16px 10%;
}
.agent-panel.is-fullscreen .ai-bubble {
  max-width: 90%;
}
</style>
