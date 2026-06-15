package com.isd.wms.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final WmsAiTools wmsAiTools;

    public ChatbotService(ChatModel chatModel, WmsAiTools wmsAiTools) {
        this.wmsAiTools = wmsAiTools;
        this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem("""
                    You are a smart AI assistant for a Warehouse Management System (WMS).
                    Your goal is to help supervisors manage inbound, storage, and dispatch operations.
                    Be concise, professional, and to the point.
                    Always use the available tools to provide accurate data from the warehouse database.
                    Do not guess inventory numbers; use the tools.
                """)
            .build();
    }

    public String askQuestion(String userMessage) {
        try {
            return chatClient.prompt()
                .user(userMessage)
                .tools(wmsAiTools)
                .call()
                .content();
        } catch (Exception e) {
            log.error("AI interaction failed: {}", e.getMessage(), e);
            return "Sorry, I am currently unable to process your request. Please try again later.";
        }
    }
}
