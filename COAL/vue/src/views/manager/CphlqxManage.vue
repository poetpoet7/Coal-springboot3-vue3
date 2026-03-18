<template>
  <div class="czkb-manage">
    <!-- 搜索栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :model="queryForm" :inline="true" label-width="80px">
        <el-form-item label="计划年度">
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
          <el-select v-model="queryForm.yuefen" placeholder="请选择月份" clearable style="width: 100px">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="单位名称">
          <!-- 管理员可选择任意单位 -->
          <el-select
            v-if="isAdmin"
            v-model="queryForm.danweiId"
            filterable
            placeholder="请选择单位"
            style="width: 250px"
            @change="handleUnitChange"
          >
            <el-option
              v-for="unit in unitList"
              :key="unit.id"
              :label="unit.mingcheng"
              :value="unit.id"
            >
              <span>{{ unit.mingcheng }}</span>
              <el-tag v-if="!unit.isBaseUnit" type="info" size="small" style="margin-left: 8px">有下级</el-tag>
            </el-option>
          </el-select>
          <!-- 普通用户显示自己的单位 -->
          <el-input v-else :value="currentUnitInfo.mingcheng" disabled style="width: 200px"></el-input>
        </el-form-item>

        <el-form-item label="产品名称">
          <el-select v-model="queryForm.chanpinmingcheng" placeholder="请选择产品" clearable style="width: 150px">
            <el-option v-for="p in productList" :key="p" :label="p" :value="p"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 添加
      </el-button>
      <el-button type="success" @click="handleBatchSubmit" :disabled="selectedRows.length === 0">
        <el-icon><Upload /></el-icon> 上报
      </el-button>
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon> 重新加载
      </el-button>
      
      <!-- 显示当前选中单位状态 -->
      <span v-if="selectedUnitInfo" style="margin-left: 20px; color: #909399; font-size: 13px;">
        当前单位：{{ selectedUnitInfo.mingcheng }}
        <el-tag v-if="selectedUnitInfo.isBaseUnit" type="success" size="small">基层单位</el-tag>
        <el-tag v-else type="warning" size="small">有下级单位</el-tag>
      </span>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <div class="report-title">主要产品货流去向填报列表</div>
      
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
        <el-table-column label="操作" width="160" fixed align="center">
          <template v-slot="scope">
            <el-button size="small" type="primary" plain @click="handleEdit(scope.row)" :disabled="!canEditRow(scope.row.zhuangtai)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(scope.row)" :disabled="!canEditRow(scope.row.zhuangtai)">删除</el-button>
          </template>
        </el-table-column>
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
          <el-table-column prop="tieluyunshu" label="铁路运输" width="120" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.tieluyunshu) }}</template>
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
      
      <el-empty v-if="tableData.length === 0 && queryForm.danweiId" description="暂无数据，请点击添加按钮新增记录"></el-empty>
      <el-empty v-if="!queryForm.danweiId" description="请先选择单位"></el-empty>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="dialog-header">
        <span>单位名称: {{ selectedUnitInfo?.mingcheng }}</span>
        <span style="margin-left: 50px">年份: {{ formData.nianfen }}</span>
        <span style="margin-left: 30px">月份: {{ formData.yuefen }}</span>
      </div>
      
      <el-form :model="formData" label-width="140px" class="report-form">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="年份" v-if="!editMode">
              <el-date-picker
                v-model="formData.nianfen"
                type="year"
                placeholder="选择年份"
                format="YYYY"
                value-format="YYYY"
                style="width: 100%"
              ></el-date-picker>
            </el-form-item>
            <el-form-item label="月份" v-if="!editMode">
              <el-select v-model="formData.yuefen" placeholder="请选择月份" style="width: 100%">
                <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="产品名称" :rules="[{ required: true, message: '请选择产品名称', trigger: 'change' }]">
               <el-select v-model="formData.chanpinmingcheng" :disabled="editMode" placeholder="请选择产品名称" style="width: 100%">
                <el-option v-for="p in productList" :key="p" :label="p" :value="p"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <!-- 左列: 销往地区 -->
          <el-col :span="12">
            <el-divider content-position="left">销往地区(吨)</el-divider>
            <el-form-item label="销往地区(合计)">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input :value="formatNumber(totalRegion)" disabled style="flex: 1">
                  <template #suffix>自动计算</template>
                </el-input>
                <el-input :value="formatNumber(totalRegionLeiji)" disabled style="flex: 1">
                  <template #prefix>累计合计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="山东省内">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.shandongshengnei" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.shandongshengneileiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="山东省外">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.shandongshengwai" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.shandongshengwaileiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="其中：国外">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.guowai" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.guowaileiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="公司自用">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.gongsiziyong" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.gongsiziyongleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
          </el-col>
          
          <!-- 右列: 运输方式 -->
          <el-col :span="12">
            <el-divider content-position="left">运输方式(吨)</el-divider>
            <el-form-item label="运输方式(合计)">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input :value="formatNumber(totalTransport)" disabled style="flex: 1">
                  <template #suffix>自动计算</template>
                </el-input>
                <el-input :value="formatNumber(totalTransportLeiji)" disabled style="flex: 1">
                  <template #prefix>累计合计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="铁路运输">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.tieluyunshu" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.tieluyunshuleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="汽车运输">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.qicheyunshu" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.qicheyunshuleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="内河运输">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.neiheyunshu" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.neiheyunshuleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="地销">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.dixiao" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.dixiaoleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="自营铁路">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.ziyingtielu" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.ziyingtieluleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="矿自用">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input-number v-model="formData.kuangziyong" :precision="2" :controls="false" placeholder="本月" style="flex: 1"></el-input-number>
                <el-input :value="formatNumber(realtimeCumulative.kuangziyongleiji)" disabled placeholder="累计" style="flex: 1">
                  <template #prefix>累计</template>
                </el-input>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirm">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed, watch } from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Plus, Upload, Refresh } from "@element-plus/icons-vue";

