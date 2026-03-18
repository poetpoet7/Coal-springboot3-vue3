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
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" style="width: 120px">
            <el-option label="待审批" value="待审批" />
            <el-option label="审批通过" value="审批通过" />
            <el-option label="返回修改" value="返回修改" />
            <el-option label="所有" value="" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批单位">
          <el-select v-model="queryForm.danweiId" filterable placeholder="请选择单位" style="width: 250px" @change="handleQuery">
            <el-option v-for="u in unitList" :key="u.id" :label="u.mingcheng" :value="u.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon> 搜索</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-button type="success" :disabled="selectedRows.length === 0" @click="batchApprove"><el-icon><Check /></el-icon> 批量审批</el-button>
      <el-button type="warning" :disabled="selectedRows.length === 0" @click="batchReturn"><el-icon><RefreshLeft /></el-icon> 批量退回</el-button>
      <el-button @click="handleQuery"><el-icon><Refresh /></el-icon> 重新加载</el-button>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <div class="report-title">{{ moduleName }} - 审批</div>
      <el-table v-loading="loading" :data="tableData" border highlight-current-row class="premium-table" style="width: 100%"
        @selection-change="selectedRows = $event"
        :header-cell-style="{background: '#f8fafc', color: '#334155', textAlign: 'center', fontSize: '13px', fontWeight: 'bold'}">
        <el-table-column type="selection" width="50" fixed></el-table-column>
        <el-table-column label="操作" width="180" fixed align="center">
          <template v-slot="scope">
            <el-button size="small" type="primary" plain :disabled="!isPending(scope.row)" @click="approve(scope.row)">审批</el-button>
            <el-button size="small" type="danger" plain :disabled="!canReturn(scope.row)" @click="ret(scope.row)">退回</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="danweiMingcheng" label="填报单位" width="180" fixed />
        <el-table-column prop="YueFen" label="月份" width="80" align="center">
          <template v-slot="scope"><el-tag size="small" effect="plain">{{ scope.row.YueFen }}月</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template v-slot="scope"><el-tag :type="statusTag(scope.row[statusColumn])">{{ scope.row[statusColumn] || '待上报' }}</el-tag></template>
        </el-table-column>
        <!-- 维度列 -->
        <el-table-column v-for="dim in dimensionColumns" :key="'dim-' + dim" :label="dimensionLabels[dim] || dim" :prop="dim" width="120" align="center" />
        <!-- 数据列 -->
        <el-table-column v-for="c in listColumns" :key="c.name" :prop="c.name" :label="c.label" min-width="120" align="right">
          <template v-slot="scope">{{ c.numeric ? formatNumber(scope.row[c.name]) : scope.row[c.name] }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="tableData.length === 0 && queryForm.danweiId" description="暂无数据"></el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Check, RefreshLeft, Refresh } from "@element-plus/icons-vue";
import request from "@/utils/request";

const route = useRoute();
const moduleKey = computed(() => route.meta.moduleKey);
const user = JSON.parse(localStorage.getItem("xm-user") || "{}");

const moduleName = ref("");
const statusColumn = ref("ZhuangTai");
const listColumns = ref([]);
const dimensionColumns = ref([]);
const dimensionLabels = ref({});
const unitList = ref([]);
const allUnits = ref([]);
const tableData = ref([]);
const selectedRows = ref([]);
const loading = ref(false);

const queryForm = reactive({
  nianfen: new Date().getFullYear().toString(),
  yuefen: null,
  status: "待审批",
  danweiId: null
});

const statusTag = s => {
  if (!s || s === "待上报") return "warning";
  if (s === "审批通过") return "success";
  if (s === "返回修改") return "danger";
  if (String(s).startsWith("待审批")) return "primary";
  return "info";
};

const formatNumber = v => {
  if (v === null || v === undefined || v === "") return "";
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return n === 0 ? "" : n.toLocaleString("zh-CN", { maximumFractionDigits: 4 });
};

const loadMeta = async () => {
  const res = await request.get(`/tongji-module/${moduleKey.value}/meta`);
  if (res.code !== "200") return;
  moduleName.value = res.data.moduleName;
  statusColumn.value = res.data.statusColumn;
  dimensionColumns.value = res.data.dimensionColumns || [];
  dimensionLabels.value = res.data.dimensionLabels || {};
  listColumns.value = (res.data.columns || []).filter(c =>
    !["ID", "DanWeiID", "NianFen", "YueFen", statusColumn.value].includes(c.name) &&
    !c.cumulative &&
    !dimensionColumns.value.includes(c.name)
  );
};

