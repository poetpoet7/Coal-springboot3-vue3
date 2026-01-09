package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 
 * @TableName Tb_TongJi_CZCPTouZiKuaiBao
 */
@TableName(value ="Tb_TongJi_CZCPTouZiKuaiBao")
@Data
public class TongjiCzcptouzikuaibao {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Integer danweiid;

    /**
     * 
     */
    private Integer nianfen;

    /**
     * 
     */
    private Integer yuefen;

    /**
     * 
     */
    private String xuhao;

    /**
     * 
     */
    private String bianma;

    /**
     * 
     */
    private BigDecimal jingyingzongzhiheji;

    /**
     * 
     */
    private BigDecimal jingyingzongzhihejileiji;

    /**
     * 
     */
    private BigDecimal gongyechanzhi;

    /**
     * 
     */
    private BigDecimal gongyechanzhileiji;

    /**
     * 
     */
    private BigDecimal qitachanzhi;

    /**
     * 
     */
    private BigDecimal qitachanzhileiji;

    /**
     * 
     */
    private BigDecimal xinchanpinjiazhi;

    /**
     * 
     */
    private BigDecimal xinchanpinjiazhileiji;

    /**
     * 
     */
    private BigDecimal gyxsczheji;

    /**
     * 
     */
    private BigDecimal gyxsczhejileiji;

    /**
     * 
     */
    private BigDecimal chukoujiaohuozhi;

    /**
     * 
     */
    private BigDecimal chukoujiaohuozhileiji;

    /**
     * 
     */
    private Long jiushinianbubianjia;

    /**
     * 
     */
    private Long jiushinianbubianjialeiji;

    /**
     * 
     */
    private Long qiyeyongdianliang;

    /**
     * 
     */
    private Long qiyeyongdianliangleiji;

    /**
     * 
     */
    private BigDecimal gdzctzheji;

    /**
     * 
     */
    private BigDecimal gdzctzhejileiji;

    /**
     * 
     */
    private BigDecimal jishugaizao;

    /**
     * 
     */
    private BigDecimal jishugaizaoleiji;

    /**
     * 
     */
    private Long yuanmei;

    /**
     * 
     */
    private Long yuanmeileiji;

    /**
     * 
     */
    private Long jingmei;

    /**
     * 
     */
    private Long jingmeileiji;

    /**
     * 
     */
    private Long niaosu;

    /**
     * 
     */
    private Long niaosuleiji;

    /**
     * 
     */
    private Long jiachun;

    /**
     * 
     */
    private Long jiachunleiji;

    /**
     * 
     */
    private Long cusuan;

    /**
     * 
     */
    private Long cusuanleiji;

    /**
     * 
     */
    private Long jiaotan;

    /**
     * 
     */
    private Long jiaotanleiji;

    /**
     * 
     */
    private Long cusuanyizhi;

    /**
     * 
     */
    private Long cusuanyizhileiji;

    /**
     * 
     */
    private Long cusuandingzhi;

    /**
     * 
     */
    private Long cusuandingzhileiji;

    /**
     * 
     */
    private Long cugan;

    /**
     * 
     */
    private Long cuganleiji;

    /**
     * 
     */
    private Long tansuanerjiazhi;

    /**
     * 
     */
    private Long tansuanerjiazhileiji;

    /**
     * 
     */
    private Long hechengan;

    /**
     * 
     */
    private Long hechenganleiji;

    /**
     * 
     */
    private Long dingchun;

    /**
     * 
     */
    private Long dingchunleiji;

    /**
     * 
     */
    private Long jujiaquan;

    /**
     * 
     */
    private Long jujiaquanleiji;

    /**
     * 
     */
    private Long gaizhiliqing;

    /**
     * 
     */
    private Long gaizhiliqingleiji;

    /**
     * 
     */
    private Long enyou;

    /**
     * 
     */
    private Long enyouleiji;

    /**
     * 
     */
    private Long fuhefei;

    /**
     * 
     */
    private Long fuhefeileiji;

    /**
     * 
     */
    private Long lvding;

    /**
     * 
     */
    private Long lvdingleiji;

    /**
     * 
     */
    private Long tansuzhipin;

    /**
     * 
     */
    private Long tansuzhipinleiji;

    /**
     * 
     */
    private Long lvzhucai;

    /**
     * 
     */
    private Long lvzhucaileiji;

    /**
     * 
     */
    private Long lvjiyacai;

    /**
     * 
     */
    private Long lvjiyacaileiji;

    /**
     * 
     */
    private Long fadianliang;

    /**
     * 
     */
    private Long fadianliangleiji;

    /**
     * 
     */
    private Long pidaiyunshuji;

    /**
     * 
     */
    private Long pidaiyunshujileiji;

    /**
     * 
     */
    private Long shusongdai;

    /**
     * 
     */
    private Long shusongdaileiji;

    /**
     * 
     */
    private Long dianlan;

    /**
     * 
     */
    private Long dianlanleiji;

    /**
     * 
     */
    private Long yeyazhijia;

    /**
     * 
     */
    private Long yeyazhijialeiji;

    /**
     * 
     */
    private Long guabanyunshuji;

    /**
     * 
     */
    private Long guabanyunshujileiji;

    /**
     * 
     */
    private Long gaolingtu;

    /**
     * 
     */
    private Long gaolingtuleiji;

