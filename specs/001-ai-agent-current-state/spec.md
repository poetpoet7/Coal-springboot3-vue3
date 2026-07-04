# Feature Specification: AI Agent Module Current State

**Feature Branch**: `001-ai-agent-current-state`

**Created**: 2026-07-04

**Status**: Current-State Documentation

**Input**: User description: "为当前旧项目中的 ai agent模块 创建一份现状规格说明；这不是新功能开发，而是把现有行为文档化。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ask Coal Business Questions (Priority: P1)

已登录用户可以从系统任意页面打开浮动的煤炭智能助手，输入与煤炭统计、填报、审批、数据查询或业务规则相关的问题，并获得助手回答。

**Why this priority**: 这是 AI Agent 模块的主入口，承载用户对统计数据和业务规则的自然语言查询。

**Independent Test**: 使用已有登录态打开浮动按钮，输入一个煤炭统计相关问题，确认聊天面板出现用户消息、助手占位消息、思考提示，最终显示助手回答或错误提示。

**Acceptance Scenarios**:

1. **Given** 浏览器本地存在 `xm-user.token`，**When** 用户在 Agent 面板输入问题并点击发送，**Then** 前端向 `/api/agent/chat` 发送包含 `message`、`sessionId`、`token` 的 POST 请求，并以流式方式展示返回内容。
2. **Given** 后端可以根据请求体 token 构建安全上下文，**When** Agent 完成处理，**Then** 后端发送 thought、tool_call、message、session、done 等 SSE 事件，并保存用户消息、工具消息和助手消息。
3. **Given** 用户询问非煤炭业务话题，**When** LLM 遵循系统提示，**Then** 助手应按现有提示边界回复自己专注于煤炭业务数据查询和咨询。

---

### User Story 2 - Query Business Data Through Tools (Priority: P1)

用户提出涉及单位、年份、月份和统计模块的问题时，Agent 可以让 LLM 选择只读工具查询单位树、统计数据、审批记录、报表摘要或知识库内容。

**Why this priority**: 工具调用是 Agent 从“聊天”转为“业务助手”的核心现有能力。

**Independent Test**: 输入包含单位和统计模块的问题，确认后端调用工具注册表中的 READ 工具，记录工具审计，并在最终回答中基于工具结果汇报。

**Acceptance Scenarios**:

1. **Given** LLM 返回工具调用 JSON，**When** 工具名存在且访问级别为 READ，**Then** AccessGuard 记录 ALLOWED 审计日志并允许工具执行。
2. **Given** LLM 调用 `query_stat_data` 且参数包含 `unitId`，**When** 当前用户无权访问目标单位，**Then** Agent 不执行该查询并把权限拒绝说明加入工具结果。
3. **Given** 工具返回空数据或错误文本，**When** Agent 生成最终回答，**Then** 当前行为是把该工具返回内容作为最终回答依据，而不是修改业务数据。

---

### User Story 3 - Manage Knowledge Base Content (Priority: P2)

用户可以进入知识库管理页面，查看知识库统计和文档列表，检索知识片段，上传文本内容入库，并删除文档的可见状态。

**Why this priority**: 知识库为业务规则问答提供来源，也提供独立管理入口。

**Independent Test**: 打开知识库管理页面，执行统计加载、搜索、上传、删除操作，确认接口返回 `Result` envelope 且页面根据 `code === '200'` 更新显示。

**Acceptance Scenarios**:

1. **Given** 知识库已有 active 文档，**When** 页面调用 `/api/knowledge/list` 和 `/api/knowledge/stats`，**Then** 返回 active 文档列表、文档数量和内存索引切片数。
2. **Given** 用户输入标题、分类和正文，**When** 页面调用 `/api/knowledge/upload`，**Then** 后端切片、索引内容、写入 `Tb_Knowledge_Document`，并返回文档元数据。
3. **Given** 用户删除某文档，**When** 页面调用 `DELETE /api/knowledge/{id}`，**Then** 后端把该文档标记为 inactive，列表不再返回该文档。

---

### User Story 4 - Clear Conversation and Submit Feedback (Priority: P3)

用户可以清空当前前端聊天窗口；系统提供反馈接口用于给会话中的最新助手消息记录赞/踩反馈。

**Why this priority**: 这是当前 Agent 体验的辅助功能，影响会话状态和回答质量记录。

**Independent Test**: 调用清空会话和反馈接口，确认内存会话移除或数据库最新助手消息 feedback 字段被更新。

**Acceptance Scenarios**:

1. **Given** 前端有本地聊天记录，**When** 用户点击清空对话，**Then** 前端清空 `messages`，重置本地 `sessionId`，并在已有 `sessionId` 时调用 `/api/agent/clear-session`。
2. **Given** 某 `sessionId` 已保存 assistant 消息，**When** 调用 `/api/agent/feedback` 提交 `feedback`，**Then** 后端更新该会话最新 assistant 消息的 feedback 值。
3. **Given** 当前前端反馈按钮组件，**When** 用户点击赞或踩，**Then** 当前组件只切换按钮状态；代码中未实际调用提交反馈函数。

