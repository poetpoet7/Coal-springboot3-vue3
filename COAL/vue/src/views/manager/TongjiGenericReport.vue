<template>
  <div class="czkb-manage">
    <!-- 搜索栏 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :model="queryForm" :inline="true" label-width="80px">
        <el-form-item label="类别">
          <el-select v-model="queryForm.leibie" style="width: 110px">
            <el-option label="本月" value="本月" />
            <el-option label="累计" value="累计" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划年度">
          <el-date-picker v-model="queryForm.nianfen" type="year" placeholder="选择年份" format="YYYY" value-format="YYYY" style="width: 120px"></el-date-picker>
        </el-form-item>
        <el-form-item label="月份">
          <el-select v-model="queryForm.yuefen" placeholder="请选择月份" style="width: 100px">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="单位名称">
          <el-select v-model="queryForm.danweiId" filterable placeholder="请选择单位" style="width: 250px">
            <el-option v-for="u in unitList" :key="u.id" :label="u.mingcheng" :value="u.id"></el-option>
          </el-select>
        </el-form-item>
        <!-- 维度筛选 -->
        <el-form-item v-for="dim in dimensionColumns" :key="dim" :label="dimensionLabels[dim] || dim">
          <el-select v-model="queryForm.dimensions[dim]" clearable filterable style="width: 180px">
            <el-option v-for="v in dimensionOptions[dim] || []" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query"><el-icon><Search /></el-icon> 搜索</el-button>
          <el-button type="success" @click="exportExcel"><el-icon><Download /></el-icon> 导出Excel</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 报表 -->
    <div class="card">
      <div class="report-header">
        <div class="report-title">{{ reportTitle }}</div>
        <div class="report-info">
          <span class="report-info-left">编制单位：{{ unitName }}</span>
          <span class="report-info-center">{{ queryForm.nianfen }}年{{ queryForm.yuefen }}月（{{ queryForm.leibie }}）</span>
          <span class="report-info-right" v-if="reportUnit">{{ reportUnit }}</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border highlight-current-row class="premium-table" style="width: 100%"
        :header-cell-style="{background: '#f8fafc', color: '#334155', textAlign: 'center', fontSize: '13px', fontWeight: 'bold'}">

        <!-- ========== 经营总值 jingyingzongzhi ========== -->
        <template v-if="moduleKey === 'jingyingzongzhi'">
          <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed />
          <el-table-column label="单位名称" width="180" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column :label="isLeiji ? '总计(累计)' : '总计'" min-width="120" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'ZongJiLeiJi' : 'ZongJi')) }}</template>
          </el-table-column>
          <el-table-column label="按产业分生产经营总值">
            <el-table-column label="第二产业">
              <el-table-column :label="isLeiji ? '合计(累计)' : '合计'" min-width="110" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'DiErChanYeHeJiLeiJi' : 'DiErChanYeHeJi')) }}</template>
              </el-table-column>
              <el-table-column label="工业">
                <el-table-column label="计" min-width="100" align="right">
                  <template v-slot="scope">{{ format(d(scope, isLeiji ? 'GongYeLeiJi' : 'GongYe')) }}</template>
                </el-table-column>
                <el-table-column label="煤炭" min-width="100" align="right">
                  <template v-slot="scope">{{ format(d(scope, isLeiji ? 'MeiTanLeiJi' : 'MeiTan')) }}</template>
                </el-table-column>
              </el-table-column>
              <el-table-column label="建筑业" min-width="100" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'JianZhuYeLeiJi' : 'JianZhuYe')) }}</template>
              </el-table-column>
            </el-table-column>
            <el-table-column label="第三产业">
              <el-table-column :label="isLeiji ? '合计(累计)' : '合计'" min-width="110" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'DiSanChanYeHeJiLeiJi' : 'DiSanChanYeHeJi')) }}</template>
              </el-table-column>
              <el-table-column label="交通运输业" min-width="110" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'JiaoTongYunShuYeLeiJi' : 'JiaoTongYunShuYe')) }}</template>
              </el-table-column>
              <el-table-column label="商贸零售" min-width="100" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'ShangMaoLingShouLeiJi' : 'ShangMaoLingShou')) }}</template>
              </el-table-column>
              <el-table-column label="餐饮业" min-width="100" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'CanYinYeLeiJi' : 'CanYinYe')) }}</template>
              </el-table-column>
              <el-table-column label="其他" min-width="100" align="right">
                <template v-slot="scope">{{ format(d(scope, isLeiji ? 'SanChanQiTaLeiJi' : 'SanChanQiTa')) }}</template>
              </el-table-column>
            </el-table-column>
          </el-table-column>
          <el-table-column label="按性质分生产经营总值">
            <el-table-column label="全民" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'QuanMinLeiJi' : 'QuanMin')) }}</template>
            </el-table-column>
            <el-table-column label="集体" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'JiTiLeiJi' : 'JiTi')) }}</template>
            </el-table-column>
            <el-table-column label="其他" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'XingZhiQiTaLeiJi' : 'XingZhiQiTa')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="本月生产经营总值" min-width="130" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'LJSCJYZZ' : 'BYSCJYZZ')) }}</template>
          </el-table-column>
        </template>

        <!-- ========== 产销存 chanpinchanxiaocun ========== -->
        <template v-else-if="moduleKey === 'chanpinchanxiaocun'">
          <el-table-column label="单位(或产品)名称" width="200" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column label="计量单位" width="80" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).JiLiangDanWei || '' }}</template>
          </el-table-column>
          <el-table-column label="设计或核定生产能力" min-width="120" align="right">
            <template v-slot="scope">{{ format(d(scope, 'HeDingNengLi')) }}</template>
          </el-table-column>
          <el-table-column label="产品产量">
            <el-table-column label="本月" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'BenYueChanLiang')) }}</template>
            </el-table-column>
            <el-table-column label="累计" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'LeiJiChanLiang')) }}</template>
            </el-table-column>
            <el-table-column label="去年同期" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'QNTQChanLiang')) }}</template>
            </el-table-column>
            <el-table-column label="比去年%" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'BQNChanLiang')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="产品销售量">
            <el-table-column label="本月" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'BenYueXiaoShouLiang')) }}</template>
            </el-table-column>
            <el-table-column label="累计" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'LeiJiXiaoShouLiang')) }}</template>
            </el-table-column>
            <el-table-column label="累计自用" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'LJZYXiaoShouLiang')) }}</template>
            </el-table-column>
            <el-table-column label="累计出口" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'LJCKXiaoShouLiang')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="库存量">
            <el-table-column label="期初库存" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'QiChuKuCunLiang')) }}</template>
            </el-table-column>
            <el-table-column label="期末库存" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'QiMoKuCunLiang')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="产品本期平均价格(元)" min-width="130" align="right">
            <template v-slot="scope">{{ format(d(scope, 'BenQiPingJunJiaGe')) }}</template>
          </el-table-column>
        </template>

        <!-- ========== 出口产品 chukouchanpin ========== -->
        <template v-else-if="moduleKey === 'chukouchanpin'">
          <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed />
          <el-table-column label="单位名称" width="200" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column label="产品名称" min-width="120" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).ChanPinMingCheng || '' }}</template>
          </el-table-column>
          <el-table-column label="计量单位" width="80" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).JiLiangDanWei || '' }}</template>
          </el-table-column>
          <el-table-column label="本月止累计生产量" min-width="120" align="right">
            <template v-slot="scope">{{ format(d(scope, 'LeiJiShengChanLiang')) }}</template>
          </el-table-column>
          <el-table-column label="本月止累计出口量" min-width="120" align="right">
            <template v-slot="scope">{{ format(d(scope, 'LeiJiChuKouLiang')) }}</template>
          </el-table-column>
          <el-table-column label="出口创汇(人民币万元)" min-width="130" align="right">
            <template v-slot="scope">{{ format(d(scope, 'ChuKouChuangHui')) }}</template>
          </el-table-column>
          <el-table-column label="主要出口方向" min-width="160" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).ChuKouFangXiang || '' }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="140" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).BeiZhu || '' }}</template>
          </el-table-column>
        </template>

        <!-- ========== 主要技术经济指标 zhuyaojishujingji ========== -->
        <template v-else-if="moduleKey === 'zhuyaojishujingji'">
          <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed />
          <el-table-column label="单位名称" width="180" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column label="工业增加值" min-width="100" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'ZengJiaZhiLeiJi' : 'ZengJiaZhi')) }}</template>
          </el-table-column>
          <el-table-column label="营业（销售）收入" align="center">
            <el-table-column label="计" min-width="110" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'YingYeShouRuHeJiLeiJi' : 'YingYeShouRuHeJi')) }}</template>
            </el-table-column>
            <el-table-column label="主营业务销售收入" min-width="140" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'ZhuYingYeWuLeiJi' : 'ZhuYingYeWu')) }}</template>
            </el-table-column>
            <el-table-column label="其它业务销售收入" min-width="140" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'FeiMeiWaiXiaoLeiJi' : 'FeiMeiWaiXiao')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="利税合计(预计）" align="center">
            <el-table-column label="计" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'LiShuiHeJiLeiJi' : 'LiShuiHeJi')) }}</template>
            </el-table-column>
            <el-table-column label="利润" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'LiRunLeiJi' : 'LiRun')) }}</template>
            </el-table-column>
            <el-table-column label="税金" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'ShuiJinLeiJi' : 'ShuiJin')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="从业人员劳动报酬" min-width="140" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'CYRYLDBaoChouLeiJi' : 'CYRYLDBaoChou')) }}</template>
          </el-table-column>
          <el-table-column label="全员价值效率(万元/人)" min-width="170" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'QYJZXiaoLvLeiJi' : 'QYJZXiaoLv')) }}</template>
          </el-table-column>
          <el-table-column label="平均从业人员(人)" min-width="140" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'PJCYRenYuanLeiJi' : 'PJCYRenYuan')) }}</template>
          </el-table-column>
        </template>

        <!-- ========== 电力技术指标 dianlijishu ========== -->
        <template v-else-if="moduleKey === 'dianlijishu'">
          <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed />
          <el-table-column label="单位名称" width="180" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column label="发电量(万千瓦时)" min-width="110" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'FaDianLiangLeiJi' : 'FaDianLiang')) }}</template>
          </el-table-column>
          <el-table-column label="供电量" align="center">
            <el-table-column label="合计(万千瓦时)" min-width="120" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'GongDianLiangHeJiLeiJi' : 'GongDianLiangHeJi')) }}</template>
            </el-table-column>
            <el-table-column label="矿用电量(万千瓦时)" min-width="140" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'KuangYongDianLiangLeiJi' : 'KuangYongDianLiang')) }}</template>
            </el-table-column>
            <el-table-column label="上网电量(万千瓦时)" min-width="140" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'ShangWangDianLiangLeiJi' : 'ShangWangDianLiang')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="供热量 蒸吨" min-width="110" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'GongReLiangLeiJi' : 'GongReLiang')) }}</template>
          </el-table-column>
          <el-table-column label="发电耗用" align="center">
            <el-table-column label="煤泥量(吨)" min-width="110" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'MeiNiLiangLeiJi' : 'MeiNiLiang')) }}</template>
            </el-table-column>
            <el-table-column label="煤矸石量(吨)" min-width="120" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'MeiGanShiLiangLeiJi' : 'MeiGanShiLiang')) }}</template>
            </el-table-column>
            <el-table-column label="洗中煤(吨)" min-width="110" align="right">
              <template v-slot="scope">{{ format(d(scope, isLeiji ? 'XiZhongMeiLiangLeiJi' : 'XiZhongMeiLiang')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="发电成本(元/千瓦时)" min-width="150" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'FaDianChengBenLeiJi' : 'FaDianChengBen')) }}</template>
          </el-table-column>
          <el-table-column label="电量收入(万元)" min-width="120" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'DianLiangShouRuLeiJi' : 'DianLiangShouRu')) }}</template>
          </el-table-column>
          <el-table-column label="利润(万元)" min-width="100" align="right">
            <template v-slot="scope">{{ format(d(scope, isLeiji ? 'LiRuiLeiJi' : 'LiRui')) }}</template>
          </el-table-column>
        </template>

        <!-- ========== 化工业务指标 huagongyewu ========== -->
        <template v-else-if="moduleKey === 'huagongyewu'">
          <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed />
          <el-table-column label="单位名称" width="180" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column label="品种" min-width="100" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).PinZhong || '' }}</template>
          </el-table-column>
          <el-table-column label="计量单位" width="80" align="center">
            <template v-slot="scope">{{ (scope.row.data || {}).JiLiangDanWei || '' }}</template>
          </el-table-column>
          <el-table-column label="产量">
            <el-table-column label="本月完成" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'BenYueWC')) }}</template>
            </el-table-column>
            <el-table-column label="累计完成" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'LeiJiWC')) }}</template>
            </el-table-column>
            <el-table-column label="上年同期" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'ShangNianTongQi')) }}</template>
            </el-table-column>
            <el-table-column label="比上年(%)" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'BiShangNian')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="生产成本">
            <el-table-column label="单位" width="80" align="center">
              <template v-slot="scope">{{ (scope.row.data || {}).SCChengBenDanWei || '' }}</template>
            </el-table-column>
            <el-table-column label="本月" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'SCChengBenBenYue')) }}</template>
            </el-table-column>
            <el-table-column label="累计" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'SCChengBenLeiJi')) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="开工率">
            <el-table-column label="单位" width="80" align="center">
              <template v-slot="scope">{{ (scope.row.data || {}).KaiGongLvDanWei || '' }}</template>
            </el-table-column>
            <el-table-column label="本月" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'KaiGongLvBenYue')) }}</template>
            </el-table-column>
            <el-table-column label="累计" min-width="100" align="right">
              <template v-slot="scope">{{ format(d(scope, 'KaiGongLvLeiJi')) }}</template>
            </el-table-column>
          </el-table-column>
        </template>

        <!-- ========== 非煤劳动工资 feimeilaodonggongzi ========== -->
        <template v-else-if="moduleKey === 'feimeilaodonggongzi'">
          <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed />
          <el-table-column label="单位名称" width="180" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column label="人数" align="center">
            <el-table-column label="合计" min-width="90" align="right"><template v-slot="scope">{{ format(d(scope, 'RenShuHeJi')) }}</template></el-table-column>
            <el-table-column label="其中：女职工" min-width="110" align="right"><template v-slot="scope">{{ format(d(scope, 'NvZhiGong')) }}</template></el-table-column>
            <el-table-column label="管理人员" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'GuanLiRenYuan')) }}</template></el-table-column>
            <el-table-column label="专业人员" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'ZhuanYeRenYuan')) }}</template></el-table-column>
            <el-table-column label="其中：全民" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'QuanMin')) }}</template></el-table-column>
            <el-table-column label="集体" min-width="90" align="right"><template v-slot="scope">{{ format(d(scope, 'JiTi')) }}</template></el-table-column>
            <el-table-column label="其他" min-width="90" align="right"><template v-slot="scope">{{ format(d(scope, 'QiTa')) }}</template></el-table-column>
            <el-table-column label="第一产业" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'YiChanYe')) }}</template></el-table-column>
            <el-table-column label="第二产业" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'ErChanYe')) }}</template></el-table-column>
            <el-table-column label="其中：工业" min-width="110" align="right"><template v-slot="scope">{{ format(d(scope, 'ErChanYeGongYe')) }}</template></el-table-column>
            <el-table-column label="建筑业" min-width="90" align="right"><template v-slot="scope">{{ format(d(scope, 'ErChanYeJianZhuYe')) }}</template></el-table-column>
            <el-table-column label="第三产业" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'SanChanYe')) }}</template></el-table-column>
          </el-table-column>
          <el-table-column label="平均人数" align="center">
            <el-table-column label="本月" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'BenYuePingJunRenShu')) }}</template></el-table-column>
            <el-table-column label="累计" min-width="100" align="right"><template v-slot="scope">{{ format(d(scope, 'LeiJiPingJunRenShu')) }}</template></el-table-column>
          </el-table-column>
          <el-table-column label="工资总额(万元)" align="center">
            <el-table-column label="本月" min-width="110" align="right"><template v-slot="scope">{{ format(d(scope, 'BenYueGongZiZongE')) }}</template></el-table-column>
            <el-table-column label="累计" min-width="110" align="right"><template v-slot="scope">{{ format(d(scope, 'LeiJiGongZiZongE')) }}</template></el-table-column>
          </el-table-column>
          <el-table-column label="平均工资（元）" align="center">
            <el-table-column label="本月" min-width="110" align="right"><template v-slot="scope">{{ format(d(scope, 'BenYuePingJunGongZi')) }}</template></el-table-column>
            <el-table-column label="累计" min-width="110" align="right"><template v-slot="scope">{{ format(d(scope, 'LeiJiPingJunGongZi')) }}</template></el-table-column>
          </el-table-column>
        </template>

        <!-- ========== 默认/回退 ========== -->
        <template v-else>
          <el-table-column prop="xuhao" label="序号" width="80" align="center" fixed />
          <el-table-column label="单位名称" width="200" fixed>
            <template v-slot="scope"><span :style="unitStyle(scope.row)">{{ scope.row.danweiMingcheng }}</span></template>
          </el-table-column>
          <el-table-column v-for="c in showColumns" :key="c.name" :label="c.label" min-width="120" align="right">
            <template v-slot="scope">{{ format((scope.row.data || {})[c.name]) }}</template>
          </el-table-column>
        </template>
      </el-table>
      <el-empty v-if="tableData.length === 0" description="暂无数据，请调整查询条件"></el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Search, Download } from "@element-plus/icons-vue";
