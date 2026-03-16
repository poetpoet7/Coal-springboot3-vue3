package com.example.mapper;

import com.example.entity.ShenPiRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投资快报审批流水记录 Mapper
 * @TableName Tb_CZCPTouZiKuaiBao_ShenPi
 */
@Mapper
public interface ShenPiRecordMapper extends BaseMapper<ShenPiRecord> {

}
