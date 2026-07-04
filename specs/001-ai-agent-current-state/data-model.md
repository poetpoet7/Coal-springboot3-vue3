# Data Model: AI Agent 最小改动增强

## Existing Persistent Entities

### AgentConversation

**Table**: `Tb_Agent_Conversation`

**Current role**: 保存用户消息、工具结果、助手回答和反馈。

**Fields used by enhancement**:

| Field | Use |
| --- | --- |
| `user_id` | 会话所属用户 |
| `session_id` | 多轮会话关联键 |
| `role` | `user` / `assistant` / `tool` |
| `content` | 消息正文或工具结果 |
| `tool_name` | 工具消息对应 Tool |
| `tool_params` | 工具参数文本 |
| `feedback` | 反馈记忆，1/0/-1 |
| `tokens_used` | 当前估算 token |
| `created_at` | 排查顺序 |

**Planned change**: 不要求 schema 变更。trace 可优先通过 `session_id`、日志和 SSE 串联。

### AgentAuditLog

**Table**: `Tb_Agent_Audit_Log`

**Current role**: 记录 Tool 调用权限判断。

**Fields used by enhancement**:

| Field | Use |
| --- | --- |
| `user_id` | 调用用户 |
| `session_id` | 会话关联 |
| `tool_name` | selectedTool |
| `tool_params` | toolArgs |
| `tool_result_summary` | toolResultSummary，可在代码中开始填充 |
| `access_level` | READ / SUGGEST / WRITE |
| `access_decision` | ALLOWED / SUGGESTED / BLOCKED |
| `llm_model` | 可选模型名 |
| `duration_ms` | 可选耗时 |

**Planned change**: 优先填充已有 `tool_result_summary`，避免新增字段。

### KnowledgeDocument

**Table**: `Tb_Knowledge_Document`

**Current role**: 保存知识库文档元数据。

**Fields used by enhancement**:

| Field | Use |
| --- | --- |
| `id` | 文档标识 |
| `title` | source |
| `category` | 知识分类 |
| `file_type` | 文档类型 |
| `chunk_count` | 切片数量 |
| `uploaded_by` | 上传来源 |
| `uploaded_at` | 排查时间 |
| `is_active` | 是否可见 |

**Planned change**: 不要求 schema 变更。chunkId 可由 `docId + section` 或 `title + section` 生成。

## Runtime / Contract Models

### AgentSseEvent

| Field | Type | Notes |
| --- | --- | --- |
| `event` | string | `thought`, `tool_trace`, `tool_call`, `message`, `session`, `done`, `error` |
| `data` | string/object | 事件数据 |

### ToolTrace

| Field | Type | Notes |
| --- | --- | --- |
| `selectedTool` | string | Tool name |
| `toolArgs` | object | LLM 选择的参数 |
| `accessLevel` | string | READ/SUGGEST/WRITE |
| `accessDecision` | string | ALLOWED/SUGGESTED/BLOCKED |
| `toolResultSummary` | string | 截断摘要，不输出敏感大文本 |
| `fallbackReason` | string | 失败或降级原因 |

### RetrievalCitation

| Field | Type | Notes |
| --- | --- | --- |
| `source` | string | 文档标题 |
| `chunkId` | string | 文档片段标识 |
| `score` | number | 相似度 |
| `contentPreview` | string | 片段摘要 |

### AgentEvalCase

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | 用例编号 |
| `category` | string | yes | `tool_calling`, `rag`, `memory`, `security`, `troubleshooting` |
| `question` | string | yes | 用户问题 |
| `expectedTool` | string/null | yes | 期望 Tool |
| `expectedBehavior` | string | yes | 期望行为 |
| `passCriteria` | array[string] | yes | 通过标准 |
| `notes` | string | no | 演示提示 |

## State Transitions

### Conversation Session

```text
new request without sessionId
  -> backend creates session
  -> SSE session event
  -> frontend stores sessionId
  -> next request reuses sessionId
  -> ConversationSession appends history
```

### Tool Call

```text
LLM selects tool
  -> ToolRegistry lookup
  -> AccessGuard decision
  -> if ALLOWED/SUGGESTED execute or suggest
  -> if BLOCKED return fallbackReason
  -> save audit log
  -> emit tool trace
```

### RAG Retrieval

```text
query
  -> embed query
  -> cosine similarity
  -> threshold filter
  -> topK
  -> format citations with source/chunkId/score
```

