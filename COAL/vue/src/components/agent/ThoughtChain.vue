<template>
  <div class="thought-chain">
    <div class="thought-toggle" @click="expanded = !expanded">
      <el-icon><component :is="expanded ? ArrowDown : ArrowRight" /></el-icon>
      <span>思考过程</span>
    </div>
    <div v-if="expanded" class="thought-content">
      {{ thought }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowRight, ArrowDown } from '@element-plus/icons-vue'

defineProps({
  thought: { type: String, default: '' }
})

const expanded = ref(false)
</script>

<style scoped>
.thought-chain {
  margin-bottom: 8px;
  font-size: 12px;
}
.thought-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  cursor: pointer;
  user-select: none;
}
.thought-toggle:hover {
  color: #606266;
}
.thought-content {
  margin-top: 4px;
  padding: 8px;
  background: #fff8e1;
  border-left: 3px solid #e6a23c;
  border-radius: 4px;
  color: #795548;
  font-style: italic;
  line-height: 1.5;
}
</style>
