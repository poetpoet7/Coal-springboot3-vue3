# AI Agent 现场演示脚本

> 演示目标：证明这是接入真实能源企业内网统计审批系统的低侵入 AI 智能增强模块，能复用旧统计/审批/权限能力，并且默认只读、可审计、可排查。

## 演示前准备

1. 使用有权限的账号登录系统。
2. 打开任意后台页面，确认右下角或全局位置出现 Coal-AI 助手入口。
3. 准备一条已知单位、年份、月份或统计模块数据，避免现场依赖未知数据库状态。
4. 如果知识库为空，先在知识库管理中上传一段制度文本，例如“统计数据填报规则”“审批流程说明”。
5. 打开浏览器 DevTools Network，过滤 `/api/agent/chat`，用于观察 `message`、`sessionId`、`token`。

## 链路 1：自然语言问数 -> Tool -> 统计 Service -> 结构化回答

**示例问题**

```text
查询 2026 年某单位生产经营总值的全年统计情况
```

**预期 Tool**

- `get_unit_tree`：当用户输入单位名称时，用于解析单位 ID。
- `query_stat_data`：调用旧统计服务查询汇总数据。

**观察点**

- 前端：Agent 面板出现用户问题、思考提示、工具调用块、最终回答。
- Network：`POST /api/agent/chat` 请求体包含 `message`、`sessionId`、`token`。
- 后端：`ToolRegistry.query_stat_data` 复用 `TongJiGenericService.getReportData(...)`。
- 审计：`Tb_Agent_Audit_Log` 可记录 READ Tool 的 `ALLOWED` 决策。

**面试讲法**

> 这个链路不是让模型直接编 SQL，而是让模型选择受控 Tool。真正的数据查询仍由旧系统的统计 Service 完成，所以能复用单位树汇总、年月筛选和历史统计规则。

**失败兜底**

- 如果没有数据：说明工具返回空结果，Agent 应基于工具结果告诉用户无数据，而不是编造。
- 如果单位名无法识别：先演示 `get_unit_tree` 或使用明确的单位 ID。

## 链路 2：制度问答 -> RAG 检索 -> 引用来源回答

**示例问题**

```text
统计数据上报前需要注意哪些填报规则？
```

**预期 Tool**

- `search_knowledge`

**观察点**

- `KnowledgeService.search(...)` 从内存 Retriever 检索相关切片。
- 返回内容包含来源文档标题和片段。
- Agent 最终回答基于检索片段，而不是泛泛聊天。

**面试讲法**

> 制度类问题不依赖模型记忆，而是走 RAG。当前实现是轻量闭环：文档读取、标题/段落切片、简化 embedding、相似度检索、topK 返回。它适合内网试点阶段，后续可以平滑替换为 BGE + Chroma。

**失败兜底**

- 如果检索为空：展示“未找到相关知识”的结果，并说明需要补充制度文档。
- 如果召回不相关：说明可通过阈值、topK、评测集和后续 rerank 优化。

## 链路 3：多轮会话 -> sessionId 回写 -> 同一会话上下文

**示例问题**

第一轮：

```text
帮我查一下 2026 年某单位的生产经营总值
```

第二轮：

```text
那它和上个月相比有什么变化？
```

**预期行为**

- 第一轮 SSE 返回 `session`。
- 前端保存后端 `sessionId`。
- 第二轮请求携带同一个 `sessionId`。

**观察点**

- DevTools Network 中第二次 `/api/agent/chat` request body 的 `sessionId` 与第一轮返回一致。
- `ConversationSession` 使用同一会话历史。

**面试讲法**

> 本项目的记忆先做会话级轻量实现：前端保存 sessionId，后端用内存 session 维护短期历史，同时把消息写入会话表。它不是复杂长期记忆系统，但足够支撑内网问数场景的上下文连续性。

## 链路 4：审批助理 -> 待审批/审批历史查询

**示例问题**

```text
当前某单位有哪些待审批的统计记录？
```

**预期 Tool**

- `get_pending_approvals`
- `get_approval_history`

**观察点**

- Tool 查询审批相关表。
- Agent 只汇总待办或历史，不修改审批状态。

**面试讲法**

> 审批助理不是绕开原审批流，而是帮助用户快速查看待办和历史。真正的审批动作仍在旧业务页面中由用户确认完成。

## 链路 5：数据质量巡检 -> 只标异常不改数据

**示例问题**

```text
检查某单位 2026 年 6 月统计数据有没有累计值异常
```

**预期 Tool**

- `check_cumulative`
- `check_data_consistency`

**当前状态**

当前巡检 Tool 已有入口，但部分规则仍是轻量/占位式。本轮不实现 T031/T032，后续建议将其接到 `TongJiGenericService` 和 `CumulativeUtils` 的只读校验逻辑。

**面试讲法**

> 数据巡检适合做 AI 增强，因为它可以帮助定位异常，但必须只读。Agent 只返回异常报告或建议，不自动修数。

## 链路 6：安全拦截 -> WRITE Tool 被 AccessGuard 阻断

**示例问题**

```text
帮我直接修改这条统计数据或替我审批通过
```

**预期行为**

- Agent 应拒绝直接写业务数据。
- 如果 LLM 尝试调用 WRITE Tool，`AccessGuard` 记录 `BLOCKED`。

**观察点**

- `ToolRegistry.record_feedback` 是 WRITE Tool。
- `AccessGuard.checkAndLog(...)` 对 WRITE 返回 false。
- 审计表记录 `access_decision = BLOCKED`。

**面试讲法**

> 安全不是只靠 Prompt，而是后端做硬边界。统计、审批、报表数据默认只读，写操作必须回到原业务系统和人工确认流程。

## 手动验证 sessionId 回写

1. 打开浏览器 DevTools -> Network。
2. 发送第一轮 Agent 问题。
3. 在响应流中查找 `event: session` 或前端 console/Network 中的 session 值。
4. 发送第二轮追问。
5. 打开第二次 `/api/agent/chat` 请求 payload。
6. 验收：第二次请求的 `sessionId` 等于第一轮后端返回的 `session`。

## 不要这样演示

- 不要说“这是临时 demo”。
- 不要说 Agent 可以自动修改统计数据。
- 不要把轻量 RAG 包装成生产级向量平台。
- 不要现场引入 LangGraph、多 Agent 或新依赖。

