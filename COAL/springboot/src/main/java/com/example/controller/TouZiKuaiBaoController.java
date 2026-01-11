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
        // 已上报的不能修改
        if ("已上报".equals(existing.getZhuangtai())) {
            return Result.error("已上报的记录不能修改");
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
}
