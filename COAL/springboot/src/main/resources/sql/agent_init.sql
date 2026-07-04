-- Coal-AI 模块数据库初始化脚本
-- 在 SQL Server 中执行此脚本创建 Agent 相关数据表

-- Agent 对话记录表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Tb_Agent_Conversation' AND xtype='U')
CREATE TABLE Tb_Agent_Conversation (
    id BIGINT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,        -- user / assistant / tool
    content NVARCHAR(MAX),
    tool_name VARCHAR(100),
    tool_params NVARCHAR(MAX),
    feedback TINYINT DEFAULT 0,       -- 1=赞, 0=无, -1=踩
    tokens_used INT,
    created_at DATETIME DEFAULT GETDATE()
);

-- Agent 审计日志表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Tb_Agent_Audit_Log' AND xtype='U')
CREATE TABLE Tb_Agent_Audit_Log (
    id BIGINT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_params NVARCHAR(MAX),
    tool_result_summary NVARCHAR(500),
    access_level VARCHAR(10) NOT NULL,   -- READ / WRITE / SUGGEST
    access_decision VARCHAR(10) NOT NULL, -- ALLOWED / BLOCKED
    llm_model VARCHAR(50),
    duration_ms INT,
    created_at DATETIME DEFAULT GETDATE()
);

-- 知识库文档元数据表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Tb_Knowledge_Document' AND xtype='U')
CREATE TABLE Tb_Knowledge_Document (
    id BIGINT IDENTITY PRIMARY KEY,
    title NVARCHAR(200) NOT NULL,
    category VARCHAR(50),              -- 制度文件 / 操作手册 / 培训材料
    file_type VARCHAR(20),
    chunk_count INT,
    uploaded_by INT,
    uploaded_at DATETIME DEFAULT GETDATE(),
    is_active BIT DEFAULT 1
);
