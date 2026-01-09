package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.Notice;
import com.example.mapper.NoticeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层方法
 */
@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    public void add(Notice notice) {
        notice.setTime(DateUtil.now());
        noticeMapper.insert(notice);
    }

    public void updateById(Notice notice) {
        noticeMapper.updateById(notice);
    }

    public void deleteById(Integer id) {
        noticeMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        noticeMapper.deleteBatchIds(ids);
    }

    public Notice selectById(Integer id) {
        return noticeMapper.selectById(id);
    }

    public List<Notice> selectAll(Notice notice) {
        return noticeMapper.selectAll(notice);
    }

    public Page<Notice> selectPage(Notice notice, Integer pageNum, Integer pageSize) {
        Page<Notice> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Notice> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(notice.getTitle())) {
            queryWrapper.like("title", notice.getTitle());
        }
        queryWrapper.orderByDesc("id");
        return noticeMapper.selectPage(page, queryWrapper);
    }

}
