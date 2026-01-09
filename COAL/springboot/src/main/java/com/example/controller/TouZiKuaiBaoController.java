package com.example.controller;

import com.example.common.Result;
import com.example.entity.Danwei;
import com.example.service.impl.TouZiKuaiBaoService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * @param leibie   类别（本月/本年）
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
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam Integer danweiId,
            @RequestParam Integer nianfen,
            @RequestParam(required = false) Integer yuefen,
            @RequestParam String leibie) {
        try {
            byte[] excelData = touZiKuaiBaoService.exportExcel(danweiId, nianfen, yuefen, leibie);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "产能投资快报_" + nianfen + (yuefen != null ? "_" + yuefen : "") + ".xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
