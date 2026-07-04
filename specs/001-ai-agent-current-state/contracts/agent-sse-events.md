# Contract: Agent SSE Events

Endpoint: `POST /api/agent/chat`

Request body:

```json
{
  "message": "查询某单位 2026 年生产经营总值",
  "sessionId": "optional-session-id",
  "token": "jwt-token"
}
```

## Existing Events

| Event | Data | Meaning |
| --- | --- | --- |
| `thought` | string | Agent 开始分析或中间提示 |
| `tool_call` | object | 当前工具名和参数 |
| `message` | string | 最终回答 |
| `session` | string | 后端会话 ID |
| `done` | string | 完成标记 |
| `error` | string | 错误说明 |

## Planned Lightweight Enhancement

Add or enrich an event for tool trace:

```text
event: tool_trace
data: {
  "selectedTool": "query_stat_data",
  "toolArgs": {
    "moduleKey": "jingyingzongzhi",
    "unitId": 1,
    "year": 2026
  },
  "accessLevel": "READ",
  "accessDecision": "ALLOWED",
  "toolResultSummary": "返回 12 个月统计汇总，首行是单位汇总数据",
  "fallbackReason": null
}
```

## Front-End Requirements

- `agent.js` must track the latest `event:` line instead of inferring type only from `data:`.
- `AgentPanel.vue` must update local `sessionId` when receiving `session`.
- Tool trace should be displayed or at least logged for interview/demo verification.

