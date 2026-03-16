package com.example.controller;

import com.example.common.Result;
import com.example.entity.Danwei;
import com.example.entity.ChanPinHuoLiuQuXiang;
import com.example.service.impl.ChanPinHuoLiuQuXiangService;
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
 * 产品货流去向控制器
 */
@RestController
@RequestMapping("/chanpinhuoliu")
public class ChanPinHuoLiuQuXiangController {

    @Resource
    private ChanPinHuoLiuQuXiangService cphlqxService;

    @GetMapping("/units")
    public Result getAllUnits() {
        List<Danwei> units = cphlqxService.getAllUnits();
        return Result.success(units);
    }

    @GetMapping("/units/accessible")
    public Result getAccessibleUnits(@RequestParam String danweiBianma, @RequestParam Integer roleid) {
        List<Danwei> units = cphlqxService.getAccessibleUnits(danweiBianma, roleid);
        return Result.success(units);
    }

    @GetMapping("/query")
    public Result queryReport(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen,
            @RequestParam String leibie,
            @RequestParam(required = false) String chanpinmingcheng) {
        List<Map<String, Object>> data = cphlqxService.getReportData(danweiId, nianfen, yuefen, leibie, chanpinmingcheng);
        return Result.success(data);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen,
            @RequestParam(required = false) String chanpinmingcheng) {
        try {
            byte[] excelData = cphlqxService.exportExcel(danweiId, nianfen, yuefen, chanpinmingcheng);

            String cpmc = (chanpinmingcheng != null && !chanpinmingcheng.isEmpty()) ? chanpinmingcheng : "无产品";
            // 文件名格式：2022年3月主要产品（原煤）货流去向.xls
            String fileName = nianfen + "年" + (yuefen != null ? yuefen : 1) + "月主要产品（" + cpmc + "）货流去向.xls";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== 基层单位填报功能 ====================

    @GetMapping("/unitInfo")
    public Result getUnitInfo(@RequestParam String danweiBianma) {
        Map<String, Object> info = cphlqxService.getUnitInfo(danweiBianma);
        return Result.success(info);
    }

    @GetMapping("/canEdit/{danweiBianma}")
    public Result canEdit(@PathVariable String danweiBianma) {
        boolean isBase = cphlqxService.isBaseUnit(danweiBianma);
        return Result.success(isBase);
    }

    @GetMapping("/list")
    public Result getLocalList(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen,
            @RequestParam(required = false) String chanpinmingcheng) {
        List<ChanPinHuoLiuQuXiang> list = cphlqxService.getLocalReportList(danweiId, nianfen, yuefen, chanpinmingcheng);
        return Result.success(list);
    }

    @GetMapping("/lastMonth")
    public Result getLastMonthData(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam Integer yuefen,
            @RequestParam(required = false) String chanpinmingcheng) {
        ChanPinHuoLiuQuXiang data = cphlqxService.getLastMonthData(danweiId, nianfen, yuefen, chanpinmingcheng);
        return Result.success(data);
    }

    @PostMapping("/add")
    public Result addReport(@RequestBody ChanPinHuoLiuQuXiang report) {
        if (cphlqxService.existsRecord(report.getDanweiid(), report.getNianfen(), report.getYuefen(), report.getChanpinmingcheng(), null)) {
            return Result.error("该月份该产品已存在记录，请勿重复添加");
        }
        report.setZhuangtai("待上报");
        cphlqxService.saveReport(report);
        return Result.success("添加成功");
    }

    @PutMapping("/update/{id}")
    public Result updateReport(@PathVariable Long id, @RequestBody ChanPinHuoLiuQuXiang report) {
        ChanPinHuoLiuQuXiang existing = cphlqxService.getById(id);
        if (existing == null) {
            return Result.error("记录不存在");
        }
        String status = existing.getZhuangtai();
        if (!"待上报".equals(status) && !"返回修改".equals(status)) {
            return Result.error("只有待上报或返回修改状态的记录才能修改");
        }
        if (cphlqxService.existsRecord(report.getDanweiid(), report.getNianfen(), report.getYuefen(), report.getChanpinmingcheng(), id)) {
            return Result.error("该月份该产品已存在其他记录");
        }
        report.setId(id);
        cphlqxService.saveReport(report);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteReport(@PathVariable Long id) {
        boolean success = cphlqxService.deleteReport(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败，记录不存在或已上报");
        }
    }

    @PostMapping("/submit/{id}")
    public Result submitReport(@PathVariable Long id, @RequestParam(required = false) Integer operatorId) {
        boolean success = cphlqxService.submitReport(id, operatorId);
        if (success) {
            return Result.success("上报成功");
        } else {
            return Result.error("上报失败，记录不存在");
        }
    }

    @GetMapping("/get/{id}")
    public Result getById(@PathVariable Long id) {
        ChanPinHuoLiuQuXiang report = cphlqxService.getById(id);
        return Result.success(report);
    }

    // ==================== 审批流程相关接口 ====================

    @PostMapping("/approve/{id}")
    public Result approveReport(@PathVariable Long id,
            @RequestParam Integer operatorDanweiId,
            @RequestParam(defaultValue = "false") boolean forceApprove,
            @RequestParam(required = false) Integer operatorId) {
        String result = cphlqxService.approveReport(id, operatorDanweiId, forceApprove, operatorId);
        if ("审批成功".equals(result)) {
            return Result.success(result);
        } else if (result.startsWith("SKIP_WARNING:")) {
            String pendingUnits = result.substring("SKIP_WARNING:".length());
            return Result.error("SKIP_WARNING:" + pendingUnits);
        } else {
            return Result.error(result);
        }
    }

    @PostMapping("/return/{id}")
    public Result returnForModification(@PathVariable Long id, @RequestParam Integer operatorDanweiId,
            @RequestParam(required = false) Integer operatorId) {
        String result = cphlqxService.returnForModification(id, operatorDanweiId, operatorId);
        if ("退回成功".equals(result)) {
            return Result.success(result);
        } else {
            return Result.error(result);
        }
    }

    @GetMapping("/canOperate")
    public Result canOperate(@RequestParam Integer operatorDanweiId, @RequestParam Integer recordDanweiId) {
        boolean canOperate = cphlqxService.canOperateRecord(operatorDanweiId, recordDanweiId);
        return Result.success(canOperate);
    }

    @GetMapping("/approvalStatus/{id}")
    public Result getApprovalStatus(@PathVariable Long id) {
        return Result.success(cphlqxService.getApprovalStatus(id));
    }
}
