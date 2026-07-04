<template>
  <div class="feedback-buttons">
    <span class="feedback-label">这个回答有帮助吗？</span>
    <el-button
      link
      :type="feedbackState === 1 ? 'primary' : 'default'"
      @click="feedback(1)"
      :disabled="feedbackState !== 0"
    >
      <el-icon><Select /></el-icon>
    </el-button>
    <el-button
      link
      :type="feedbackState === -1 ? 'danger' : 'default'"
      @click="feedback(-1)"
      :disabled="feedbackState !== 0"
    >
      <el-icon><CloseBold /></el-icon>
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Select, CloseBold } from '@element-plus/icons-vue'
import { submitFeedback } from '../../utils/agent'

defineProps({
  messageId: { type: String, default: '' }
})

const feedbackState = ref(0)

function feedback(value) {
  feedbackState.value = value
  // submitFeedback(messageId, value) would be called with actual sessionId
  // For now just toggle UI state
}
</script>

<style scoped>
.feedback-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #ebeef5;
}
.feedback-label {
  font-size: 11px;
  color: #b0b3bb;
}
</style>
