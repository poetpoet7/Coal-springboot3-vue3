<template>
  <div class="czkb-approve">
    <!-- 查询条件 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :inline="true" :model="queryForm">
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
          <el-select v-model="queryForm.yuefen" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" style="width: 120px">
            <el-option label="全部" value=""></el-option>
            <el-option label="待审批" value="待审批"></el-option>
            <el-option label="审批通过" value="审批通过"></el-option>
            <el-option label="退回修改" value="退回修改"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="单位" v-if="isAdmin">
          <el-select v-model="queryForm.danweiId" placeholder="请选择" filterable @change="handleQuery" style="width: 200px">
            <el-option 
              v-for="unit in unitList" 
              :key="unit.id" 
              :label="unit.mingcheng" 
              :value="unit.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-button type="success" @click="handleBatchApprove" :disabled="selectedRows.length === 0">
        <el-icon><Select /></el-icon> 批量审批
      </el-button>
      <el-button type="warning" @click="handleBatchReturn" :disabled="selectedRows.length === 0">
        <el-icon><RefreshLeft /></el-icon> 批量退回
      </el-button>
      
      <span v-if="selectedUnitInfo" style="margin-left: 20px; color: #909399; font-size: 13px;">
        当前单位：{{ selectedUnitInfo.mingcheng }}
        <el-tag v-if="!selectedUnitInfo.isBaseUnit" type="primary" size="small">有下级单位</el-tag>
      </span>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <div class="report-title">主要产品货流去向 - 审批</div>
      
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        highlight-current-row
        class="premium-table"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :header-cell-style="{background: '#f8fafc', color: '#334155', textAlign: 'center', fontSize: '13px', fontWeight: 'bold'}"
      >
        <el-table-column type="selection" width="50" fixed :selectable="canSelect"></el-table-column>
        <el-table-column label="操作" width="180" fixed align="center">
          <template v-slot="scope">
            <el-button size="small" type="primary" plain @click="handleApprove(scope.row)" :disabled="!isApproveStatus(scope.row.zhuangtai)">审批</el-button>
            <el-button size="small" type="danger" plain @click="handleReturn(scope.row)" :disabled="scope.row.zhuangtai === '待上报' || scope.row.zhuangtai === '返回修改'">退回</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="danweiMingcheng" label="单位名称" width="180" fixed align="left"></el-table-column>
        <el-table-column prop="yuefen" label="月份" width="80" fixed align="center">
          <template v-slot="scope">
            <el-tag size="small" effect="plain">{{ scope.row.yuefen }}月</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chanpinmingcheng" label="产品名称" width="120" fixed align="center">
          <template v-slot="scope">
            <el-tag type="info">{{ scope.row.chanpinmingcheng }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="zhuangtai" label="状态" width="120" fixed align="center">
          <template v-slot="scope">
            <el-tag :type="getStatusTagType(scope.row.zhuangtai)">
              {{ scope.row.zhuangtai || '待上报' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <!-- 销往地区 -->
        <el-table-column label="销往地区(吨)" align="center">
          <el-table-column label="合计" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(sumRegion(scope.row)) }}</template>
          </el-table-column>
          <el-table-column prop="shandongshengnei" label="山东省内" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.shandongshengnei) }}</template>
          </el-table-column>
          <el-table-column prop="shandongshengwai" label="山东省外" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.shandongshengwai) }}</template>
          </el-table-column>
          <el-table-column prop="guowai" label="其中：国外" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.guowai) }}</template>
          </el-table-column>
          <el-table-column prop="gongsiziyong" label="公司自用" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.gongsiziyong) }}</template>
          </el-table-column>
        </el-table-column>
        
        <!-- 运输方式 -->
        <el-table-column label="运输方式(吨)" align="center">
          <el-table-column label="合计" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(sumTransport(scope.row)) }}</template>
          </el-table-column>
          <el-table-column prop="tieluyunshu" label="铁路运输" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.tieluyunshu) }}</template>
          </el-table-column>
          <el-table-column prop="qicheyunshu" label="汽车运输" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.qicheyunshu) }}</template>
          </el-table-column>
          <el-table-column prop="neiheyunshu" label="内河运输" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.neiheyunshu) }}</template>
          </el-table-column>
          <el-table-column prop="dixiao" label="地销" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.dixiao) }}</template>
          </el-table-column>
          <el-table-column prop="ziyingtielu" label="自营铁路" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.ziyingtielu) }}</template>
          </el-table-column>
          <el-table-column prop="kuangziyong" label="矿自用" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.kuangziyong) }}</template>
          </el-table-column>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="tableData.length === 0 && queryForm.danweiId" :description="getEmptyDescription()"></el-empty>
      <el-empty v-if="!queryForm.danweiId" description="请先选择单位"></el-empty>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Refresh, Select, RefreshLeft } from "@element-plus/icons-vue";

// 用户信息
const user = JSON.parse(localStorage.getItem("xm-user") || "{}");

// 判断是否为管理员
const isAdmin = computed(() => {
  return user.roleid === 1 || user.loginname === 'admin';
});

