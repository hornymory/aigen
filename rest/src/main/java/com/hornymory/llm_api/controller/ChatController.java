package com.hornymory.llm_api.controller;

import com.hornymory.llm_api.dto.ChatRequest;
import com.hornymory.llm_api.dto.ChatResponse;
import com.hornymory.llm_api.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private AiService aiService;

    @PostMapping()
    public ChatResponse send(@RequestBody ChatRequest request){
        return aiService.askModel(request);
    }
}