const router = useRouter();

// 用户信息
const user = JSON.parse(localStorage.getItem("xm-user") || "{}");

// 判断是否为管理员（roleid为1或loginname为admin）
const isAdmin = computed(() => {
  return user.roleid === 1 || user.loginname === 'admin';
});

// 单位列表（管理员使用）
const unitList = ref([]);

// 产品列表定义
const productList = ["原煤", "精煤", "尿素", "甲醇", "醋酸", "焦炭", "醋酸乙酯", "醋酸丁酯", "醋酐", "碳酸二甲酯", "合成氨", "丁醇", "聚甲醛", "改质沥青", "蒽油", "复合肥", "铝锭", "碳素制品", "铝铸材", "铝挤压材", "高岭土", "轻柴油", "重柴油", "石脑油", "液化石油气", "硫磺", "硫酸铵", "其他产品"];

// 当前用户单位信息（非管理员）
const currentUnitInfo = ref({ id: null, mingcheng: '', bianma: '', isBaseUnit: false });

// 当前选中的单位信息
const selectedUnitInfo = computed(() => {
  if (isAdmin.value) {
    return unitList.value.find(u => u.id === queryForm.danweiId);
  }
  return currentUnitInfo.value;
});

// 合计自动计算
const totalRegion = computed(() => {
  return (formData.shandongshengnei || 0) + (formData.shandongshengwai || 0) + (formData.gongsiziyong || 0);
});
const totalRegionLeiji = computed(() => {
  return (realtimeCumulative.value.shandongshengneileiji || 0) + (realtimeCumulative.value.shandongshengwaileiji || 0) + (realtimeCumulative.value.gongsiziyongleiji || 0);
});