// 单位列表
const unitList = ref([]);

// 当前用户单位信息
const currentUnitInfo = ref({ id: null, mingcheng: '', bianma: '', isBaseUnit: false });

// 当前选中的单位信息
const selectedUnitInfo = computed(() => {
  if (isAdmin.value) {
    return unitList.value.find(u => u.id === queryForm.danweiId);
  }
  return currentUnitInfo.value;
});

// 查询表单
const queryForm = reactive({
  nianfen: new Date().getFullYear().toString(),
  yuefen: null,
  danweiId: null,
  status: '待审批' // 默认只查看待审批
});

// 加载状态
const loading = ref(false);

// 表单数据
const tableData = ref([]);
const selectedRows = ref([]);

// 加载单位列表（只加载有下级单位的）
const loadUnitList = async () => {
  try {
    const res = await request.get('/chanpinhuoliu/units');
    if (res.code === '200') {
      const units = res.data || [];
      // 标记每个单位是否为基层单位
      for (const unit of units) {
        const infoRes = await request.get('/chanpinhuoliu/unitInfo', {
          params: { danweiBianma: unit.bianma }
        });
        if (infoRes.code === '200' && infoRes.data.exists) {
          unit.isBaseUnit = infoRes.data.isBaseUnit;
        } else {
          unit.isBaseUnit = true;
        }
      }
      // 只保留有下级单位的（可以审批的）
      unitList.value = units.filter(u => !u.isBaseUnit);
    }
  } catch (error) {
    console.error('加载单位列表失败:', error);
  }
};

// 加载当前用户单位信息
const loadCurrentUnitInfo = async () => {
  if (!user.danweibianma) return;
  
  try {
    const res = await request.get('/chanpinhuoliu/unitInfo', {
      params: { danweiBianma: user.danweibianma }
    });
    
    if (res.code === '200' && res.data.exists) {
      currentUnitInfo.value = res.data;
      // 只有非基层单位才能审批
      if (!res.data.isBaseUnit) {
        queryForm.danweiId = res.data.id;
        handleQuery();
      } else {
        ElMessage.info('基层单位无审批权限');
      }
    }
  } catch (error) {
    console.error('加载单位信息失败:', error);
  }
};

// 查询
const handleQuery = async () => {
  if (!queryForm.danweiId) {
    tableData.value = [];
    return;
  }
  
  loading.value = true;
  try {
    const allUnits = await request.get('/chanpinhuoliu/units');
    if (allUnits.code !== '200') return;
    
    const units = allUnits.data || [];
    
    const getAllDescendants = (parentId) => {
      const directChildren = units.filter(u => u.shangjidanweiid === parentId);
      let allDescendants = [...directChildren];
      for (const child of directChildren) {
        allDescendants = allDescendants.concat(getAllDescendants(child.id));
      }
      return allDescendants;
    };
    
    const childUnits = getAllDescendants(queryForm.danweiId);
    
    if (childUnits.length === 0) {
      tableData.value = [];
      ElMessage.info('该单位没有下级单位');
      return;
    }
    
    const allData = [];
    for (const child of childUnits) {
      const res = await request.get('/chanpinhuoliu/list', {
        params: {
          danweiId: child.id,
          nianfen: parseInt(queryForm.nianfen),
          yuefen: queryForm.yuefen || null
        }
      });
      
      if (res.code === '200' && res.data) {
        res.data.forEach(item => {
          item.danweiMingcheng = child.mingcheng;
        });
        allData.push(...res.data);
      }
    }
    
    // 过滤状态
    if (queryForm.status) {
      if (queryForm.status === '待审批') {
        tableData.value = allData.filter(item => item.zhuangtai && item.zhuangtai.startsWith('待审批'));
      } else if (queryForm.status === '审批通过') {
        tableData.value = allData.filter(item => item.zhuangtai === '审批通过');
      } else if (queryForm.status === '返回修改') {
        tableData.value = allData.filter(item => item.zhuangtai === '返回修改');
      } else {
        tableData.value = allData.filter(item => item.zhuangtai === queryForm.status);
      }
    } else {
      tableData.value = allData;
    }
    
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

// 刷新
const handleRefresh = () => {
  handleQuery();
  ElMessage.success('刷新成功');
};

// 选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection;
};

// 判断是否可选择（排除待上报和返回修改）
const canSelect = (row) => {
  if (!row.zhuangtai) return false;
  return row.zhuangtai !== '待上报' && row.zhuangtai !== '返回修改';
};

// 获取状态标签类型
const getStatusTagType = (status) => {
  if (!status || status === '待上报') return 'warning';
  if (status === '审批通过') return 'success';
  if (status === '返回修改') return 'danger';
  if (status.startsWith('待审批')) return 'primary';
  return 'info';
};

// 获取空数据提示文字
const getEmptyDescription = () => {
  const status = queryForm.status;
  if (status === '待审批') return '暂无待审批的记录';
  if (status === '审批通过') return '暂无审批通过的记录';
  if (status === '退回修改') return '暂无退回修改的记录';
  return '暂无记录';
};

// 判断是否可审批（待审批状态）
const isApproveStatus = (status) => {
  return status && status.startsWith('待审批');
};

// 处理审批（支持跳级审批确认）
const handleApprove = async (row, forceApprove = false) => {
  const operatorDanweiId = queryForm.danweiId;
  
  const doApprove = async (force) => {
    try {
      const res = await request.post(`/chanpinhuoliu/approve/${row.id}`, null, {
        params: { operatorDanweiId, forceApprove: force, operatorId: user.id }
      });
      if (res.code === '200') {
        ElMessage.success('审批成功');
        handleQuery();
      } else if (res.msg && res.msg.startsWith('SKIP_WARNING:')) {
        const pendingUnits = res.msg.substring('SKIP_WARNING:'.length);
        const unitListHtml = pendingUnits.split('、').map(u => `&bull; ${u}`).join('<br>');
        ElMessageBox.confirm(
          `以下单位还未进行审批：<br><br>${unitListHtml}<br><br>是否直接审批？`, 
          '审批确认', 
          {
            confirmButtonText: '直接审批',
            cancelButtonText: '取消',
            type: 'warning',
            dangerouslyUseHTMLString: true
          }
        ).then(() => {
          doApprove(true);
        }).catch(() => {});
      } else {
        ElMessage.error(res.msg || '审批失败');
      }
    } catch (error) {
      ElMessage.error('审批失败');
    }
  };
  
  if (forceApprove) {
    doApprove(true);
  } else {
    ElMessageBox.confirm(
      `确定要审批通过该记录吗？`, 
      '审批确认', 
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      doApprove(false);
    }).catch(() => {});
  }
};

