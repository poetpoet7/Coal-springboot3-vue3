package com.example.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.UserInfo;
import com.example.exception.CustomException;
import com.example.service.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器（只支持UserInfo用户）
 */
@Component
public class JWTInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JWTInterceptor.class);

    @Resource
    private UserInfoService userInfoService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. 从http请求标头里面拿到token
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isNull(token)) {
            // 如果没拿到，那么再从请求参数里拿一次
            token = request.getParameter(Constants.TOKEN);
        }
        // 2. 开始执行认证
        if (ObjectUtil.isNull(token)) {
            log.warn("Token为空");
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }

        Account account = null;
        try {
            String audience = JWT.decode(token).getAudience().get(0);
            log.info("Token audience: {}", audience);

            String[] parts = audience.split("@@"); // 使用@@分隔，避免与密码hash中的-冲突
            String userId = parts[0];
            log.info("解析Token - userId: {}, parts length: {}", userId, parts.length);

            // 查询UserInfo用户
            UserInfo userInfo = userInfoService.selectByIdWithPassword(Integer.valueOf(userId));
            if (userInfo != null) {
                log.info("查询到用户: {}, 密码: {}", userInfo.getLoginname(), userInfo.getPassword());
                // 将UserInfo转换为Account（用于密码验证）
                account = new Account();
                account.setPassword(userInfo.getPassword());
            } else {
                log.warn("未找到用户: {}", userId);
            }
        } catch (Exception e) {
            log.error("解析Token失败", e);
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }

        // 根据token里面携带的用户ID查询用户
        if (ObjectUtil.isNull(account)) {
            log.warn("Account为空");
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }

        try {
            // 通过用户的密码作为密钥再次验证token的合法性
            log.info("使用密码验证Token: {}", account.getPassword());
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(account.getPassword())).build();
            jwtVerifier.verify(token); // 验证token
            log.info("Token验证成功");
        } catch (JWTVerificationException e) {
            log.error("Token验证失败: {}", e.getMessage());
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }

}
