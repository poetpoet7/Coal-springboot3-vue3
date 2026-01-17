<template>
  <div>
    <!-- 查询表单 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :model="queryForm" :inline="true" label-width="80px">
        <el-form-item label="类别">
          <el-select v-model="queryForm.leibie" placeholder="请选择类别" style="width: 120px">
            <el-option label="本月" value="本月"></el-option>
            <el-option label="累计" value="累计"></el-option>
          </el-select>
        </el-form-item>
        
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
          <el-select v-model="queryForm.yuefen" placeholder="请选择月份" style="width: 100px">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="单位">
          <el-select
            v-model="queryForm.danweiId"
            filterable
            placeholder="请选择单位"
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
        
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button type="success" @click="handleExport">导出为Excel</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <!-- 标题 -->
      <div class="report-title">产值、主要产品产量及固定资产投资快报</div>
      
      <!-- 编制单位和日期 -->
      <div class="report-info">
        <span class="report-company">编制单位：{{ selectedUnitName }}</span>
        <span class="report-date">{{ reportDate }}</span>
      </div>
      
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', textAlign: 'center', fontSize: '12px'}"
        :cell-style="getCellStyle"
      >
        <!-- 序号 -->
        <el-table-column prop="xuhao" label="" width="60" align="center" fixed></el-table-column>
        <!-- 单位名称 -->
        <el-table-column prop="danweiMingcheng" label="单位名称" width="150" fixed></el-table-column>
        
        <!-- 经营总值(万元) -->
        <el-table-column label="经营总值(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'jingyingzongzhiheji')) }}
            </template>
          </el-table-column>
          <el-table-column label="工业产值(现价)" width="100" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'gongyechanzhi')) }}
            </template>
          </el-table-column>
          <el-table-column label="其他产值、营业额" width="100" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'qitachanzhi')) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 新产品值价(万元) -->
        <el-table-column label="新产品值价(万元)" width="90" align="right">
          <template v-slot="scope">
            {{ formatNumber(getFieldValue(scope.row, 'xinchanpinjiazhi')) }}
          </template>
        </el-table-column>
        
        <!-- 工业销售产值(万元) -->
        <el-table-column label="工业销售产值（万元）" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'gyxsczheji')) }}
            </template>
          </el-table-column>
          <el-table-column label="其中：出口交货值" width="100" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'chukoujiaohuozhi')) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 主要产品产量 -->
        <el-table-column label="主要产品产量" align="center">
          <!-- 原煤(吨) -->
          <el-table-column label="原煤(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'yuanmei')) }}
            </template>
          </el-table-column>
          <!-- 精煤(吨) -->
          <el-table-column label="精煤(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'jingmei')) }}
            </template>
          </el-table-column>
          <!-- 尿素(吨) -->
          <el-table-column label="尿素(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'niaosu')) }}
            </template>
          </el-table-column>
          <!-- 甲醇(吨) -->
          <el-table-column label="甲醇(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'jiachun')) }}
            </template>
          </el-table-column>
          <!-- 醋酸(吨) -->
          <el-table-column label="醋酸(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'cusuan')) }}
            </template>
          </el-table-column>
          <!-- 焦炭(吨) -->
          <el-table-column label="焦炭(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'jiaotan')) }}
            </template>
          </el-table-column>
          <!-- 醋酸乙酯(吨) -->
          <el-table-column label="醋酸乙酯(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'cusuanyizhi')) }}
            </template>
          </el-table-column>
          <!-- 醋酸丁酯(吨) -->
          <el-table-column label="醋酸丁酯(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'cusuandingzhi')) }}
            </template>
          </el-table-column>
          <!-- 醋酐(吨) -->
          <el-table-column label="醋酐(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'cugan')) }}
            </template>
          </el-table-column>
          <!-- 碳酸二甲酯(吨) -->
          <el-table-column label="碳酸二甲酯(吨)" width="100" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'tansuanerjiazhi')) }}
            </template>
          </el-table-column>
          <!-- 合成氨(吨) -->
          <el-table-column label="合成氨(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'hechengan')) }}
            </template>
          </el-table-column>
          <!-- 丁醇(吨) -->
          <el-table-column label="丁醇(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'dingchun')) }}
            </template>
          </el-table-column>
          <!-- 聚甲醛(吨) -->
          <el-table-column label="聚甲醛(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'jujiaquan')) }}
            </template>
          </el-table-column>
          <!-- 改质沥青(吨) -->
          <el-table-column label="改质沥青(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'gaizhiliqing')) }}
            </template>
          </el-table-column>
          <!-- 蒽油(吨) -->
          <el-table-column label="蒽油(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'enyou')) }}
            </template>
          </el-table-column>
          <!-- 复合肥(吨) -->
          <el-table-column label="复合肥(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'fuhefei')) }}
            </template>
          </el-table-column>
          <!-- 铝锭(吨) -->
          <el-table-column label="铝锭(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'lvding')) }}
            </template>
          </el-table-column>
          <!-- 碳素制品(吨) -->
          <el-table-column label="碳素制品(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'tansuzhipin')) }}
            </template>
          </el-table-column>
          <!-- 铝铸材(吨) -->
          <el-table-column label="铝铸材(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'lvzhucai')) }}
            </template>
          </el-table-column>
          <!-- 铝挤压材(吨) -->
          <el-table-column label="铝挤压材(吨)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'lvjiyacai')) }}
            </template>
          </el-table-column>
          <!-- 发电量(万千瓦时) -->
          <el-table-column label="发电量(万千瓦时)" width="110" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'fadianliang')) }}
            </template>
          </el-table-column>
          <!-- 皮带运输机(台) -->
          <el-table-column label="皮带运输机(台)" width="100" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'pidaiyunshuji')) }}
            </template>
          </el-table-column>
          <!-- 输送带(M2) -->
          <el-table-column label="输送带(M2)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'shusongdai')) }}
            </template>
          </el-table-column>
          <!-- 电缆(米) -->
          <el-table-column label="电缆(米)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'dianlan')) }}
            </template>
          </el-table-column>
          <!-- 液压支架(架) -->
          <el-table-column label="液压支架(架)" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'yeyazhijia')) }}
            </template>
          </el-table-column>
          <!-- 刮板运输机(台) -->
          <el-table-column label="刮板运输机(台)" width="100" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'guabanyunshuji')) }}
            </template>
          </el-table-column>
          <!-- 高岭土(吨) -->
          <el-table-column label="高岭土(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'gaolingtu')) }}
            </template>
          </el-table-column>
          <!-- 轻柴油(吨) -->
          <el-table-column label="轻柴油(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'qingchaiyou')) }}
            </template>
          </el-table-column>
          <!-- 重柴油(吨) -->
          <el-table-column label="重柴油(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'zhongchaiyou')) }}
            </template>
          </el-table-column>
          <!-- 石脑油(吨) -->
          <el-table-column label="石脑油(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'shinaoyou')) }}
            </template>
          </el-table-column>
          <!-- 液化石油气 -->
          <el-table-column label="液化石油气" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'yehuashiyouqi')) }}
            </template>
          </el-table-column>
          <!-- 硫磺(吨) -->
          <el-table-column label="硫磺(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'liuhuang')) }}
            </template>
          </el-table-column>
          <!-- 硫酸铵(吨) -->
          <el-table-column label="硫酸铵(吨)" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'liuhuangan')) }}
            </template>
          </el-table-column>
          <!-- 其他产品 -->
          <el-table-column label="其他产品" width="80" align="right">
            <template v-slot="scope">
              {{ formatLong(getFieldValue(scope.row, 'qitachanpin')) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 企业用电量(万千瓦时) -->
        <el-table-column label="企业用电量(万千瓦时)" width="120" align="right">
          <template v-slot="scope">
            {{ formatLong(getFieldValue(scope.row, 'qiyeyongdianliang')) }}
          </template>
        </el-table-column>
        
        <!-- 固定资产投资(万元) -->
        <el-table-column label="固定资产投资（万元）" align="center">
          <el-table-column label="合计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'gdzctzheji')) }}
            </template>
          </el-table-column>
          <el-table-column label="技术改造" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(getFieldValue(scope.row, 'jishugaizao')) }}
            </template>
          </el-table-column>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref, computed } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const queryForm = reactive({
  leibie: "本月",
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1,
  danweiId: null
});