### Edge Cases

- 请求 `/api/agent/chat` 时请求体 token 为空或解析失败，当前后端通过 SSE 发送 error 事件并完成连接。
- Agent 控制器路径被 Spring JWT 拦截器放行；聊天接口自身从请求体 token 构建安全上下文。
- Knowledge 接口路径也被 Spring JWT 拦截器放行，控制器代码本身未看到额外 token 校验。
- 如果 `TokenUtils.getCurrentUser()` 在 Agent 自建安全上下文时返回 null，当前代码 fallback 为 admin 权限上下文。
- LLM Provider 调用失败时，Provider 返回包含错误说明的字符串，而不是抛出到控制器统一异常。
- LLM 未返回工具调用时，Agent 直接把 LLM 文本作为最终回答。
- LLM 返回未知工具名时，Agent 将未知工具错误加入工具结果，并继续流程。
- AccessLevel 为 WRITE 的工具会被 AccessGuard 阻断；当前注册的 `record_feedback` 工具为 WRITE，因此 Agent 工具调用路径不执行它。
- Agent 每次最多进行 2 轮 ReAct 工具循环；最后一轮会要求 LLM 基于工具结果生成最终回答。
- 前端 `agent.js` 忽略 SSE `event:` 行，只根据 `data:` 内容结构推断 thought、tool_call、message、done；当前没有处理 `session` 事件来更新面板的 `sessionId`。
- `ConversationService.saveFeedback` 使用 `.last("LIMIT 1")` 查询最新 assistant 消息；该 SQL 片段在当前代码中按字面存在。
- 知识库启动加载只扫描 `documents/` 下 `*.md` 和 `*.txt`，并把切片放入内存索引，同时写入文档元数据。
- 知识库删除只更新数据库文档 active 状态，不从内存 Retriever 索引移除已索引切片。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a global front-end Agent entry through the floating Agent button rendered by `App.vue`.
- **FR-002**: System MUST allow users to open/close the Agent panel and send non-empty text messages while no request is loading.
- **FR-003**: System MUST send chat requests to `/api/agent/chat` with `message`, optional `sessionId`, and token copied from `localStorage.xm-user`.
- **FR-004**: System MUST stream chat results as SSE events with current event types `thought`, `tool_call`, `message`, `session`, `done`, and `error`.
- **FR-005**: System MUST save user, tool, and assistant messages to `Tb_Agent_Conversation` when the Agent chat path processes a valid security context.
- **FR-006**: System MUST build Agent security context from the request body token, user information, role, unit code, unit name, unit id, and accessible unit ids when available.
- **FR-007**: System MUST allow admin users to access all units in Agent data queries and restrict non-admin users to their own accessible unit id set for `query_stat_data`.
- **FR-008**: System MUST register available Agent tools through `ToolRegistry` and expose their schemas to the selected LLM provider.
- **FR-009**: System MUST check every tool call through AccessGuard before execution and write an audit row to `Tb_Agent_Audit_Log`.
- **FR-010**: System MUST allow READ tools, allow SUGGEST tools with `SUGGESTED` audit decision, and block WRITE tools with `BLOCKED` audit decision.
- **FR-011**: System MUST use `TongJiGenericService.getReportData(...)` for the existing `query_stat_data` tool.
- **FR-012**: System MUST provide knowledge-base endpoints for listing, uploading text content, soft deleting, statistics, and search under `/api/knowledge`.
- **FR-013**: System MUST store knowledge document metadata in `Tb_Knowledge_Document` and keep retrieval chunks in the current in-memory Retriever index.
- **FR-014**: System MUST load markdown and text documents from the back-end `documents/` directory during knowledge service startup when the directory exists.
- **FR-015**: System MUST return JSON knowledge responses using the existing `Result.success(...)` envelope.
- **FR-016**: System MUST support configured LLM providers through current provider selection values: Ollama default/fallback, OpenAI, DeepSeek, and custom OpenAI-compatible API.
- **FR-017**: System MUST preserve current behavior that `/api/agent/**` and `/api/knowledge/**` are excluded from the global JWT interceptor.
- **FR-018**: System MUST preserve current front-end behavior where feedback buttons only update local UI state unless separately wired to `submitFeedback`.
- **FR-019**: System MUST preserve current front-end behavior where the Agent panel does not update its local `sessionId` from the server `session` event.
- **FR-020**: System MUST document this module as current-state behavior only; this specification does not request new Agent features or source changes.

### Key Entities *(include if feature involves data)*

