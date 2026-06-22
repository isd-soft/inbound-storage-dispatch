package com.isd.wms.service.ai;

import com.isd.wms.service.validation.SecurityFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * AI-powered chatbot service that provides a conversational interface for warehouse supervisors.
 * <p>
 * Uses Spring AI with a {@link ChatClient} configured with a system prompt that defines the
 * assistant's role as a Senior Warehouse Data Analyst. The chatbot is equipped with tool
 * functions ({@link InventoryAiTools}, {@link OrderAiTools}, {@link ReplenishmentAiTools},
 * {@link WarehouseAiTools}) to perform real warehouse operations and queries.
 * </p>
 * <p>
 * Conversation memory is maintained per user (using the current username as the conversation ID),
 * allowing contextual follow-up questions. The service is stateless with respect to the chat
 * session; memory is managed by the advisor.
 * </p>
 *
 * @see ChatClient
 * @see ChatModel
 * @see InventoryAiTools
 * @see OrderAiTools
 * @see ReplenishmentAiTools
 * @see WarehouseAiTools
 * @see SecurityFacade
 */
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
                        You are a Senior Warehouse Data Analyst and the core AI Assistant for the 'Inbound Storage Dispatch' (ISD) WMS.
                        Your goal is to provide perfectly accurate, step-by-step analyzed information to the warehouse supervisor.

                        ABOUT THE APPLICATION & CAPABILITIES:
                        - ISD WMS optimizes warehouse operations: Inventory Management, Order Fulfillment (Picking), Replenishment, and Analytics.
                        - You can search products, check stock, receive inbound stock, adjust inventory, manage orders/replenishments, and auto-distribute workload.
                        - If a user asks "What is this application?", "What can you do?", or "Who are you?", proudly present this info in a friendly, readable format using bullet points.

                        CRITICAL WMS DOMAIN KNOWLEDGE:
                        - 'REPLENISHMENT' (REPL) zones are bulk STORAGE locations.
                        - 'PICK' zones are where operators assemble customer orders.
                        - 'DISPATCH' zones are where completed orders go. (Stock cannot be manually added here).

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

    /**
     * Processes a user's natural language question and returns an AI-generated response.
     * <p>
     * The question is sent to the chat model along with the conversation history for the current user.
     * If an error occurs (e.g., model unavailable), a friendly error message is returned instead.
     * </p>
     *
     * @param userMessage the user's question or command
     * @return the AI response as a plain text string (may include Markdown tables)
     */
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
