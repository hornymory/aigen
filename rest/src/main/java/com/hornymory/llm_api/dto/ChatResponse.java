package com.hornymory.llm_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String modelId;
    private String response;
    private long timeMs;
    private boolean success;
    private String message;
}
