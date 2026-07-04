package com.example.agent.controller;

import com.example.agent.entity.KnowledgeDocument;
import com.example.agent.rag.KnowledgeService;
import com.example.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @GetMapping("/list")
    public Result list() {
        List<KnowledgeDocument> docs = knowledgeService.listDocuments();
        return Result.success(docs);
    }

    @PostMapping("/upload")
    public Result upload(@RequestBody KnowledgeUploadRequest request) {
        KnowledgeDocument doc = knowledgeService.uploadDocument(
            request.getTitle(), request.getCategory(), request.getContent());
        return Result.success(doc);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result stats() {
        return Result.success(java.util.Map.of(
            "documentCount", knowledgeService.listDocuments().size(),
            "chunkCount", knowledgeService.getChunkCount()
        ));
    }

    @GetMapping("/search")
    public Result search(@RequestParam String query) {
        String result = knowledgeService.search(query);
        return Result.success(result);
    }

    // 请求体定义
    public static class KnowledgeUploadRequest {
        private String title;
        private String category;
        private String content;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