const tableData = ref([]);
const unitList = ref([]);

// 当前显示的查询参数（只有点击查询后才更新）
const displayParams = ref({
  leibie: "本月",
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1
});

// 获取选中单位名称
const selectedUnitName = computed(() => {
  const unit = unitList.value.find(u => u.id === queryForm.danweiId);
  return unit ? unit.mingcheng : '兖矿集团公司';
});

// 获取报表日期显示（使用当前显示的参数）
const reportDate = computed(() => {
  return `${displayParams.value.nianfen}年${displayParams.value.yuefen}月（${displayParams.value.leibie}）`;
});

// 根据类别获取字段值（本月或累计）- 使用当前显示的参数
const getFieldValue = (row, fieldName) => {
  if (!row.data) return null;
  
  if (displayParams.value.leibie === '累计') {
    // 累计模式：使用带leiji后缀的字段
    const leijiFieldName = fieldName + 'leiji';
    // 处理特殊字段名
    if (fieldName === 'qingchaiyou') {
      return row.data['qingchaiiyouleiji'];
    }
    return row.data[leijiFieldName];
  } else {
    // 本月模式：直接使用字段
    return row.data[fieldName];
  }
};

// 设置单元格样式
const getCellStyle = ({ row }) => {
  if (row.isTotal) {
    return {
      fontWeight: 'bold',
      backgroundColor: '#f5f7fa'
    };
  }
  return {};
};

