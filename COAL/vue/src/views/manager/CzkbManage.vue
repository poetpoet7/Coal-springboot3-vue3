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
      <div class="report-title">产值、主要产品产量及固定资产投资快报</div>
      
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', textAlign: 'center', fontSize: '12px'}"
      >
        <el-table-column type="selection" width="50" :selectable="canSelect"></el-table-column>
        <el-table-column label="操作" width="180" fixed>
          <template v-slot="scope">
            <el-button size="small" type="primary" @click="handleEdit(scope.row)" :disabled="!canEditRow(scope.row.zhuangtai)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)" :disabled="!canEditRow(scope.row.zhuangtai)">删除</el-button>
            <!-- 只有管理员或非基层单位才显示审批和退回按钮 -->
            <!-- <el-button size="small" type="success" @click="handleApprove(scope.row)" v-if="canApproveRecord(scope.row)" :disabled="!isApproveStatus(scope.row.zhuangtai)">审批</el-button> -->
            <!-- <el-button size="small" type="warning" @click="handleReturn(scope.row)" v-if="canApproveRecord(scope.row)" :disabled="!canReturnRow(scope.row.zhuangtai)">退回</el-button> -->
          </template>
        </el-table-column>
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
          <el-table-column prop="qitachanzhi" label="其他产值、营业额" width="130" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.qitachanzhi) }}</template>
          </el-table-column>
        </el-table-column>
        
        <!-- 新产品价值 -->
        <el-table-column prop="xinchanpinjiazhi" label="新产品价值(万元)" width="120" align="right">
          <template v-slot="scope">{{ formatNumber(scope.row.xinchanpinjiazhi) }}</template>
        </el-table-column>
        
        <!-- 工业销售产值 -->
        <el-table-column label="工业销售产值(万元)" align="center">
          <el-table-column prop="gyxsczheji" label="计" width="100" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.gyxsczheji) }}</template>
          </el-table-column>
          <el-table-column prop="chukoujiaohuozhi" label="其中：出口交货值" width="130" align="right">
            <template v-slot="scope">{{ formatNumber(scope.row.chukoujiaohuozhi) }}</template>
          </el-table-column>
        </el-table-column>
        
        <!-- 主要产品产量 -->
        <el-table-column label="主要产品产量" align="center">
          <!-- 原煤(吨) -->
          <el-table-column prop="yuanmei" label="原煤(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.yuanmei) }}</template>
          </el-table-column>
          <!-- 精煤(吨) -->
          <el-table-column prop="jingmei" label="精煤(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.jingmei) }}</template>
          </el-table-column>
          <!-- 尿素(吨) -->
          <el-table-column prop="niaosu" label="尿素(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.niaosu) }}</template>
          </el-table-column>
          <!-- 甲醇(吨) -->
          <el-table-column prop="jiachun" label="甲醇(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.jiachun) }}</template>
          </el-table-column>
          <!-- 醋酸(吨) -->
          <el-table-column prop="cusuan" label="醋酸(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.cusuan) }}</template>
          </el-table-column>
          <!-- 焦炭(吨) -->
          <el-table-column prop="jiaotan" label="焦炭(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.jiaotan) }}</template>
          </el-table-column>
          <!-- 醋酸乙酯(吨) -->
          <el-table-column prop="cusuanyizhi" label="醋酸乙酯(吨)" width="120" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.cusuanyizhi) }}</template>
          </el-table-column>
          <!-- 醋酸丁酯(吨) -->
          <el-table-column prop="cusuandingzhi" label="醋酸丁酯(吨)" width="120" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.cusuandingzhi) }}</template>
          </el-table-column>
          <!-- 醋酐(吨) -->
          <el-table-column prop="cugan" label="醋酐(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.cugan) }}</template>
          </el-table-column>
          <!-- 碳酸二甲酯(吨) -->
          <el-table-column prop="tansuanerjiazhi" label="碳酸二甲酯(吨)" width="120" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.tansuanerjiazhi) }}</template>
          </el-table-column>
          <!-- 合成氨(吨) -->
          <el-table-column prop="hechengan" label="合成氨(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.hechengan) }}</template>
          </el-table-column>
          <!-- 丁醇(吨) -->
          <el-table-column prop="dingchun" label="丁醇(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.dingchun) }}</template>
          </el-table-column>
          <!-- 聚甲醛(吨) -->
          <el-table-column prop="jujiaquan" label="聚甲醛(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.jujiaquan) }}</template>
          </el-table-column>
          <!-- 改质沥青(吨) -->
          <el-table-column prop="gaizhiliqing" label="改质沥青(吨)" width="110" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.gaizhiliqing) }}</template>
          </el-table-column>
          <!-- 蒽油(吨) -->
          <el-table-column prop="enyou" label="蒽油(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.enyou) }}</template>
          </el-table-column>
          <!-- 复合肥(吨) -->
          <el-table-column prop="fuhefei" label="复合肥(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.fuhefei) }}</template>
          </el-table-column>
          <!-- 铝锭(吨) -->
          <el-table-column prop="lvding" label="铝锭(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.lvding) }}</template>
          </el-table-column>
          <!-- 碳素制品(吨) -->
          <el-table-column prop="tansuzhipin" label="碳素制品(吨)" width="110" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.tansuzhipin) }}</template>
          </el-table-column>
          <!-- 铝铸材(吨) -->
          <el-table-column prop="lvzhucai" label="铝铸材(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.lvzhucai) }}</template>
          </el-table-column>
          <!-- 铝挤压材(吨) -->
          <el-table-column prop="lvjiyacai" label="铝挤压材(吨)" width="110" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.lvjiyacai) }}</template>
          </el-table-column>
          <!-- 发电量(万千瓦时) -->
          <el-table-column prop="fadianliang" label="发电量(万千瓦时)" width="130" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.fadianliang) }}</template>
          </el-table-column>
          <!-- 皮带运输机(台) -->
          <el-table-column prop="pidaiyunshuji" label="皮带运输机(台)" width="120" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.pidaiyunshuji) }}</template>
          </el-table-column>
          <!-- 输送带(M2) -->
          <el-table-column prop="shusongdai" label="输送带(M2)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.shusongdai) }}</template>
          </el-table-column>
          <!-- 电缆(米) -->
          <el-table-column prop="dianlan" label="电缆(米)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.dianlan) }}</template>
          </el-table-column>
          <!-- 液压支架(架) -->
          <el-table-column prop="yeyazhijia" label="液压支架(架)" width="110" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.yeyazhijia) }}</template>
          </el-table-column>
          <!-- 刮板运输机(台) -->
          <el-table-column prop="guabanyunshuji" label="刮板运输机(台)" width="120" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.guabanyunshuji) }}</template>
          </el-table-column>
          <!-- 高岭土(吨) -->
          <el-table-column prop="gaolingtu" label="高岭土(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.gaolingtu) }}</template>
          </el-table-column>
          <!-- 轻柴油(吨) -->
          <el-table-column prop="qingchaiyou" label="轻柴油(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.qingchaiyou) }}</template>
          </el-table-column>
          <!-- 重柴油(吨) -->
          <el-table-column prop="zhongchaiyou" label="重柴油(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.zhongchaiyou) }}</template>
          </el-table-column>
          <!-- 石脑油(吨) -->
          <el-table-column prop="shinaoyou" label="石脑油(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.shinaoyou) }}</template>
          </el-table-column>
          <!-- 液化石油气 -->
          <el-table-column prop="yehuashiyouqi" label="液化石油气" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.yehuashiyouqi) }}</template>
          </el-table-column>
          <!-- 硫磺(吨) -->
          <el-table-column prop="liuhuang" label="硫磺(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.liuhuang) }}</template>
          </el-table-column>
          <!-- 硫酸铵(吨) -->
          <el-table-column prop="liuhuangan" label="硫酸铵(吨)" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.liuhuangan) }}</template>
          </el-table-column>
          <!-- 其他产品 -->
          <el-table-column prop="qitachanpin" label="其他产品" width="100" align="right">
            <template v-slot="scope">{{ formatLong(scope.row.qitachanpin) }}</template>
          </el-table-column>
        </el-table-column>
        
        <!-- 企业用电量(万千瓦时) -->
        <el-table-column prop="qiyeyongdianliang" label="企业用电量(万千瓦时)" width="160" align="right">
          <template v-slot="scope">{{ formatLong(scope.row.qiyeyongdianliang) }}</template>
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
      </el-table>
      
      <el-empty v-if="tableData.length === 0 && queryForm.danweiId" description="暂无数据，请点击添加按钮新增记录"></el-empty>
      <el-empty v-if="!queryForm.danweiId" description="请先选择单位"></el-empty>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="1100px"
      :close-on-click-modal="false"
    >
      <div class="dialog-header">
        <span>单位名称: {{ selectedUnitInfo?.mingcheng }}</span>
        <span style="margin-left: 50px">年份: {{ formData.nianfen }}</span>
        <span style="margin-left: 30px">月份: {{ formData.yuefen }}</span>
      </div>
      
      <el-form :model="formData" label-width="160px" class="report-form">
        <el-row :gutter="20">
          <!-- 左列 -->
          <el-col :span="12">
            <!-- 年份月份选择 -->
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
            
            <el-divider content-position="left">经营总值(万元)</el-divider>
            <el-form-item label="经营总值(计)">
              <el-input :value="formatNumber(calculatedJingyingzongzhi)" disabled style="width: 100%">
                <template #suffix>自动计算</template>
              </el-input>
            </el-form-item>
            <el-form-item label="其中：工业产值(现价)">
              <el-input-number v-model="formData.gongyechanzhi" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="其他产值、营业额">
              <el-input-number v-model="formData.qitachanzhi" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            
            <el-divider content-position="left">新产品价值(万元)</el-divider>
            <el-form-item label="新产品价值">
              <el-input-number v-model="formData.xinchanpinjiazhi" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            
            <el-divider content-position="left">工业销售产值(万元)</el-divider>
            <el-form-item label="工业销售产值(计)">
              <el-input-number v-model="formData.gyxsczheji" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="其中：出口交货值">
              <el-input-number v-model="formData.chukoujiaohuozhi" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            
            <el-divider content-position="left">企业用电量(万千瓦时)</el-divider>
            <el-form-item label="企业用电量">
              <el-input-number v-model="formData.qiyeyongdianliang" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            
            <el-divider content-position="left">固定资产投资(万元)</el-divider>
            <el-form-item label="固定资产投资(合计)">
              <el-input-number v-model="formData.gdzctzheji" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="其中：技术改造">
              <el-input-number v-model="formData.jishugaizao" :precision="2" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          
          <!-- 右列 - 主要产品产量 -->
          <el-col :span="12">
            <el-divider content-position="left">主要产品产量</el-divider>
            
            <el-form-item label="原煤(吨)">
              <el-input-number v-model="formData.yuanmei" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="精煤(吨)">
              <el-input-number v-model="formData.jingmei" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="尿素(吨)">
              <el-input-number v-model="formData.niaosu" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="甲醇(吨)">
              <el-input-number v-model="formData.jiachun" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="醋酸(吨)">
              <el-input-number v-model="formData.cusuan" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="焦炭(吨)">
              <el-input-number v-model="formData.jiaotan" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="醋酸乙酯(吨)">
              <el-input-number v-model="formData.cusuanyizhi" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="醋酸丁酯(吨)">
              <el-input-number v-model="formData.cusuandingzhi" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="醋酐(吨)">
              <el-input-number v-model="formData.cugan" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="碳酸二甲酯(吨)">
              <el-input-number v-model="formData.tansuanerjiazhi" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="合成氨(吨)">
              <el-input-number v-model="formData.hechengan" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
            </el-form-item>
            <el-form-item label="发电量(万千瓦时)">
              <el-input-number v-model="formData.fadianliang" :precision="0" :controls="false" placeholder="请输入" style="width: 100%"></el-input-number>
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
import { reactive, ref, onMounted, computed } from "vue";
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

