# AI Agent 故障排查 Playbook

> 适用范围：真实能源企业内网统计审批系统上的低侵入 AI 智能增强模块。排查目标是快速判断问题来自模型、RAG、Prompt、Tool、权限、数据库/旧业务 Service，避免把 AI 问题和旧业务问题混在一起。

## 1. 快速分层

```text
用户问题
  -> 前端 SSE/session
  -> AgentChatController
  -> CoalAssistantAgent / Prompt
  -> LLM Provider
  -> ToolRegistry
  -> AccessGuard
  -> KnowledgeService or legacy business Service
  -> Conversation/Audit tables
```

优先判断：

1. 请求是否发出。
2. SSE 是否返回 `error`。
3. 是否选择了正确 Tool。
4. Tool 参数是否正确。
5. AccessGuard 是否拦截。
6. Tool 结果是否为空或异常。
7. 最终回答是否忠实于 Tool/RAG 结果。

## 2. completion rate 下降怎么排查

| 层级 | 典型症状 | 看哪里 | 判断方式 |
| --- | --- | --- | --- |
| 前端/SSE | 请求中断、一直 loading | DevTools Network `/api/agent/chat` | 是否收到 `done` 或 `error`；是否 Abort |
| Token/session | 返回未登录或 session 丢失 | request body、`agent.js`, `AgentPanel.vue` | token 是否为空；第二轮是否携带 sessionId |
| LLM Provider | 大量 provider error | `OpenAiProvider`, `OllamaProvider` 返回内容 | 是否 API key、baseUrl、model 配置错误 |
| Tool Calling | 没有调用预期 Tool | `ToolRegistry`, SSE tool_call | LLM 是否返回 tool_calls；tool name 是否存在 |
| 权限 | 工具被拒绝 | `AccessGuard`, `Tb_Agent_Audit_Log` | `access_decision` 是否 BLOCKED |
| RAG | 制度问答无结果 | `KnowledgeService.search`, `/api/knowledge/stats` | chunkCount 是否为 0；score 是否低于阈值 |
| 旧业务 Service | 问数无结果或报错 | `TongJiGenericService.getReportData` | moduleKey/unitId/year/month 是否正确，DB 是否有数据 |

## 3. 幻觉、RAG、Prompt、Tool 问题如何归因

| 问题表现 | 更可能原因 | 证据 | 处理方式 |
| --- | --- | --- | --- |
| 回答给出不存在的数据 | 模型幻觉或 Prompt 约束不足 | Tool 未返回数据但回答有数字 | 强化“只基于工具结果回答”；评测集中加入空结果问题 |
| 回答引用了无关制度 | RAG 召回不准 | 检索片段 source/score 不相关 | 调整阈值、topK、切片；补充无关召回案例 |
| Tool 参数错，如年份/月错 | Tool Calling 参数抽取问题 | SSE tool_call 或审计表 `tool_params` | 在 prompt 和 eval case 中补参数格式样例 |
| Tool 结果正确但总结错 | LLM 总结问题 | Tool 原始结果和最终 message 不一致 | 要求回答标注数据来源，降低自由发挥 |
| 权限内数据查不到 | 权限或单位映射问题 | `SecurityContext`, `PermissionService`, `unitId` | 检查用户单位、角色、accessibleDanweiIds |
| RAG 检索为空 | 知识库为空或阈值过高 | `/api/knowledge/stats`, `Retriever.size()` | 上传知识文档，检查 chunkCount 和 query |

## 4. Tool Calling 排查路径

1. 查看用户问题是否属于业务域。
2. 查看 LLM 返回是否包含 tool_calls。
3. 查看 tool name 是否在 `ToolRegistry` 中。
4. 查看参数是否包含必填项，例如 `moduleKey`、`unitId`、`year`。
5. 查看 `AccessGuard` 决策。
6. 查看 Tool 执行结果。
7. 查看最终回答是否基于 Tool 结果。

当前可观察位置：

- 前端工具调用块：`ToolCallBlock.vue`
- 会话记录：`Tb_Agent_Conversation`
- 审计记录：`Tb_Agent_Audit_Log`
- 后端 Tool 注册：`ToolRegistry.java`

## 5. RAG 排查路径

1. 调用 `/api/knowledge/stats` 看 `documentCount` 和 `chunkCount`。
2. 调用 `/api/knowledge/search?query=...` 看是否能检索到片段。
3. 检查返回片段是否和问题相关。
4. 检查相似度阈值 `coal.ai.rag.retrieval.similarity-threshold`。
5. 检查切片参数 `coal.ai.rag.chunk.size` 和 `overlap`。
6. 如果多个来源冲突，回答应展示来源并说明存在冲突，不应自行合并成确定结论。

当前边界：

- 轻量 embedding 是试点实现，适合展示链路，不包装成生产级向量平台。
- 本轮不引入 Chroma/BGE；后续可替换。

## 6. Agent/Knowledge 鉴权边界

当前仓库中 `WebConfig` 对 `/api/agent/**` 和 `/api/knowledge/**` 做了全局 JWT interceptor 放行。

当前边界说明：

- `POST /api/agent/chat` 自身从 request body 的 `token` 构建 `SecurityContext`，这是 Agent chat 的主要身份边界。
- `AccessGuard` 是 Tool 执行边界，READ/SUGGEST/WRITE 分级控制工具调用。
- `SecurityContext` 和 `PermissionService` 控制单位数据访问范围。
- Knowledge 管理接口当前代码层面的显式鉴权较弱，本轮不执行 T025，不改 `KnowledgeController`，但需要在面试中诚实说明这是后续应收紧的安全点。

推荐表述：

> Agent 安全不是依赖 Prompt，而是依赖后端 token 上下文、单位权限、Tool 访问级别和审计。当前 chat 接口已有自校验，Knowledge 管理接口需要后续进一步收紧，本轮先明确边界和风险，不伪装成已经完成企业级权限体系。

## 7. 敏感配置排查

检查命令示例：

```powershell
rg -n "sk-|api-key:.*[A-Za-z0-9]|password: 123456" COAL\springboot\src\main\resources\application.yml
```

验收目标：

- 不出现真实 API key 默认值。
- 数据库密码默认值使用环境变量占位。
- 本地部署需要在环境变量或本地私有配置中设置真实值。

## 8. 偶发错误排查

| 偶发现象 | 优先检查 |
| --- | --- |
| 第一次能问，第二次失忆 | 前端是否回写 `sessionId` |
| 偶尔回答空白 | LLM Provider 是否超时或返回空 content |
| 偶尔工具未调用 | 问题表达是否触发 tool_choice；Prompt 是否要求业务问题优先 Tool |
| 知识库删除后仍能搜到 | 当前 delete 只软删数据库，不清理内存 Retriever，这是现状边界 |
| 普通用户查到不该查的数据 | `SecurityContext.isForbidden`, `PermissionService.getAccessibleDanweiIds` |

## 9. 本轮不做的排查增强

- 不新增 traceId 字段。
- 不改审计表 schema。
- 不实现 T018-T023 的 Tool trace/RAG 代码增强。
- 不实现 T025 的 KnowledgeController 鉴权代码。
- 不做 P1/P2 的复杂观测面板或自动评测脚本。

