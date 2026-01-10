package com.example.utils;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.common.Constants;
import com.example.entity.UserInfo;
import com.example.service.UserInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

/**
 * Token工具类（完全使用UserInfo）
 */
@Component
public class TokenUtils {
    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    @Resource
    private UserInfoService userInfoService;

    private static UserInfoService staticUserInfoService;

    // 由于静态方法里面不能直接注入成员变量，所以利用staticUserInfoService进行了转换
    // @PostConstruct 它是一个生命周期回调方法，使得该方法在依赖注入完成后、对象初始化完成之前执行。
    @PostConstruct
    public void init() {
        staticUserInfoService = userInfoService;
    }

    /**
     * 生成JWT token
     */
    public static String createToken(String data, String sign) {
        // audience是存储数据的一个媒介(一个数组，可以存放多个，这里只存放了一个data) 存储用户ID，例如：1@@USERINFO@@1
        return JWT.create().withAudience(data)
                .withExpiresAt(DateUtil.offsetDay(new Date(), 1)) // 设置过期时间1天后
                .sign(Algorithm.HMAC256(sign)); // 使用指定的密钥进行签名，在登录方法中我们目前使用的是用户密码作为密钥
    }

    /**
     * 获取当前登录的用户（UserInfo）
     */
    public static UserInfo getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest();
            String token = request.getHeader(Constants.TOKEN);
            String audience = JWT.decode(token).getAudience().get(0);
            String[] parts = audience.split("@@"); // 使用@@分隔符
            Integer userId = Integer.valueOf(parts[0]);
            // parts[1] 是 USERINFO

            // 返回UserInfo用户
            return staticUserInfoService.selectById(userId);
        } catch (Exception e) {
            log.error("获取当前登录用户出错", e);
        }
        return null;
    }

}