import request from "@/utils/request";

const route = useRoute();
const moduleKey = computed(() => route.meta.moduleKey);
const user = JSON.parse(localStorage.getItem("xm-user") || "{}");

const moduleName = ref("");
const columns = ref([]);
const dimensionColumns = ref([]);
const dimensionLabels = ref({});
const dimensionOptions = ref({});
const tableData = ref([]);
const unitList = ref([]);
const loading = ref(false);

const queryForm = reactive({
  leibie: "本月",
  nianfen: new Date().getFullYear().toString(),
  yuefen: new Date().getMonth() + 1,
  danweiId: null,
  dimensions: {}
});

const isLeiji = computed(() => queryForm.leibie === "累计");

// 当前选中单位的名称
const unitName = computed(() => {
  const u = unitList.value.find(u => u.id === queryForm.danweiId);
  return u ? u.mingcheng : "兖矿集团公司";
});

// 报表标题（中文名称）
const reportTitleMap = {
  jingyingzongzhi: "生产经营总值",
  chanpinchanxiaocun: "主要工业产品产、销、存",
  chukouchanpin: "出口产品情况统计表",
  zhuyaojishujingji: "主要技术经济指标",
  dianlijishu: "电力企业主营业务技术指标",
  huagongyewu: "化工企业主营业务技术指标",
  feimeilaodonggongzi: "非煤产业劳动工资统计表"
};
const reportTitle = computed(() => reportTitleMap[moduleKey.value] || moduleName.value);

