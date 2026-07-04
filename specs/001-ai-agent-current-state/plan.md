# Implementation Plan: AI Agent 最小改动面试增强

**Branch**: `001-ai-agent-current-state` | **Date**: 2026-07-04 | **Spec**: [spec.md](./spec.md)

**Input**: 基于当前 AI Agent 现状规格、项目架构地图、AI Agent 面试覆盖率评审，以及用户要求的“真实企业内网统计审批系统 AI 增强模块”最小改动优化目标。

## Summary

本计划面向真实能源/煤矿企业内网统计审批系统上的 AI 能力增强模块。目标不是把项目改造成企业级 Agent 平台，也不是引入重型编排框架，而是在 1-3 天内用最小改动补强面试高频问题的代码和材料支撑。

核心策略：

- 文档优先补齐面试矩阵、演示脚本、故障排查、框架取舍和评测集。
- 小代码改动聚焦现有 Agent 链路的可演示稳定性：SSE/session、Tool trace、RAG 引用、只读安全边界。
- 智能问数继续复用 `TongJiGenericService` 等旧业务 Service，不重写统计、审批、单位汇总逻辑。
- RAG 保持轻量实现，只增强 source/chunkId/score 和引用展示，不强行接入重型向量库。
- 默认只读，WRITE/SUGGEST 继续经 `AccessGuard` 阻断或二次确认，审计可追溯。

## A. 最小改动架构方案

### A1. 总体链路

```text
Vue AgentPanel
  -> src/utils/agent.js fetch/SSE
  -> AgentChatController
  -> token/session/security context
  -> CoalAssistantAgent
  -> LLM provider with Tool schemas
  -> ToolRegistry
  -> AccessGuard
  -> Existing services: TongJiGenericService / PermissionService / KnowledgeService
  -> SSE: thought / tool_trace / tool_call / message / session / done / error
  -> Tb_Agent_Conversation + Tb_Agent_Audit_Log
```

### A2. 文档与评测材料层

新增以下面试支撑材料：

- `docs/agent-interview-matrix.md`: AI 高频面试题、当前支撑点、演示入口、诚实边界。
- `docs/agent-demo-script.md`: 现场演示脚本，覆盖问数、RAG、审批助理、数据巡检、安全拦截。
- `docs/agent-troubleshooting-playbook.md`: completion rate 下降、效果波动、幻觉/RAG/Prompt/Tool 归因。
- `docs/agent-langchain-langgraph-choice.md`: 为什么不直接引入 LangChain/LangGraph，为什么选择单 Agent + 多 Tool。
- `demo/eval/agent_qa_cases.json`: 黄金问题集，每条包含 `expectedTool`、`expectedBehavior`、`passCriteria`。

### A3. 智能问数补强

保留现有 `query_stat_data -> TongJiGenericService.getReportData(...)` 链路，不重写旧统计逻辑。补强点是可解释性：

- 文档说明自然语言如何由 LLM 映射到 `get_unit_tree`、`query_stat_data` 等 Tool。
- SSE 或日志暴露 `selectedTool`、`toolArgs`、`toolResultSummary`。
- 演示问题集中包含单位、年份、月份、模块名的问数样例。
- 查询类 Tool 均保持 READ，不引入写操作。

### A4. RAG 补强

保持当前 `KnowledgeService + TextSplitter + EmbeddingService + Retriever` 轻量链路。补强点：

- 检索结果返回或展示 `source`、`chunkId`、`score`。
- Agent 最终回答中可展示引用来源。
- 文档说明上下文超限、无关文本混入、信息冲突、长文档断裂的处理策略。
- 不在本轮引入 Chroma/BGE/复杂 rerank，只保留后续替换路径说明。

### A5. Tool Calling 可解释性

在现有 `ToolRegistry` 基础上补轻量能力：

- 增加工具清单输出能力，包含 `name`、`description`、`parameters`、`accessLevel`。
- `AgentChatController` 可新增只读工具清单接口，或在文档/演示中使用后端已有 `getToolSchemas()`。
- SSE 新增或复用事件展示 `selectedTool`、`toolArgs`、`toolResultSummary`。
- Tool 失败时返回 `fallbackReason`，便于区分模型未选工具、工具参数错误、权限拦截、业务查询为空、LLM provider 失败。

### A6. 记忆/session 补强

当前后端会发送 `session` SSE 事件，但前端没有回写。最小改动：

