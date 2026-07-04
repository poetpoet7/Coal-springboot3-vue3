# Contract: Tool Registry Metadata

The Agent tool registry should expose or document the following shape for every tool.

```json
{
  "name": "query_stat_data",
  "description": "Query a generic coal statistics module by unit/year/month.",
  "parameters": {
    "type": "object",
    "properties": {
      "moduleKey": { "type": "string" },
      "unitId": { "type": "integer" },
      "year": { "type": "integer" },
      "month": { "type": "integer" }
    },
    "required": ["moduleKey", "unitId", "year"]
  },
  "accessLevel": "READ"
}
```

## Required Fields

| Field | Required | Notes |
| --- | --- | --- |
| `name` | yes | Stable Tool identifier |
| `description` | yes | Natural language purpose |
| `parameters` | yes | JSON-schema-like parameter definition |
| `accessLevel` | yes | READ/SUGGEST/WRITE |

## Compatibility

- Existing LLM provider schema generation must continue to work.
- Any UI or docs endpoint should use `Result.success(...)`.
- WRITE tools must remain blocked by `AccessGuard` unless a future feature explicitly adds human confirmation.

