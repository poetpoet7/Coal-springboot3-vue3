package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.Result;
import com.example.entity.TbDanWei;
import com.example.mapper.TbDanWeiMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 单位信息控制器
 */
@RestController
@RequestMapping("/danwei")
public class TbDanWeiController {

    @Resource
    private TbDanWeiMapper tbDanWeiMapper;

    /**
     * 获取所有单位列表（用于下拉选择）
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        QueryWrapper<TbDanWei> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("BianMa");
        List<TbDanWei> list = tbDanWeiMapper.selectList(queryWrapper);
        return Result.success(list);
    }
}