const totalTransport = computed(() => {
  return (formData.tieluyunshu || 0) + (formData.qicheyunshu || 0) + (formData.neiheyunshu || 0) + (formData.dixiao || 0) + (formData.ziyingtielu || 0) + (formData.kuangziyong || 0);
});
const totalTransportLeiji = computed(() => {
  return (realtimeCumulative.value.tieluyunshuleiji || 0) + (realtimeCumulative.value.qicheyunshuleiji || 0) + (realtimeCumulative.value.neiheyunshuleiji || 0) + (realtimeCumulative.value.dixiaoleiji || 0) + (realtimeCumulative.value.ziyingtieluleiji || 0) + (realtimeCumulative.value.kuangziyongleiji || 0);
});

// 查询表单
const queryForm = reactive({
  nianfen: new Date().getFullYear().toString(),
  yuefen: null,
  danweiId: null,
  chanpinmingcheng: null
});

// 加载状态
const loading = ref(false);

// 表格数据
const tableData = ref([]);
const selectedRows = ref([]);

// 对话框
const dialogVisible = ref(false);
const dialogTitle = ref("添加填报");
const editMode = ref(false);
const formData = reactive({
  id: null,
  danweiid: null,
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1,
  chanpinmingcheng: null,
  shandongshengnei: null,
  shandongshengwai: null,
  guowai: null,
  gongsiziyong: null,
  tieluyunshu: null,
  qicheyunshu: null,
  neiheyunshu: null,
  dixiao: null,
  ziyingtielu: null,
  kuangziyong: null
});

// 记录上一个月的累计数据
const lastMonthData = reactive({
  shandongshengneileiji: 0,
  shandongshengwaileiji: 0,
  guowaileiji: 0,
  gongsiziyongleiji: 0,
  tieluyunshuleiji: 0,
  qicheyunshuleiji: 0,
  neiheyunshuleiji: 0,
  dixiaoleiji: 0,
  ziyingtieluleiji: 0,
  kuangziyongleiji: 0
});

// 实时计算当前累计 = 上月累计 + 本月输入
const realtimeCumulative = computed(() => {
  return {
    shandongshengneileiji: (lastMonthData.shandongshengneileiji || 0) + (formData.shandongshengnei || 0),
    shandongshengwaileiji: (lastMonthData.shandongshengwaileiji || 0) + (formData.shandongshengwai || 0),
    guowaileiji: (lastMonthData.guowaileiji || 0) + (formData.guowai || 0),
    gongsiziyongleiji: (lastMonthData.gongsiziyongleiji || 0) + (formData.gongsiziyong || 0),
    tieluyunshuleiji: (lastMonthData.tieluyunshuleiji || 0) + (formData.tieluyunshu || 0),
    qicheyunshuleiji: (lastMonthData.qicheyunshuleiji || 0) + (formData.qicheyunshu || 0),
    neiheyunshuleiji: (lastMonthData.neiheyunshuleiji || 0) + (formData.neiheyunshu || 0),
    dixiaoleiji: (lastMonthData.dixiaoleiji || 0) + (formData.dixiao || 0),
    ziyingtieluleiji: (lastMonthData.ziyingtieluleiji || 0) + (formData.ziyingtielu || 0),
    kuangziyongleiji: (lastMonthData.kuangziyongleiji || 0) + (formData.kuangziyong || 0)
  };
});