- `agent.js` 正确维护 SSE `event:` 类型。
- `AgentPanel.vue` 通过 callback 保存 server `sessionId`。
- 第二轮请求携带相同 `sessionId`。
- 文档说明四层记忆：会话内记忆、会话摘要记忆、知识库记忆、反馈记忆。
- 不做复杂长期记忆系统，不做用户画像/自动偏好学习。

### A7. 安全补强

保持 Agent 默认只读和审计策略：

- 明确 `/api/agent/**`、`/api/knowledge/**` 当前被 JWT interceptor 放行的事实和边界。
- Agent chat 继续从请求 token 构建 `SecurityContext`，但移除或收紧开发 fallback admin 口径。
- Knowledge 管理接口补充鉴权边界：至少文档明确，优先小改代码校验 token。
- 敏感配置从仓库默认值移出，使用环境变量。
- WRITE Tool 继续由 `AccessGuard` 阻断；SUGGEST Tool 需要二次确认，不直接写业务表。

### A8. 评测与排查补强

新增黄金问题集和排查文档：

- 每条评测包含 `id`、`category`、`question`、`expectedTool`、`expectedBehavior`、`passCriteria`。
- 故障排查按六类分层：模型、RAG、Prompt、Tool、权限、数据库/旧业务 Service。
- completion rate 下降从请求数、SSE error、Tool audit、Provider error、空检索、权限拒绝、超时分层排查。

## B. 涉及文件列表

### 文档与评测材料

| 文件 | 操作 | 用途 |
| --- | --- | --- |
| `docs/agent-interview-matrix.md` | 新增 | 面试题覆盖矩阵 |
| `docs/agent-demo-script.md` | 新增 | 现场演示脚本 |
| `docs/agent-troubleshooting-playbook.md` | 新增 | 故障排查和归因 |
| `docs/agent-langchain-langgraph-choice.md` | 新增 | LangChain/LangGraph 取舍 |
| `demo/eval/agent_qa_cases.json` | 新增 | 黄金问题集 |

### 小代码改动候选

| 文件 | 操作 | 用途 |
| --- | --- | --- |
| `COAL/vue/src/utils/agent.js` | 修改 | 正确解析 SSE event，回调 session/tool trace/error |
| `COAL/vue/src/components/agent/AgentPanel.vue` | 修改 | 保存 sessionId，保持多轮同一会话 |
| `COAL/vue/src/components/agent/FeedbackButtons.vue` | 可选修改 | 打通反馈提交 |
| `COAL/vue/src/components/agent/ChatMessage.vue` | 可选修改 | 显示引用来源或 trace 摘要 |
| `COAL/springboot/src/main/java/com/example/agent/controller/AgentChatController.java` | 修改 | 输出 tool trace、可选工具清单接口、鉴权边界说明 |
| `COAL/springboot/src/main/java/com/example/agent/agent/CoalAssistantAgent.java` | 修改 | 返回 toolResultSummary/fallbackReason |
| `COAL/springboot/src/main/java/com/example/agent/tool/ToolRegistry.java` | 修改 | 输出工具清单、最小巡检 Tool 补强 |
| `COAL/springboot/src/main/java/com/example/agent/tool/AccessGuard.java` | 可选修改 | 审计中补充 result summary 或 trace 字段时同步 |
| `COAL/springboot/src/main/java/com/example/agent/rag/KnowledgeService.java` | 修改 | RAG 返回 source/chunkId/score |
| `COAL/springboot/src/main/java/com/example/agent/rag/Retriever.java` | 修改 | 暴露 chunkId/score 所需字段 |
| `COAL/springboot/src/main/resources/application.yml` | 修改 | 移除敏感默认值 |

### 旧业务依赖，不重写

| 文件 | 只读依赖 |
| --- | --- |
| `COAL/springboot/src/main/java/com/example/service/impl/TongJiGenericService.java` | 智能问数复用 `getReportData(...)` |
| `COAL/springboot/src/main/java/com/example/service/impl/PermissionService.java` | 单位权限边界 |
| `COAL/springboot/src/main/java/com/example/utils/CumulativeUtils.java` | 累计值规则说明和巡检参考 |

## C. 只写文档的任务

