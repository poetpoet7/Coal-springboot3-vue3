# Tasks: AI Agent 最小改动面试增强

**Input**: Design documents from `specs/001-ai-agent-current-state/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [quickstart.md](./quickstart.md), [contracts/](./contracts/)

**排序规则**: 面试收益 > 改动风险 > 工程完整度。工程完整度仍必须保证可演示链路不答非所问。

**Tests**: 当前仓库缺少可见自动化测试基础。文档任务用 Markdown/JSON 检查验证；代码任务优先用可执行手工链路验证，涉及编译的后端/前端改动需补充 `mvn -q -DskipTests compile` 或 `npm run build`。

**Task Format**: `- [ ] T001 [P?] [US?] Description with file path`

## Phase 1: Setup and Legacy Impact Review

**Purpose**: 锁定真实业务系统背景、低侵入边界和安全约束，避免后续任务把项目误写成 demo 或平台化重构。

- [ ] T001 Review `docs/ai-agent-interview-review.md`, `docs/project-map.md`, and `.specify/memory/constitution.md`; 覆盖面试题: 项目背景/技术取舍; 具体动作: 提取“真实企业内网统计审批系统 AI 增强模块”统一口径; 是否改代码: 否; 风险: 低; 验收: 后续文档不得将项目表述为临时 demo
- [ ] T002 Review `specs/001-ai-agent-current-state/plan.md`; 覆盖面试题: 架构方案/最小改动边界; 具体动作: 确认 P0/P1/P2 范围、禁止项、涉及文件; 是否改代码: 否; 风险: 低; 验收: tasks.md 中所有代码任务均能追溯到 plan.md
- [ ] T003 [P] Review `COAL/springboot/src/main/java/com/example/agent` and `COAL/vue/src/components/agent`; 覆盖面试题: Agent 当前能力; 具体动作: 标记 SSE/session、Tool trace、RAG 引用、安全审计现状; 是否改代码: 否; 风险: 低; 验收: P0 代码任务均有明确现状依据
- [ ] T004 [P] Review `COAL/springboot/src/main/java/com/example/service/impl/TongJiGenericService.java`, `COAL/springboot/src/main/java/com/example/service/impl/PermissionService.java`, and `COAL/springboot/src/main/java/com/example/utils/CumulativeUtils.java`; 覆盖面试题: 智能问数/权限/累计值; 具体动作: 确认只复用旧业务逻辑、不重写统计审批; 是否改代码: 否; 风险: 低; 验收: P1 巡检任务只读且不改变旧业务语义

---

## Phase 2: Foundational Safety and Verification

**Purpose**: 所有 P0/P1 任务共用的基础约束，必须先明确：默认只读、敏感配置、鉴权边界、评测验证方式。

- [ ] T005 Create implementation checklist in `specs/001-ai-agent-current-state/quickstart.md`; 覆盖面试题: 验证方式; 具体动作: 补充 P0 文档、JSON、SSE/session、Tool trace、RAG 引用、安全拦截的验证顺序; 是否改代码: 否; 风险: 低; 验收: quickstart 覆盖所有 P0 验收步骤
- [ ] T006 Inspect `COAL/springboot/src/main/resources/application.yml`; 覆盖面试题: 敏感配置安全; 具体动作: 标记所有 API key/密码默认值及环境变量替代方式; 是否改代码: 否; 风险: 低; 验收: 形成后续 T024 的准确修改点
- [ ] T007 Inspect `COAL/springboot/src/main/java/com/example/common/config/WebConfig.java`, `COAL/springboot/src/main/java/com/example/agent/controller/AgentChatController.java`, and `COAL/springboot/src/main/java/com/example/agent/controller/KnowledgeController.java`; 覆盖面试题: Agent/Knowledge 鉴权边界; 具体动作: 明确 `/api/agent/**`、`/api/knowledge/**` 放行现状和最小补强方式; 是否改代码: 否; 风险: 低; 验收: T025/T026 有清晰鉴权方案
- [ ] T008 [P] Validate no new schema migration is required in `COAL/springboot/src/main/resources/sql/agent_init.sql`; 覆盖面试题: 审计/可观测; 具体动作: 确认优先复用 `tool_result_summary`, `session_id`, `tool_params`; 是否改代码: 否; 风险: 低; 验收: Tool trace 不依赖数据库 schema 变更

---

## Phase 3: User Story 1 - P0 面试材料与评测支撑

**Goal**: 先补最小面试材料，让真实业务背景、AI 能力覆盖、演示脚本、评测和排查可被直接展示。

**Independent Test**: Markdown 文件存在且包含指定章节；`demo/eval/agent_qa_cases.json` 可被 PowerShell `ConvertFrom-Json` 解析，且至少 15 条用例覆盖 Tool/RAG/Memory/Security/Approval/DataQuality/Troubleshooting。

- [ ] T009 [P] [US1] Create `docs/agent-interview-matrix.md`; 覆盖面试题: AI 应用开发、RAG、Tool Calling、记忆、安全、评测、排查; 具体动作: 每题写当前代码支撑、面试讲法、可信度、是否补强; 是否改代码: 否; 风险: 低; 验收: 包含不少于 20 个面试题映射且不出现“临时 demo”口径
- [ ] T010 [P] [US1] Create `docs/agent-demo-script.md`; 覆盖面试题: 现场演示能力; 具体动作: 写自然语言问数、RAG 制度问答、数据巡检、审批助理、安全拦截五条链路; 是否改代码: 否; 风险: 低; 验收: 每条链路包含输入问题、预期 Tool、预期界面/日志观察点、失败兜底
- [ ] T011 [P] [US1] Create `docs/agent-troubleshooting-playbook.md`; 覆盖面试题: completion rate 下降、线上波动、偶发错误、幻觉归因; 具体动作: 按模型/RAG/Prompt/Tool/权限/数据库六类写排查路径; 是否改代码: 否; 风险: 低; 验收: 每类包含症状、证据位置、排查命令或界面、修复方向
- [ ] T012 [P] [US1] Create `demo/eval/agent_qa_cases.json`; 覆盖面试题: 如何做评测; 具体动作: 按 `contracts/eval-cases-schema.md` 写至少 15 条黄金问题; 是否改代码: 否; 风险: 低; 验收: 每条包含 `id`, `category`, `question`, `expectedTool`, `expectedBehavior`, `passCriteria`
- [ ] T013 [US1] Validate `demo/eval/agent_qa_cases.json`; 覆盖面试题: 评测集质量; 具体动作: 运行 `Get-Content demo\\eval\\agent_qa_cases.json -Raw | ConvertFrom-Json`; 是否改代码: 否; 风险: 低; 验收: JSON 可解析且 category 覆盖 `tool_calling`, `rag`, `memory`, `security`, `approval`, `data_quality`, `troubleshooting`
- [ ] T014 [US1] Update `docs/agent-interview-matrix.md` with AccessGuard read-only safety demo path; 覆盖面试题: Agent 越权防护; 具体动作: 明确 WRITE Tool 被 `AccessGuard` BLOCKED、审计落 `Tb_Agent_Audit_Log`; 是否改代码: 否; 风险: 低; 验收: 安全题可指向 `AccessGuard.java` 和 `agent_init.sql`

---

## Phase 4: User Story 2 - P0 对话稳定性、Tool Trace、RAG 引用与安全边界

**Goal**: 让核心演示链路稳定：多轮同一 session、Tool 调用可解释、RAG 有来源、安全边界清楚。

**Independent Test**: 启动前后端后，用户第一轮 Agent 请求收到 session，第二轮请求携带同一 session；问数时可观察 selectedTool/toolArgs/toolResultSummary/fallbackReason；RAG 检索结果可见 source/chunkId/score；敏感默认 key 不再出现在配置默认值中。

- [ ] T015 [US2] Modify `COAL/vue/src/utils/agent.js`; 覆盖面试题: 多轮记忆/session; 具体动作: 正确维护 SSE `event:` 类型并新增 `onSession`, `onToolTrace`, `onError` 回调分发; 是否改代码: 是; 风险: 低; 验收: `session` 事件不再被当作普通 message 拼接
- [ ] T016 [US2] Modify `COAL/vue/src/components/agent/AgentPanel.vue`; 覆盖面试题: 多轮记忆/session; 具体动作: 在 callbacks 中接收 server sessionId 并写入 `sessionId.value`; 是否改代码: 是; 风险: 低; 验收: 第二轮 `sendMessage` 入参使用同一个 sessionId
- [ ] T017 [US2] Manually verify SSE session flow in `COAL/vue/src/components/agent/AgentPanel.vue` and `COAL/vue/src/utils/agent.js`; 覆盖面试题: 记忆演示; 具体动作: 按 quickstart 发两轮问题并检查 network request body; 是否改代码: 否; 风险: 低; 验收: 第二轮 `/api/agent/chat` body 包含第一轮返回的 sessionId
- [ ] T018 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/agent/CoalAssistantAgent.java`; 覆盖面试题: Tool Calling trace; 具体动作: 在 `AgentResponse` 或 `ToolCall` 附加 `accessDecision`, `toolResultSummary`, `fallbackReason` 的轻量字段; 是否改代码: 是; 风险: 中低; 验收: 工具成功、未知工具、权限拒绝、查询为空均有摘要或 fallbackReason
- [ ] T019 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/controller/AgentChatController.java`; 覆盖面试题: Tool Calling 可解释性; 具体动作: 向 SSE 输出 `tool_trace` 或增强 `tool_call` data，包含 selectedTool/toolArgs/toolResultSummary/fallbackReason; 是否改代码: 是; 风险: 中低; 验收: 前端或浏览器 EventStream 可看到 Tool trace 字段
- [ ] T020 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/tool/ToolRegistry.java`; 覆盖面试题: ToolRegistry 工具清单; 具体动作: 增加只读 metadata 输出方法，包含 name/description/parameters/accessLevel; 是否改代码: 是; 风险: 低; 验收: 不影响现有 `getToolSchemas()` 给 LLM 使用
- [ ] T021 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/controller/AgentChatController.java`; 覆盖面试题: Tool schema/工具清单; 具体动作: 可选新增只读 `/api/agent/tools` 或等价接口返回 `Result.success(tool metadata)`; 是否改代码: 是; 风险: 中低; 验收: 响应 envelope 使用现有 `Result.success(...)` 且不暴露敏感信息
- [ ] T022 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/rag/Retriever.java`; 覆盖面试题: RAG chunk/source/score; 具体动作: 为 `DocumentChunk` 或 `RetrievalResult` 暴露稳定 `chunkId` 并保留 similarity score; 是否改代码: 是; 风险: 中低; 验收: 检索结果可定位 source + section/chunkId + score
- [ ] T023 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/rag/KnowledgeService.java`; 覆盖面试题: RAG 引用来源; 具体动作: `search` 返回文本中包含 source、chunkId、score，并限制 contentPreview 长度; 是否改代码: 是; 风险: 中低; 验收: `/api/knowledge/search` 和 `search_knowledge` Tool 结果可见来源和分数
- [ ] T024 [US2] Modify `COAL/springboot/src/main/resources/application.yml`; 覆盖面试题: 敏感配置安全; 具体动作: 移除真实默认 API key/密码类敏感默认值，保留环境变量占位; 是否改代码: 是; 风险: 低; 验收: `rg -n "sk-|password: 123456|api-key: .*[^}]$" application.yml` 不再命中真实敏感默认值
- [ ] T025 [US2] Modify `COAL/springboot/src/main/java/com/example/agent/controller/KnowledgeController.java`; 覆盖面试题: Agent/Knowledge 鉴权边界; 具体动作: 对知识库管理接口增加 token/current user 校验或明确拒绝未登录请求; 是否改代码: 是; 风险: 中; 验收: 未登录不能 upload/delete/list 管理知识库，且返回现有 `Result` envelope
- [ ] T026 [US2] Document Agent/Knowledge auth boundary in `docs/agent-troubleshooting-playbook.md`; 覆盖面试题: 安全边界; 具体动作: 说明 WebConfig 放行现状、Agent chat 自校验 token、Knowledge 最小鉴权策略; 是否改代码: 否; 风险: 低; 验收: 面试可解释为什么不是依赖 Prompt 做安全
- [ ] T027 [US2] Run affected build checks for `COAL/springboot` and `COAL/vue`; 覆盖面试题: 代码质量保证; 具体动作: 运行 `mvn -q -DskipTests compile` 和 `npm run build` 或记录环境阻塞原因; 是否改代码: 否; 风险: 低; 验收: 命令通过或在 `quickstart.md`/最终报告记录具体失败原因

---

## Phase 5: User Story 3 - P1 记忆设计、巡检 Tool 与观测字段

**Goal**: 在 P0 稳定基础上，把“记忆、数据巡检、completion/latency/fallback 排查”补成技术亮点，但仍保持只读和低侵入。

**Independent Test**: 文档能解释四层记忆；累计值/一致性 Tool 至少对一个模块返回真实只读检查结果；trace 或日志中能看到耗时/降级/失败摘要字段。

- [ ] T028 [P] [US3] Create or update `docs/agent-memory-design.md`; 覆盖面试题: Agent 记忆层次; 具体动作: 说明会话内记忆、会话摘要记忆、知识库记忆、反馈记忆四层及当前实现边界; 是否改代码: 否; 风险: 低; 验收: 明确不宣称复杂长期记忆系统
- [ ] T029 [US3] Modify `COAL/vue/src/components/agent/FeedbackButtons.vue`; 覆盖面试题: 反馈记忆/评测闭环; 具体动作: 调用 `submitFeedback(sessionId, feedback)` 而不是只切换本地 UI; 是否改代码: 是; 风险: 低; 验收: 点赞/踩会请求 `/api/agent/feedback`
- [ ] T030 [US3] Modify `COAL/vue/src/components/agent/ChatMessage.vue` and `COAL/vue/src/components/agent/AgentPanel.vue`; 覆盖面试题: 反馈记忆; 具体动作: 将真实 sessionId 传给 FeedbackButtons 或消息组件; 是否改代码: 是; 风险: 中低; 验收: feedback 请求 body 包含当前 sessionId 和 feedback 值
- [ ] T031 [US3] Modify `COAL/springboot/src/main/java/com/example/agent/tool/ToolRegistry.java`; 覆盖面试题: 累计值校验 Tool; 具体动作: 将 `check_cumulative` 从占位文案改为只读查询指定 module/unit/year/month 并基于累计字段命名或 `TongJiGenericService` 结果检查异常; 是否改代码: 是; 风险: 中; 验收: 返回具体异常明细或“未发现异常”，不写业务表
- [ ] T032 [US3] Modify `COAL/springboot/src/main/java/com/example/agent/tool/ToolRegistry.java`; 覆盖面试题: 数据一致性巡检 Tool; 具体动作: 为 `check_data_consistency` 实现一个最小真实规则，例如同模块关键字段非负/空值/汇总异常检查; 是否改代码: 是; 风险: 中; 验收: 至少一个模块可返回真实检查结果且不修改数据
- [ ] T033 [US3] Add manual verification steps to `docs/agent-demo-script.md`; 覆盖面试题: 数据巡检演示; 具体动作: 增加累计值校验和一致性巡检的输入问题、预期 Tool、预期异常/无异常结果; 是否改代码: 否; 风险: 低; 验收: 演示脚本能独立验证巡检 Tool
- [ ] T034 [US3] Modify `COAL/springboot/src/main/java/com/example/agent/agent/CoalAssistantAgent.java` and `COAL/springboot/src/main/java/com/example/agent/tool/AccessGuard.java`; 覆盖面试题: completion rate/latency/fallback 统计字段; 具体动作: 记录 Tool 执行耗时、fallbackReason、toolResultSummary 到日志或已有审计字段; 是否改代码: 是; 风险: 中; 验收: 故障排查文档能指向可观察字段
- [ ] T035 [US3] Update `docs/agent-troubleshooting-playbook.md`; 覆盖面试题: completion rate / latency / fallback 排查; 具体动作: 加入从 SSE error、审计表、会话表、provider error、RAG 空结果逐层排查流程; 是否改代码: 否; 风险: 低; 验收: 每个问题有“看哪里”和“如何判断”的步骤

---

## Phase 6: User Story 4 - P1 框架取舍文档

**Goal**: 把 LangChain/LangGraph 的取舍讲清楚，避免面试中被理解成“不了解框架”或“为了面试临时拼接”。

**Independent Test**: 文档能解释 LangChain、LangGraph、当前 Java 轻量实现的区别，并能从旧 Java 系统、内网、合规、低侵入、维护成本说明原因。

- [ ] T036 [P] [US4] Create `docs/agent-langchain-langgraph-choice.md`; 覆盖面试题: LangChain vs LangGraph; 具体动作: 对比 Chain/Agent/Graph、Tool Calling、状态管理、工作流编排适用场景; 是否改代码: 否; 风险: 低; 验收: 文档明确本项目借鉴思想但不直接引入重框架
- [ ] T037 [US4] Add project-specific decision section to `docs/agent-langchain-langgraph-choice.md`; 覆盖面试题: 为什么没用框架; 具体动作: 从 Java/Spring Boot、旧 Service 复用、内网部署、权限审计、维护成本说明取舍; 是否改代码: 否; 风险: 低; 验收: 不出现“因为只是 demo”表述
- [ ] T038 [US4] Link `docs/agent-langchain-langgraph-choice.md` from `docs/agent-interview-matrix.md`; 覆盖面试题: 框架取舍可引用; 具体动作: 在相关面试题条目加入文档路径; 是否改代码: 否; 风险: 低; 验收: 面试矩阵中 LangChain/LangGraph 问题可跳转到取舍文档

---

## Phase 7: User Story 5 - P2 拓展设计与更完整评测

**Goal**: 只作为拓展叙事补充，不落地复杂多 Agent 或 Deep Research 系统。

**Independent Test**: 文档明确“设计方向，不是当前已实现能力”，更完整评测脚本不影响主系统。

- [ ] T039 [P] [US5] Create `docs/agent-deep-research-design.md`; 覆盖面试题: 如何设计 Deep Research 系统; 具体动作: 设计任务拆解、证据池、RAG/搜索、多轮阅读、引用、评测、人工确认; 是否改代码: 否; 风险: 低; 验收: 文档明确这是后续设计，不声称已实现
- [ ] T040 [P] [US5] Create `docs/agent-multi-agent-evolution.md`; 覆盖面试题: 多 Agent 演进方案; 具体动作: 说明何时从单 Agent + 多 Tool 演进到工作流/多 Agent，以及为什么当前不落地; 是否改代码: 否; 风险: 低; 验收: 不包含实现任务，不引入框架依赖
- [ ] T041 [P] [US5] Create `demo/eval/README.md`; 覆盖面试题: 更完整 RAG/Agent 评测脚本; 具体动作: 说明如何人工或脚本化执行 `agent_qa_cases.json`、记录命中 Tool/引用/通过标准; 是否改代码: 否; 风险: 低; 验收: README 能指导后续补脚本但不要求本轮落地
- [ ] T042 [US5] Optionally create `demo/eval/run_agent_eval.md`; 覆盖面试题: 评测流程; 具体动作: 写伪命令/手工评测流程，不接真实 LLM 自动评测; 是否改代码: 否; 风险: 低; 验收: 不依赖外部服务即可理解评测步骤

---

## Phase 8: Polish and Cross-Cutting Verification

**Purpose**: 确认任务产物不越界、不泄露敏感配置、不破坏旧业务。

- [ ] T043 [P] Validate Markdown headings in `docs/agent-interview-matrix.md`, `docs/agent-demo-script.md`, `docs/agent-troubleshooting-playbook.md`, `docs/agent-langchain-langgraph-choice.md`; 覆盖面试题: 文档可读性; 具体动作: 运行 `rg -n "^#|^##|^###" docs\\agent-*.md`; 是否改代码: 否; 风险: 低; 验收: 每个文档标题层级清晰
- [ ] T044 Validate JSON eval set in `demo/eval/agent_qa_cases.json`; 覆盖面试题: 评测集质量; 具体动作: 运行 `Get-Content demo\\eval\\agent_qa_cases.json -Raw | ConvertFrom-Json`; 是否改代码: 否; 风险: 低; 验收: JSON 可解析且至少 15 条
- [ ] T045 Search for forbidden positioning terms in `docs/` and `specs/001-ai-agent-current-state/tasks.md`; 覆盖面试题: 项目真实背景; 具体动作: 检查是否出现“临时 demo”“只是 demo 所以”等错误口径; 是否改代码: 否; 风险: 低; 验收: 错误口径只允许出现在“不要这么说”的上下文
- [ ] T046 Search for sensitive defaults in `COAL/springboot/src/main/resources/application.yml`; 覆盖面试题: 敏感配置安全; 具体动作: 运行针对 `sk-`、真实 api-key、明文密码默认值的搜索; 是否改代码: 否; 风险: 低; 验收: 不存在真实密钥默认值
- [ ] T047 Run back-end compile check for `COAL/springboot`; 覆盖面试题: 代码质量; 具体动作: 运行 `mvn -q -DskipTests compile` 或记录环境阻塞原因; 是否改代码: 否; 风险: 中低; 验收: 编译通过或记录可解释阻塞
- [ ] T048 Run front-end build check for `COAL/vue`; 覆盖面试题: 前端质量; 具体动作: 运行 `npm run build` 或记录环境阻塞原因; 是否改代码: 否; 风险: 中低; 验收: 构建通过或记录可解释阻塞
- [ ] T049 Review git diff for `docs/`, `demo/eval/`, `COAL/springboot/src/main/java/com/example/agent`, `COAL/vue/src/components/agent`, `COAL/vue/src/utils/agent.js`, and `COAL/springboot/src/main/resources/application.yml`; 覆盖面试题: 低侵入工程取舍; 具体动作: 确认没有重写旧 CRUD、统计审批、权限平台或引入重框架; 是否改代码: 否; 风险: 低; 验收: diff 只包含本任务清单范围内变化

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: 无依赖。
- **Phase 2 Foundational**: 依赖 Phase 1，阻塞所有代码任务。
- **Phase 3 P0 文档与评测支撑**: 依赖 Phase 2，可与部分 P0 代码任务并行。
- **Phase 4 P0 核心链路稳定性**: 依赖 Phase 2，建议在 Phase 3 至少完成 T009-T012 后开始。
- **Phase 5 P1 记忆/巡检/观测**: 依赖 Phase 4 的 SSE/session 和 Tool trace。
- **Phase 6 P1 框架取舍文档**: 依赖 Phase 3，可并行。
- **Phase 7 P2 拓展设计**: 依赖 Phase 3，可最后做。
- **Phase 8 Polish**: 依赖所选 Phase 完成。

### Story Completion Order

1. **US1 P0 文档与评测支撑**: T009-T014
2. **US2 P0 核心链路稳定性**: T015-T027
3. **US3 P1 记忆/巡检/观测字段**: T028-T035
4. **US4 P1 框架取舍文档**: T036-T038
5. **US5 P2 拓展设计**: T039-T042

### Parallel Opportunities

- T009, T010, T011, T012 可并行，均只写不同文档/JSON。
- T015/T016 需顺序衔接，但 T018/T019/T020/T022/T023 可在 API 约定稳定后分后端小组并行。
- T028 和 T036 可并行，均为文档任务。
- T039, T040, T041 可并行，均为 P2 文档。
- T043-T046 可并行做静态检查。

## Implementation Strategy

### MVP First: 1 天版本

1. 完成 T001-T008。
2. 完成 T009-T014，先把面试矩阵、演示脚本、排查 playbook、评测集落地。
3. 完成 T015-T017，修复 sessionId 回写。
4. 完成 T024/T026，处理敏感配置和鉴权边界说明。
5. 运行 T043-T046。

### 3 天版本

1. 完成 MVP。
2. 完成 T018-T023，补 Tool trace 和 RAG 引用。
3. 完成 T028-T035，补轻量记忆文档、反馈闭环、累计值/一致性巡检、fallback/latency 字段。
4. 完成 T036-T038，补 LangChain/LangGraph 取舍文档。
5. 运行 T047-T049。

### 1 周版本

1. 完成 3 天版本。
2. 完成 T039-T042，补 Deep Research、多 Agent 演进和更完整评测流程设计。
3. 复查所有 docs/demo/specs，确保不夸大为企业级 Agent 平台，也不贬低为临时 demo。

## Notes

- 不引入复杂多 Agent。
- 不强行接 LangGraph 或 LangChain。
- 不重写旧 CRUD、统计、审批、单位汇总和 Excel 导出。
- 不做企业级权限平台，只补 Agent/Knowledge 边界。
- 不把轻量 RAG 包装成生产级向量平台。
- 不让 Agent 直接执行业务写操作。
- 所有巡检 Tool 必须只读，只返回异常报告或建议。
