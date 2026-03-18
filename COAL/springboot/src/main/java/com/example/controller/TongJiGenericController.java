package com.example.controller;

import com.example.common.Result;
import com.example.entity.Danwei;
import com.example.service.impl.TongJiGenericService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tongji-module/{moduleKey}")
public class TongJiGenericController {

    @Resource
    private TongJiGenericService service;

    @GetMapping("/meta")
    public Result meta(@PathVariable String moduleKey) {
        return Result.success(service.getMeta(moduleKey));
    }

    @GetMapping("/units")
    public Result units(@PathVariable String moduleKey) {
        List<Danwei> units = service.getAllUnits();
        return Result.success(units);
    }

    @GetMapping("/units/accessible")
    public Result accessible(@PathVariable String moduleKey, @RequestParam String danweiBianma, @RequestParam Integer roleid) {
        return Result.success(service.getAccessibleUnits(danweiBianma, roleid));
    }

    @GetMapping("/unitInfo")
    public Result unitInfo(@PathVariable String moduleKey, @RequestParam String danweiBianma) {
        return Result.success(service.getUnitInfo(danweiBianma));
    }

    @GetMapping("/canEdit/{danweiBianma}")
    public Result canEdit(@PathVariable String moduleKey, @PathVariable String danweiBianma) {
        return Result.success(service.isBaseUnit(danweiBianma));
    }

    @GetMapping("/list")
    public Result list(@PathVariable String moduleKey,
                       @RequestParam Integer danweiId,
                       @RequestParam Integer nianfen,
                       @RequestParam(required = false) Integer yuefen,
                       @RequestParam Map<String, String> allParams) {
        return Result.success(service.getLocalList(moduleKey, danweiId, nianfen, yuefen, extractFilters(moduleKey, allParams)));
    }

    @GetMapping("/query")
    public Result query(@PathVariable String moduleKey,
                        @RequestParam Integer danweiId,
                        @RequestParam Integer nianfen,
                        @RequestParam(required = false) Integer yuefen,
                        @RequestParam(defaultValue = "本月") String leibie,
                        @RequestParam Map<String, String> allParams) {
        return Result.success(service.getReportData(moduleKey, danweiId, nianfen, yuefen, leibie, extractFilters(moduleKey, allParams)));
    }

    @GetMapping("/lastMonth")
    public Result lastMonth(@PathVariable String moduleKey,
                            @RequestParam Integer danweiId,
                            @RequestParam Integer nianfen,
                            @RequestParam Integer yuefen,
                            @RequestParam Map<String, String> allParams) {
        return Result.success(service.getLastMonthData(moduleKey, danweiId, nianfen, yuefen, extractFilters(moduleKey, allParams)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@PathVariable String moduleKey,
                                         @RequestParam Integer danweiId,
                                         @RequestParam Integer nianfen,
                                         @RequestParam(required = false) Integer yuefen,
                                         @RequestParam(defaultValue = "本月") String leibie,
                                         @RequestParam Map<String, String> allParams) {
        try {
            byte[] data = service.exportExcel(moduleKey, danweiId, nianfen, yuefen, leibie, extractFilters(moduleKey, allParams));
            String encoded = service.buildExportFileName(moduleKey, nianfen, yuefen, leibie);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add")
    public Result add(@PathVariable String moduleKey, @RequestBody Map<String, Object> payload) {
        if (service.existsRecord(moduleKey, payload, null)) {
            return Result.error("同单位同年月记录已存在");
        }
        String statusColumn = service.getModule(moduleKey).getStatusColumn();
        payload.put(statusColumn, "待上报");
        service.saveOrUpdate(moduleKey, payload);
        return Result.success("添加成功");
    }

    @PutMapping("/update/{id}")
    public Result update(@PathVariable String moduleKey, @PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> existing = service.getById(moduleKey, id);
        if (existing == null) {
            return Result.error("记录不存在");
        }
        String status = String.valueOf(existing.getOrDefault(service.getModule(moduleKey).getStatusColumn(), ""));
        if (!(status.isEmpty() || "待上报".equals(status) || "返回修改".equals(status))) {
            return Result.error("当前状态不可修改");
        }
        if (service.existsRecord(moduleKey, payload, id)) {
            return Result.error("同单位同年月记录已存在");
        }
        payload.put("ID", id);
        service.saveOrUpdate(moduleKey, payload);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable String moduleKey, @PathVariable Long id) {
        return service.deleteRecord(moduleKey, id) ? Result.success("删除成功") : Result.error("删除失败，状态不允许或记录不存在");
    }

    @PostMapping("/submit/{id}")
    public Result submit(@PathVariable String moduleKey, @PathVariable Long id, @RequestParam(required = false) Integer operatorId) {
        return service.submitRecord(moduleKey, id, operatorId) ? Result.success("上报成功") : Result.error("上报失败");
    }

    @PostMapping("/approve/{id}")
    public Result approve(@PathVariable String moduleKey, @PathVariable Long id,
                          @RequestParam Integer operatorDanweiId,
                          @RequestParam(defaultValue = "false") boolean forceApprove,
                          @RequestParam(required = false) Integer operatorId) {
        String result = service.approveRecord(moduleKey, id, operatorDanweiId, forceApprove, operatorId);
        if ("审批成功".equals(result)) {
            return Result.success(result);
        }
        if (result.startsWith("SKIP_WARNING:")) {
            return Result.error(result);
        }
        return Result.error(result);
    }

    @PostMapping("/return/{id}")
    public Result ret(@PathVariable String moduleKey, @PathVariable Long id,
                      @RequestParam Integer operatorDanweiId,
                      @RequestParam(required = false) Integer operatorId) {
        String result = service.returnForModification(moduleKey, id, operatorDanweiId, operatorId);
        if ("退回成功".equals(result)) {
            return Result.success(result);
        }
        return Result.error(result);
    }

    @GetMapping("/approvalStatus/{id}")
    public Result approvalStatus(@PathVariable String moduleKey, @PathVariable Long id) {
        return Result.success(service.getApprovalStatus(moduleKey, id));
    }

    private Map<String, Object> extractFilters(String moduleKey, Map<String, String> allParams) {
        Map<String, Object> filters = new HashMap<>();
        for (String dim : service.getModule(moduleKey).getDimensionColumns()) {
            if (allParams.containsKey(dim)) {
                filters.put(dim, allParams.get(dim));
            }
        }
        return filters;
    }
}

