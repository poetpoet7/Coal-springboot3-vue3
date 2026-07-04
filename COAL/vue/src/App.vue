<template>
  <RouterView />
  <!-- 全局 Agent 浮动按钮（所有页面可见） -->
  <AgentFloatingButton />
</template>

<script setup>
import AgentFloatingButton from '@/components/agent/AgentFloatingButton.vue'
</script>