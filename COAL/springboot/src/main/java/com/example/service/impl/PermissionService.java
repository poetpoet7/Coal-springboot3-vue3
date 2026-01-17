package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.Danwei;
import com.example.mapper.DanweiMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限管理服务类
 * 集中管理所有权限相关的业务逻辑，便于扩展
 */
@Service
public class PermissionService {

    @Resource
    private DanweiMapper danweiMapper;

    // 管理员角色ID
    private static final int ADMIN_ROLE_ID = 1;

    /**
     * 判断是否为管理员
     * 
     * @param roleid 角色ID
     * @return true=管理员，拥有所有权限
     */
    public boolean isAdmin(Integer roleid) {
        return roleid != null && roleid == ADMIN_ROLE_ID;
    }

    /**
     * 获取用户所属单位信息
     * 
     * @param danweibianma 单位编码
     * @return 单位实体，不存在则返回null
     */
    public Danwei getUserDanwei(String danweibianma) {
        if (danweibianma == null || danweibianma.trim().isEmpty()) {
            return null;
        }
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("BianMa", danweibianma);
        return danweiMapper.selectOne(wrapper);
    }

    /**
     * 获取用户可访问的单位ID列表
     * 管理员返回所有单位ID，普通用户返回本单位及下属单位ID
     * 
     * @param danweibianma 用户单位编码
     * @param roleid       角色ID
     * @return 可访问的单位ID列表
     */
    public List<Integer> getAccessibleDanweiIds(String danweibianma, Integer roleid) {
        List<Danwei> units = getAccessibleDanweiList(danweibianma, roleid);
        return units.stream()
                .map(d -> d.getId().intValue())
                .collect(Collectors.toList());
    }

    /**
     * 获取用户可访问的单位列表
     * 管理员返回所有单位，普通用户返回本单位及下属单位
     * 
     * @param danweibianma 用户单位编码
     * @param roleid       角色ID
     * @return 可访问的单位列表
     */
    public List<Danwei> getAccessibleDanweiList(String danweibianma, Integer roleid) {
        List<Danwei> allUnits = danweiMapper.selectList(null);

        // 管理员返回所有单位
        if (isAdmin(roleid)) {
            return allUnits;
        }

        // 普通用户：返回本单位及所有下属单位
        Danwei userUnit = getUserDanwei(danweibianma);
        if (userUnit == null) {
            return new ArrayList<>();
        }

        // 使用Set避免重复
        Set<Integer> accessibleIds = new HashSet<>();
        accessibleIds.add(userUnit.getId().intValue());

        // 递归收集所有下属单位ID
        collectSubordinateIds(userUnit.getId().intValue(), allUnits, accessibleIds);

        // 过滤返回可访问的单位
        return allUnits.stream()
                .filter(u -> accessibleIds.contains(u.getId().intValue()))
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否可以访问目标单位的数据
     * 
     * @param danweibianma   用户单位编码
     * @param targetDanweiId 目标单位ID
     * @param roleid         角色ID
     * @return true=可访问
     */
    public boolean canAccessDanwei(String danweibianma, Integer targetDanweiId, Integer roleid) {
        // 管理员可访问所有
        if (isAdmin(roleid)) {
            return true;
        }

        // 获取可访问的单位ID列表
        List<Integer> accessibleIds = getAccessibleDanweiIds(danweibianma, roleid);
        return accessibleIds.contains(targetDanweiId);
    }

    /**
     * 检查操作者是否可以操作目标单位的记录
     * （用于审批等场景：上级可以操作下级的记录）
     * 
     * @param operatorDanweiId 操作者单位ID
     * @param targetDanweiId   目标单位ID
     * @param roleid           角色ID
     * @return true=可操作
     */
    public boolean canOperateRecord(Integer operatorDanweiId, Integer targetDanweiId, Integer roleid) {
        // 管理员可操作所有
        if (isAdmin(roleid)) {
            return true;
        }

        // 获取目标单位信息
        Danwei targetUnit = danweiMapper.selectById(targetDanweiId);
        if (targetUnit == null) {
            return false;
        }

        // 检查操作者是否是目标单位的上级
        return isAncestor(operatorDanweiId, targetDanweiId);
    }

    /**
     * 判断某单位是否是另一单位的上级（祖先）
     * 
     * @param ancestorId   可能的上级单位ID
     * @param descendantId 可能的下级单位ID
     * @return true=是上级
     */
    public boolean isAncestor(Integer ancestorId, Integer descendantId) {
        if (ancestorId == null || descendantId == null) {
            return false;
        }
        if (ancestorId.equals(descendantId)) {
            return false; // 自己不是自己的上级
        }

        // 从下级向上追溯
        Danwei current = danweiMapper.selectById(descendantId);
        while (current != null && current.getShangjidanweiid() != null) {
            if (current.getShangjidanweiid().equals(ancestorId)) {
                return true;
            }
            current = danweiMapper.selectById(current.getShangjidanweiid());
        }
        return false;
    }

    /**
     * 获取单位的层级深度（从根节点到该单位的距离）
     * 
     * @param danweiId 单位ID
     * @return 层级深度，根节点为0
     */
    public int getDanweiLevel(Integer danweiId) {
        int level = 0;
        Danwei current = danweiMapper.selectById(danweiId);
        while (current != null && current.getShangjidanweiid() != null) {
            level++;
            current = danweiMapper.selectById(current.getShangjidanweiid());
        }
        return level;
    }

    /**
     * 递归收集所有下属单位ID
     */
    private void collectSubordinateIds(int parentId, List<Danwei> allUnits, Set<Integer> result) {
        for (Danwei unit : allUnits) {
            if (unit.getShangjidanweiid() != null && unit.getShangjidanweiid().equals(parentId)) {
                result.add(unit.getId().intValue());
                // 递归收集该单位的下属
                collectSubordinateIds(unit.getId().intValue(), allUnits, result);
            }
        }
    }

    /**
     * 获取所有单位列表
     * 
     * @return 所有单位
     */
    public List<Danwei> getAllDanweiList() {
        return danweiMapper.selectList(null);
    }
}
