package com.example.agent.rag;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档读取器。
 * 支持 TXT、MD 格式，后续可扩展 PDF/DOCX（通过 Tika）。
 */
@Component
public class DocumentReader {

    /**
     * 读取文档文件并返回纯文本内容。
     */
    public String read(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(filePath);
        }

        // 其他格式暂用 Tika 兜底
        try {
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            return tika.parseToString(filePath.toFile());
        } catch (Exception e) {
            throw new IOException("无法解析文档: " + filePath, e);
        }
    }

    /**
     * 读取纯文本内容。
     */
    public String readText(String text) {
        return text;
    }
}
