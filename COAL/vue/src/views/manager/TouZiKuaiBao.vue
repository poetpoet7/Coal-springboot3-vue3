<template>
  <div>
    <!-- 查询表单 -->
    <div class="card" style="margin-bottom: 10px">
      <el-form :model="queryForm" :inline="true" label-width="80px">
        <el-form-item label="类别">
          <el-select v-model="queryForm.leibie" placeholder="请选择类别" style="width: 120px">
            <el-option label="本月" value="本月"></el-option>
            <el-option label="本年" value="本年"></el-option>
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
        
        <el-form-item label="月份" v-if="queryForm.leibie === '本月'">
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
      <!-- 日期 -->
      <div class="report-date">{{ reportDate }}</div>
      
      <!-- 编制单位 -->
      <div class="report-company">编制单位：{{ selectedUnitName }}</div>
      
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', textAlign: 'center', fontSize: '12px'}"
        :cell-style="getCellStyle"
      >
        <el-table-column prop="xuhao" label="序号" width="60" align="center" fixed></el-table-column>
        <el-table-column prop="danweiMingcheng" label="单位名称" width="120" fixed></el-table-column>
        
        <!-- 经营总值(万元) - 只显示计 -->
        <el-table-column label="经营总值(万元)" width="100" align="right">
          <template v-slot="scope">
            {{ formatNumber(scope.row.data?.jingyingzongzhiheji) }}
          </template>
        </el-table-column>
        
        <!-- 工业产值(现价) - 只显示计 -->
        <el-table-column label="工业产值(现价)(万元)" width="110" align="right">
          <template v-slot="scope">
            {{ formatNumber(scope.row.data?.gongyechanzhi) }}
          </template>
        </el-table-column>
        
        <!-- 其他产值 -->
        <el-table-column label="其他产值(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.qitachanzhi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.qitachanzhileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 新产品产值 -->
        <el-table-column label="新产品产值(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.xinchanpinjiazhi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.xinchanpinjiazhileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 工业销售产值 -->
        <el-table-column label="工业销售产值(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.gyxsczheji) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.gyxsczhejileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 出口交货值 -->
        <el-table-column label="出口交货值(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.chukoujiaohuozhi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.chukoujiaohuozhileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 90年不变价 -->
        <el-table-column label="90年不变价" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jiushinianbubianjia) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jiushinianbubianjialeiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 企业用电量 -->
        <el-table-column label="企业用电量(度)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qiyeyongdianliang) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qiyeyongdianliangleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 固定资产投资 -->
        <el-table-column label="固定资产投资(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.gdzctzheji) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.gdzctzhejileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 技术改造 -->
        <el-table-column label="技术改造(万元)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.jishugaizao) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatNumber(scope.row.data?.jishugaizaoleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 原煤 -->
        <el-table-column label="原煤(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.yuanmei) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.yuanmeileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 精煤 -->
        <el-table-column label="精煤(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jingmei) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jingmeileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 尿素 -->
        <el-table-column label="尿素(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.niaosu) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.niaosuleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 甲醇 -->
        <el-table-column label="甲醇(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jiachun) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jiachunleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 醋酸 -->
        <el-table-column label="醋酸(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cusuan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cusuanleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 焦炭 -->
        <el-table-column label="焦炭(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jiaotan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jiaotanleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 醋酸乙酯 -->
        <el-table-column label="醋酸乙酯(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cusuanyizhi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cusuanyizhileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 醋酸丁酯 -->
        <el-table-column label="醋酸丁酯(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cusuandingzhi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cusuandingzhileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 醋酐 -->
        <el-table-column label="醋酐(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cugan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.cuganleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 碳酸二甲酯 -->
        <el-table-column label="碳酸二甲酯(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.tansuanerjiazhi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.tansuanerjiazhileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 合成氨 -->
        <el-table-column label="合成氨(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.hechengan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.hechenganleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 丁醇 -->
        <el-table-column label="丁醇(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.dingchun) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.dingchunleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 聚甲醛 -->
        <el-table-column label="聚甲醛(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jujiaquan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.jujiaquanleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 改质沥青 -->
        <el-table-column label="改质沥青(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.gaizhiliqing) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.gaizhiliqingleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 蒽油 -->
        <el-table-column label="蒽油(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.enyou) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.enyouleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 复合肥 -->
        <el-table-column label="复合肥(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.fuhefei) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.fuhefeileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 铝锭 -->
        <el-table-column label="铝锭(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.lvding) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.lvdingleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 碳素制品 -->
        <el-table-column label="碳素制品(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.tansuzhipin) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.tansuzhipinleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 铝铸材 -->
        <el-table-column label="铝铸材(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.lvzhucai) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.lvzhucaileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 铝挤压材 -->
        <el-table-column label="铝挤压材(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.lvjiyacai) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.lvjiyacaileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 发电量 -->
        <el-table-column label="发电量(度)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.fadianliang) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.fadianliangleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 皮带运输机 -->
        <el-table-column label="皮带运输机(米)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.pidaiyunshuji) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.pidaiyunshujileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 输送带 -->
        <el-table-column label="输送带(米)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.shusongdai) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.shusongdaileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 电缆 -->
        <el-table-column label="电缆(米)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.dianlan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.dianlanleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 液压支架 -->
        <el-table-column label="液压支架(架)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.yeyazhijia) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.yeyazhijialeiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 刮板运输机 -->
        <el-table-column label="刮板运输机(台)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.guabanyunshuji) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.guabanyunshujileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 高岭土 -->
        <el-table-column label="高岭土(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.gaolingtu) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.gaolingtuleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 轻柴油 -->
        <el-table-column label="轻柴油(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qingchaiyou) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qingchaiiyouleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 重柴油 -->
        <el-table-column label="重柴油(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.zhongchaiyou) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.zhongchaiyouleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 石脑油 -->
        <el-table-column label="石脑油(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.shinaoyou) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.shinaoyouleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 液化石油气 -->
        <el-table-column label="液化石油气(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.yehuashiyouqi) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.yehuashiyouqileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 其他产品 -->
        <el-table-column label="其他产品(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qitachanpin) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qitachanpinleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 硫磺 -->
        <el-table-column label="硫磺(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.liuhuang) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.liuhuangleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 硫酸铵 -->
        <el-table-column label="硫酸铵(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.liuhuangan) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.liuhuanganleiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 去年原煤 -->
        <el-table-column label="去年原煤(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qunianyuanmei) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qunianyuanmeileiji) }}
            </template>
          </el-table-column>
        </el-table-column>
        
        <!-- 去年精煤 -->
        <el-table-column label="去年精煤(吨)" align="center">
          <el-table-column label="计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qunianjingmei) }}
            </template>
          </el-table-column>
          <el-table-column label="累计" width="90" align="right">
            <template v-slot="scope">
              {{ formatLong(scope.row.data?.qunianjingmeileiji) }}
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

