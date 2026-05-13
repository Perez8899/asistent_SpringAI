package com.ejemploSpringAi.controllers;

import com.openai.models.vectorstores.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/docs")
public class DocsController {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/java21.ai.st")
    private Resource stPromptTemplate;

    public DocsController(ChatClient.Builder chatBuilder, VectorStore vectorStore) {
        this.chatClient = chatBuilder
                .defaultAdvisors(new PromptChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
        this.vectorStore = vectorStore;
    }


    @GetMapping("/chat")
    private String generateResponse(@RequestParam String query) {
        PromptTemplate promptTemplate = new PromptTemplate(stPromptTemplate);
        var promptParameters = new HashMap<String, Object>();
        promptParameters.put("input", query);
        promptParameters.put("documents", String.join("\n", this.findSimilarDocuments(query)));

        var prompt = promptTemplate.create(promptParameters);
        var response = this.chatClient.prompt(prompt).call().chatResponse();
        return response.getResult().getOutput().getContent();
    }

    private List<String> findSimilarDocuments(String query) {
        List<Document> similarDocuments = vectorStore
                .similaritySearch(SearchRequest.query(query).withTopK(3));

        return similarDocuments.stream().map(Document::getContent).toList();
    }
}