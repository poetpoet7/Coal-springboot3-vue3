# Research: AI Agent 最小改动面试增强

## Decision 1: 使用文档与评测集先补面试支撑

**Decision**: 新增 `docs/agent-interview-matrix.md`、`docs/agent-demo-script.md`、`docs/agent-troubleshooting-playbook.md`、`docs/agent-langchain-langgraph-choice.md` 和 `demo/eval/agent_qa_cases.json`。

**Rationale**: 面试高频问题不仅要求代码存在，还要求能解释链路、取舍、评测和排查。文档和黄金问题集改动小、风险低、收益高。

**Alternatives considered**:

- 只改代码不写文档：无法支撑面试表达和排查题。
- 写长篇 README：信息容易混杂，不如按场景拆分文档。

## Decision 2: 保持单 Agent + 多 Tool

**Decision**: 继续使用 `CoalAssistantAgent + ToolRegistry + AccessGuard`，不引入复杂多 Agent。

**Rationale**: 当前业务域集中在统计问数、审批查询、制度问答和巡检。单 Agent 编排、多 Tool 接业务能力，最符合低侵入、可审计、可维护。

**Alternatives considered**:

- 多 Agent：增加状态传递、权限边界和排查复杂度。
- 工作流平台：超出 1-3 天目标，也不符合当前旧系统低侵入约束。

## Decision 3: 不直接引入 LangChain / LangGraph

**Decision**: 本轮只补取舍文档，不引入 LangChain/LangGraph。

**Rationale**: 原系统是 Java Spring Boot 技术栈，旧业务 Service 和 SQL Server 规则已存在。直接引入重型 AI 编排框架会增加依赖、部署、维护成本。当前更适合借鉴 Tool Calling、ReAct、状态管理思想，在现有 Java 代码中轻量实现。

**Alternatives considered**:

- 接 LangGraph：更适合复杂状态机和多阶段任务，不适合本轮最小增强。
- 接 LangChain4j：可作为后续 Java 生态选项，但本轮会改变依赖面。

## Decision 4: Tool trace 走 SSE/日志轻量增强

**Decision**: 在现有 SSE 链路中补 `tool_trace` 或增强 `tool_call` 内容，并在日志/审计中能看到 selectedTool、toolArgs、toolResultSummary、fallbackReason。

**Rationale**: Tool Calling 可解释性是面试重点。SSE 和日志增强改动小，不需要新数据库即可提升演示和排查能力。

**Alternatives considered**:

- 新增完整观测平台：过重。
- 修改审计表 schema 增加大量字段：本轮避免 schema 迁移。

## Decision 5: RAG 保持轻量，实现引用字段

**Decision**: 不接重型向量库；在当前 `Retriever`/`KnowledgeService` 返回中补 source、chunkId、score。

**Rationale**: 当前 RAG 链路已包括文档读取、切片、embedding、相似度检索。面试更需要能解释 chunk、embedding、retrieval、context 和引用，而不是引入外部向量库。

**Alternatives considered**:

- Chroma/BGE：后续可替换，但本轮会增加部署和依赖成本。
- 只写文档不改 RAG 输出：演示引用来源不够直观。

## Decision 6: 先修前端 session，再谈记忆

**Decision**: P0 修复 `agent.js` SSE event 解析和 `AgentPanel.vue` sessionId 回写。

**Rationale**: 后端已有 `ConversationSession` 和数据库会话记录，但前端不回写 session 会让多轮记忆演示失真。修复成本低、收益高。

**Alternatives considered**:

- 做复杂长期记忆：超出范围，也没有评测支撑。
- 只在文档解释：现场演示会暴露问题。

## Decision 7: 默认只读和 AccessGuard 不变

**Decision**: 保持 READ/SUGGEST/WRITE 分级；WRITE 阻断，SUGGEST 二次确认或仅建议。

**Rationale**: 真实能源企业统计审批数据有合规要求。AI 只做查询、解释、辅助巡检，不直接改业务表，是正确边界。

**Alternatives considered**:

- 让 Agent 自动审批或修数：风险高，不符合真实业务系统。
- 只靠 Prompt 禁止写：不够可靠，必须有后端 AccessGuard。

## Decision 8: 巡检 Tool 只做最小只读补强

**Decision**: P1 可把 `check_cumulative` 和一个 `check_data_consistency` 场景接成真实只读校验，但不改旧统计数据。

**Rationale**: 数据质量巡检贴合业务且面试亮点高。复用 `TongJiGenericService`、`CumulativeUtils` 规则或查询结果，做只读异常报告即可。

**Alternatives considered**:

- 完整规则引擎：过重。
- 保持占位文案：面试可信度不足。

