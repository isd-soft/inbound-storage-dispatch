package com.isd.wms.controller;

import com.isd.wms.dto.ai.AiChatRequest;
import com.isd.wms.dto.ai.AiChatResponse;
import com.isd.wms.service.ai.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatbotService chatbotService;


    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        if (request.message() == null || request.message().trim().isEmpty()) {
            return new AiChatResponse("Please provide a valid message.");
        }

        String reply = chatbotService.askQuestion(request.message());
        return new AiChatResponse(reply);
    }
}