// 获取选中单位名称
const selectedUnitName = computed(() => {
  const unit = unitList.value.find(u => u.id === queryForm.danweiId);
  return unit ? unit.mingcheng : '';
});

// 获取报表日期显示
const reportDate = computed(() => {
  if (queryForm.leibie === '本月' && queryForm.yuefen) {
    return `${queryForm.nianfen}年${queryForm.yuefen}月（本月）`;
  } else {
    return `${queryForm.nianfen}年`;
  }
});

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

// 加载单位列表
const loadUnits = () => {
  request.get('/touzikuaibao/units').then(res => {
    if (res.code === '200') {
      unitList.value = res.data || [];
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
    leibie: queryForm.leibie
  };
  
  if (queryForm.leibie === '本月' && queryForm.yuefen) {
    params.yuefen = queryForm.yuefen;
  }
  
  request.get('/touzikuaibao/query', { params }).then(res => {
    if (res.code === '200') {
      tableData.value = res.data || [];
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
  
  const params = new URLSearchParams({
    danweiId: queryForm.danweiId,
    nianfen: queryForm.nianfen,
    leibie: queryForm.leibie
  });
  
  if (queryForm.leibie === '本月' && queryForm.yuefen) {
    params.append('yuefen', queryForm.yuefen);
  }
  
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
  margin-bottom: 5px;
}

/* 报表日期 */
.report-date {
  text-align: center;
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
}

/* 表格包装器 */
.table-wrapper {
  display: flex;
  gap: 10px;
}

/* 编制单位 */
.report-company {
  text-align: left;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  padding-left: 5px;
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
