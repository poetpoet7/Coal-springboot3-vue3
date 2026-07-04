<template>
  <div class="chat-message" :class="'msg-' + role">
    <!-- 用户消息 -->
    <div v-if="role === 'user'" class="bubble user-bubble">
      {{ content }}
    </div>

    <!-- AI 消息 -->
    <div v-else class="bubble ai-bubble">
      <!-- 思考过程 -->
      <ThoughtChain v-if="thought" :thought="thought" />

      <!-- 工具调用 -->
      <div v-if="toolCalls && toolCalls.length > 0" class="tool-calls">
        <ToolCallBlock
          v-for="(tc, idx) in toolCalls"
          :key="idx"
          :tool="tc.tool"
          :params="tc.params"
        />
      </div>

      <!-- AI 回答内容 -->
      <div class="ai-content" v-html="formatContent(content)"></div>

      <!-- 反馈按钮 -->
      <FeedbackButtons v-if="content && !isError" :message-id="messageId" />
    </div>
  </div>
</template>

<script setup>
import ToolCallBlock from './ToolCallBlock.vue'
import ThoughtChain from './ThoughtChain.vue'
import FeedbackButtons from './FeedbackButtons.vue'

const props = defineProps({
  role: { type: String, default: 'user' },
  content: { type: String, default: '' },
  toolCalls: { type: Array, default: null },
  thought: { type: String, default: '' }
})

const messageId = Math.random().toString(36).substring(7)
const isError = props.content?.startsWith('抱歉')

function formatContent(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/【(.+?)】/g, '<span class="source-tag">【$1】</span>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}
</script>

<style scoped>
.chat-message {
  margin-bottom: 12px;
  animation: fadeIn 0.3s ease;
}

.bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

.user-bubble {
  background: #409EFF;
  color: white;
  margin-left: auto;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: #f4f4f5;
  color: #303133;
  margin-right: auto;
  border-bottom-left-radius: 4px;
}

.ai-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.tool-calls {
  margin-bottom: 8px;
}

.source-tag {
  color: #409EFF;
  font-weight: 600;
  font-size: 12px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