    /**
     * 
     */
    private Long qunianyuanmei;

    /**
     * 
     */
    private Long qunianyuanmeileiji;

    /**
     * 
     */
    private Long qunianjingmei;

    /**
     * 
     */
    private Long qunianjingmeileiji;

    /**
     * 
     */
    private Long beizhu1;

    /**
     * 
     */
    private Long beizhu2;

    /**
     * 
     */
    private Long beizhu3;

    /**
     * 
     */
    private Long beizhu4;

    /**
     * 
     */
    private Long beizhu5;

    /**
     * 
     */
    private Long beizhu6;

    /**
     * 
     */
    private String beizhu;

    /**
     * 
     */
    private String beizhu0;

    /**
     * 
     */
    private String zhuangtai;

    /**
     * 
     */
    private Long qingchaiyou;

    /**
     * 
     */
    private Long qingchaiiyouleiji;

    /**
     * 
     */
    private Long zhongchaiyou;

    /**
     * 
     */
    private Long zhongchaiyouleiji;

    /**
     * 
     */
    private Long shinaoyou;

    /**
     * 
     */
    private Long shinaoyouleiji;

    /**
     * 
     */
    private Long yehuashiyouqi;

    /**
     * 
     */
    private Long yehuashiyouqileiji;

    /**
     * 
     */
    private Long qitachanpin;

    /**
     * 
     */
    private Long qitachanpinleiji;

    /**
     * 
     */
    private Long liuhuang;

    /**
     * 
     */
    private Long liuhuangleiji;

    /**
     * 
     */
    private Long liuhuangan;

