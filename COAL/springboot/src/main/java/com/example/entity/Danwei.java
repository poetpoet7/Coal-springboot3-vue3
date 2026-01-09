package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName Tb_DanWei
 */
@TableName(value ="Tb_DanWei")
@Data
public class Danwei {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 单位编码
     */
    private String bianma;

    /**
     * 单位名称
     */
    private String mingcheng;

    /**
     * 上级单位ID
     */
    private Integer shangjidanweiid;

    /**
     * 单位负责人
     */
    private String fuzeren;

    /**
     * 部门负责人
     */
    private String bumenfuzeren;

    /**
     * 备注
     */
    private String beizhu;

    /**
     * 
     */
    private String hangye;

    /**
     * 
     */
    private String leibie;

    /**
     * 
     */
    private String shifouzhuanyehuags;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Danwei other = (Danwei) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getBianma() == null ? other.getBianma() == null : this.getBianma().equals(other.getBianma()))
            && (this.getMingcheng() == null ? other.getMingcheng() == null : this.getMingcheng().equals(other.getMingcheng()))
            && (this.getShangjidanweiid() == null ? other.getShangjidanweiid() == null : this.getShangjidanweiid().equals(other.getShangjidanweiid()))
            && (this.getFuzeren() == null ? other.getFuzeren() == null : this.getFuzeren().equals(other.getFuzeren()))
            && (this.getBumenfuzeren() == null ? other.getBumenfuzeren() == null : this.getBumenfuzeren().equals(other.getBumenfuzeren()))
            && (this.getBeizhu() == null ? other.getBeizhu() == null : this.getBeizhu().equals(other.getBeizhu()))
            && (this.getHangye() == null ? other.getHangye() == null : this.getHangye().equals(other.getHangye()))
            && (this.getLeibie() == null ? other.getLeibie() == null : this.getLeibie().equals(other.getLeibie()))
            && (this.getShifouzhuanyehuags() == null ? other.getShifouzhuanyehuags() == null : this.getShifouzhuanyehuags().equals(other.getShifouzhuanyehuags()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getBianma() == null) ? 0 : getBianma().hashCode());
        result = prime * result + ((getMingcheng() == null) ? 0 : getMingcheng().hashCode());
        result = prime * result + ((getShangjidanweiid() == null) ? 0 : getShangjidanweiid().hashCode());
        result = prime * result + ((getFuzeren() == null) ? 0 : getFuzeren().hashCode());
        result = prime * result + ((getBumenfuzeren() == null) ? 0 : getBumenfuzeren().hashCode());
        result = prime * result + ((getBeizhu() == null) ? 0 : getBeizhu().hashCode());
        result = prime * result + ((getHangye() == null) ? 0 : getHangye().hashCode());
        result = prime * result + ((getLeibie() == null) ? 0 : getLeibie().hashCode());
        result = prime * result + ((getShifouzhuanyehuags() == null) ? 0 : getShifouzhuanyehuags().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", bianma=").append(bianma);
        sb.append(", mingcheng=").append(mingcheng);
        sb.append(", shangjidanweiid=").append(shangjidanweiid);
        sb.append(", fuzeren=").append(fuzeren);
        sb.append(", bumenfuzeren=").append(bumenfuzeren);
        sb.append(", beizhu=").append(beizhu);
        sb.append(", hangye=").append(hangye);
        sb.append(", leibie=").append(leibie);
        sb.append(", shifouzhuanyehuags=").append(shifouzhuanyehuags);
        sb.append("]");
        return sb.toString();
    }
}