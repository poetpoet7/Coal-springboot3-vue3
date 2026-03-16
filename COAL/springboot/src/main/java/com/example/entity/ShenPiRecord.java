package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投资快报审批流水记录
 * @TableName Tb_CZCPTouZiKuaiBao_ShenPi
 */
@TableName(value = "Tb_CZCPTouZiKuaiBao_ShenPi")
@Data
public class ShenPiRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的快报记录ID（指向 Tb_TongJi_CZCPTouZiKuaiBao.ID）
     */
    private Integer shenqingid;

    /**
     * 类型
     */
    private String leixing;

    /**
     * 审批人ID
     */
    private Integer shenpirenid;

    /**
     * 审批时间
     */
    private Date shenpishijian;

    /**
     * 审批结果（如：上报数据、审批通过、返回修改）
     */
    private String shenpijieguo;

    /**
     * 审批意见
     */
    private String shenpiyijian;

    /**
     * 备注（如：待审批3(兖州煤业)）
     */
    private String beizhu;
}
