<template>
  <div class="agent-floating-btn" @click="toggle">
    <el-icon :size="24">
      <ChatDotRound v-if="!visible" />
      <Close v-else />
    </el-icon>
  </div>

  <!-- 对话面板 -->
  <Teleport to="body">
    <AgentPanel
      v-if="visible"
      @close="visible = false"
    />
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { ChatDotRound, Close } from '@element-plus/icons-vue'
import AgentPanel from './AgentPanel.vue'

const visible = ref(false)

function toggle() {
  visible.value = !visible.value
}
</script>

<style scoped>
.agent-floating-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #337ECC);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  z-index: 9998;
  transition: transform 0.2s, box-shadow 0.2s;
}
.agent-floating-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.5);
}
</style>