// 获取上月数据
const fetchLastMonthData = async () => {
  if (!formData.danweiid || !formData.nianfen || !formData.yuefen || !formData.chanpinmingcheng) {
    resetLastMonthData();
    return;
  }
  
  try {
    const res = await request.get('/chanpinhuoliu/lastMonth', {
      params: {
        danweiId: formData.danweiid,
        nianfen: parseInt(formData.nianfen),
        yuefen: parseInt(formData.yuefen),
        chanpinmingcheng: formData.chanpinmingcheng
      }
    });
    
    if (res.code === '200' && res.data) {
      const d = res.data;
      lastMonthData.shandongshengneileiji = d.shandongshengneileiji || 0;
      lastMonthData.shandongshengwaileiji = d.shandongshengwaileiji || 0;
      lastMonthData.guowaileiji = d.guowaileiji || 0;
      lastMonthData.gongsiziyongleiji = d.gongsiziyongleiji || 0;
      lastMonthData.tieluyunshuleiji = d.tieluyunshuleiji || 0;
      lastMonthData.qicheyunshuleiji = d.qicheyunshuleiji || 0;
      lastMonthData.neiheyunshuleiji = d.neiheyunshuleiji || 0;
      lastMonthData.dixiaoleiji = d.dixiaoleiji || 0;
      lastMonthData.ziyingtieluleiji = d.ziyingtieluleiji || 0;
      lastMonthData.kuangziyongleiji = d.kuangziyongleiji || 0;
      
      // 如果处于编辑模式，对于当前月的数据，我们希望
      // 真实累计 = 当前填报的累计
      // 所以在这里不覆盖，前端计算的 realtimeCumulative 将是:
      // (上月累计) + 本月表单输入
      // 恰好等于当前应该显示的累计值。
      // 因为数据库的计算逻辑也是 `累计 = 上月累计 + 本月`
    } else {
      resetLastMonthData();
    }
  } catch (error) {
    console.error('获取上月数据失败', error);
    resetLastMonthData();
  }
};

const resetLastMonthData = () => {
  Object.keys(lastMonthData).forEach(key => lastMonthData[key] = 0);
};

// 监听参数变化，自动获取上月数据以计算累计
watch(
  [() => formData.danweiid, () => formData.nianfen, () => formData.yuefen, () => formData.chanpinmingcheng],
  () => {
    if (dialogVisible.value) {
      fetchLastMonthData();
    }
  },
  { deep: true }
);

// 加载单位列表（管理员）
const loadUnitList = async () => {
  try {
    const res = await request.get('/chanpinhuoliu/units');
    if (res.code === '200') {
      const units = res.data || [];
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
      unitList.value = units;
    }
  } catch (error) {
    console.error('加载单位列表失败:', error);
  }
};

// 加载当前用户单位信息（非管理员）
const loadCurrentUnitInfo = async () => {
  if (!user.danweibianma) return;
  
  try {
    const res = await request.get('/chanpinhuoliu/unitInfo', {
      params: { danweiBianma: user.danweibianma }
    });
    
    if (res.code === '200' && res.data.exists) {
      currentUnitInfo.value = res.data;
      queryForm.danweiId = res.data.id;
      handleQuery();
    }
  } catch (error) {
    console.error('加载单位信息失败:', error);
  }
};

// 单位变化（管理员切换单位时）
const handleUnitChange = () => {
  tableData.value = [];
  if (queryForm.danweiId) {
    handleQuery();
  }
};

// 查询数据
const handleQuery = async () => {
  if (!queryForm.danweiId) {
    ElMessage.warning('请先选择单位');
    return;
  }
  
  loading.value = true;
  try {
    const res = await request.get('/chanpinhuoliu/list', {
      params: {
        danweiId: queryForm.danweiId,
        nianfen: parseInt(queryForm.nianfen),
        yuefen: queryForm.yuefen || null,
        chanpinmingcheng: queryForm.chanpinmingcheng || null
      }
    });
    
    if (res.code === '200') {
      tableData.value = res.data || [];
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

// 添加
const handleAdd = () => {
  if (!queryForm.danweiId) {
    ElMessage.warning('请先选择单位');
    return;
  }
  
  const unitInfo = selectedUnitInfo.value;
  if (!unitInfo) {
    ElMessage.warning('请先选择单位');
    return;
  }
  
  if (!unitInfo.isBaseUnit) {
    ElMessage.warning('只有基层单位才能填写，当前选择的单位存在下级单位');
    return;
  }
  
  editMode.value = false;
  dialogTitle.value = "添加货流去向记录";
  resetFormData();
  formData.danweiid = queryForm.danweiId;
  
  dialogVisible.value = true;
};

// 编辑
const handleEdit = (row) => {
  editMode.value = true;
  dialogTitle.value = "编辑货流去向记录";
  
  Object.assign(formData, row);
  formData.nianfen = row.nianfen.toString();
  
  dialogVisible.value = true;
};

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除这条记录吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await request.delete(`/chanpinhuoliu/delete/${row.id}`);
    if (res.code === '200') {
      ElMessage.success('删除成功');
      handleQuery();
    } else {
      ElMessage.error(res.msg || '删除失败');
    }
  }).catch(() => {});
};

