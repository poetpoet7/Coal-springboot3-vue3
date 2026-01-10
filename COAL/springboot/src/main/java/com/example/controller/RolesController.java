package com.example.controller;

import com.example.common.Result;
import com.example.entity.Roles;
import com.example.mapper.RolesMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/roles")
public class RolesController {

    @Resource
    private RolesMapper rolesMapper;

    /**
     * 查询所有角色
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        List<Roles> list = rolesMapper.selectList(null);
        return Result.success(list);
    }
}
