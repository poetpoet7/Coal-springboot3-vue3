# Contract: Agent Evaluation Case JSON

Path: `demo/eval/agent_qa_cases.json`

Top-level shape:

```json
[
  {
    "id": "QA-001",
    "category": "tool_calling",
    "question": "查询 2026 年某单位生产经营总值",
    "expectedTool": "query_stat_data",
    "expectedBehavior": "Agent should call query_stat_data and summarize returned business data without modifying records.",
    "passCriteria": [
      "Uses a READ tool",
      "Includes unit/year/module in tool args",
      "Final answer is based on tool result"
    ],
    "notes": "Use available unit names from the local database."
  }
]
```

## Categories

- `tool_calling`
- `rag`
- `memory`
- `security`
- `approval`
- `data_quality`
- `troubleshooting`

## Validation

- JSON must parse.
- Every case must include `id`, `category`, `question`, `expectedTool`, `expectedBehavior`, and non-empty `passCriteria`.
- `expectedTool` may be `null` for pure safety/fallback questions.

