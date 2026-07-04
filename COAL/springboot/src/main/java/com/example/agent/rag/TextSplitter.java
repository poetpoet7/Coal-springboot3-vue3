package com.example.agent.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片器。
 * 按语义边界（段落/标题）将长文本切分为适合向量化的片段。
 */
@Component
public class TextSplitter {

    @Value("${coal.ai.rag.chunk.size:512}")
    private int chunkSize;

    @Value("${coal.ai.rag.chunk.overlap:50}")
    private int overlap;

    /**
     * 按段落分割文本，保持语义完整性。
     * 优先在 ## 标题边界切分，其次在段落边界。
     */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();

        // 先按 ## 标题分割
        String[] sections = text.split("(?=## )");
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            // 如果段落本身太长，再按段落切
            if (countTokens(section) > chunkSize * 1.5) {
                chunks.addAll(splitByParagraph(section));
            } else {
                chunks.add(section.trim());
            }
        }

        // 合并过短的片段
        return mergeShortChunks(chunks);
    }

    private List<String> splitByParagraph(String text) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (countTokens(current.toString()) + countTokens(para) > chunkSize) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    // 保留重叠部分
                    String[] words = current.toString().split("\\s+");
                    int start = Math.max(0, words.length - overlap / 2);
                    current = new StringBuilder();
                    for (int i = start; i < words.length; i++) {
                        current.append(words[i]).append(" ");
                    }
                }
            }
            current.append(para).append("\n\n");
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private List<String> mergeShortChunks(List<String> chunks) {
        List<String> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String chunk : chunks) {
            if (countTokens(buffer.toString()) + countTokens(chunk) > chunkSize) {
                if (buffer.length() > 0) {
                    merged.add(buffer.toString().trim());
                    buffer = new StringBuilder();
                }
            }
            buffer.append(chunk).append("\n\n");
        }
        if (buffer.length() > 0) {
            merged.add(buffer.toString().trim());
        }
        return merged;
    }

    /**
     * 估算 token 数：中文约 1.5 字符/token，英文约 4 字符/token
     */
    private int countTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    public int getChunkSize() { return chunkSize; }
    public int getOverlap() { return overlap; }
}