const loadUnits = async () => {
  const [u1, u2] = await Promise.all([
    request.get(`/tongji-module/${moduleKey.value}/units`),
    request.get(`/tongji-module/${moduleKey.value}/units/accessible`, { params: { danweiBianma: user.danweibianma, roleid: user.roleid } })
  ]);
  allUnits.value = u1.code === "200" ? (u1.data || []) : [];
  unitList.value = (u2.code === "200" ? (u2.data || []) : []).filter(u => hasChild(u.id));
  if (!queryForm.danweiId && unitList.value.length) queryForm.danweiId = unitList.value[0].id;
};

const hasChild = id => allUnits.value.some(u => u.shangjidanweiid === id);

const descendants = (id, acc = []) => {
  const children = allUnits.value.filter(u => u.shangjidanweiid === id);
  children.forEach(c => { acc.push(c); descendants(c.id, acc); });
  return acc;
};

const handleQuery = async () => {
  if (!queryForm.danweiId) return;
  loading.value = true;
  try {
    const children = descendants(queryForm.danweiId);
    const rows = [];
    for (const c of children) {
      const res = await request.get(`/tongji-module/${moduleKey.value}/list`, { params: { danweiId: c.id, nianfen: queryForm.nianfen, yuefen: queryForm.yuefen } });
      if (res.code === "200") (res.data || []).forEach(r => rows.push({ ...r, danweiMingcheng: c.mingcheng }));
    }
    if (queryForm.status === "待审批") {
      tableData.value = rows.filter(r => String(r[statusColumn.value] || "").startsWith("待审批"));
    } else if (queryForm.status) {
      tableData.value = rows.filter(r => (r[statusColumn.value] || "") === queryForm.status);
    } else {
      tableData.value = rows; // 所有
    }
  } finally { loading.value = false; }
};

const isPending = row => String(row[statusColumn.value] || "").startsWith("待审批");
const canReturn = row => { const s = row[statusColumn.value]; return !!s && s !== "待上报" && s !== "返回修改"; };

const doApprove = async (row, forceApprove = false) => {
  const res = await request.post(`/tongji-module/${moduleKey.value}/approve/${row.ID}`, null, { params: { operatorDanweiId: queryForm.danweiId, forceApprove, operatorId: user.id } });
  if (res.code === "200") return ElMessage.success("审批成功");
  if (String(res.msg || "").startsWith("SKIP_WARNING:")) {
    const pending = String(res.msg).replace("SKIP_WARNING:", "").split("、").filter(Boolean).join("<br/>");
    return ElMessageBox.confirm(`以下单位未审批：<br/>${pending}<br/>是否直接审批？`, "提示", { dangerouslyUseHTMLString: true }).then(() => doApprove(row, true)).catch(() => {});
  }
  ElMessage.error(res.msg || "审批失败");
};

const approve = row => ElMessageBox.confirm("确认审批通过？", "提示").then(() => doApprove(row)).then(() => handleQuery()).catch(() => {});
const ret = row => ElMessageBox.confirm("确认退回？", "提示").then(async () => {
  const res = await request.post(`/tongji-module/${moduleKey.value}/return/${row.ID}`, null, { params: { operatorDanweiId: queryForm.danweiId, operatorId: user.id } });
  if (res.code === "200") ElMessage.success("退回成功"); else ElMessage.error(res.msg || "退回失败");
  handleQuery();
}).catch(() => {});

const batchApprove = async () => {
  const rows = selectedRows.value.filter(isPending);
  if (!rows.length) return ElMessage.warning("没有可审批记录");
  await ElMessageBox.confirm(`确认审批 ${rows.length} 条记录？`, "提示").catch(() => { throw new Error("cancel"); });
  for (const r of rows) await doApprove(r);
  handleQuery();
};

const batchReturn = async () => {
  const rows = selectedRows.value.filter(canReturn);
  if (!rows.length) return ElMessage.warning("没有可退回记录");
  await ElMessageBox.confirm(`确认退回 ${rows.length} 条记录？`, "提示").catch(() => { throw new Error("cancel"); });
  for (const r of rows) await request.post(`/tongji-module/${moduleKey.value}/return/${r.ID}`, null, { params: { operatorDanweiId: queryForm.danweiId, operatorId: user.id } });
  ElMessage.success("批量退回完成");
  handleQuery();
};

watch(moduleKey, async () => { tableData.value = []; listColumns.value = []; await loadMeta(); await loadUnits(); await handleQuery(); });

onMounted(async () => { await loadMeta(); await loadUnits(); await handleQuery(); });
</script>

<style scoped>
.czkb-manage { padding: 0; }
.card { background: #fff; padding: 16px; border-radius: 6px; }
.report-title { font-size: 18px; font-weight: 700; margin-bottom: 12px; text-align: center; color: #303133; }
.premium-table { font-size: 13px; }
</style>
