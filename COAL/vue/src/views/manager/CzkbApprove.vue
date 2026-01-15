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
            <el-option label="返回修改" value="返回修改"></el-option>
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
      <div class="report-title">产值、主要产品产量及固定资产投资快报 - 审批</div>
      
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', textAlign: 'center', fontSize: '12px'}"
      >
        <el-table-column type="selection" width="50" :selectable="canSelect"></el-table-column>
        <el-table-column label="操作" width="200" fixed>
          <template v-slot="scope">
            <el-button size="small" type="success" @click="handleApprove(scope.row)" :disabled="!isApproveStatus(scope.row.zhuangtai)">审批</el-button>
            <el-button size="small" type="warning" @click="handleReturn(scope.row)" :disabled="scope.row.zhuangtai === '待上报' || scope.row.zhuangtai === '返回修改'">退回</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="danweiMingcheng" label="单位" width="150" align="left"></el-table-column>
        <el-table-column prop="yuefen" label="月份" width="80" align="center">
          <template v-slot="scope">{{ scope.row.yuefen }}月</template>
        </el-table-column>
        <el-table-column prop="zhuangtai" label="状态" width="100" align="center">
          <template v-slot="scope">
            <el-tag :type="getStatusTagType(scope.row.zhuangtai)">
              {{ scope.row.zhuangtai || '待上报' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <!-- 经营总值(万元) -->
        <el-table-column label="经营总值(万元)" align="center">
          <el-table-column prop="jingyingzongzhiheji" label="计" width="100" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.jingyingzongzhiheji) }}</template>
          </el-table-column>
          <el-table-column prop="gongyechanzhi" label="工业产值(现价)" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.gongyechanzhi) }}</template>
          </el-table-column>
          <el-table-column prop="qitachanzhi" label="其他产值" width="100" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.qitachanzhi) }}</template>
          </el-table-column>
        </el-table-column>
        
        <!-- 固定资产投资(万元) -->
        <el-table-column label="固定资产投资(万元)" align="center">
          <el-table-column prop="gdzctzheji" label="合计" width="100" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.gdzctzheji) }}</template>
          </el-table-column>
          <el-table-column prop="jishugaizao" label="技术改造" width="100" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.jishugaizao) }}</template>
          </el-table-column>
        </el-table-column>

        <!-- 主要产品产量 -->
        <el-table-column label="主要产品产量" align="center">
          <el-table-column prop="yuanmei" label="原煤(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.yuanmei) }}</template>
          </el-table-column>
          <el-table-column prop="jingmei" label="精煤(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.jingmei) }}</template>
          </el-table-column>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="tableData.length === 0 && queryForm.danweiId" description="暂无待审批的记录"></el-empty>
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

// 表格数据
const tableData = ref([]);
const selectedRows = ref([]);

// 加载单位列表（只加载有下级单位的）
const loadUnitList = async () => {
  try {
    const res = await request.get('/touzikuaibao/units');
    if (res.code === '200') {
      const units = res.data || [];
      // 标记每个单位是否为基层单位
      for (const unit of units) {
        const infoRes = await request.get('/touzikuaibao/unitInfo', {
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
    const res = await request.get('/touzikuaibao/unitInfo', {
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

// 查询数据（查询所有下级单位的记录，含间接下级）
const handleQuery = async () => {
  if (!queryForm.danweiId) {
    ElMessage.warning('请先选择单位');
    return;
  }
  
  try {
    // 获取所有单位列表
    const allUnits = await request.get('/touzikuaibao/units');
    if (allUnits.code !== '200') return;
    
    const units = allUnits.data || [];
    
    // 递归获取所有下级单位（包括间接下级）
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
    
    // 查询所有下级单位的数据
    const allData = [];
    for (const child of childUnits) {
      const res = await request.get('/touzikuaibao/list', {
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
        // 匹配所有待审批状态
        tableData.value = allData.filter(item => item.zhuangtai && item.zhuangtai.startsWith('待审批'));
      } else if (queryForm.status === '审批通过') {
        // 匹配审批通过
        tableData.value = allData.filter(item => item.zhuangtai === '审批通过');
      } else if (queryForm.status === '返回修改') {
        // 匹配返回修改状态
        tableData.value = allData.filter(item => item.zhuangtai === '返回修改');
      } else {
        tableData.value = allData.filter(item => item.zhuangtai === queryForm.status);
      }
    } else {
      // 全部：显示所有记录
      tableData.value = allData;
    }
    
  } catch (error) {
    ElMessage.error('查询失败');
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

// 判断是否可审批（待审批状态）
const isApproveStatus = (status) => {
  return status && status.startsWith('待审批');
};

// 处理审批（支持跳级审批确认）
const handleApprove = async (row, forceApprove = false) => {
  const operatorDanweiId = queryForm.danweiId;
  
  // 执行审批请求
  const doApprove = async (force) => {
    try {
      const res = await request.post(`/touzikuaibao/approve/${row.id}`, null, {
        params: { operatorDanweiId, forceApprove: force }
      });
      if (res.code === '200') {
        ElMessage.success('审批成功');
        handleQuery();
      } else if (res.msg && res.msg.startsWith('SKIP_WARNING:')) {
        // 跳级审批警告，弹出确认框
        const pendingUnits = res.msg.substring('SKIP_WARNING:'.length);
        // 使用 HTML 格式化，确保每个单位单独一行
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
          // 用户确认跳级审批
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
    // 强制审批，直接执行
    doApprove(true);
  } else {
    // 先确认
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
      const res = await request.post(`/touzikuaibao/return/${row.id}`, null, {
        params: { operatorDanweiId }
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
      const res = await request.post(`/touzikuaibao/approve/${row.id}`, null, {
        params: { operatorDanweiId }
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
      const res = await request.post(`/touzikuaibao/return/${row.id}`, null, {
        params: { operatorDanweiId }
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
  if (value === null || value === undefined) return '';
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

// 格式化长整型
const formatLong = (value) => {
  if (value === null || value === undefined || value === 0) return '';
  return Number(value).toLocaleString('zh-CN');
};

onMounted(async () => {
  if (isAdmin.value) {
    await loadUnitList();
  } else {
    await loadCurrentUnitInfo();
  }
});
</script>

<style scoped>
.czkb-approve {
  padding: 0;
}

.card {
  background: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.report-title {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 15px;
}

:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table th) {
  font-weight: bold;
}
</style>
