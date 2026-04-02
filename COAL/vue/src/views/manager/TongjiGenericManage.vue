<template>
  <div class="czkb-manage">
    <!-- 搜索栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :model="queryForm" :inline="true" label-width="80px">
        <el-form-item label="计划年度">
          <el-date-picker v-model="queryForm.nianfen" type="year" placeholder="选择年份" format="YYYY" value-format="YYYY" style="width: 120px"></el-date-picker>
        </el-form-item>
        <el-form-item label="月份">
          <el-select v-model="queryForm.yuefen" placeholder="请选择月份" clearable style="width: 100px">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="单位名称">
          <el-select v-if="isAdmin" v-model="queryForm.danweiId" filterable placeholder="请选择单位" style="width: 250px" @change="handleQuery">
            <el-option v-for="unit in unitList" :key="unit.id" :label="unit.mingcheng" :value="unit.id">
              <span>{{ unit.mingcheng }}</span>
              <el-tag v-if="!unit.isBaseUnit" type="info" size="small" style="margin-left: 8px">有下级</el-tag>
            </el-option>
          </el-select>
          <el-input v-else :value="currentUnitInfo.mingcheng" disabled style="width: 200px"></el-input>
        </el-form-item>
        <!-- 维度筛选 -->
        <el-form-item v-for="dim in dimensionColumns" :key="dim" :label="dimensionLabels[dim] || dim">
          <el-select v-model="queryForm.dimensions[dim]" clearable filterable allow-create default-first-option style="width: 180px">
            <el-option v-for="v in dimensionOptions[dim] || []" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon> 搜索</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 添加</el-button>
      <el-button type="success" @click="batchSubmit" :disabled="selectedRows.length === 0"><el-icon><Upload /></el-icon> 上报</el-button>
      <el-button @click="handleQuery"><el-icon><Refresh /></el-icon> 重新加载</el-button>
      <span v-if="selectedUnitInfo" style="margin-left: 20px; color: #909399; font-size: 13px;">
        当前单位：{{ selectedUnitInfo.mingcheng }}
        <el-tag v-if="selectedUnitInfo.isBaseUnit" type="success" size="small">基层单位</el-tag>
        <el-tag v-else type="warning" size="small">有下级单位</el-tag>
      </span>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <div class="report-title">{{ moduleName }}</div>
      <el-table v-loading="loading" :data="tableData" border highlight-current-row class="premium-table" style="width: 100%"
        @selection-change="selectedRows = $event"
        :header-cell-style="{background: '#f8fafc', color: '#334155', textAlign: 'center', fontSize: '13px', fontWeight: 'bold'}">
        <el-table-column type="selection" width="50" fixed :selectable="canSelectRow"></el-table-column>
        <el-table-column label="操作" width="160" fixed align="center">
          <template v-slot="scope">
            <el-button size="small" type="primary" plain @click="openEdit(scope.row)" :disabled="!canEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="removeRow(scope.row)" :disabled="!canEdit(scope.row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="YueFen" label="月份" width="80" fixed align="center">
          <template v-slot="scope"><el-tag size="small" effect="plain">{{ scope.row.YueFen }}月</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="120" fixed align="center">
          <template v-slot="scope"><el-tag :type="statusTag(scope.row[statusColumn])">{{ scope.row[statusColumn] || '待上报' }}</el-tag></template>
        </el-table-column>
        <!-- 维度列 -->
        <el-table-column v-for="dim in dimensionColumns" :key="'dim-' + dim" :label="dimensionLabels[dim] || dim" :prop="dim" width="120" align="center" />
        <!-- 数据列 -->
        <el-table-column v-for="c in listColumns" :key="c.name" :prop="c.name" :label="c.label" min-width="120" align="right">
          <template v-slot="scope">{{ c.numeric ? formatNumber(scope.row[c.name]) : scope.row[c.name] }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="tableData.length === 0 && queryForm.danweiId" description="暂无数据，请点击添加按钮新增记录"></el-empty>
      <el-empty v-if="!queryForm.danweiId" description="请先选择单位"></el-empty>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1100px" :close-on-click-modal="false">
      <div class="dialog-header">
        <span>单位名称: {{ selectedUnitInfo?.mingcheng }}</span>
        <span style="margin-left: 50px">年份: {{ formData.NianFen }}</span>
        <span style="margin-left: 30px">月份: {{ formData.YueFen }}</span>
      </div>
      <el-form :model="formData" label-width="160px" class="report-form">
        <el-row :gutter="40">
          <el-col :span="12">
            <!-- 年份月份选择（新增模式才显示） -->
            <el-form-item label="年份" v-if="!isEdit">
              <el-date-picker v-model="formData.NianFen" type="year" placeholder="选择年份" format="YYYY" value-format="YYYY" style="width: 100%"></el-date-picker>
            </el-form-item>
            <el-form-item label="月份" v-if="!isEdit">
              <el-select v-model="formData.YueFen" placeholder="请选择月份" style="width: 100%">
                <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
              </el-select>
            </el-form-item>
            <!-- 维度列 -->
            <el-form-item v-for="dim in dimensionColumns" :key="'f-' + dim" :label="dimensionLabels[dim] || dim">
              <el-select v-model="formData[dim]" filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="v in dimensionOptions[dim] || []" :key="v" :label="v" :value="v" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="40">
          <!-- 左侧全为输入 -->
          <el-col :span="12">
            <el-divider content-position="left">本月数据 (编辑)</el-divider>
            <!-- 数据列 -->
            <el-form-item v-for="c in editColumns" :key="'e-' + c.name" :label="c.label">
              <el-input-number v-if="c.numeric" v-model="formData[c.name]" :controls="false" :precision="2" placeholder="请输入本月数据" style="width: 100%"></el-input-number>
              <el-input v-else v-model="formData[c.name]" placeholder="请输入" />
            </el-form-item>
          </el-col>

          <!-- 右侧全为累计计算 -->
          <el-col :span="12">
            <el-divider content-position="left">累计数据 (自动计算/只读)</el-divider>
            <el-form-item v-for="c in editColumns" :key="'cumul-' + c.name" :label="c.label + '累计'">
              <el-input 
                :value="getRealtimeCumulative(c.name)" 
                disabled 
                style="width: 100%">
                <template #suffix>系统自动计算</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Plus, Upload, Refresh } from "@element-plus/icons-vue";
import request from "@/utils/request";

const route = useRoute();
const moduleKey = computed(() => route.meta.moduleKey);
const user = JSON.parse(localStorage.getItem("xm-user") || "{}");

// 管理员判断
const isAdmin = computed(() => user.roleid === 1 || user.loginname === 'admin');

const moduleName = ref("");
const statusColumn = ref("ZhuangTai");
const columns = ref([]);
const dimensionColumns = ref([]);
const dimensionLabels = ref({});
const dimensionOptions = ref({});

const unitList = ref([]);
const currentUnitInfo = ref({ id: null, mingcheng: '', bianma: '', isBaseUnit: false });
const tableData = ref([]);
const loading = ref(false);
const selectedRows = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref("添加");
const isEdit = ref(false);
const formData = reactive({});
const lastMonthData = ref({});

// 实时累计计算函数
const getRealtimeCumulative = (cName) => {
  const cumulCol = getCumulativeColumn(cName);
  if (!cumulCol) return '-';
  const current = Number(formData[cName] || 0);
  const last = Number(lastMonthData.value?.[cumulCol.name] || 0);
  return formatNumber(current + last);
};

const queryForm = reactive({
  nianfen: new Date().getFullYear().toString(),
  yuefen: null,
  danweiId: null,
  dimensions: {}
});

// 选中单位信息
const selectedUnitInfo = computed(() => {
  if (isAdmin.value) return unitList.value.find(u => u.id === queryForm.danweiId);
  return currentUnitInfo.value;
});

// 过滤出显示列（排除元数据列和累计列）
const listColumns = computed(() => columns.value.filter(c => {
  const name = c.name.toLowerCase();
  // 排除元数据、状态、备注、序号、编码、累计列
  return !["id", "danweiid", "nianfen", "yuefen", "xuhao", "bianma", statusColumn.value.toLowerCase()].includes(name) &&
         !name.startsWith("beizhu") &&
         !name.startsWith("zhuangtai") &&
         !c.cumulative &&
         !dimensionColumns.value.includes(c.name);
}));

const editColumns = computed(() => listColumns.value.filter(c => {
  const name = c.name.toLowerCase();
  // 编辑时额外排除明确不应由用户填写的字段（实际上 listColumns 已经排除大部分）
  return !["id", "danweiid", "nianfen", "yuefen", "xuhao", "bianma"].includes(name) &&
         !name.startsWith("beizhu") && 
         !name.startsWith("zhuangtai");
}));

const normalizeMetricName = (name) => {
  if (!name) return "";
  let n = String(name).toLowerCase();
  n = n.replace(/benyue/g, "");
  n = n.replace(/leiji/g, "");
  n = n.replace(/^by/, "");
  n = n.replace(/^lj/, "");
  return n;
};

const findColumnByNameIgnoreCase = (name) => {
  const key = String(name || "").toLowerCase();
  return columns.value.find(c => String(c.name || "").toLowerCase() === key);
};

const getCumulativeColumn = (baseName) => {
  const baseCol = findColumnByNameIgnoreCase(baseName);
  if (!baseCol) return null;

  const cumulativeCols = columns.value.filter(c => c.cumulative);
  const b = String(baseCol.name || "");
  const bLower = b.toLowerCase();

  // 1) 常规后缀命名：Xxx -> XxxLeiJi / Xxx_LeiJi
  const direct = cumulativeCols.find(c => {
    const n = String(c.name || "").toLowerCase();
    return n === bLower + "leiji" || n === bLower + "_leiji";
  });
  if (direct) return direct;

  // 2) 前缀替换命名：BenYueXxx -> LeiJiXxx，BYxxx -> LJxxx
  const transformedNames = [];
  if (b.startsWith("BenYue")) transformedNames.push("LeiJi" + b.slice("BenYue".length));
  if (b.startsWith("BY")) transformedNames.push("LJ" + b.slice(2));
  if (b.includes("BenYue")) transformedNames.push(b.replace("BenYue", "LeiJi"));
  transformedNames.push("LeiJi" + b);
  transformedNames.push("LJ" + b);
  for (const n of transformedNames) {
    const hit = findColumnByNameIgnoreCase(n);
    if (hit && hit.cumulative) return hit;
  }

  // 3) 特殊缩写/历史命名差异
  const specialMap = {
    ziyongxiaoshouliang: "LJZYXiaoShouLiang",
    chukouxiaoshouliang: "LJCKXiaoShouLiang",
    byscjyzz: "LJSCJYZZ"
  };
  const special = specialMap[bLower];
  if (special) {
    const hit = findColumnByNameIgnoreCase(special);
    if (hit && hit.cumulative) return hit;
  }

  // 4) 标签名兜底：同一中文标签的累计列（兼容个别拼写不一致字段）
  const byLabel = cumulativeCols.find(c => String(c.label || "").trim() && String(c.label || "").trim() === String(baseCol.label || "").trim());
  if (byLabel) return byLabel;

  // 5) 语义归一兜底
  const normalized = normalizeMetricName(b);
  const byNormalized = cumulativeCols.find(c => normalizeMetricName(c.name) === normalized);
  return byNormalized || null;
};

const loadMeta = async () => {
  const res = await request.get(`/tongji-module/${moduleKey.value}/meta`);
  if (res.code !== "200") return;
  moduleName.value = res.data.moduleName;
  statusColumn.value = res.data.statusColumn;
  columns.value = res.data.columns || [];
  dimensionColumns.value = res.data.dimensionColumns || [];
  dimensionOptions.value = res.data.dimensionOptions || {};
  dimensionLabels.value = res.data.dimensionLabels || {};
  dimensionColumns.value.forEach(k => (queryForm.dimensions[k] = null));
};

const loadUnits = async () => {
  if (isAdmin.value) {
    const res = await request.get(`/tongji-module/${moduleKey.value}/units/accessible`, { params: { danweiBianma: user.danweibianma, roleid: user.roleid } });
    if (res.code === "200") {
      unitList.value = res.data || [];
      // 标记基层单位
      const allIds = unitList.value.map(u => u.id);
      unitList.value.forEach(u => { u.isBaseUnit = !unitList.value.some(o => o.shangjidanweiid === u.id); });
      if (!queryForm.danweiId && unitList.value.length) queryForm.danweiId = unitList.value[0].id;
    }
  } else {
    // 普通用户获取自己的单位
    const res = await request.get(`/tongji-module/${moduleKey.value}/unitInfo`, { params: { danweiBianma: user.danweibianma } });
    if (res.code === "200" && res.data.exists) {
      currentUnitInfo.value = res.data;
      queryForm.danweiId = res.data.id;
    }
  }
};

const handleQuery = async () => {
  if (!queryForm.danweiId) return ElMessage.warning("请先选择单位");
  loading.value = true;
  try {
    const params = { danweiId: queryForm.danweiId, nianfen: queryForm.nianfen, yuefen: queryForm.yuefen, ...queryForm.dimensions };
    const res = await request.get(`/tongji-module/${moduleKey.value}/list`, { params });
    if (res.code === "200") tableData.value = res.data || [];
  } finally { loading.value = false; }
};

const canEdit = row => { 
  if (moduleKey.value !== 'chanpinchanxiaocun' && selectedUnitInfo.value && !selectedUnitInfo.value.isBaseUnit) return false;
  const s = row[statusColumn.value]; 
  return !s || s === "待上报" || s === "返回修改"; 
};
const canSelectRow = row => canEdit(row);

const statusTag = s => {
  if (!s || s === "待上报") return "warning";
  if (s === "审批通过") return "success";
  if (s === "返回修改") return "danger";
  if (String(s).startsWith("待审批")) return "primary";
  return "info";
};

const formatNumber = v => {
  if (v === null || v === undefined || v === "") return "0";
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return n.toLocaleString("zh-CN", { maximumFractionDigits: 4 });
};

// 获取上月数据
const fetchLastMonthData = async () => {
  if (!formData.DanWeiID || !formData.NianFen || !formData.YueFen) return;
  const params = { 
    danweiId: formData.DanWeiID, 
    nianfen: formData.NianFen, 
    yuefen: formData.YueFen,
    ...Object.fromEntries(dimensionColumns.value.map(dim => [dim, formData[dim]]))
  };
  const res = await request.get(`/tongji-module/${moduleKey.value}/lastMonth`, { params });
  if (res.code === "200") {
    lastMonthData.value = res.data || {};
  }
};

const openAdd = () => {
  if (moduleKey.value !== 'chanpinchanxiaocun' && selectedUnitInfo.value && !selectedUnitInfo.value.isBaseUnit) {
    ElMessage.warning('只有基层单位才能填报，当前选择的单位存在下级单位');
    return;
  }
  isEdit.value = false;
  dialogTitle.value = "添加";
  Object.keys(formData).forEach(k => delete formData[k]);
  formData.DanWeiID = queryForm.danweiId;
  formData.NianFen = queryForm.nianfen;
  formData.YueFen = queryForm.yuefen || new Date().getMonth() + 1;
  dimensionColumns.value.forEach(k => (formData[k] = queryForm.dimensions[k] || null));
  lastMonthData.value = {};
  fetchLastMonthData();
  dialogVisible.value = true;
};

const openEdit = row => {
  isEdit.value = true;
  dialogTitle.value = "编辑";
  Object.keys(formData).forEach(k => delete formData[k]);
  Object.assign(formData, row);
  lastMonthData.value = {};
  fetchLastMonthData();
  dialogVisible.value = true;
};

const save = async () => {
  let res;
  if (isEdit.value) {
    res = await request.put(`/tongji-module/${moduleKey.value}/update/${formData.ID}`, formData);
  } else {
    res = await request.post(`/tongji-module/${moduleKey.value}/add`, formData);
  }
  if (res.code === "200") { ElMessage.success("保存成功"); dialogVisible.value = false; handleQuery(); }
  else ElMessage.error(res.msg || "保存失败");
};

const removeRow = row => {
  ElMessageBox.confirm("确认删除该记录？", "提示").then(async () => {
    const res = await request.delete(`/tongji-module/${moduleKey.value}/delete/${row.ID}`);
    if (res.code === "200") { ElMessage.success("删除成功"); handleQuery(); }
    else ElMessage.error(res.msg || "删除失败");
  }).catch(() => {});
};

const batchSubmit = () => {
  const rows = selectedRows.value.filter(canEdit);
  if (!rows.length) return ElMessage.warning("没有可上报记录");
  ElMessageBox.confirm(`确认上报 ${rows.length} 条记录？`, "提示").then(async () => {
    let ok = 0;
    for (const r of rows) {
      const res = await request.post(`/tongji-module/${moduleKey.value}/submit/${r.ID}`, null, { params: { operatorId: user.id } });
      if (res.code === "200") ok++;
    }
    ElMessage.success(`成功上报 ${ok} 条`);
    handleQuery();
  }).catch(() => {});
};

// 监听年月单位或维度变化刷新上月数据
watch(() => [formData.DanWeiID, formData.NianFen, formData.YueFen, ...dimensionColumns.value.map(d => formData[d])], () => {
  if (dialogVisible.value) fetchLastMonthData();
});

// 路由变化时重新加载（同一组件不同模块切换）
watch(moduleKey, async () => {
  tableData.value = [];
  columns.value = [];
  await loadMeta();
  await loadUnits();
  await handleQuery();
});

onMounted(async () => {
  await loadMeta();
  await loadUnits();
  await handleQuery();
});
</script>

<style scoped>
.czkb-manage { padding: 0; }
.card { background: #fff; padding: 16px; border-radius: 6px; }
.report-title { font-size: 18px; font-weight: 700; margin-bottom: 12px; text-align: center; color: #303133; }
.dialog-header { padding: 0 20px 10px; color: #606266; font-size: 14px; border-bottom: 1px solid #ebeef5; margin-bottom: 10px; }
.report-form { padding: 10px 20px 0; }
.premium-table { font-size: 13px; }
</style>
