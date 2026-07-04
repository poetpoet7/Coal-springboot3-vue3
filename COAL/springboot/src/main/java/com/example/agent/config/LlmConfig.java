package com.example.agent.config;

import com.example.agent.llm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM Provider 工厂配置。
 * 支持 Ollama、OpenAI、DeepSeek 及所有 OpenAI 兼容的 API（阿里百炼、智谱、硅基流动等）。
 *
 * 切换方式：改 application.yml 中 coal.ai.llm.provider 即可，无需改代码。
 */
@Configuration
public class LlmConfig {

    @Value("${coal.ai.llm.provider:ollama}")
    private String provider;

    // ---- Ollama ----
    @Value("${coal.ai.llm.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    @Value("${coal.ai.llm.ollama.model:qwen2.5:7b}")
    private String ollamaModel;

    // ---- OpenAI ----
    @Value("${coal.ai.llm.openai.api-key:}")
    private String openaiApiKey;
    @Value("${coal.ai.llm.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;
    @Value("${coal.ai.llm.openai.model:gpt-4o-mini}")
    private String openaiModel;

    // ---- DeepSeek (OpenAI 兼容) ----
    @Value("${coal.ai.llm.deepseek.api-key:}")
    private String deepseekApiKey;
    @Value("${coal.ai.llm.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;
    @Value("${coal.ai.llm.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    // ---- 自定义 OpenAI 兼容 API ----
    @Value("${coal.ai.llm.custom.api-key:}")
    private String customApiKey;
    @Value("${coal.ai.llm.custom.base-url:}")
    private String customBaseUrl;
    @Value("${coal.ai.llm.custom.model:}")
    private String customModel;

    @Bean
    public LlmProvider llmProvider() {
        return switch (provider.toLowerCase()) {
            case "openai"  -> new OpenAiProvider(openaiBaseUrl, openaiApiKey, openaiModel);
            case "deepseek" -> new OpenAiProvider(deepseekBaseUrl, deepseekApiKey, deepseekModel);
            case "custom"  -> new OpenAiProvider(customBaseUrl, customApiKey, customModel);
            default        -> new OllamaProvider(ollamaBaseUrl, ollamaModel);
        };
    }
}