// 加载单位列表（基于权限过滤）
const loadUnits = () => {
  // 获取当前登录用户信息
  const user = JSON.parse(localStorage.getItem("xm-user") || '{}');
  const danweibianma = user.danweibianma || '';
  const roleid = user.roleid || 0;

  // 使用权限过滤接口获取可访问的单位列表
  request.get('/touzikuaibao/units/accessible', {
    params: { danweiBianma: danweibianma, roleid: roleid }
  }).then(res => {
    if (res.code === '200') {
      unitList.value = res.data || [];
      // 如果列表中只有一个单位，自动选中
      if (unitList.value.length === 1) {
        queryForm.danweiId = unitList.value[0].id;
      }
    }
  });
};

// 查询数据
const handleQuery = () => {
  if (!queryForm.danweiId) {
    ElMessage.warning("请选择单位");
    return;
  }
  
  const params = {
    danweiId: queryForm.danweiId,
    nianfen: parseInt(queryForm.nianfen),
    yuefen: queryForm.yuefen,
    leibie: queryForm.leibie
  };
  
  request.get('/touzikuaibao/query', { params }).then(res => {
    if (res.code === '200') {
      tableData.value = res.data || [];
      // 更新显示参数 - 只有查询成功后才同步
      displayParams.value = {
        leibie: queryForm.leibie,
        nianfen: queryForm.nianfen,
        yuefen: queryForm.yuefen
      };
      if (tableData.value.length === 0) {
        ElMessage.info("暂无数据");
      }
    } else {
      ElMessage.error(res.msg || "查询失败");
    }
  });
};

// 导出Excel
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
    leibie: queryForm.leibie,
    token: token
  });
  
  const url = `${import.meta.env.VITE_BASE_URL}/touzikuaibao/export?${params.toString()}`;
  window.open(url, '_blank');
  ElMessage.success("导出成功");
};

// 格式化数字(BigDecimal类型)
const formatNumber = (value) => {
  if (value === null || value === undefined) {
    return '';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

// 格式化长整型
const formatLong = (value) => {
  if (value === null || value === undefined || value === 0) {
    return '';
  }
  return Number(value).toLocaleString('zh-CN');
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

/* 报表标题 */
.report-title {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

/* 报表信息行 */
.report-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 0 5px;
}

/* 编制单位 */
.report-company {
  font-size: 13px;
  color: #606266;
}

/* 报表日期 */
.report-date {
  font-size: 13px;
  color: #606266;
}

/* 表格样式优化 */
:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table th) {
  font-weight: bold;
  background-color: #f5f7fa !important;
}

:deep(.el-table td) {
  padding: 8px 0;
}
</style>
