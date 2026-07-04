package com.example.agent.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Embedding 向量化服务。
 * 当前版本使用简化实现（随机向量 + 关键词匹配），
 * 生产环境可替换为 BGE-large-zh 或 OpenAI Embeddings。
 */
@Service
public class EmbeddingService {

    @Value("${coal.ai.rag.embedding.dimension:1024}")
    private int dimension;

    /**
     * 将文本向量化。
     * TODO: 接入 BGE-large-zh 模型（通过 ONNX Runtime 或 HTTP API）
     */
    public float[] embed(String text) {
        // 简化实现：基于字符 n-gram 的词袋模型生成伪向量
        // 相同词汇的文本会有相似的向量，可以实现基本的语义匹配
        // 生产环境应替换为 BGE-large-zh 或 OpenAI Embeddings
        float[] vector = new float[dimension];
        String normalized = text.toLowerCase().replaceAll("[\\p{Punct}\\s]+", "");

        // 使用 n-gram 特征
        for (int n = 1; n <= 3; n++) {
            for (int i = 0; i <= normalized.length() - n; i++) {
                String gram = normalized.substring(i, i + n);
                int idx = Math.abs(gram.hashCode()) % dimension;
                vector[idx] += 1.0f;
            }
        }

        // 归一化
        float norm = 0;
        for (float v : vector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) vector[i] /= norm;
        }
        return vector;
    }

    public int getDimension() {
        return dimension;
    }
}