// 单位后缀
const reportUnitMap = {
  jingyingzongzhi: "金额单位：万元",
  chanpinchanxiaocun: "单位：万元"
};
const reportUnit = computed(() => reportUnitMap[moduleKey.value] || "");

// 根据类别过滤显示列（仅用于默认/回退模板）
const showColumns = computed(() => {
  return columns.value
    .filter(c => !["ID", "DanWeiID", "NianFen", "YueFen"].includes(c.name))
    .filter(c => queryForm.leibie === "累计" ? c.cumulative : !c.cumulative);
});

// 单位名称样式（缩进+加粗）
const unitStyle = (row) => ({
  paddingLeft: (row.level || 0) * 16 + 'px',
  fontWeight: row.isTotal ? 'bold' : 'normal'
});

// 从scope中提取data字段值的快捷方法
const d = (scope, col) => (scope.row.data || {})[col];

const loadMeta = async () => {
  const res = await request.get(`/tongji-module/${moduleKey.value}/meta`);
  if (res.code !== "200") return;
  moduleName.value = res.data.moduleName;
  columns.value = res.data.columns || [];
  dimensionColumns.value = res.data.dimensionColumns || [];
  dimensionOptions.value = res.data.dimensionOptions || {};
  dimensionLabels.value = res.data.dimensionLabels || {};
  dimensionColumns.value.forEach(k => (queryForm.dimensions[k] = null));
};

