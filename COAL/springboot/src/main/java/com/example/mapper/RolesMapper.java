package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Roles;
import org.apache.ibatis.annotations.Mapper;

/**
 * Roles表的Mapper接口
 */
@Mapper
public interface RolesMapper extends BaseMapper<Roles> {
}
