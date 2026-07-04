# Contract: RAG Retrieval Result

Current `KnowledgeService.search(...)` returns formatted text. The lightweight enhancement should preserve readable output while including structured citation fields in the returned text or an adjacent metadata object.

Recommended citation shape:

```json
{
  "source": "煤炭统计填报规范",
  "chunkId": "煤炭统计填报规范#§2",
  "score": 0.83,
  "contentPreview": "制度片段摘要..."
}
```

## Requirements

- `source` must identify the document title.
- `chunkId` must identify a stable chunk within the current lightweight index.
- `score` must expose similarity enough for debugging; it may be rounded for UI.
- Retrieval should remain bounded by `top-k` and `similarity-threshold`.
- No Chroma/BGE integration is required for this iteration.

## RAG Risk Notes

| Risk | Current/Planned Mitigation |
| --- | --- |
| Context overflow | Keep topK bounded; chunk text before injecting into prompt |
| Irrelevant text | Similarity threshold; eval cases for no-hit/low-score questions |
| Conflicting information | Show sources and state conflict instead of silently merging |
| Long document breakage | Split by headings/paragraphs and keep overlap |

