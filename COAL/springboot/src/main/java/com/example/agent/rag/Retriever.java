package com.example.agent.rag;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义检索器。
 * 当前版本使用简化实现（内存存储 + 余弦相似度），
 * 生产环境可替换为 ChromaDB。
 */
@Service
public class Retriever {

    @Value("${coal.ai.rag.retrieval.top-k:5}")
    private int topK;

    @Value("${coal.ai.rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Resource
    private EmbeddingService embeddingService;

    // 内存中的向量索引（生产环境应替换为 Chroma）
    private final List<DocumentChunk> index = Collections.synchronizedList(new ArrayList<>());
    private int idCounter = 0;

    /**
     * 将文档块加入索引。
     */
    public void index(String docId, String title, String section, String content) {
        float[] vector = embeddingService.embed(content);
        int id = ++idCounter;
        index.add(new DocumentChunk(id, docId, title, section, content, vector));
    }

    /**
     * 语义检索，返回最相关的文档块。
     */
    public List<RetrievalResult> search(String query) {
        if (index.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embeddingService.embed(query);

        return index.stream()
            .map(chunk -> {
                double similarity = cosineSimilarity(queryVector, chunk.vector);
                return new RetrievalResult(chunk, similarity);
            })
            .filter(r -> r.similarity >= similarityThreshold)
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * 清空索引。
     */
    public void clear() {
        index.clear();
    }

    /**
     * 获取索引文档数量。
     */
    public int size() {
        return index.size();
    }

    // ---- 内部类 ----

    public static class DocumentChunk {
        public final int chunkId;       // 稳定索引ID
        public final String docId;
        public final String title;
        public final String section;
        public final String content;
        public final float[] vector;

        public DocumentChunk(int chunkId, String docId, String title, String section, String content, float[] vector) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.title = title;
            this.section = section;
            this.content = content;
            this.vector = vector;
        }
    }

    public static class RetrievalResult {
        public final DocumentChunk chunk;
        public final double similarity;   // 余弦相似度 score

        public RetrievalResult(DocumentChunk chunk, double similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }
    }

    // ---- 辅助方法 ----

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
