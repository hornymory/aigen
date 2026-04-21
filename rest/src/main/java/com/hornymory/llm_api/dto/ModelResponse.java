package com.hornymory.llm_api.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModelResponse {
    private String id;
    private String path;
    private boolean loaded;
    private String statusMessage;
}