| 任务 | 覆盖问题 | 验收标准 |
| --- | --- | --- |
| 编写 `docs/agent-interview-matrix.md` | AI 应用开发、RAG、Tool、记忆、安全、评测 | 每个问题都有代码/文档支撑点和诚实边界 |
| 编写 `docs/agent-demo-script.md` | 现场演示 | 至少 5 条链路：问数、RAG、巡检、审批、安全拦截 |
| 编写 `docs/agent-troubleshooting-playbook.md` | completion rate、幻觉归因、偶发错误 | 包含模型/RAG/Prompt/Tool/权限/DB 六类归因 |
| 编写 `docs/agent-langchain-langgraph-choice.md` | 框架取舍 | 明确低侵入、Java 栈、内网、合规、维护成本 |
| 编写 `demo/eval/agent_qa_cases.json` | 评测集 | 至少 15 条黄金问题，含 expectedTool/expectedBehavior/passCriteria |
| 补记忆四层设计说明 | 记忆面试题 | 明确会话内、摘要、知识库、反馈四层，不宣称复杂长期记忆 |
| 补 RAG 风险治理说明 | RAG 上下文、无关召回、冲突、断裂 | 每类问题有当前策略和后续扩展策略 |

## D. 需要小改代码的任务

| 任务 | 覆盖问题 | 涉及文件 | 风险 | 预计耗时 | 验收标准 |
| --- | --- | --- | --- | --- | --- |
| 修复 SSE event 解析和 sessionId 回写 | 多轮记忆、流式响应 | `agent.js`, `AgentPanel.vue` | 低 | 0.5 天 | 第二轮请求携带同一个 sessionId |
| 增加 Tool trace SSE 或日志输出 | Tool Calling 可解释性、排查 | `AgentChatController`, `CoalAssistantAgent` | 中低 | 0.5-1 天 | 前端或日志可见 selectedTool/toolArgs/toolResultSummary/fallbackReason |
| 输出 ToolRegistry 工具清单 | Tool schema、框架对比 | `ToolRegistry`, `AgentChatController` | 低 | 0.5 天 | 可获取 name/description/parameters/accessLevel |
| RAG 检索结果带 source/chunkId/score | RAG 引用与可解释性 | `KnowledgeService`, `Retriever`, 可选 `ChatMessage.vue` | 中低 | 0.5-1 天 | 检索结果和 Agent 回答可见引用来源 |
| 明确 Knowledge 接口鉴权 | 安全边界 | `KnowledgeController` 或 `WebConfig` | 中 | 0.5-1 天 | 未登录不能管理知识库，或文档明确内网边界和 token 校验 |
| 移除敏感默认配置 | 安全、合规 | `application.yml` | 低 | 0.5 天 | 仓库中不包含真实默认 API key |
| 最小累计值巡检 Tool | 数据质量巡检 | `ToolRegistry`, 复用统计查询/CumulativeUtils 规则 | 中 | 1 天 | 只读返回异常明细或未发现异常 |
| 打通反馈按钮提交 | 评测/反馈记忆 | `FeedbackButtons.vue`, `ChatMessage.vue`, `AgentPanel.vue` | 低 | 0.5 天 | 点赞/踩写入最新 assistant 消息 |

## E. 不建议做的任务

| 不建议做 | 原因 |
| --- | --- |
| 引入复杂多 Agent | 当前业务域集中，单 Agent + 多 Tool 更符合低侵入和审计要求 |
| 强行接 LangGraph | 1-3 天收益低，增加依赖和维护成本 |
| 重写旧 CRUD、统计、审批逻辑 | 破坏真实业务系统稳定性，不符合 constitution |
| 做企业级权限平台 | 复用现有 `PermissionService` 和单位树权限即可 |
| 把轻量 RAG 包装成生产级向量平台 | 当前是试点阶段轻量实现，应诚实说明替换路径 |
| 让 Agent 执行业务写操作 | 统计/审批数据必须保留人工确认和原流程 |
| 为模型效果大改业务系统 | Prompt/模型优化必须建立在评测和日志基础上 |
| 引入新数据库或新部署体系 | 超出 1-3 天最小增强目标 |

## F. 推荐先实现的 P0 任务清单

| 顺序 | P0 任务 | 为什么先做 | 改动类型 |
| --- | --- | --- | --- |
| 1 | `docs/agent-interview-matrix.md` | 立刻提升面试表达结构 | 文档 |
| 2 | `docs/agent-demo-script.md` | 让现场演示可控 | 文档 |
| 3 | `demo/eval/agent_qa_cases.json` | 支撑“如何评测”高频题 | 文档/数据 |
| 4 | `docs/agent-troubleshooting-playbook.md` | 支撑 completion rate 和问题归因 | 文档 |
| 5 | 修复 SSE/sessionId | 支撑多轮记忆演示 | 小代码 |
| 6 | 移除敏感默认配置 | 支撑安全合规表达 | 小代码 |
| 7 | Tool trace 输出 | 支撑 Tool Calling 可解释性 | 小代码 |

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.1) back end; JavaScript with Vue 3.4/Vite 5 front end

