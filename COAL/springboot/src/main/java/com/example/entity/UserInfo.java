package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * 用户信息表实体类（对应C#系统的userinfo表）
 */
@Data
@TableName("userinfo")
public class UserInfo {

    @TableId(value = "userid", type = IdType.AUTO)
    private Integer userid;

    private String loginname; // 登录名
    private String username; // 用户姓名
    private String password; // 密码（MD5加密，UTF-16LE编码）
    private Integer roleid; // 角色ID
    private String usersex; // 性别
    private String danweibianma; // 单位编码
    private String telephone; // 电话
    private String email; // 邮箱
    private Date logintime; // 登录时间
    private Date createtime; // 创建时间

    // 非数据库字段
    @TableField(exist = false)
    private String token; // JWT token

    @TableField(exist = false)
    private String rolename; // 角色名称（关联查询）

    /**
     * 兼容前端 - 返回id（实际是userid）
     */
    @JsonProperty("id")
    public Integer getId() {
        return this.userid;
    }

    /**
     * 兼容前端 - 返回name（实际是username）
     */
    @JsonProperty("name")
    public String getName() {
        return this.username;
    }

    /**
     * 兼容前端 - 返回avatar（默认头像）
     */
    @JsonProperty("avatar")
    public String getAvatar() {
        return ""; // 默认空头像
    }
}
