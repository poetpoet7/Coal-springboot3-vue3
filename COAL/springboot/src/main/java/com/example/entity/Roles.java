package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色表实体类（对应C#系统的roles表）
 */
@Data
@TableName("roles")
public class Roles {

    @TableId("roleid")
    private Integer roleid; // 角色ID
    private String roleno; // 角色编号
    private String rolename; // 角色名称
    private String beizhu; // 备注
}