- **AgentConversation**: Conversation row stored in `Tb_Agent_Conversation`; includes user id, session id, role, content, tool name, tool params, feedback, token estimate, and creation time.
- **AgentAuditLog**: Tool-call audit row stored in `Tb_Agent_Audit_Log`; includes user id, session id, tool name, params, access level, decision, optional model/duration fields, and creation time.
- **KnowledgeDocument**: Knowledge-base document metadata stored in `Tb_Knowledge_Document`; includes title, category, file type, chunk count, uploader, upload time, and active flag.
- **ConversationSession**: In-memory short-term conversation state keyed by session id; stores recent messages, tool call messages, a short summary, token estimate, and configured token thresholds.
- **SecurityContext**: Runtime Agent access boundary containing user id, user name, unit code/name/id, admin flag, and accessible unit ids.
- **ToolDefinition**: Runtime tool contract containing tool name, description, access level, parameter schema, and executor.
- **Retriever DocumentChunk**: In-memory knowledge chunk with doc id, title, section, content, and vector.

### Current API Surface

| Method | Path | Input | Output | Current notes |
| --- | --- | --- | --- | --- |
| POST | `/api/agent/chat` | JSON `{ message, sessionId, token }` | SSE stream | Uses request body token; timeout 120s |
| POST | `/api/agent/feedback` | JSON `{ sessionId, feedback }` | `Result.success()` | Updates latest assistant message for session |
| POST | `/api/agent/clear-session` | JSON `{ sessionId }` | `Result.success()` | Removes active in-memory session if id exists |
| GET | `/api/knowledge/list` | none | `Result.success(List<KnowledgeDocument>)` | Active documents only |
| POST | `/api/knowledge/upload` | JSON `{ title, category, content }` | `Result.success(KnowledgeDocument)` | Text content upload, not multipart |
| DELETE | `/api/knowledge/{id}` | path id | `Result.success()` | Soft delete by setting inactive |
| GET | `/api/knowledge/stats` | none | `Result.success({ documentCount, chunkCount })` | document count from active docs; chunk count from memory index |
| GET | `/api/knowledge/search` | query param `query` | `Result.success(String)` | Returns formatted matching chunks or empty-result message |

### Current Tool Surface

| Tool | Access | Current purpose |
| --- | --- | --- |
| `query_stat_data` | READ | Query a generic statistics module for a unit/year/month through report aggregation service |
| `get_unit_tree` | READ | Return unit id/code/name/parent rows from `Tb_DanWei` |
| `get_pending_approvals` | READ | Attempt to list pending approval records across configured approval tables |
| `get_approval_history` | READ | Attempt to read approval history for a module and record |
| `check_data_consistency` | READ | Return a current placeholder-style consistency completion message |
| `check_cumulative` | READ | Return a current placeholder-style cumulative-check completion message |
| `search_knowledge` | READ | Route knowledge search through `KnowledgeService.search(...)` in Agent layer |
| `summarize_report` | READ | Return a simple record-count summary for a module/unit/year |
| `compare_periods` | READ | Return a simple current-period vs comparison-period message |
| `record_feedback` | WRITE | Registered as a write tool and blocked by AccessGuard in Agent tool execution |

### Legacy Compatibility Points *(mandatory for this project)*

- **Database/schema impact**: Current module uses `Tb_Agent_Conversation`, `Tb_Agent_Audit_Log`, `Tb_Knowledge_Document`, business tables queried by tools, and `Tb_DanWei`.
- **Approval/status impact**: Agent can query or summarize approval-related information through tools, but the chat path does not mutate approval status.
- **Cumulative/reporting impact**: Agent reads report data through existing generic report aggregation and may ask LLM to explain results; it does not recalculate or persist report values.
- **API contract impact**: Existing routes under `/api/agent` and `/api/knowledge`, existing JSON envelopes, and SSE event names are current contracts.
- **Frontend route/UI impact**: `App.vue` renders `AgentFloatingButton`; `Manager` route includes `knowledge`; `AgentPanel` and `KnowledgeManager` consume Agent/Knowledge endpoints.
- **Export/file impact**: No Excel export is owned by this module; knowledge startup reads files from `documents/`; knowledge upload uses text body rather than file upload.
- **Security/auth impact**: `/api/agent/**` and `/api/knowledge/**` are excluded from global JWT interceptor; Agent chat self-checks token from request body; Knowledge endpoints have no controller-level auth check in current code.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A maintainer can identify all current Agent and Knowledge public endpoints, their inputs, and outputs from this specification without reading controllers.
- **SC-002**: A maintainer can identify all current Agent database tables and their ownership from this specification.
- **SC-003**: A maintainer can verify the current permission boundary for Agent chat and knowledge management, including the interceptor exclusion behavior.
- **SC-004**: A maintainer can execute the listed acceptance scenarios manually against the current app and decide whether behavior matches the documented current state.
- **SC-005**: The specification makes no request for refactoring, no new feature behavior, and no source-code modification.

## Assumptions

- This specification documents the repository state visible on 2026-07-04 and does not assert production runtime correctness.
- The user is asking for current-state documentation, so API names, table names, middleware behavior, and code-level boundaries are intentionally included.
- The current Agent module is treated as part of the legacy project contract even though the related files are currently untracked in git status.
- Validation for this specification is documentation review and static code cross-check, not service startup or database execution.
