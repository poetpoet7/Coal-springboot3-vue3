package com.example.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("Tb_Knowledge_Document")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String category;        // 制度文件 / 操作手册 / 培训材料
    private String fileType;
    private Integer chunkCount;
    private Integer uploadedBy;
    private LocalDateTime uploadedAt;
    private Boolean isActive;
}
