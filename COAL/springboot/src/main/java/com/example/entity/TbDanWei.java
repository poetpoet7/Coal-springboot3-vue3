package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 单位表实体类（对应C#系统的Tb_DanWei表）
 */
@Data
@TableName("Tb_DanWei")
public class TbDanWei {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField("BianMa")
    private String bianMa; // 单位编码

    @TableField("MingCheng")
    private String mingCheng; // 单位名称

    @TableField("ShangJiDanWeiID")
    private Integer shangJiDanWeiID; // 上级单位ID

    @TableField("FuZeRen")
    private String fuZeRen; // 负责人

    @TableField("BuMenFuZeRen")
    private String buMenFuZeRen; // 部门负责人

    @TableField("BeiZhu")
    private String beiZhu; // 备注

    @TableField("HangYe")
    private String hangYe; // 行业

    @TableField("LeiBie")
    private String leiBie; // 类别

    @TableField("ShiFouZhuanYeHuaGS")
    private String shiFouZhuanYeHuaGS; // 是否专业化公司
}
