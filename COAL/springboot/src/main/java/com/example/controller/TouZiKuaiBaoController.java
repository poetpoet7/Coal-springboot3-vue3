package com.example.controller;

import com.example.common.Result;
import com.example.entity.Danwei;
import com.example.entity.TongjiCzcptouzikuaibao;
import com.example.service.impl.TouZiKuaiBaoService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 产能投资快报控制器
 */
@RestController
@RequestMapping("/touzikuaibao")
public class TouZiKuaiBaoController {

    @Resource
    private TouZiKuaiBaoService touZiKuaiBaoService;

    /**
     * 获取所有单位列表（用于下拉选择）
     */
    @GetMapping("/units")
    public Result getAllUnits() {
        List<Danwei> units = touZiKuaiBaoService.getAllUnits();
        return Result.success(units);
    }

    /**
     * 获取用户可访问的单位列表（基于权限过滤）
     * 管理员返回全部，普通用户返回本单位及下属单位
     * 
     * @param danweiBianma 用户的单位编码
     * @param roleid       用户的角色ID
     */
    @GetMapping("/units/accessible")
    public Result getAccessibleUnits(@RequestParam String danweiBianma, @RequestParam Integer roleid) {
        List<Danwei> units = touZiKuaiBaoService.getAccessibleUnits(danweiBianma, roleid);
        return Result.success(units);
    }

    /**
     * 查询投资快报数据
     * 
     * @param danweiId 单位ID
     * @param nianfen  年份
     * @param yuefen   月份
     * @param leibie   类别（本月/累计）
     */
    @GetMapping("/query")
    public Result queryReport(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen,
            @RequestParam String leibie) {
        List<Map<String, Object>> data = touZiKuaiBaoService.getReportData(danweiId, nianfen, yuefen, leibie);
        return Result.success(data);
    }

    /**
     * 导出Excel
     * 文件名格式：{年}年{月}月产值、主要产品产量及固定资产投资快报({本月/累计}）.xlsx
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen,
            @RequestParam String leibie) {
        try {
            byte[] excelData = touZiKuaiBaoService.exportExcel(danweiId, nianfen, yuefen, leibie);

            // 构建文件名，格式与Excel文件一致
            // 例如：2025年5月产值、主要产品产量及固定资产投资快报(本月）.xlsx
            String fileName = nianfen + "年" + (yuefen != null ? yuefen : 1) + "月产值、主要产品产量及固定资产投资快报(" + leibie
                    + "）.xlsx";
            // URL编码中文文件名
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 使用RFC 5987标准格式，同时设置filename和filename*以兼容不同浏览器
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== 基层单位填报功能 ====================

    /**
     * 获取单位信息（根据单位编码）
     * 返回单位ID、名称、是否为基层单位等信息
     */
    @GetMapping("/unitInfo")
    public Result getUnitInfo(@RequestParam String danweiBianma) {
        Map<String, Object> info = touZiKuaiBaoService.getUnitInfo(danweiBianma);
        return Result.success(info);
    }

    /**
     * 检查是否为基层单位（可填报）
     */
    @GetMapping("/canEdit/{danweiBianma}")
    public Result canEdit(@PathVariable String danweiBianma) {
        boolean isBase = touZiKuaiBaoService.isBaseUnit(danweiBianma);
        return Result.success(isBase);
    }

    /**
     * 查询本单位填报列表
     */
    @GetMapping("/list")
    public Result getLocalList(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen) {
        List<TongjiCzcptouzikuaibao> list = touZiKuaiBaoService.getLocalReportList(danweiId, nianfen, yuefen);
        return Result.success(list);
    }

    /**
     * 获取上月数据（用于计算累计值）
     */
    @GetMapping("/lastMonth")
    public Result getLastMonthData(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam Integer yuefen) {
        TongjiCzcptouzikuaibao data = touZiKuaiBaoService.getLastMonthData(danweiId, nianfen, yuefen);
        return Result.success(data);
    }

