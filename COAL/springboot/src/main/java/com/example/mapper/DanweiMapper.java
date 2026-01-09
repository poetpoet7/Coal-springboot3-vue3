package com.example.mapper;

import com.example.entity.Danwei;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author DayDream
 * @description 针对表【Tb_DanWei】的数据库操作Mapper
 * @createDate 2026-01-09 19:58:24
 * @Entity com.example.entity.Danwei
 */
@Mapper
public interface DanweiMapper extends BaseMapper<Danwei> {

}