    /**
     * 
     */
    private Long liuhuanganleiji;

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
        TongjiCzcptouzikuaibao other = (TongjiCzcptouzikuaibao) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getDanweiid() == null ? other.getDanweiid() == null : this.getDanweiid().equals(other.getDanweiid()))
            && (this.getNianfen() == null ? other.getNianfen() == null : this.getNianfen().equals(other.getNianfen()))
            && (this.getYuefen() == null ? other.getYuefen() == null : this.getYuefen().equals(other.getYuefen()))
            && (this.getXuhao() == null ? other.getXuhao() == null : this.getXuhao().equals(other.getXuhao()))
            && (this.getBianma() == null ? other.getBianma() == null : this.getBianma().equals(other.getBianma()))
            && (this.getJingyingzongzhiheji() == null ? other.getJingyingzongzhiheji() == null : this.getJingyingzongzhiheji().equals(other.getJingyingzongzhiheji()))
            && (this.getJingyingzongzhihejileiji() == null ? other.getJingyingzongzhihejileiji() == null : this.getJingyingzongzhihejileiji().equals(other.getJingyingzongzhihejileiji()))
            && (this.getGongyechanzhi() == null ? other.getGongyechanzhi() == null : this.getGongyechanzhi().equals(other.getGongyechanzhi()))
            && (this.getGongyechanzhileiji() == null ? other.getGongyechanzhileiji() == null : this.getGongyechanzhileiji().equals(other.getGongyechanzhileiji()))
            && (this.getQitachanzhi() == null ? other.getQitachanzhi() == null : this.getQitachanzhi().equals(other.getQitachanzhi()))
            && (this.getQitachanzhileiji() == null ? other.getQitachanzhileiji() == null : this.getQitachanzhileiji().equals(other.getQitachanzhileiji()))
            && (this.getXinchanpinjiazhi() == null ? other.getXinchanpinjiazhi() == null : this.getXinchanpinjiazhi().equals(other.getXinchanpinjiazhi()))
            && (this.getXinchanpinjiazhileiji() == null ? other.getXinchanpinjiazhileiji() == null : this.getXinchanpinjiazhileiji().equals(other.getXinchanpinjiazhileiji()))
            && (this.getGyxsczheji() == null ? other.getGyxsczheji() == null : this.getGyxsczheji().equals(other.getGyxsczheji()))
            && (this.getGyxsczhejileiji() == null ? other.getGyxsczhejileiji() == null : this.getGyxsczhejileiji().equals(other.getGyxsczhejileiji()))
            && (this.getChukoujiaohuozhi() == null ? other.getChukoujiaohuozhi() == null : this.getChukoujiaohuozhi().equals(other.getChukoujiaohuozhi()))
            && (this.getChukoujiaohuozhileiji() == null ? other.getChukoujiaohuozhileiji() == null : this.getChukoujiaohuozhileiji().equals(other.getChukoujiaohuozhileiji()))
            && (this.getJiushinianbubianjia() == null ? other.getJiushinianbubianjia() == null : this.getJiushinianbubianjia().equals(other.getJiushinianbubianjia()))
            && (this.getJiushinianbubianjialeiji() == null ? other.getJiushinianbubianjialeiji() == null : this.getJiushinianbubianjialeiji().equals(other.getJiushinianbubianjialeiji()))
            && (this.getQiyeyongdianliang() == null ? other.getQiyeyongdianliang() == null : this.getQiyeyongdianliang().equals(other.getQiyeyongdianliang()))
            && (this.getQiyeyongdianliangleiji() == null ? other.getQiyeyongdianliangleiji() == null : this.getQiyeyongdianliangleiji().equals(other.getQiyeyongdianliangleiji()))
            && (this.getGdzctzheji() == null ? other.getGdzctzheji() == null : this.getGdzctzheji().equals(other.getGdzctzheji()))
            && (this.getGdzctzhejileiji() == null ? other.getGdzctzhejileiji() == null : this.getGdzctzhejileiji().equals(other.getGdzctzhejileiji()))
            && (this.getJishugaizao() == null ? other.getJishugaizao() == null : this.getJishugaizao().equals(other.getJishugaizao()))
            && (this.getJishugaizaoleiji() == null ? other.getJishugaizaoleiji() == null : this.getJishugaizaoleiji().equals(other.getJishugaizaoleiji()))
            && (this.getYuanmei() == null ? other.getYuanmei() == null : this.getYuanmei().equals(other.getYuanmei()))
            && (this.getYuanmeileiji() == null ? other.getYuanmeileiji() == null : this.getYuanmeileiji().equals(other.getYuanmeileiji()))
            && (this.getJingmei() == null ? other.getJingmei() == null : this.getJingmei().equals(other.getJingmei()))
            && (this.getJingmeileiji() == null ? other.getJingmeileiji() == null : this.getJingmeileiji().equals(other.getJingmeileiji()))
            && (this.getNiaosu() == null ? other.getNiaosu() == null : this.getNiaosu().equals(other.getNiaosu()))
            && (this.getNiaosuleiji() == null ? other.getNiaosuleiji() == null : this.getNiaosuleiji().equals(other.getNiaosuleiji()))
            && (this.getJiachun() == null ? other.getJiachun() == null : this.getJiachun().equals(other.getJiachun()))
            && (this.getJiachunleiji() == null ? other.getJiachunleiji() == null : this.getJiachunleiji().equals(other.getJiachunleiji()))
            && (this.getCusuan() == null ? other.getCusuan() == null : this.getCusuan().equals(other.getCusuan()))
            && (this.getCusuanleiji() == null ? other.getCusuanleiji() == null : this.getCusuanleiji().equals(other.getCusuanleiji()))
            && (this.getJiaotan() == null ? other.getJiaotan() == null : this.getJiaotan().equals(other.getJiaotan()))
            && (this.getJiaotanleiji() == null ? other.getJiaotanleiji() == null : this.getJiaotanleiji().equals(other.getJiaotanleiji()))
            && (this.getCusuanyizhi() == null ? other.getCusuanyizhi() == null : this.getCusuanyizhi().equals(other.getCusuanyizhi()))
            && (this.getCusuanyizhileiji() == null ? other.getCusuanyizhileiji() == null : this.getCusuanyizhileiji().equals(other.getCusuanyizhileiji()))
            && (this.getCusuandingzhi() == null ? other.getCusuandingzhi() == null : this.getCusuandingzhi().equals(other.getCusuandingzhi()))
            && (this.getCusuandingzhileiji() == null ? other.getCusuandingzhileiji() == null : this.getCusuandingzhileiji().equals(other.getCusuandingzhileiji()))
            && (this.getCugan() == null ? other.getCugan() == null : this.getCugan().equals(other.getCugan()))
            && (this.getCuganleiji() == null ? other.getCuganleiji() == null : this.getCuganleiji().equals(other.getCuganleiji()))
            && (this.getTansuanerjiazhi() == null ? other.getTansuanerjiazhi() == null : this.getTansuanerjiazhi().equals(other.getTansuanerjiazhi()))
            && (this.getTansuanerjiazhileiji() == null ? other.getTansuanerjiazhileiji() == null : this.getTansuanerjiazhileiji().equals(other.getTansuanerjiazhileiji()))
            && (this.getHechengan() == null ? other.getHechengan() == null : this.getHechengan().equals(other.getHechengan()))
            && (this.getHechenganleiji() == null ? other.getHechenganleiji() == null : this.getHechenganleiji().equals(other.getHechenganleiji()))
            && (this.getDingchun() == null ? other.getDingchun() == null : this.getDingchun().equals(other.getDingchun()))
            && (this.getDingchunleiji() == null ? other.getDingchunleiji() == null : this.getDingchunleiji().equals(other.getDingchunleiji()))
            && (this.getJujiaquan() == null ? other.getJujiaquan() == null : this.getJujiaquan().equals(other.getJujiaquan()))
            && (this.getJujiaquanleiji() == null ? other.getJujiaquanleiji() == null : this.getJujiaquanleiji().equals(other.getJujiaquanleiji()))
            && (this.getGaizhiliqing() == null ? other.getGaizhiliqing() == null : this.getGaizhiliqing().equals(other.getGaizhiliqing()))
            && (this.getGaizhiliqingleiji() == null ? other.getGaizhiliqingleiji() == null : this.getGaizhiliqingleiji().equals(other.getGaizhiliqingleiji()))
            && (this.getEnyou() == null ? other.getEnyou() == null : this.getEnyou().equals(other.getEnyou()))
            && (this.getEnyouleiji() == null ? other.getEnyouleiji() == null : this.getEnyouleiji().equals(other.getEnyouleiji()))
            && (this.getFuhefei() == null ? other.getFuhefei() == null : this.getFuhefei().equals(other.getFuhefei()))
            && (this.getFuhefeileiji() == null ? other.getFuhefeileiji() == null : this.getFuhefeileiji().equals(other.getFuhefeileiji()))
            && (this.getLvding() == null ? other.getLvding() == null : this.getLvding().equals(other.getLvding()))
            && (this.getLvdingleiji() == null ? other.getLvdingleiji() == null : this.getLvdingleiji().equals(other.getLvdingleiji()))
            && (this.getTansuzhipin() == null ? other.getTansuzhipin() == null : this.getTansuzhipin().equals(other.getTansuzhipin()))
            && (this.getTansuzhipinleiji() == null ? other.getTansuzhipinleiji() == null : this.getTansuzhipinleiji().equals(other.getTansuzhipinleiji()))
            && (this.getLvzhucai() == null ? other.getLvzhucai() == null : this.getLvzhucai().equals(other.getLvzhucai()))
            && (this.getLvzhucaileiji() == null ? other.getLvzhucaileiji() == null : this.getLvzhucaileiji().equals(other.getLvzhucaileiji()))
            && (this.getLvjiyacai() == null ? other.getLvjiyacai() == null : this.getLvjiyacai().equals(other.getLvjiyacai()))
            && (this.getLvjiyacaileiji() == null ? other.getLvjiyacaileiji() == null : this.getLvjiyacaileiji().equals(other.getLvjiyacaileiji()))
            && (this.getFadianliang() == null ? other.getFadianliang() == null : this.getFadianliang().equals(other.getFadianliang()))
            && (this.getFadianliangleiji() == null ? other.getFadianliangleiji() == null : this.getFadianliangleiji().equals(other.getFadianliangleiji()))
            && (this.getPidaiyunshuji() == null ? other.getPidaiyunshuji() == null : this.getPidaiyunshuji().equals(other.getPidaiyunshuji()))
            && (this.getPidaiyunshujileiji() == null ? other.getPidaiyunshujileiji() == null : this.getPidaiyunshujileiji().equals(other.getPidaiyunshujileiji()))
            && (this.getShusongdai() == null ? other.getShusongdai() == null : this.getShusongdai().equals(other.getShusongdai()))
            && (this.getShusongdaileiji() == null ? other.getShusongdaileiji() == null : this.getShusongdaileiji().equals(other.getShusongdaileiji()))
            && (this.getDianlan() == null ? other.getDianlan() == null : this.getDianlan().equals(other.getDianlan()))
            && (this.getDianlanleiji() == null ? other.getDianlanleiji() == null : this.getDianlanleiji().equals(other.getDianlanleiji()))
            && (this.getYeyazhijia() == null ? other.getYeyazhijia() == null : this.getYeyazhijia().equals(other.getYeyazhijia()))
            && (this.getYeyazhijialeiji() == null ? other.getYeyazhijialeiji() == null : this.getYeyazhijialeiji().equals(other.getYeyazhijialeiji()))
            && (this.getGuabanyunshuji() == null ? other.getGuabanyunshuji() == null : this.getGuabanyunshuji().equals(other.getGuabanyunshuji()))
            && (this.getGuabanyunshujileiji() == null ? other.getGuabanyunshujileiji() == null : this.getGuabanyunshujileiji().equals(other.getGuabanyunshujileiji()))
            && (this.getGaolingtu() == null ? other.getGaolingtu() == null : this.getGaolingtu().equals(other.getGaolingtu()))
            && (this.getGaolingtuleiji() == null ? other.getGaolingtuleiji() == null : this.getGaolingtuleiji().equals(other.getGaolingtuleiji()))
            && (this.getQunianyuanmei() == null ? other.getQunianyuanmei() == null : this.getQunianyuanmei().equals(other.getQunianyuanmei()))
            && (this.getQunianyuanmeileiji() == null ? other.getQunianyuanmeileiji() == null : this.getQunianyuanmeileiji().equals(other.getQunianyuanmeileiji()))
            && (this.getQunianjingmei() == null ? other.getQunianjingmei() == null : this.getQunianjingmei().equals(other.getQunianjingmei()))
            && (this.getQunianjingmeileiji() == null ? other.getQunianjingmeileiji() == null : this.getQunianjingmeileiji().equals(other.getQunianjingmeileiji()))
            && (this.getBeizhu1() == null ? other.getBeizhu1() == null : this.getBeizhu1().equals(other.getBeizhu1()))
            && (this.getBeizhu2() == null ? other.getBeizhu2() == null : this.getBeizhu2().equals(other.getBeizhu2()))
            && (this.getBeizhu3() == null ? other.getBeizhu3() == null : this.getBeizhu3().equals(other.getBeizhu3()))
            && (this.getBeizhu4() == null ? other.getBeizhu4() == null : this.getBeizhu4().equals(other.getBeizhu4()))
            && (this.getBeizhu5() == null ? other.getBeizhu5() == null : this.getBeizhu5().equals(other.getBeizhu5()))
            && (this.getBeizhu6() == null ? other.getBeizhu6() == null : this.getBeizhu6().equals(other.getBeizhu6()))
            && (this.getBeizhu() == null ? other.getBeizhu() == null : this.getBeizhu().equals(other.getBeizhu()))
            && (this.getBeizhu0() == null ? other.getBeizhu0() == null : this.getBeizhu0().equals(other.getBeizhu0()))
            && (this.getZhuangtai() == null ? other.getZhuangtai() == null : this.getZhuangtai().equals(other.getZhuangtai()))
            && (this.getQingchaiyou() == null ? other.getQingchaiyou() == null : this.getQingchaiyou().equals(other.getQingchaiyou()))
            && (this.getQingchaiiyouleiji() == null ? other.getQingchaiiyouleiji() == null : this.getQingchaiiyouleiji().equals(other.getQingchaiiyouleiji()))
            && (this.getZhongchaiyou() == null ? other.getZhongchaiyou() == null : this.getZhongchaiyou().equals(other.getZhongchaiyou()))
            && (this.getZhongchaiyouleiji() == null ? other.getZhongchaiyouleiji() == null : this.getZhongchaiyouleiji().equals(other.getZhongchaiyouleiji()))
            && (this.getShinaoyou() == null ? other.getShinaoyou() == null : this.getShinaoyou().equals(other.getShinaoyou()))
            && (this.getShinaoyouleiji() == null ? other.getShinaoyouleiji() == null : this.getShinaoyouleiji().equals(other.getShinaoyouleiji()))
            && (this.getYehuashiyouqi() == null ? other.getYehuashiyouqi() == null : this.getYehuashiyouqi().equals(other.getYehuashiyouqi()))
            && (this.getYehuashiyouqileiji() == null ? other.getYehuashiyouqileiji() == null : this.getYehuashiyouqileiji().equals(other.getYehuashiyouqileiji()))
            && (this.getQitachanpin() == null ? other.getQitachanpin() == null : this.getQitachanpin().equals(other.getQitachanpin()))
            && (this.getQitachanpinleiji() == null ? other.getQitachanpinleiji() == null : this.getQitachanpinleiji().equals(other.getQitachanpinleiji()))
            && (this.getLiuhuang() == null ? other.getLiuhuang() == null : this.getLiuhuang().equals(other.getLiuhuang()))
            && (this.getLiuhuangleiji() == null ? other.getLiuhuangleiji() == null : this.getLiuhuangleiji().equals(other.getLiuhuangleiji()))
            && (this.getLiuhuangan() == null ? other.getLiuhuangan() == null : this.getLiuhuangan().equals(other.getLiuhuangan()))
            && (this.getLiuhuanganleiji() == null ? other.getLiuhuanganleiji() == null : this.getLiuhuanganleiji().equals(other.getLiuhuanganleiji()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getDanweiid() == null) ? 0 : getDanweiid().hashCode());
        result = prime * result + ((getNianfen() == null) ? 0 : getNianfen().hashCode());
        result = prime * result + ((getYuefen() == null) ? 0 : getYuefen().hashCode());
        result = prime * result + ((getXuhao() == null) ? 0 : getXuhao().hashCode());
        result = prime * result + ((getBianma() == null) ? 0 : getBianma().hashCode());
        result = prime * result + ((getJingyingzongzhiheji() == null) ? 0 : getJingyingzongzhiheji().hashCode());
        result = prime * result + ((getJingyingzongzhihejileiji() == null) ? 0 : getJingyingzongzhihejileiji().hashCode());
        result = prime * result + ((getGongyechanzhi() == null) ? 0 : getGongyechanzhi().hashCode());
        result = prime * result + ((getGongyechanzhileiji() == null) ? 0 : getGongyechanzhileiji().hashCode());
        result = prime * result + ((getQitachanzhi() == null) ? 0 : getQitachanzhi().hashCode());
        result = prime * result + ((getQitachanzhileiji() == null) ? 0 : getQitachanzhileiji().hashCode());
        result = prime * result + ((getXinchanpinjiazhi() == null) ? 0 : getXinchanpinjiazhi().hashCode());
        result = prime * result + ((getXinchanpinjiazhileiji() == null) ? 0 : getXinchanpinjiazhileiji().hashCode());
        result = prime * result + ((getGyxsczheji() == null) ? 0 : getGyxsczheji().hashCode());
        result = prime * result + ((getGyxsczhejileiji() == null) ? 0 : getGyxsczhejileiji().hashCode());
        result = prime * result + ((getChukoujiaohuozhi() == null) ? 0 : getChukoujiaohuozhi().hashCode());
        result = prime * result + ((getChukoujiaohuozhileiji() == null) ? 0 : getChukoujiaohuozhileiji().hashCode());
        result = prime * result + ((getJiushinianbubianjia() == null) ? 0 : getJiushinianbubianjia().hashCode());
        result = prime * result + ((getJiushinianbubianjialeiji() == null) ? 0 : getJiushinianbubianjialeiji().hashCode());
        result = prime * result + ((getQiyeyongdianliang() == null) ? 0 : getQiyeyongdianliang().hashCode());
        result = prime * result + ((getQiyeyongdianliangleiji() == null) ? 0 : getQiyeyongdianliangleiji().hashCode());
        result = prime * result + ((getGdzctzheji() == null) ? 0 : getGdzctzheji().hashCode());
        result = prime * result + ((getGdzctzhejileiji() == null) ? 0 : getGdzctzhejileiji().hashCode());
        result = prime * result + ((getJishugaizao() == null) ? 0 : getJishugaizao().hashCode());
        result = prime * result + ((getJishugaizaoleiji() == null) ? 0 : getJishugaizaoleiji().hashCode());
        result = prime * result + ((getYuanmei() == null) ? 0 : getYuanmei().hashCode());
        result = prime * result + ((getYuanmeileiji() == null) ? 0 : getYuanmeileiji().hashCode());
        result = prime * result + ((getJingmei() == null) ? 0 : getJingmei().hashCode());
        result = prime * result + ((getJingmeileiji() == null) ? 0 : getJingmeileiji().hashCode());
        result = prime * result + ((getNiaosu() == null) ? 0 : getNiaosu().hashCode());
        result = prime * result + ((getNiaosuleiji() == null) ? 0 : getNiaosuleiji().hashCode());
        result = prime * result + ((getJiachun() == null) ? 0 : getJiachun().hashCode());
        result = prime * result + ((getJiachunleiji() == null) ? 0 : getJiachunleiji().hashCode());
        result = prime * result + ((getCusuan() == null) ? 0 : getCusuan().hashCode());
        result = prime * result + ((getCusuanleiji() == null) ? 0 : getCusuanleiji().hashCode());
        result = prime * result + ((getJiaotan() == null) ? 0 : getJiaotan().hashCode());
        result = prime * result + ((getJiaotanleiji() == null) ? 0 : getJiaotanleiji().hashCode());
        result = prime * result + ((getCusuanyizhi() == null) ? 0 : getCusuanyizhi().hashCode());
        result = prime * result + ((getCusuanyizhileiji() == null) ? 0 : getCusuanyizhileiji().hashCode());
        result = prime * result + ((getCusuandingzhi() == null) ? 0 : getCusuandingzhi().hashCode());
        result = prime * result + ((getCusuandingzhileiji() == null) ? 0 : getCusuandingzhileiji().hashCode());
        result = prime * result + ((getCugan() == null) ? 0 : getCugan().hashCode());
        result = prime * result + ((getCuganleiji() == null) ? 0 : getCuganleiji().hashCode());
        result = prime * result + ((getTansuanerjiazhi() == null) ? 0 : getTansuanerjiazhi().hashCode());
        result = prime * result + ((getTansuanerjiazhileiji() == null) ? 0 : getTansuanerjiazhileiji().hashCode());
        result = prime * result + ((getHechengan() == null) ? 0 : getHechengan().hashCode());
        result = prime * result + ((getHechenganleiji() == null) ? 0 : getHechenganleiji().hashCode());
        result = prime * result + ((getDingchun() == null) ? 0 : getDingchun().hashCode());
        result = prime * result + ((getDingchunleiji() == null) ? 0 : getDingchunleiji().hashCode());
        result = prime * result + ((getJujiaquan() == null) ? 0 : getJujiaquan().hashCode());
        result = prime * result + ((getJujiaquanleiji() == null) ? 0 : getJujiaquanleiji().hashCode());
        result = prime * result + ((getGaizhiliqing() == null) ? 0 : getGaizhiliqing().hashCode());
        result = prime * result + ((getGaizhiliqingleiji() == null) ? 0 : getGaizhiliqingleiji().hashCode());
        result = prime * result + ((getEnyou() == null) ? 0 : getEnyou().hashCode());
        result = prime * result + ((getEnyouleiji() == null) ? 0 : getEnyouleiji().hashCode());
        result = prime * result + ((getFuhefei() == null) ? 0 : getFuhefei().hashCode());
        result = prime * result + ((getFuhefeileiji() == null) ? 0 : getFuhefeileiji().hashCode());
        result = prime * result + ((getLvding() == null) ? 0 : getLvding().hashCode());
        result = prime * result + ((getLvdingleiji() == null) ? 0 : getLvdingleiji().hashCode());
        result = prime * result + ((getTansuzhipin() == null) ? 0 : getTansuzhipin().hashCode());
        result = prime * result + ((getTansuzhipinleiji() == null) ? 0 : getTansuzhipinleiji().hashCode());
        result = prime * result + ((getLvzhucai() == null) ? 0 : getLvzhucai().hashCode());
        result = prime * result + ((getLvzhucaileiji() == null) ? 0 : getLvzhucaileiji().hashCode());
        result = prime * result + ((getLvjiyacai() == null) ? 0 : getLvjiyacai().hashCode());
        result = prime * result + ((getLvjiyacaileiji() == null) ? 0 : getLvjiyacaileiji().hashCode());
        result = prime * result + ((getFadianliang() == null) ? 0 : getFadianliang().hashCode());
        result = prime * result + ((getFadianliangleiji() == null) ? 0 : getFadianliangleiji().hashCode());
        result = prime * result + ((getPidaiyunshuji() == null) ? 0 : getPidaiyunshuji().hashCode());
        result = prime * result + ((getPidaiyunshujileiji() == null) ? 0 : getPidaiyunshujileiji().hashCode());
        result = prime * result + ((getShusongdai() == null) ? 0 : getShusongdai().hashCode());
        result = prime * result + ((getShusongdaileiji() == null) ? 0 : getShusongdaileiji().hashCode());
        result = prime * result + ((getDianlan() == null) ? 0 : getDianlan().hashCode());
        result = prime * result + ((getDianlanleiji() == null) ? 0 : getDianlanleiji().hashCode());
        result = prime * result + ((getYeyazhijia() == null) ? 0 : getYeyazhijia().hashCode());
        result = prime * result + ((getYeyazhijialeiji() == null) ? 0 : getYeyazhijialeiji().hashCode());
        result = prime * result + ((getGuabanyunshuji() == null) ? 0 : getGuabanyunshuji().hashCode());
        result = prime * result + ((getGuabanyunshujileiji() == null) ? 0 : getGuabanyunshujileiji().hashCode());
        result = prime * result + ((getGaolingtu() == null) ? 0 : getGaolingtu().hashCode());
        result = prime * result + ((getGaolingtuleiji() == null) ? 0 : getGaolingtuleiji().hashCode());
        result = prime * result + ((getQunianyuanmei() == null) ? 0 : getQunianyuanmei().hashCode());
        result = prime * result + ((getQunianyuanmeileiji() == null) ? 0 : getQunianyuanmeileiji().hashCode());
        result = prime * result + ((getQunianjingmei() == null) ? 0 : getQunianjingmei().hashCode());
        result = prime * result + ((getQunianjingmeileiji() == null) ? 0 : getQunianjingmeileiji().hashCode());
        result = prime * result + ((getBeizhu1() == null) ? 0 : getBeizhu1().hashCode());
        result = prime * result + ((getBeizhu2() == null) ? 0 : getBeizhu2().hashCode());
        result = prime * result + ((getBeizhu3() == null) ? 0 : getBeizhu3().hashCode());
        result = prime * result + ((getBeizhu4() == null) ? 0 : getBeizhu4().hashCode());
        result = prime * result + ((getBeizhu5() == null) ? 0 : getBeizhu5().hashCode());
        result = prime * result + ((getBeizhu6() == null) ? 0 : getBeizhu6().hashCode());
        result = prime * result + ((getBeizhu() == null) ? 0 : getBeizhu().hashCode());
        result = prime * result + ((getBeizhu0() == null) ? 0 : getBeizhu0().hashCode());
        result = prime * result + ((getZhuangtai() == null) ? 0 : getZhuangtai().hashCode());
        result = prime * result + ((getQingchaiyou() == null) ? 0 : getQingchaiyou().hashCode());
        result = prime * result + ((getQingchaiiyouleiji() == null) ? 0 : getQingchaiiyouleiji().hashCode());
        result = prime * result + ((getZhongchaiyou() == null) ? 0 : getZhongchaiyou().hashCode());
        result = prime * result + ((getZhongchaiyouleiji() == null) ? 0 : getZhongchaiyouleiji().hashCode());
        result = prime * result + ((getShinaoyou() == null) ? 0 : getShinaoyou().hashCode());
        result = prime * result + ((getShinaoyouleiji() == null) ? 0 : getShinaoyouleiji().hashCode());
        result = prime * result + ((getYehuashiyouqi() == null) ? 0 : getYehuashiyouqi().hashCode());
        result = prime * result + ((getYehuashiyouqileiji() == null) ? 0 : getYehuashiyouqileiji().hashCode());
        result = prime * result + ((getQitachanpin() == null) ? 0 : getQitachanpin().hashCode());
        result = prime * result + ((getQitachanpinleiji() == null) ? 0 : getQitachanpinleiji().hashCode());
        result = prime * result + ((getLiuhuang() == null) ? 0 : getLiuhuang().hashCode());
        result = prime * result + ((getLiuhuangleiji() == null) ? 0 : getLiuhuangleiji().hashCode());
        result = prime * result + ((getLiuhuangan() == null) ? 0 : getLiuhuangan().hashCode());
        result = prime * result + ((getLiuhuanganleiji() == null) ? 0 : getLiuhuanganleiji().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", danweiid=").append(danweiid);
        sb.append(", nianfen=").append(nianfen);
        sb.append(", yuefen=").append(yuefen);
        sb.append(", xuhao=").append(xuhao);
        sb.append(", bianma=").append(bianma);
        sb.append(", jingyingzongzhiheji=").append(jingyingzongzhiheji);
        sb.append(", jingyingzongzhihejileiji=").append(jingyingzongzhihejileiji);
        sb.append(", gongyechanzhi=").append(gongyechanzhi);
        sb.append(", gongyechanzhileiji=").append(gongyechanzhileiji);
        sb.append(", qitachanzhi=").append(qitachanzhi);
        sb.append(", qitachanzhileiji=").append(qitachanzhileiji);
        sb.append(", xinchanpinjiazhi=").append(xinchanpinjiazhi);
        sb.append(", xinchanpinjiazhileiji=").append(xinchanpinjiazhileiji);
        sb.append(", gyxsczheji=").append(gyxsczheji);
        sb.append(", gyxsczhejileiji=").append(gyxsczhejileiji);
        sb.append(", chukoujiaohuozhi=").append(chukoujiaohuozhi);
        sb.append(", chukoujiaohuozhileiji=").append(chukoujiaohuozhileiji);
        sb.append(", jiushinianbubianjia=").append(jiushinianbubianjia);
        sb.append(", jiushinianbubianjialeiji=").append(jiushinianbubianjialeiji);
        sb.append(", qiyeyongdianliang=").append(qiyeyongdianliang);
        sb.append(", qiyeyongdianliangleiji=").append(qiyeyongdianliangleiji);
        sb.append(", gdzctzheji=").append(gdzctzheji);
        sb.append(", gdzctzhejileiji=").append(gdzctzhejileiji);
        sb.append(", jishugaizao=").append(jishugaizao);
        sb.append(", jishugaizaoleiji=").append(jishugaizaoleiji);
        sb.append(", yuanmei=").append(yuanmei);
        sb.append(", yuanmeileiji=").append(yuanmeileiji);
        sb.append(", jingmei=").append(jingmei);
        sb.append(", jingmeileiji=").append(jingmeileiji);
        sb.append(", niaosu=").append(niaosu);
        sb.append(", niaosuleiji=").append(niaosuleiji);
        sb.append(", jiachun=").append(jiachun);
        sb.append(", jiachunleiji=").append(jiachunleiji);
        sb.append(", cusuan=").append(cusuan);
        sb.append(", cusuanleiji=").append(cusuanleiji);
        sb.append(", jiaotan=").append(jiaotan);
        sb.append(", jiaotanleiji=").append(jiaotanleiji);
        sb.append(", cusuanyizhi=").append(cusuanyizhi);
        sb.append(", cusuanyizhileiji=").append(cusuanyizhileiji);
        sb.append(", cusuandingzhi=").append(cusuandingzhi);
        sb.append(", cusuandingzhileiji=").append(cusuandingzhileiji);
        sb.append(", cugan=").append(cugan);
        sb.append(", cuganleiji=").append(cuganleiji);
        sb.append(", tansuanerjiazhi=").append(tansuanerjiazhi);
        sb.append(", tansuanerjiazhileiji=").append(tansuanerjiazhileiji);
        sb.append(", hechengan=").append(hechengan);
        sb.append(", hechenganleiji=").append(hechenganleiji);
        sb.append(", dingchun=").append(dingchun);
        sb.append(", dingchunleiji=").append(dingchunleiji);
        sb.append(", jujiaquan=").append(jujiaquan);
        sb.append(", jujiaquanleiji=").append(jujiaquanleiji);
        sb.append(", gaizhiliqing=").append(gaizhiliqing);
        sb.append(", gaizhiliqingleiji=").append(gaizhiliqingleiji);
        sb.append(", enyou=").append(enyou);
        sb.append(", enyouleiji=").append(enyouleiji);
        sb.append(", fuhefei=").append(fuhefei);
        sb.append(", fuhefeileiji=").append(fuhefeileiji);
        sb.append(", lvding=").append(lvding);
        sb.append(", lvdingleiji=").append(lvdingleiji);
        sb.append(", tansuzhipin=").append(tansuzhipin);
        sb.append(", tansuzhipinleiji=").append(tansuzhipinleiji);
        sb.append(", lvzhucai=").append(lvzhucai);
        sb.append(", lvzhucaileiji=").append(lvzhucaileiji);
        sb.append(", lvjiyacai=").append(lvjiyacai);
        sb.append(", lvjiyacaileiji=").append(lvjiyacaileiji);
        sb.append(", fadianliang=").append(fadianliang);
        sb.append(", fadianliangleiji=").append(fadianliangleiji);
        sb.append(", pidaiyunshuji=").append(pidaiyunshuji);
        sb.append(", pidaiyunshujileiji=").append(pidaiyunshujileiji);
        sb.append(", shusongdai=").append(shusongdai);
        sb.append(", shusongdaileiji=").append(shusongdaileiji);
        sb.append(", dianlan=").append(dianlan);
        sb.append(", dianlanleiji=").append(dianlanleiji);
        sb.append(", yeyazhijia=").append(yeyazhijia);
        sb.append(", yeyazhijialeiji=").append(yeyazhijialeiji);
        sb.append(", guabanyunshuji=").append(guabanyunshuji);
        sb.append(", guabanyunshujileiji=").append(guabanyunshujileiji);
        sb.append(", gaolingtu=").append(gaolingtu);
        sb.append(", gaolingtuleiji=").append(gaolingtuleiji);
        sb.append(", qunianyuanmei=").append(qunianyuanmei);
        sb.append(", qunianyuanmeileiji=").append(qunianyuanmeileiji);
        sb.append(", qunianjingmei=").append(qunianjingmei);
        sb.append(", qunianjingmeileiji=").append(qunianjingmeileiji);
        sb.append(", beizhu1=").append(beizhu1);
        sb.append(", beizhu2=").append(beizhu2);
        sb.append(", beizhu3=").append(beizhu3);
        sb.append(", beizhu4=").append(beizhu4);
        sb.append(", beizhu5=").append(beizhu5);
        sb.append(", beizhu6=").append(beizhu6);
        sb.append(", beizhu=").append(beizhu);
        sb.append(", beizhu0=").append(beizhu0);
        sb.append(", zhuangtai=").append(zhuangtai);
        sb.append(", qingchaiyou=").append(qingchaiyou);
        sb.append(", qingchaiiyouleiji=").append(qingchaiiyouleiji);
        sb.append(", zhongchaiyou=").append(zhongchaiyou);
        sb.append(", zhongchaiyouleiji=").append(zhongchaiyouleiji);
        sb.append(", shinaoyou=").append(shinaoyou);
        sb.append(", shinaoyouleiji=").append(shinaoyouleiji);
        sb.append(", yehuashiyouqi=").append(yehuashiyouqi);
        sb.append(", yehuashiyouqileiji=").append(yehuashiyouqileiji);
        sb.append(", qitachanpin=").append(qitachanpin);
        sb.append(", qitachanpinleiji=").append(qitachanpinleiji);
        sb.append(", liuhuang=").append(liuhuang);
        sb.append(", liuhuangleiji=").append(liuhuangleiji);
        sb.append(", liuhuangan=").append(liuhuangan);
        sb.append(", liuhuanganleiji=").append(liuhuanganleiji);
        sb.append("]");
        return sb.toString();
    }
}