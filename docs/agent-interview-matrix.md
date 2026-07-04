# AI Agent 面试覆盖矩阵

> 定位口径：本项目是实习期间参与的真实能源/煤矿企业内网统计审批系统上的低侵入 AI 智能增强模块。AI Agent 用于增强产量/统计数据填报、单位汇总、逐级审批、报表管理等既有后台能力，不是临时演示项目，也不是企业级 Agent 平台重构。

## 1. 面试总述

推荐开场：

> 原系统是 Java Spring Boot + Vue 的企业内网后台系统，核心业务是产量/统计数据填报、单位树汇总、逐级审批和报表管理。我在不重写旧 CRUD、统计和审批逻辑的前提下，新增 AI Agent 能力：自然语言问数、RAG 制度问答、审批助理、数据质量巡检、知识库管理、会话记忆、只读安全控制和审计。技术取舍重点是低侵入、可控、合规、复用旧 Service、默认只读和可追溯。

## 2. 覆盖矩阵

| 面试题 | 当前代码/文档支撑 | 面试讲法 | 可信度 | 是否还需补强 |
| --- | --- | --- | --- | --- |
| AI 应用开发和传统后端开发的区别 | `CoalAssistantAgent`, `ToolRegistry`, `KnowledgeService`, `ConversationService`, `docs/project-map.md` | 传统后台是确定性 CRUD/审批流；AI 增强多了 Prompt、RAG、Tool Calling、会话状态、权限审计、评测和不稳定输出治理。 | 5 | 否 |
| 如何使用 AI 编程工具并保证质量 | `docs/agent-demo-script.md`, `demo/eval/agent_qa_cases.json`, 本矩阵 | 用 AI 辅助整理规格、测试问题和解释材料，但质量靠代码审查、静态检查、黄金问题集和手动链路验证兜底。 | 3 | 后续可补 AI 代码审查 checklist |
| LangChain 和 LangGraph 区别，为什么本项目未直接使用 | `docs/ai-agent-interview-review.md`, `specs/001-ai-agent-current-state/research.md` | 借鉴 Tool、RAG、ReAct、状态管理思想；不硬接框架是因为旧 Java 系统、内网部署、低侵入、权限审计和维护成本约束。 | 4 | P1 可补专门取舍文档 |
| 为什么选择单 Agent + 多 Tool | `CoalAssistantAgent`, `ToolRegistry` | 业务域集中在问数、审批、知识库、巡检；一个 Agent 做意图理解，多 Tool 复用真实业务能力，链路短且可审计。 | 5 | 否 |
| 智能问数怎么实现 | `ToolRegistry.query_stat_data`, `TongJiGenericService.getReportData` | 用户自然语言问题由 LLM 选择 `get_unit_tree`/`query_stat_data`，后端复用旧统计 Service 查询和汇总，不重写统计逻辑。 | 5 | 可继续补 Tool trace |
| Tool Calling 从输入到执行的链路 | `AgentChatController`, `CoalAssistantAgent`, `ToolRegistry`, `AccessGuard` | 前端发 SSE 请求，Agent 携带工具 schema 调 LLM，解析 tool_calls，经过 AccessGuard，执行 READ Tool，保存会话/审计，最终总结。 | 4 | 本轮修 session；后续补更完整 trace |
| Tool 调用如何可解释 | `ToolCallBlock.vue`, `Tb_Agent_Audit_Log`, 本轮文档 | 当前能看到工具名和参数，审计表记录 tool_name/tool_params/access_decision；后续可补 toolResultSummary/fallbackReason。 | 3 | 是，T018-T023 后续做 |
| RAG 的作用和完整链路 | `KnowledgeController`, `KnowledgeService`, `TextSplitter`, `EmbeddingService`, `Retriever` | 制度/流程类问题不靠模型记忆，走文档读取、切片、向量化、相似度检索、返回来源片段，再由 Agent 组织回答。 | 4 | 本轮只文档说明，后续补 source/score/chunk |
| RAG 上下文超限怎么处理 | `TextSplitter`, `Retriever` topK/threshold, `application.yml` | 通过 chunk size、overlap、topK、similarity threshold 控制注入上下文规模；长文档先切片再检索。 | 3 | 后续可补 rerank/引用去重 |
| RAG 无关文本混入怎么处理 | `Retriever.search`, `similarity-threshold` | 使用相似度阈值过滤，评测集中准备无关问题观察是否拒答或提示无结果。 | 3 | 后续可补低分拒答策略 |
| RAG 信息冲突怎么处理 | `KnowledgeService.search`, 本文档 | 当前策略是展示来源并避免隐式合并冲突；面试中诚实说明未做复杂冲突仲裁，后续可按来源时间/权威级别排序。 | 2 | 是 |
| 长文档检索如何避免章节断裂 | `TextSplitter.split` | 优先按 `##` 标题和段落切片，保留 overlap，让片段尽量保持章节语义。 | 4 | 否 |
| Agent 记忆有哪些层 | `ConversationSession`, `ConversationService`, `Tb_Agent_Conversation`, `Tb_Knowledge_Document` | 会话内短期记忆、超限摘要记忆、知识库记忆、反馈记忆四层；当前不做复杂用户画像长期记忆。 | 4 | 本轮修前端 session |
| 本项目记忆遇到什么问题 | `agent.js`, `AgentPanel.vue` | 后端已发 `session`，但前端原来没有回写，导致多轮会话可能无法复用同一 session；本轮修复。 | 5 | 本轮处理 |
| 为什么默认只读 | `AccessGuard`, `AccessLevel`, `ToolRegistry.record_feedback` | 统计、审批、报表数据有合规要求；AI 只做查询、解释、巡检，不直接改生产数据，WRITE Tool 被后端拦截。 | 5 | 否 |
| Agent 如何防止越权 | `SecurityContext`, `PermissionService`, `AccessGuard` | 单位权限来自旧系统单位树；非管理员只能查本单位及下级；Tool 层还有 READ/WRITE/SUGGEST 分级审计。 | 4 | 后续可收紧 Knowledge 鉴权代码 |
| 审计怎么做 | `Tb_Agent_Audit_Log`, `AccessGuard.checkAndLog` | 每次 Tool 调用记录 userId、sessionId、toolName、params、accessLevel、decision、createdAt，便于追溯。 | 4 | 后续补 result summary |
| completion rate 下降如何排查 | `AgentChatController` SSE error, LLM Provider error, `Tb_Agent_Audit_Log`, `Tb_Agent_Conversation` | 按模型调用、SSE、Tool、权限、RAG、DB 逐层排查；看错误事件、工具审计、会话记录和 Provider 返回。 | 3 | 本轮补 playbook |
| 如何判断幻觉/RAG/Prompt/Tool 问题 | `KnowledgeService.search`, `ToolRegistry`, `CoalAssistantAgent`, `docs/agent-troubleshooting-playbook.md` | 有来源片段但回答错偏 Prompt/模型；无来源或低相关偏 RAG；Tool 参数错偏 Tool Calling；Tool 结果错偏业务 Service/DB。 | 3 | 本轮补 playbook |
| 如何做评测 | `demo/eval/agent_qa_cases.json` | 用黄金问题集标注 expectedTool、expectedBehavior、passCriteria，人工或脚本记录工具命中、引用、答案是否基于工具结果。 | 4 | 本轮处理 |
| 响应时间优化做了什么 | SSE、`maxRounds=2`, RAG topK, 一次查全年提示 | 用 SSE 降低感知等待；限制 ReAct 轮次和检索数量；提示全年数据不要逐月查。 | 4 | 后续补 latency 字段 |
| Deep Research 如何设计 | `docs/ai-agent-interview-review.md` | 当前模块不是 Deep Research；可设计任务拆解、证据池、引用报告、人工确认和评测，但不声称已实现。 | 2 | P2 文档可补 |

## 3. 安全演示链路

面试中可以直接讲这条链路：

```text
LLM 选择 Tool
  -> ToolRegistry 找到 ToolDefinition
  -> AccessGuard.checkAndLog
  -> READ: ALLOWED
  -> SUGGEST: SUGGESTED，需要人工确认
  -> WRITE: BLOCKED
  -> Tb_Agent_Audit_Log 记录 toolName/toolParams/accessDecision
```

当前最适合演示的 WRITE 安全点是 `record_feedback`：它在 `ToolRegistry` 中注册为 WRITE，因此如果由 Agent 工具调用路径触发，会被 `AccessGuard` 阻断。真实反馈应通过受控业务接口 `/api/agent/feedback`，而不是让 LLM 自行写数据。

## 4. 诚实边界

- 当前是低侵入 AI 增强模块，不是企业级 Agent 平台。
- 当前 RAG 是试点阶段轻量实现，不包装成生产级向量平台。
- 当前不做复杂多 Agent，不强行接 LangGraph。
- 当前巡检 Tool 仍需后续接更多真实规则。
- 当前 Knowledge 接口鉴权边界需要在后续代码任务中进一步收紧；本轮先在排查文档中明确风险和边界。