// 当前用户单位信息（非管理员）
const currentUnitInfo = ref({ id: null, mingcheng: '', bianma: '', isBaseUnit: false });

// 当前选中的单位信息
const selectedUnitInfo = computed(() => {
  if (isAdmin.value) {
    return unitList.value.find(u => u.id === queryForm.danweiId);
  }
  return currentUnitInfo.value;
});

// 经营总值自动计算（工业产值 + 其他产值）
const calculatedJingyingzongzhi = computed(() => {
  const gongyechanzhi = formData.gongyechanzhi || 0;
  const qitachanzhi = formData.qitachanzhi || 0;
  return gongyechanzhi + qitachanzhi;
});

// 查询表单
const queryForm = reactive({
  nianfen: new Date().getFullYear().toString(),
  yuefen: null,
  danweiId: null
});

// 表格数据
const tableData = ref([]);
const selectedRows = ref([]);

// 对话框
const dialogVisible = ref(false);
const dialogTitle = ref("添加快报");
const editMode = ref(false);
const formData = reactive({
  id: null,
  danweiid: null,
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1,
  // 经营总值
  jingyingzongzhiheji: null,
  gongyechanzhi: null,
  qitachanzhi: null,
  // 新产品价值
  xinchanpinjiazhi: null,
  // 工业销售产值
  gyxsczheji: null,
  chukoujiaohuozhi: null,
  // 企业用电量
  qiyeyongdianliang: null,
  // 固定资产投资
  gdzctzheji: null,
  jishugaizao: null,
  // 主要产品产量
  yuanmei: null,
  jingmei: null,
  niaosu: null,
  jiachun: null,
  cusuan: null,
  jiaotan: null,
  cusuanyizhi: null,
  cusuandingzhi: null,
  cugan: null,
  tansuanerjiazhi: null,
  hechengan: null,
  fadianliang: null
});