**Primary Dependencies**: MyBatis-Plus 3.5.5, SQL Server JDBC, Hutool, Lombok, JWT, Apache POI/Tika, Element Plus, Vue Router, Axios/fetch/SSE

**Storage**: SQL Server business schema; Agent metadata tables `Tb_Agent_Conversation`, `Tb_Agent_Audit_Log`, `Tb_Knowledge_Document`; local back-end `documents/` for knowledge startup loading; local `files/` for existing uploads

**Testing**: Existing automated tests are not visible. Use Markdown inspection for documentation; JSON validation for eval set; targeted manual validation for Agent SSE/session/RAG/Tool trace; build checks for modified front-end/back-end code when implementation happens.

**Target Platform**: Spring Boot service on port 9090 plus Vue SPA served by Vite/build output in an enterprise intranet-style environment.

**Project Type**: Legacy web application with separated Spring Boot back end and Vue 3 front end, enhanced by AI Agent/RAG/Tool Calling module.

**Performance Goals**: Preserve current Agent response behavior while improving perceived latency observability. Keep ReAct tool loop bounded to current small round count. Keep RAG topK bounded and avoid new heavy vector database dependency in this iteration.

**Constraints**: Preserve legacy SQL Server schema, approval/cumulative semantics, unit tree permissions, API envelopes, token/localStorage contract, front-end routes, existing Agent/Knowledge routes, default read-only Agent behavior, and low-intrusion integration with old services.

**Scale/Scope**: 1-3 day enhancement covering AI Agent, RAG, Tool trace, interview/eval documentation, and front-end session stability. No enterprise Agent platform, no multi-Agent system, no major schema migration, no rewrite of statistics/approval services.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Legacy business semantics**: PASS. Plan does not alter fill-in, submit, approve, return, aggregate, export, or cumulative business semantics. New巡检 Tool is read-only and reports observations only.
- **Module boundaries**: PASS. Back-end edits stay under `com/example/agent` except read-only reuse of `TongJiGenericService`, `PermissionService`, and `CumulativeUtils`. Front-end edits stay under `src/utils/agent.js` and `src/components/agent`.
- **Data access pattern**: PASS. Intelligent question answering continues to reuse `TongJiGenericService.getReportData(...)`; no new ORM or raw unparameterized user SQL is planned.
- **API/frontend contract**: PASS with note. Existing `/api/agent/chat`, `/feedback`, `/clear-session`, and `/api/knowledge/*` routes remain. Optional tool list endpoint must use `Result.success(...)` and document auth boundary.
- **Security-sensitive config**: PASS with action. Plan explicitly removes sensitive default API key values and keeps environment variable injection.
- **Verification**: PASS. Documentation has Markdown/JSON checks; code changes require front-end build or targeted manual browser validation and back-end build where feasible.
- **Unrelated refactor avoidance**: PASS. No old CRUD/statistics/approval rewrite, no LangGraph/LangChain migration, no new deployment stack.

## Project Structure

### Documentation (this feature)

```text
specs/001-ai-agent-current-state/
  plan.md
  research.md
  data-model.md
  quickstart.md
  contracts/
    agent-sse-events.md
    tool-registry-contract.md
    rag-retrieval-contract.md
    eval-cases-schema.md
```

### Source Code (repository root)

```text
COAL/
  springboot/
    src/main/java/com/example/
      agent/
        controller/        # AgentChatController, KnowledgeController
        agent/             # CoalAssistantAgent, ConversationService/Session
        tool/              # ToolRegistry, AccessGuard, ToolDefinition
        rag/               # KnowledgeService, Retriever, TextSplitter, EmbeddingService
      service/impl/        # reused TongJiGenericService, PermissionService
      utils/               # reused CumulativeUtils
    src/main/resources/
      application.yml      # sensitive defaults removed when implemented
      sql/agent_init.sql
  vue/
    src/
      utils/agent.js
      components/agent/
  docs/
  demo/eval/
```

**Structure Decision**: Use existing Agent package and Vue Agent components. Add only documentation/eval directories already aligned with repository-level docs and demo assets. Do not add new framework directories.

## Complexity Tracking

No constitution violations are planned. The only moderate-risk work is optional authentication adjustment for Knowledge endpoints and optional cumulative/data consistency Tool enhancement; both must stay read-only and preserve legacy services.

