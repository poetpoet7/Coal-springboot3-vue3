package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品货流去向统计实体类
 * 对应表 Tb_TongJi_ChanPinHuoLiuQuXiang
 */
@TableName("Tb_TongJi_ChanPinHuoLiuQuXiang")
@Data
public class ChanPinHuoLiuQuXiang {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer danweiid;

    private Integer nianfen;

    private Integer yuefen;

    private String xuhao;

    private String bianma;

    /**
     * 产品名称（如：原煤，精煤等）
     */
    private String chanpinmingcheng;

    private BigDecimal shandongshengnei;
    private BigDecimal shandongshengneileiji;

    private BigDecimal shandongshengwai;
    private BigDecimal shandongshengwaileiji;

    private BigDecimal guowai;
    private BigDecimal guowaileiji;

    private BigDecimal gongsiziyong;
    private BigDecimal gongsiziyongleiji;

    private BigDecimal tieluyunshu;
    private BigDecimal tieluyunshuleiji;

    private BigDecimal neiheyunshu;
    private BigDecimal neiheyunshuleiji;

    private BigDecimal dixiao;
    private BigDecimal dixiaoleiji;

    private BigDecimal ziyingtielu;
    private BigDecimal ziyingtieluleiji;

    private BigDecimal kuangziyong;
    private BigDecimal kuangziyongleiji;

    @TableField("BeiZhu1")
    private BigDecimal qicheyunshu;
    
    @TableField("BeiZhu2")
    private BigDecimal qicheyunshuleiji;

    /**
     * 系统复用 BeiZhu3 作为审批状态
     */
    @TableField("BeiZhu3")
    private String zhuangtai;

    // 非数据库字段
    @TableField(exist = false)
    private String danweiMingcheng;
    
    @TableField(exist = false)
    private Boolean isTotal = false;
}
