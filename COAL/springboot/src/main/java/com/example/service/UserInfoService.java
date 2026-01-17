package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.UserInfo;
import com.example.exception.CustomException;
import com.example.mapper.UserInfoMapper;
import com.example.utils.MD5Utils;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 用户信息服务类（基于C#系统的userinfo表）
 */
@Service
public class UserInfoService {

    @Resource
    private UserInfoMapper userInfoMapper;

    /**
     * 新增用户
     */
    public void add(UserInfo userInfo) {
        // 验证登录名格式：只能是英文字母和数字，必须以字母开头
        String loginname = userInfo.getLoginname();
        if (ObjectUtil.isEmpty(loginname) || !loginname.matches("^[a-zA-Z][a-zA-Z0-9]*$")) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        // 检查登录名是否已存在
        UserInfo dbUser = userInfoMapper.selectByLoginName(userInfo.getLoginname());
        if (ObjectUtil.isNotNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }

        // 设置默认密码（如果没有提供）
        if (ObjectUtil.isEmpty(userInfo.getPassword())) {
            userInfo.setPassword(MD5Utils.md5WithUtf16LE(Constants.USER_DEFAULT_PASSWORD));
        } else {
            // 加密用户提供的密码
            userInfo.setPassword(MD5Utils.md5WithUtf16LE(userInfo.getPassword()));
        }

        // 设置默认用户名（如果没有提供）
        if (ObjectUtil.isEmpty(userInfo.getUsername())) {
            userInfo.setUsername(userInfo.getLoginname());
        }

        // 设置创建时间
        userInfo.setCreatetime(new Date());

        userInfoMapper.insert(userInfo);
    }

    /**
     * 更新用户信息
     */
    public void updateById(UserInfo userInfo) {
        // 如果包含密码更新,需要加密
        if (ObjectUtil.isNotEmpty(userInfo.getPassword())) {
            userInfo.setPassword(MD5Utils.md5WithUtf16LE(userInfo.getPassword()));
        }
        userInfoMapper.updateById(userInfo);
    }

    /**
     * 删除用户
     */
    public void deleteById(Integer userid) {
        userInfoMapper.deleteById(userid);
    }

    /**
     * 批量删除用户
     */
    public void deleteBatch(List<Integer> userids) {
        userInfoMapper.deleteBatchIds(userids);
    }

    /**
     * 根据用户ID查询用户信息（对外接口，不返回密码）
     */
    public UserInfo selectById(Integer userid) {
        UserInfo user = userInfoMapper.selectById(userid);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return user;
    }

    /**
     * 根据用户ID查询用户信息（内部使用，包含密码用于JWT验证）
     */
    public UserInfo selectByIdWithPassword(Integer userid) {
        return userInfoMapper.selectById(userid);
    }

    /**
     * 查询所有用户
     */
    public List<UserInfo> selectAll(UserInfo userInfo) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(userInfo.getUsername())) {
            queryWrapper.like("username", userInfo.getUsername());
        }
        if (ObjectUtil.isNotEmpty(userInfo.getLoginname())) {
            queryWrapper.like("loginname", userInfo.getLoginname());
        }
        if (userInfo.getRoleid() != null) {
            queryWrapper.eq("roleid", userInfo.getRoleid());
        }
        queryWrapper.orderByDesc("userid");

        List<UserInfo> list = userInfoMapper.selectList(queryWrapper);
        // 清除密码
        list.forEach(user -> user.setPassword(null));
        return list;
    }

    /**
     * 分页查询用户
     */
    public Page<UserInfo> selectPage(UserInfo userInfo, Integer pageNum, Integer pageSize) {
        Page<UserInfo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        if (ObjectUtil.isNotEmpty(userInfo.getUsername())) {
            queryWrapper.like("username", userInfo.getUsername());
        }
        if (ObjectUtil.isNotEmpty(userInfo.getLoginname())) {
            queryWrapper.like("loginname", userInfo.getLoginname());
        }
        if (userInfo.getRoleid() != null) {
            queryWrapper.eq("roleid", userInfo.getRoleid());
        }
        queryWrapper.orderByDesc("userid");

        Page<UserInfo> resultPage = userInfoMapper.selectPage(page, queryWrapper);
        // 清除密码
        resultPage.getRecords().forEach(user -> user.setPassword(null));
        return resultPage;
    }

    /**
     * 用户登录 - 兼容C#系统的密码验证
     * 
     * @param account 账户信息（包含用户名和密码）
     * @return 登录成功的用户信息（包含token）
     */
    public UserInfo login(Account account) {
        // 1. 根据登录名查询用户
        UserInfo user = userInfoMapper.selectByLoginName(account.getUsername());
        if (ObjectUtil.isNull(user)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        // 2. 验证密码（使用UTF-16LE的MD5）
        if (!MD5Utils.verify(account.getPassword(), user.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }

        // 3. 更新登录时间
        user.setLogintime(new Date());
        userInfoMapper.updateById(user);

        // 4. 生成JWT token（使用userid@@USERINFO@@roleid作为标识，避免与密码hash中的-冲突）
        String tokenData = user.getUserid() + "@@USERINFO@@" + user.getRoleid();
        String passwordForToken = user.getPassword();
        System.out.println("=== 生成Token ===");
        System.out.println("Token Data: " + tokenData);
        System.out.println("Password for signing: " + passwordForToken);

        String token = TokenUtils.createToken(tokenData, passwordForToken);
        System.out.println("Generated Token: " + token);
        user.setToken(token);

        // 5. 清除密码字段（不返回给前端）
        user.setPassword(null);

        return user;
    }

    /**
     * 修改密码 - 使用UTF-16LE MD5加密
     * 
     * @param account 包含旧密码和新密码的账户信息
     */
    public void updatePassword(Account account) {
        // 1. 根据登录名查询用户
        UserInfo user = userInfoMapper.selectByLoginName(account.getUsername());
        if (ObjectUtil.isNull(user)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        // 2. 验证旧密码
        if (!MD5Utils.verify(account.getPassword(), user.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }

        // 3. 设置新密码（使用UTF-16LE MD5加密）
        String newHashedPassword = MD5Utils.md5WithUtf16LE(account.getNewPassword());
        user.setPassword(newHashedPassword);
        userInfoMapper.updateById(user);
    }
}
