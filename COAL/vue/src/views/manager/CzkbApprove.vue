<template>
  <div>
    <div class="card" style="margin-bottom: 10px">
      <div class="page-title">产值、主要产品产量及固定资产投资快报 - 审批</div>
      <div class="page-desc">此页面用于审批产值、主要产品产量及固定资产投资快报数据</div>
    </div>
    
    <div class="card">
      <el-empty description="功能开发中，敬请期待">
        <el-button type="primary" @click="goToReport">前往统计报表查看</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goToReport = () => {
  router.push('/manager/touzikuaibao');
};
</script>

<style scoped>
.card {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
}
</style>
