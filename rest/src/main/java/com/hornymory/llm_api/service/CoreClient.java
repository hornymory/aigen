package com.hornymory.llm_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoreClient {
    private final RestClient coreRestClient;

    public JsonNode models() {
        return coreRestClient.get().uri("/models").retrieve().body(JsonNode.class);
    }

    public JsonNode load(String model, Integer ctxSize, Integer threads, String nGpuLayers) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        if (ctxSize != null) body.put("ctxSize", ctxSize);
        if (threads != null) body.put("threads", threads);
        if (nGpuLayers != null) body.put("nGpuLayers", nGpuLayers);

        return coreRestClient.post().uri("/load").body(body).retrieve().body(JsonNode.class);
    }

    public JsonNode generate(String prompt, Integer maxTokens, Double temperature, Double topP, String model, String system) {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        if (maxTokens != null) body.put("maxTokens", maxTokens);
        if (temperature != null) body.put("temperature", temperature);
        if (topP != null) body.put("topP", topP);
        if (model != null) body.put("model", model);
        if (system != null) body.put("system", system);

        return coreRestClient.post().uri("/generate").body(body).retrieve().body(JsonNode.class);
    }

}
