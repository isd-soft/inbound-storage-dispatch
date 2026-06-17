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
    private final WmsAiTools wmsAiTools;
    private final SecurityFacade securityFacade;

    public ChatbotService(ChatModel chatModel, WmsAiTools wmsAiTools, SecurityFacade securityFacade) {
        this.wmsAiTools = wmsAiTools;
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
                    - 'PICK' zones are where operators assemble customer orders. The PICK zone is replenished FROM the REPLENISHMENT zone.
                    - 'DISPATCH' zones are where completed customer orders are dropped off to be shipped.
                    - If a product is 'Reserved' in a REPL location, it means a Replenishment Task is moving it OUT of REPL and INTO a PICK location. (REPL is the SOURCE, PICK is the DESTINATION).
                    - If a product is 'Reserved' in a PICK location, it means a Picking Order Task is moving it OUT of PICK and INTO a DISPATCH location.

                    FORMATTING RULES:
                    1. Always format your responses using Markdown.
                    2. Use Markdown tables when returning lists of data.
                    3. ALWAYS format product names as clickable links using their barcode: [Product Name](/supervisor/products?barcode=THE_BARCODE)
                    4. NEVER display internal database IDs (like Task ID, Replenishment ID, or Order ID) in your responses or tables to the user, unless explicitly asked. The supervisor only cares about Product Names, Quantities, and Locations. Keep the IDs in your memory to use with tools, but hide them in the UI.

                    REASONING RULE (Chain of Thought):
                    Before answering complex questions about stock movements, reservations, or destinations, briefly think step-by-step about the warehouse logic, then provide the final clear answer.
                """)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    }

    public String askQuestion(String userMessage) {
        try {
            String conversationId = securityFacade.getCurrentUsername();

            return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(wmsAiTools)
                .call()
                .content();

        } catch (Exception e) {
            log.error("AI interaction failed: {}", e.getMessage(), e);
            return "Sorry, I am currently unable to process your request. Please try again later.";
        }
    }
}
