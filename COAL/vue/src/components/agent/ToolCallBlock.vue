<template>
  <div class="tool-call-block">
    <div class="tool-header">
      <el-icon><Setting /></el-icon>
      <span class="tool-name">{{ toolLabel }}</span>
      <el-tag :type="tagType" size="small">{{ status }}</el-tag>
    </div>
    <div v-if="params && Object.keys(params).length > 0" class="tool-params">
      <span v-for="(val, key) in params" :key="key" class="param-item">
        {{ key }}: <code>{{ val }}</code>
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Setting } from '@element-plus/icons-vue'

const props = defineProps({
  tool: { type: String, default: '' },
  params: { type: Object, default: null },
  status: { type: String, default: '已执行' }
})

const toolLabel = computed(() => {
  const labels = {
    query_stat_data: '查询统计数据',
    get_unit_tree: '获取单位树',
    get_pending_approvals: '查询待审批',
    get_approval_history: '查询审批历史',
    check_data_consistency: '数据一致性检查',
    check_cumulative: '累计值校验',
    search_knowledge: '知识库检索',
    summarize_report: '报表摘要',
    compare_periods: '同比环比分析',
    record_feedback: '记录反馈'
  }
  return labels[props.tool] || props.tool
})

const tagType = computed(() => 'success')
</script>

<style scoped>
.tool-call-block {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 4px;
  font-size: 12px;
}
.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #606266;
}
.tool-params {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.param-item {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #909399;
  font-size: 11px;
}
.param-item code {
  color: #409EFF;
  font-family: monospace;
}
</style>