// 处理退回
const handleReturn = async (row) => {
  const operatorDanweiId = queryForm.danweiId;
  
  ElMessageBox.confirm(
    `确定要退回该记录吗？退回后基层可重新修改。`, 
    '退回确认', 
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await request.post(`/chanpinhuoliu/return/${row.id}`, null, {
        params: { operatorDanweiId, operatorId: user.id }
      });
      if (res.code === '200') {
        ElMessage.success('退回成功');
        handleQuery();
      } else {
        ElMessage.error(res.msg || '退回失败');
      }
    } catch (error) {
      ElMessage.error('退回失败');
    }
  }).catch(() => {});
};

// 批量审批
const handleBatchApprove = () => {
  const pendingRows = selectedRows.value.filter(row => isApproveStatus(row.zhuangtai));
  if (pendingRows.length === 0) {
    ElMessage.warning('没有可审批的记录');
    return;
  }
  
  ElMessageBox.confirm(
    `确定要审批通过选中的 ${pendingRows.length} 条记录吗？`, 
    '批量审批确认', 
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    let successCount = 0;
    const operatorDanweiId = queryForm.danweiId;
    for (const row of pendingRows) {
      const res = await request.post(`/chanpinhuoliu/approve/${row.id}`, null, {
        params: { operatorDanweiId, operatorId: user.id }
      });
      if (res.code === '200') {
        successCount++;
      }
    }
    ElMessage.success(`成功审批 ${successCount} 条记录`);
    handleQuery();
  }).catch(() => {});
};

// 批量退回
const handleBatchReturn = () => {
  const returnableRows = selectedRows.value.filter(row => row.zhuangtai !== '待上报' && row.zhuangtai !== '返回修改');
  if (returnableRows.length === 0) {
    ElMessage.warning('没有可退回的记录');
    return;
  }
  
  ElMessageBox.confirm(
    `确定要退回选中的 ${returnableRows.length} 条记录吗？`, 
    '批量退回确认', 
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    let successCount = 0;
    const operatorDanweiId = queryForm.danweiId;
    for (const row of returnableRows) {
      const res = await request.post(`/chanpinhuoliu/return/${row.id}`, null, {
        params: { operatorDanweiId, operatorId: user.id }
      });
      if (res.code === '200') {
        successCount++;
      }
    }
    ElMessage.success(`成功退回 ${successCount} 条记录`);
    handleQuery();
  }).catch(() => {});
};

// 格式化数字
const formatNumber = (value) => {
  if (value === null || value === undefined) return '0.00';
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

// 计算合计
const sumRegion = (row) => {
  return (row.shandongshengnei || 0) + (row.shandongshengwai || 0) + (row.guowai || 0) + (row.gongsiziyong || 0);
};

const sumTransport = (row) => {
  return (row.tieluyunshu || 0) + (row.qicheyunshu || 0) + (row.neiheyunshu || 0) + (row.dixiao || 0) + (row.ziyingtielu || 0) + (row.kuangziyong || 0);
};

onMounted(() => {
  if (isAdmin.value) {
    loadUnitList();
  } else {
    loadCurrentUnitInfo();
  }
});
</script>

<style scoped>
.czkb-approve {
  padding: 20px;
}
.card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
.report-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e2e8f0;
}
.premium-table {
  --el-table-border-color: #e2e8f0;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f1f5f9;
}
</style>
