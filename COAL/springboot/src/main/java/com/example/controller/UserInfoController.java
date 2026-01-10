package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.UserInfo;
import com.example.service.UserInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信息控制器（完全替换Admin，使用userinfo表）
 */
@RestController
@RequestMapping("/userinfo")
public class UserInfoController {

    @Resource
    private UserInfoService userInfoService;

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public Result add(@RequestBody UserInfo userInfo) {
        userInfoService.add(userInfo);
        return Result.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/update")
    public Result update(@RequestBody UserInfo userInfo) {
        userInfoService.updateById(userInfo);
        return Result.success();
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        userInfoService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        userInfoService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        UserInfo userInfo = userInfoService.selectById(id);
        return Result.success(userInfo);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(UserInfo userInfo) {
        List<UserInfo> list = userInfoService.selectAll(userInfo);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(UserInfo userInfo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<UserInfo> page = userInfoService.selectPage(userInfo, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 用户登录接口（兼容C#系统老用户）
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        UserInfo userInfo = userInfoService.login(account);
        return Result.success(userInfo);
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        userInfoService.updatePassword(account);
        return Result.success();
    }
}