    /**
     * 添加快报记录
     */
    @PostMapping("/add")
    public Result addReport(@RequestBody TongjiCzcptouzikuaibao report) {
        // 检查是否已存在相同年月记录
        if (touZiKuaiBaoService.existsRecord(report.getDanweiid(), report.getNianfen(), report.getYuefen(), null)) {
            return Result.error("该月份已存在记录，请勿重复添加");
        }
        // 设置初始状态为待上报
        report.setZhuangtai("待上报");
        touZiKuaiBaoService.saveReport(report);
        return Result.success("添加成功");
    }

    /**
     * 更新快报记录
     */
    @PutMapping("/update/{id}")
    public Result updateReport(@PathVariable Long id, @RequestBody TongjiCzcptouzikuaibao report) {
        // 检查记录是否存在
        TongjiCzcptouzikuaibao existing = touZiKuaiBaoService.getById(id);
        if (existing == null) {
            return Result.error("记录不存在");
        }
        // 只有"待上报"或"返回修改"状态才能修改
        String status = existing.getZhuangtai();
        if (!"待上报".equals(status) && !"返回修改".equals(status)) {
            return Result.error("只有待上报或返回修改状态的记录才能修改");
        }
        // 检查是否会与其他记录冲突
        if (touZiKuaiBaoService.existsRecord(report.getDanweiid(), report.getNianfen(), report.getYuefen(), id)) {
            return Result.error("该月份已存在其他记录");
        }
        report.setId(id);
        touZiKuaiBaoService.saveReport(report);
        return Result.success("更新成功");
    }

    /**
     * 删除快报记录
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteReport(@PathVariable Long id) {
        boolean success = touZiKuaiBaoService.deleteReport(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败，记录不存在或已上报");
        }
    }

    /**
     * 上报记录
     */
    @PostMapping("/submit/{id}")
    public Result submitReport(@PathVariable Long id) {
        boolean success = touZiKuaiBaoService.submitReport(id);
        if (success) {
            return Result.success("上报成功");
        } else {
            return Result.error("上报失败，记录不存在");
        }
    }

    /**
     * 根据ID获取单条记录
     */
    @GetMapping("/get/{id}")
    public Result getById(@PathVariable Long id) {
        TongjiCzcptouzikuaibao report = touZiKuaiBaoService.getById(id);
        return Result.success(report);
    }

    // ==================== 审批流程相关接口 ====================

    /**
     * 审批通过
     * 
     * @param id               记录ID
     * @param operatorDanweiId 操作者单位ID
     * @param forceApprove     是否强制审批（跳过下级未审批警告）
     */
    @PostMapping("/approve/{id}")
    public Result approveReport(@PathVariable Long id,
            @RequestParam Integer operatorDanweiId,
            @RequestParam(defaultValue = "false") boolean forceApprove) {
        String result = touZiKuaiBaoService.approveReport(id, operatorDanweiId, forceApprove);
        if ("审批成功".equals(result)) {
            return Result.success(result);
        } else if (result.startsWith("SKIP_WARNING:")) {
            // 跳级审批警告，返回特殊格式让前端处理
            String pendingUnits = result.substring("SKIP_WARNING:".length());
            return Result.error("SKIP_WARNING:" + pendingUnits);
        } else {
            return Result.error(result);
        }
    }

    /**
     * 返回修改（退回）
     * 
     * @param id               记录ID
     * @param operatorDanweiId 操作者单位ID
     */
    @PostMapping("/return/{id}")
    public Result returnForModification(@PathVariable Long id, @RequestParam Integer operatorDanweiId) {
        String result = touZiKuaiBaoService.returnForModification(id, operatorDanweiId);
        if ("退回成功".equals(result)) {
            return Result.success(result);
        } else {
            return Result.error(result);
        }
    }

    /**
     * 检查操作权限
     * 
     * @param operatorDanweiId 操作者单位ID
     * @param recordDanweiId   记录所属单位ID
     */
    @GetMapping("/canOperate")
    public Result canOperate(@RequestParam Integer operatorDanweiId, @RequestParam Integer recordDanweiId) {
        boolean canOperate = touZiKuaiBaoService.canOperateRecord(operatorDanweiId, recordDanweiId);
        return Result.success(canOperate);
    }

    /**
     * 获取记录的审批状态信息
     */
    @GetMapping("/approvalStatus/{id}")
    public Result getApprovalStatus(@PathVariable Long id) {
        return Result.success(touZiKuaiBaoService.getApprovalStatus(id));
    }
}
