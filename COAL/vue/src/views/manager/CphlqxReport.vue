<template>
  <div>
    <!-- 查询表单 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :model="queryForm" :inline="true" label-width="80px">
        <el-form-item label="年份">
          <el-date-picker
            v-model="queryForm.nianfen"
            type="year"
            placeholder="选择年份"
            format="YYYY"
            value-format="YYYY"
            style="width: 120px"
          ></el-date-picker>
        </el-form-item>
        
        <el-form-item label="月份">
          <el-select v-model="queryForm.yuefen" placeholder="请选择" style="width: 100px">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="单位">
          <el-select
            v-model="queryForm.danweiId"
            filterable
            placeholder="请选择"
            style="width: 200px"
          >
            <el-option
              v-for="unit in unitList"
              :key="unit.id"
              :label="unit.mingcheng"
              :value="unit.id"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="产品名称">
          <el-select v-model="queryForm.chanpinmingcheng" placeholder="请选择" clearable style="width: 150px">
            <el-option v-for="p in productList" :key="p" :label="p" :value="p"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button type="success" @click="handleExport">导出为Excel</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <div class="report-title">主要产品（{{ displayParams.chanpinmingcheng }}）货流去向</div>
      
      <div class="report-info-new">
        <div class="info-left">编制单位：{{ selectedUnitName }}</div>
        <div class="info-center">{{ displayParams.nianfen }}年{{ displayParams.yuefen.toString().padStart(2, '0') }}月</div>
        <div class="info-right">单位：吨</div>
      </div>

      <div v-loading="loading" class="custom-report-container">
        <el-empty v-if="!reportData" description="暂无数据，请尝试更改查询条件"></el-empty>
        
        <table v-else class="report-table">
          <thead>
            <tr>
              <th colspan="3" class="header-group">销往地区</th>
              <th colspan="3" class="header-group">运输方式</th>
            </tr>
            <tr>
              <th>项目</th>
              <th>本月(吨)</th>
              <th>累计(吨)</th>
              <th>项目</th>
              <th>本月(吨)</th>
              <th>累计(吨)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="item-name font-bold">合计</td>
              <td class="num font-bold">{{ formatNumber(sumRegionBenyue()) }}</td>
              <td class="num font-bold">{{ formatNumber(sumRegionLeiji()) }}</td>
              <td class="item-name font-bold">合计</td>
              <td class="num font-bold">{{ formatNumber(sumTransportBenyue()) }}</td>
              <td class="num font-bold">{{ formatNumber(sumTransportLeiji()) }}</td>
            </tr>
            <tr>
              <td class="item-name">山东省内</td>
              <td class="num">{{ formatNumber(reportData.shandongshengnei) }}</td>
              <td class="num">{{ formatNumber(reportData.shandongshengneileiji) }}</td>
              <td class="item-name">铁路运输</td>
              <td class="num">{{ formatNumber(reportData.tieluyunshu) }}</td>
              <td class="num">{{ formatNumber(reportData.tieluyunshuleiji) }}</td>
            </tr>
            <tr>
              <td class="item-name">山东省外</td>
              <td class="num">{{ formatNumber(reportData.shandongshengwai) }}</td>
              <td class="num">{{ formatNumber(reportData.shandongshengwaileiji) }}</td>
              <td class="item-name">汽车运输</td>
              <td class="num">{{ formatNumber(reportData.qicheyunshu) }}</td>
              <td class="num">{{ formatNumber(reportData.qicheyunshuleiji) }}</td>
            </tr>
            <tr>
              <td class="item-name indent">其中：国外</td>
              <td class="num">{{ formatNumber(reportData.guowai) }}</td>
              <td class="num">{{ formatNumber(reportData.guowaileiji) }}</td>
              <td class="item-name">内河运输</td>
              <td class="num">{{ formatNumber(reportData.neiheyunshu) }}</td>
              <td class="num">{{ formatNumber(reportData.neiheyunshuleiji) }}</td>
            </tr>
            <tr>
              <td class="item-name">公司自用</td>
              <td class="num">{{ formatNumber(reportData.gongsiziyong) }}</td>
              <td class="num">{{ formatNumber(reportData.gongsiziyongleiji) }}</td>
              <td class="item-name">地销</td>
              <td class="num">{{ formatNumber(reportData.dixiao) }}</td>
              <td class="num">{{ formatNumber(reportData.dixiaoleiji) }}</td>
            </tr>
            <tr>
              <td></td><td></td><td></td>
              <td class="item-name">自营铁路</td>
              <td class="num">{{ formatNumber(reportData.ziyingtielu) }}</td>
              <td class="num">{{ formatNumber(reportData.ziyingtieluleiji) }}</td>
            </tr>
            <tr>
              <td></td><td></td><td></td>
              <td class="item-name">矿自用</td>
              <td class="num">{{ formatNumber(reportData.kuangziyong) }}</td>
              <td class="num">{{ formatNumber(reportData.kuangziyongleiji) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref, computed } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const queryForm = reactive({
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1,
  danweiId: null,
  chanpinmingcheng: "原煤"
});

const productList = ["原煤", "甲醇", "尿素", "焦炭"];

const unitList = ref([]);
const loading = ref(false);

const displayParams = ref({
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1,
  chanpinmingcheng: "原煤"
});

// data containing specific node's info
const reportData = ref(null);

const selectedUnitName = computed(() => {
  const unit = unitList.value.find(u => u.id === queryForm.danweiId);
  return unit ? unit.mingcheng : '各单位合计';
});

const reportDate = computed(() => {
  let cpmc = displayParams.value.chanpinmingcheng || "所有产品";
  return `${displayParams.value.nianfen}年${displayParams.value.yuefen}月（${cpmc}）`;
});

const loadUnits = () => {
  const user = JSON.parse(localStorage.getItem("xm-user") || '{}');
  const danweibianma = user.danweibianma || '';
  const roleid = user.roleid || 0;

  request.get('/chanpinhuoliu/units/accessible', {
    params: { danweiBianma: danweibianma, roleid: roleid }
  }).then(res => {
    if (res.code === '200') {
      unitList.value = res.data || [];
      if (unitList.value.length >= 1) {
        queryForm.danweiId = unitList.value[0].id;
      }
    }
  });
};

const handleQuery = () => {
  if (!queryForm.danweiId) {
    ElMessage.warning("请选择单位");
    return;
  }
  
  loading.value = true;
  const params = {
    danweiId: queryForm.danweiId,
    nianfen: parseInt(queryForm.nianfen),
    yuefen: queryForm.yuefen,
    leibie: "本月", // Backend signature requires it, but it ignores it for dual-calculation
    chanpinmingcheng: queryForm.chanpinmingcheng
  };
  
  request.get('/chanpinhuoliu/query', { params }).then(res => {
    loading.value = false;
    if (res.code === '200') {
      let dataList = res.data || [];
      if (dataList.length > 0) {
        reportData.value = dataList[0].data; // Take root node's mapped data
      } else {
        reportData.value = null;
      }
      
      displayParams.value = {
        nianfen: queryForm.nianfen,
        yuefen: queryForm.yuefen,
        chanpinmingcheng: queryForm.chanpinmingcheng
      };
      
      if (!reportData.value) {
        ElMessage.info("暂无数据");
      }
    } else {
      ElMessage.error(res.msg || "查询失败");
    }
  }).catch(() => {
    loading.value = false;
  });
};

const handleExport = () => {
  if (!queryForm.danweiId) {
    ElMessage.warning("请选择单位");
    return;
  }

  // 获取本地存储中的token
  const user = JSON.parse(localStorage.getItem("xm-user") || '{}');
  const token = user.token || '';
  
  const params = new URLSearchParams({
    danweiId: queryForm.danweiId,
    nianfen: queryForm.nianfen,
    yuefen: queryForm.yuefen,
    chanpinmingcheng: queryForm.chanpinmingcheng || '',
    token: token
  });
  
  const url = `${import.meta.env.VITE_BASE_URL}/chanpinhuoliu/export?${params.toString()}`;
  window.open(url, '_blank');
  ElMessage.success("导出请求已发送");
};

const formatNumber = (value) => {
  if (value === null || value === undefined) {
    return '0.00';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

// Summations
const sumRegionBenyue = () => {
  if (!reportData.value) return 0;
  // 销往地区合计 = 山东省内 + 山东省外 + 国外 + 公司自用
  return (reportData.value.shandongshengnei || 0) + 
         (reportData.value.shandongshengwai || 0) + 
         (reportData.value.guowai || 0) + 
         (reportData.value.gongsiziyong || 0);
};

const sumRegionLeiji = () => {
  if (!reportData.value) return 0;
  return (reportData.value.shandongshengneileiji || 0) + 
         (reportData.value.shandongshengwaileiji || 0) + 
         (reportData.value.guowaileiji || 0) + 
         (reportData.value.gongsiziyongleiji || 0);
};

const sumTransportBenyue = () => {
  if (!reportData.value) return 0;
  return (reportData.value.tieluyunshu || 0) + 
         (reportData.value.qicheyunshu || 0) + 
         (reportData.value.neiheyunshu || 0) + 
         (reportData.value.dixiao || 0) + 
         (reportData.value.ziyingtielu || 0) + 
         (reportData.value.kuangziyong || 0);
};

const sumTransportLeiji = () => {
  if (!reportData.value) return 0;
  return (reportData.value.tieluyunshuleiji || 0) + 
         (reportData.value.qicheyunshuleiji || 0) + 
         (reportData.value.neiheyunshuleiji || 0) + 
         (reportData.value.dixiaoleiji || 0) + 
         (reportData.value.ziyingtieluleiji || 0) + 
         (reportData.value.kuangziyongleiji || 0);
};

onMounted(() => {
  loadUnits();
});
</script>

<style scoped>
.card {
  background: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.report-title {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #000;
  letter-spacing: 2px;
}

.report-info-new {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 5px;
  padding: 0 2px;
  font-size: 14px;
  color: #000;
}

.info-left {
  flex: 1;
  text-align: left;
}

.info-center {
  flex: 1;
  text-align: center;
}

.info-right {
  flex: 1;
  text-align: right;
}

/* 自定义报表表格样式 */
.custom-report-container {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  text-align: center;
  font-size: 14px;
  color: #334155;
}

.report-table th, .report-table td {
  border: 1px solid #cbd5e1;
  padding: 10px 8px;
}

.report-table thead {
  background-color: #f1f5f9;
}

.report-table th {
  font-weight: bold;
  color: #1e293b;
}

.header-group {
  font-size: 15px;
  background-color: #e2e8f0;
}

.item-name {
  text-align: left;
  padding-left: 20px !important;
}

.indent {
  padding-left: 40px !important;
  color: #64748b;
}

.num {
  text-align: right;
  font-family: 'Consolas', 'Courier New', monospace;
  width: 15%;
}

.font-bold {
  font-weight: bold;
  background-color: #fafafa;
}

.report-table tbody tr:hover {
  background-color: #f8fafc;
}
</style>
