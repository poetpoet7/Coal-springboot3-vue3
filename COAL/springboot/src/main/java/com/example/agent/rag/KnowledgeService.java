package com.example.agent.rag;

import com.example.agent.entity.KnowledgeDocument;
import com.example.agent.mapper.KnowledgeDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理服务。
 * 负责文档的上传、解析、切片、向量化和检索。
 */
@Service
public class KnowledgeService {

    @Resource private DocumentReader documentReader;
    @Resource private TextSplitter textSplitter;
    @Resource private Retriever retriever;
    @Resource private KnowledgeDocumentMapper knowledgeDocumentMapper;

    private static final String DOCUMENTS_DIR = "documents/";

    /**
     * 启动时自动加载 documents/ 目录下的所有知识文档。
     */
    @PostConstruct
    public void loadDocuments() {
        try {
            Path docDir = Paths.get(DOCUMENTS_DIR);
            if (!Files.exists(docDir)) {
                Files.createDirectories(docDir);
                System.out.println("[RAG] 知识库目录已创建: " + docDir.toAbsolutePath());
                return;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(docDir, "*.{md,txt}")) {
                for (Path file : stream) {
                    try {
                        String content = documentReader.read(file);
                        String title = file.getFileName().toString().replace(".md", "").replace(".txt", "");
                        List<String> chunks = textSplitter.split(content);

                        for (int i = 0; i < chunks.size(); i++) {
                            retriever.index(title, title, "§" + (i + 1), chunks.get(i));
                        }

                        // 同时写入数据库
                        KnowledgeDocument doc = new KnowledgeDocument();
                        doc.setTitle(title);
                        doc.setCategory("制度文件");
                        doc.setFileType(file.toString().endsWith(".md") ? "md" : "txt");
                        doc.setChunkCount(chunks.size());
                        doc.setUploadedBy(1); // 系统自动导入
                        doc.setUploadedAt(LocalDateTime.now());
                        doc.setIsActive(true);
                        knowledgeDocumentMapper.insert(doc);

                        System.out.println("[RAG] 已加载文档: " + title + " (" + chunks.size() + " 个切片)");
                    } catch (Exception e) {
                        System.err.println("[RAG] 加载文档失败: " + file + " - " + e.getMessage());
                    }
                }
            }
            System.out.println("[RAG] 知识库加载完成，共 " + retriever.size() + " 个切片");
        } catch (IOException e) {
            System.err.println("[RAG] 知识库初始化失败: " + e.getMessage());
        }
    }

    /**
     * 上传新文档。
     */
    public KnowledgeDocument uploadDocument(String title, String category, String content) {
        List<String> chunks = textSplitter.split(content);
        for (int i = 0; i < chunks.size(); i++) {
            retriever.index(title, title, "§" + (i + 1), chunks.get(i));
        }

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setCategory(category != null ? category : "制度文件");
        doc.setFileType("txt");
        doc.setChunkCount(chunks.size());
        doc.setUploadedBy(1);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setIsActive(true);
        knowledgeDocumentMapper.insert(doc);
        return doc;
    }

    /**
     * RAG 检索：根据用户问题返回相关知识片段。
     */
    public String search(String query) {
        List<Retriever.RetrievalResult> results = retriever.search(query);

        if (results.isEmpty()) {
            return "未找到相关知识。当前知识库共 " + retriever.size() + " 个文档片段，建议上传相关制度文档。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(results.size()).append(" 个相关知识片段：\n\n");
        for (int i = 0; i < results.size(); i++) {
            Retriever.RetrievalResult r = results.get(i);
            sb.append("【来源：").append(r.chunk.title).append(" ").append(r.chunk.section).append("】")
              .append("（相关度：").append(String.format("%.0f%%", r.similarity * 100)).append("）\n")
              .append(r.chunk.content).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 列出所有知识库文档。
     */
    public List<KnowledgeDocument> listDocuments() {
        return knowledgeDocumentMapper.selectList(
            new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getIsActive, true)
                .orderByDesc(KnowledgeDocument::getUploadedAt)
        );
    }

    /**
     * 删除文档。
     */
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(id);
        if (doc != null) {
            doc.setIsActive(false);
            knowledgeDocumentMapper.updateById(doc);
        }
    }

    public int getChunkCount() {
        return retriever.size();
    }
}
