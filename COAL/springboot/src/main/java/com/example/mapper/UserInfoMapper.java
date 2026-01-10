package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * UserInfo表的Mapper接口
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /**
     * 根据登录名查询用户
     */
    @Select("SELECT u.*, r.rolename " +
            "FROM userinfo u " +
            "LEFT JOIN roles r ON u.roleid = r.roleid " +
            "WHERE u.loginname = #{loginname}")
    UserInfo selectByLoginName(@Param("loginname") String loginname);
}
