package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.service.UserInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebController {

    @Resource
    private UserInfoService userInfoService;

    /**
     * 默认请求接口
     */
    @GetMapping("/")
    public Result hello() {
        return Result.success();
    }

    /**
     * 统一登录接口（优先使用UserInfo表，兼容C#系统老用户）
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        // 直接调用UserInfoService登录（使用userinfo表）
        // 这样可以兼容C#系统的老用户
        return Result.success(userInfoService.login(account));
    }

    /**
     * 注册 (根据项目需求进行补全)
     */
    @PostMapping("/register")
    public Result register() {
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        // 调用UserInfoService修改密码
        userInfoService.updatePassword(account);
        return Result.success();
    }

}
