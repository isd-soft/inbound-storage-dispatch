package com.isd.wms.service.ai;

import com.isd.wms.service.validation.SecurityFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final SecurityFacade securityFacade;

    public ChatbotService(
        ChatModel chatModel,
        SecurityFacade securityFacade,
        InventoryAiTools inventoryAiTools,
        OrderAiTools orderAiTools,
        ReplenishmentAiTools replenishmentAiTools,
        WarehouseAiTools warehouseAiTools
    ) {
        this.securityFacade = securityFacade;

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem("""
                    You are a Senior Warehouse Data Analyst (WMS).
                    Your goal is to provide perfectly accurate, step-by-step analyzed information to the warehouse supervisor.

                    CRITICAL WMS DOMAIN KNOWLEDGE:
                    - 'REPLENISHMENT' (REPL) zones are bulk STORAGE locations.
                    - 'PICK' zones are where operators assemble customer orders.
                    - 'DISPATCH' zones are where completed orders go.

                    FORMATTING RULES:
                    1. The tools you use will return pre-formatted Markdown tables and relative links (e.g. /supervisor/products...).
                    2. YOU MUST OUTPUT THESE TABLES AND LINKS EXACTLY AS PROVIDED. DO NOT prepend "https://example.com" or any other domain to the links. Keep them strictly as relative paths starting with "/".
                    3. NEVER display internal database IDs (like Task ID or Order ID) in your text responses, unless explicitly asked. (Ref IDs inside tables are allowed).
                    4. If a tool returns an 'Error' or 'Failed' message, you MUST inform the user exactly what went wrong. Never pretend an action was successful if it failed.
                    5. NEVER guess missing parameters. If the user says "assign an order" but doesn't specify WHICH order or WHICH operator, YOU MUST ASK them to clarify.

                    REASONING RULE (Chain of Thought):
                    Before answering complex questions, briefly think step-by-step about the warehouse logic, then provide the final clear answer.
                    """)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .defaultTools(inventoryAiTools, orderAiTools, replenishmentAiTools, warehouseAiTools)
            .build();
    }

    public String askQuestion(String userMessage) {
        try {
            return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, securityFacade.getCurrentUsername()))
                .call()
                .content();
        } catch (Exception e) {
            log.error("AI interaction failed: {}", e.getMessage(), e);
            return "Sorry, I am currently unable to process your request. Please try again later.";
        }
    }
}