const loadUnits = async () => {
  const res = await request.get(`/tongji-module/${moduleKey.value}/units/accessible`, { params: { danweiBianma: user.danweibianma, roleid: user.roleid } });
  if (res.code !== "200") return;
  unitList.value = res.data || [];
  if (!queryForm.danweiId && unitList.value.length) queryForm.danweiId = unitList.value[0].id;
};

const query = async () => {
  if (!queryForm.danweiId) return ElMessage.warning("请先选择单位");
  loading.value = true;
  try {
    const params = { danweiId: queryForm.danweiId, nianfen: queryForm.nianfen, yuefen: queryForm.yuefen, leibie: queryForm.leibie, ...queryForm.dimensions };
    const res = await request.get(`/tongji-module/${moduleKey.value}/query`, { params });
    if (res.code === "200") tableData.value = res.data || [];
  } finally { loading.value = false; }
};

const exportExcel = () => {
  if (!queryForm.danweiId) return ElMessage.warning("请先选择单位");
  const token = user.token || "";
  const p = new URLSearchParams({ danweiId: queryForm.danweiId, nianfen: queryForm.nianfen, yuefen: queryForm.yuefen, leibie: queryForm.leibie, token });
  Object.entries(queryForm.dimensions).forEach(([k, v]) => {
    if (v !== null && v !== undefined && String(v).trim() !== "") p.append(k, v);
  });
  window.open(`${import.meta.env.VITE_BASE_URL}/tongji-module/${moduleKey.value}/export?${p.toString()}`, "_blank");
};

const format = v => {
  if (v === null || v === undefined || v === "") return "";
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return n === 0 ? "" : n.toLocaleString("zh-CN", { maximumFractionDigits: 4 });
};

watch(moduleKey, async () => { tableData.value = []; columns.value = []; await loadMeta(); await loadUnits(); await query(); });

onMounted(async () => { await loadMeta(); await loadUnits(); await query(); });
</script>

<style scoped>
.czkb-manage { padding: 0; }
.card { background: #fff; padding: 16px; border-radius: 6px; }
.report-header { margin-bottom: 12px; }
.report-title { font-size: 22px; font-weight: 700; text-align: center; color: #303133; margin-bottom: 8px; }
.report-info { display: flex; justify-content: space-between; align-items: center; padding: 0 10px; color: #606266; font-size: 14px; }
.report-info-left { font-weight: 500; }
.report-info-center { font-weight: 500; }
.report-info-right { font-weight: 500; color: #909399; }
.premium-table { font-size: 13px; }
</style>
