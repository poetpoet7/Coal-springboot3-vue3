# Quickstart: AI Agent 最小改动增强验证

This guide validates the planned 1-3 day enhancement after implementation. It does not require converting the project into an enterprise Agent platform.

## Prerequisites

- Back end dependencies installed for `COAL/springboot`.
- Front end dependencies installed for `COAL/vue`.
- SQL Server test/dev database available if running full Agent flows.
- LLM provider configured by environment variable, not hard-coded secrets.

## Documentation Validation

Check planned documents exist:

```powershell
Test-Path docs\agent-interview-matrix.md
Test-Path docs\agent-demo-script.md
Test-Path docs\agent-troubleshooting-playbook.md
Test-Path docs\agent-langchain-langgraph-choice.md
Test-Path demo\eval\agent_qa_cases.json
```

Validate eval JSON:

```powershell
Get-Content demo\eval\agent_qa_cases.json -Raw | ConvertFrom-Json
```

Expected: JSON parses and contains at least 15 cases.

## Front-End Session Validation

1. Start the app.
2. Log in with a valid user.
3. Open Agent panel.
4. Ask a first business question.
5. Confirm the response stream includes a `session` event.
6. Ask a second follow-up question.

Expected:

- `AgentPanel.vue` keeps the same `sessionId`.
- The second `/api/agent/chat` request body includes the previous `sessionId`.

## Tool Trace Validation

Ask a statistics question requiring `query_stat_data`.

Expected:

- A trace/log/SSE event shows `selectedTool`.
- Tool args include `moduleKey`, `unitId`, and `year`.
- Access decision is READ/ALLOWED.
- Final answer is based on the tool result.

## RAG Citation Validation

Upload or load a small markdown/text knowledge document, then ask a制度/规则 question.

Expected:

- Retrieval output includes `source`, `chunkId`, and `score`.
- Final answer includes a visible source reference or source section.
- No write operation is triggered.

## Security Validation

Ask or simulate a WRITE tool call such as feedback through tool calling.

Expected:

- `AccessGuard` records BLOCKED for WRITE.
- Agent returns a refusal/fallback reason.
- No business table is modified.

## Build/Check Commands

When code changes are implemented, run the relevant checks:

```powershell
cd COAL\springboot
mvn -q -DskipTests compile
```

```powershell
cd COAL\vue
npm run build
```

If full builds are blocked by local environment, document the failure reason and run targeted manual validation above.