// 加载单位列表（管理员）
const loadUnitList = async () => {
  try {
    const res = await request.get('/touzikuaibao/units');
    if (res.code === '200') {
      // 获取每个单位是否为基层单位
      const units = res.data || [];
      // 为每个单位标记是否为基层单位
      for (const unit of units) {
        const infoRes = await request.get('/touzikuaibao/unitInfo', {
          params: { danweiBianma: unit.bianma }
        });
        if (infoRes.code === '200' && infoRes.data.exists) {
          unit.isBaseUnit = infoRes.data.isBaseUnit;
        } else {
          unit.isBaseUnit = true; // 默认为基层
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
    const res = await request.get('/touzikuaibao/unitInfo', {
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
  
  try {
    const res = await request.get('/touzikuaibao/list', {
      params: {
        danweiId: queryForm.danweiId,
        nianfen: parseInt(queryForm.nianfen),
        yuefen: queryForm.yuefen || null
      }
    });
    
    if (res.code === '200') {
      tableData.value = res.data || [];
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

// 添加
const handleAdd = () => {
  if (!queryForm.danweiId) {
    ElMessage.warning('请先选择单位');
    return;
  }
  
  // 检查是否为基层单位
  const unitInfo = selectedUnitInfo.value;
  if (!unitInfo) {
    ElMessage.warning('请先选择单位');
    return;
  }
  
  // 只有基层单位才能填报（管理员和普通用户规则一致）
  if (!unitInfo.isBaseUnit) {
    ElMessage.warning('只有基层单位才能填写快报，当前选择的单位存在下级单位');
    return;
  }
  
  editMode.value = false;
  dialogTitle.value = "添加快报";
  resetFormData();
  formData.danweiid = queryForm.danweiId;
  
  dialogVisible.value = true;
};

// 编辑
const handleEdit = (row) => {
  editMode.value = true;
  dialogTitle.value = "编辑快报";
  
  // 复制数据到表单
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
    const res = await request.delete(`/touzikuaibao/delete/${row.id}`);
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
  
// 过滤出待上报或返回修改的记录（这两种状态可以上报）
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
      const res = await request.post(`/touzikuaibao/submit/${row.id}`);
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

// 判断是否可选择（待上报或返回修改可以选择上报）
const canSelect = (row) => {
  return row.zhuangtai === '待上报' || row.zhuangtai === '返回修改' || !row.zhuangtai;
};

// 确认保存
const handleConfirm = async () => {
  if (!formData.nianfen || !formData.yuefen) {
    ElMessage.warning('请选择年份和月份');
    return;
  }
  
  formData.danweiid = queryForm.danweiId;
  // 经营总值 = 工业产值 + 其他产值
  formData.jingyingzongzhiheji = calculatedJingyingzongzhi.value;
  
  try {
    let res;
    if (editMode.value) {
      res = await request.put(`/touzikuaibao/update/${formData.id}`, formData);
    } else {
      res = await request.post('/touzikuaibao/add', formData);
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
  // 使用查询表单的年月作为默认值
  formData.nianfen = queryForm.nianfen || new Date().getFullYear().toString();
  formData.yuefen = queryForm.yuefen || new Date().getMonth() + 1;
  formData.jingyingzongzhiheji = null;
  formData.gongyechanzhi = null;
  formData.qitachanzhi = null;
  formData.xinchanpinjiazhi = null;
  formData.gyxsczheji = null;
  formData.chukoujiaohuozhi = null;
  formData.qiyeyongdianliang = null;
  formData.gdzctzheji = null;
  formData.jishugaizao = null;
  formData.yuanmei = null;
  formData.jingmei = null;
  formData.niaosu = null;
  formData.jiachun = null;
  formData.cusuan = null;
  formData.jiaotan = null;
  formData.cusuanyizhi = null;
  formData.cusuandingzhi = null;
  formData.cugan = null;
  formData.tansuanerjiazhi = null;
  formData.hechengan = null;
  formData.fadianliang = null;
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

// ==================== 审批流程相关方法 ====================

// 获取状态标签类型
const getStatusTagType = (status) => {
  if (!status || status === '待上报') return 'warning';
  if (status === '审批通过') return 'success';
  if (status === '返回修改') return 'danger';
  if (status.startsWith('待审批')) return 'primary';
  return 'info';
};

// 判断是否可编辑（待上报或返回修改）
const canEditRow = (status) => {
  return status === '待上报' || status === '返回修改' || !status;
};

// 判断是否可退回（待审批或审批通过才可退回）
const canReturnRow = (status) => {
  if (!status) return false;
  return status.startsWith('待审批') || status === '审批通过';
};

// 判断是否可审批（待审批状态）
const isApproveStatus = (status) => {
  return status && status.startsWith('待审批');
};

// 判断当前用户是否可以审批该记录（上级可以审批下级）
const canApproveRecord = (row) => {
  // 管理员可以审批所有记录
  if (isAdmin.value) return true;
  
  // 非管理员需要检查单位层级关系
  const currentDanweiId = currentUnitInfo.value?.id;
  const recordDanweiId = row.danweiid;
  
  // 基层单位没有审批权限，不显示按钮
  if (currentUnitInfo.value?.isBaseUnit) return false;
  
  // 同一单位不能审批自己的记录（需要上级审批）
  if (currentDanweiId === recordDanweiId) return false;
  
  // 有下级单位的才能审批
  return true;
};

// 处理审批
const handleApprove = async (row) => {
  const operatorDanweiId = isAdmin.value ? queryForm.danweiId : currentUnitInfo.value?.id;
  
  ElMessageBox.confirm(
    `确定要审批通过该记录吗？`, 
    '审批确认', 
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await request.post(`/touzikuaibao/approve/${row.id}`, null, {
        params: { operatorDanweiId }
      });
      if (res.code === '200') {
        ElMessage.success('审批成功');
        handleQuery();
      } else {
        ElMessage.error(res.msg || '审批失败');
      }
    } catch (error) {
      ElMessage.error('审批失败');
    }
  }).catch(() => {});
};

// 处理退回
const handleReturn = async (row) => {
  const operatorDanweiId = isAdmin.value ? queryForm.danweiId : currentUnitInfo.value?.id;
  
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


onMounted(async () => {
  if (isAdmin.value) {
    // 管理员加载所有单位列表
    await loadUnitList();
  } else {
    // 普通用户加载自己的单位
    await loadCurrentUnitInfo();
  }
});
</script>

<style scoped>
.czkb-manage {
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

.dialog-header {
  background: #f5f7fa;
  padding: 12px 20px;
  margin-bottom: 20px;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  display: flex;
  justify-content: space-between;
}

.report-form {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 15px;
}

.report-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.report-form :deep(.el-form-item__label) {
  font-size: 13px;
  line-height: 1.4;
  padding-right: 8px;
}

.report-form :deep(.el-divider) {
  margin: 20px 0 15px 0;
}

.report-form :deep(.el-divider__text) {
  font-weight: bold;
  color: #409eff;
  font-size: 14px;
}

.report-form :deep(.el-input-number) {
  width: 100%;
}

.report-form :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table th) {
  font-weight: bold;
}
</style>