// 批量上报
const handleBatchSubmit = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要上报的记录');
    return;
  }
  
  const pendingRows = selectedRows.value.filter(row => row.zhuangtai === '待上报' || row.zhuangtai === '返回修改' || !row.zhuangtai);
  if (pendingRows.length === 0) {
    ElMessage.warning('选中的记录都已上报或正在审批中');
    return;
  }
  
  ElMessageBox.confirm(
    `确定要上报选中的 ${pendingRows.length} 条记录吗？上报后将无法修改。`, 
    '上报确认', 
    {
      confirmButtonText: '确定上报',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    let successCount = 0;
    for (const row of pendingRows) {
      const res = await request.post(`/chanpinhuoliu/submit/${row.id}`, null, {
        params: { operatorId: user.id }
      });
      if (res.code === '200') {
        successCount++;
      }
    }
    ElMessage.success(`成功上报 ${successCount} 条记录`);
    handleQuery();
  }).catch(() => {});
};

// 选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection;
};

// 判断是否可选择
const canSelect = (row) => {
  return row.zhuangtai === '待上报' || row.zhuangtai === '返回修改' || !row.zhuangtai;
};

// 确认保存
const handleConfirm = async () => {
  if (!formData.nianfen || !formData.yuefen) {
    ElMessage.warning('请选择年份和月份');
    return;
  }
  if (!formData.chanpinmingcheng) {
    ElMessage.warning('请选择产品名称');
    return;
  }
  
  formData.danweiid = queryForm.danweiId;
  
  try {
    let res;
    if (editMode.value) {
      res = await request.put(`/chanpinhuoliu/update/${formData.id}`, formData);
    } else {
      res = await request.post('/chanpinhuoliu/add', formData);
    }
    
    if (res.code === '200') {
      ElMessage.success(editMode.value ? '更新成功' : '添加成功');
      dialogVisible.value = false;
      handleQuery();
    } else {
      ElMessage.error(res.msg || '操作失败');
    }
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

// 重置表单
const resetFormData = () => {
  formData.id = null;
  formData.nianfen = queryForm.nianfen || new Date().getFullYear().toString();
  formData.yuefen = queryForm.yuefen || new Date().getMonth() + 1;
  formData.chanpinmingcheng = queryForm.chanpinmingcheng || null;
  formData.shandongshengnei = null;
  formData.shandongshengwai = null;
  formData.guowai = null;
  formData.gongsiziyong = null;
  formData.tieluyunshu = null;
  formData.qicheyunshu = null;
  formData.neiheyunshu = null;
  formData.dixiao = null;
  formData.ziyingtielu = null;
  formData.kuangziyong = null;
};

// 格式化数字
const formatNumber = (value) => {
  if (value === null || value === undefined) return '';
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

// ==================== 审批流程相关方法 ====================

const getStatusTagType = (status) => {
  if (!status || status === '待上报') return 'warning';
  if (status === '审批通过') return 'success';
  if (status === '返回修改') return 'danger';
  if (status.startsWith('待审批')) return 'primary';
  return 'info';
};

const canEditRow = (status) => {
  return status === '待上报' || status === '返回修改' || !status;
};

// 初始化
onMounted(() => {
  if (isAdmin.value) {
    loadUnitList();
  } else {
    loadCurrentUnitInfo();
  }
});
</script>

<style scoped>
.czkb-manage {
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
.dialog-header {
  margin-bottom: 20px;
  padding: 10px 15px;
  background-color: #f8fafc;
  border-radius: 4px;
  border-left: 4px solid #3b82f6;
  font-weight: bold;
  color: #334155;
  display: flex;
  align-items: center;
}
.premium-table {
  --el-table-border-color: #e2e8f0;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f1f5f9;
}
</style>
