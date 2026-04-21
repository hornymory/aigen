package com.hornymory.llm_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hornymory.llm_api.dto.ChatRequest;
import com.hornymory.llm_api.dto.ChatResponse;
import com.hornymory.llm_api.dto.ModelResponse;
import com.hornymory.llm_api.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final CoreClient coreClient;
    private final ModelService modelService;

    @Value("${ai.core.default-model:}")
    private String defaultModelId;

    private volatile String loadError = "NO_LOAD_ATTEMPT";

    public synchronized boolean loadCurrentModel() {
        Model current = modelService.getCurrentModel();
        if (current == null) {
            modelService.setCurrentModelLoaded(false);
            loadError = "NO_MODEL_SELECTED";
            return false;
        }

        try {
            JsonNode response = coreClient.load(current.getId(), null, null, null);
            boolean ok = isLoadSuccess(response, current.getId());
            modelService.setCurrentModelLoaded(ok);
            loadError = ok ? "OK" : extractCoreError(response, "LOAD_FAILED");
            return ok;
        } catch (RestClientResponseException ex) {
            modelService.setCurrentModelLoaded(false);
            loadError = "LOAD_HTTP_" + ex.getStatusCode().value() + ": " + compact(ex.getResponseBodyAsString());
            return false;
        } catch (Exception ex) {
            modelService.setCurrentModelLoaded(false);
            loadError = "LOAD_EXCEPTION: " + compact(ex.getMessage());
            return false;
        }
    }

    public synchronized boolean loadFirstAvailableModel() {
        String modelId = pickModelForBootstrap();
        if (modelId == null) {
            modelService.clearCurrentModel();
            loadError = "NO_MODELS_FOUND";
            return false;
        }

        modelService.setCurrentModelById(modelId);
        return loadCurrentModel();
    }

    public ChatResponse askModel(ChatRequest request) {
        long startedAt = System.currentTimeMillis();

        String prompt = request == null ? null : request.getMessage();
        if (prompt == null || prompt.trim().isEmpty()) {
            return failChat(null, startedAt, "EMPTY_PROMPT");
        }

        Model current = modelService.getCurrentModel();
        if (current == null || !current.isLoaded()) {
            if (!loadFirstAvailableModel()) {
                return failChat(current == null ? null : current.getId(), startedAt, loadError);
            }
            current = modelService.getCurrentModel();
        }

        try {
            JsonNode response = coreClient.generate(
                    prompt,
                    256,
                    0.7,
                    0.95,
                    current.getId(),
                    null
            );

            String answer = response.path("output").asText("").trim();
            String modelId = response.path("model").asText(current.getId());

            if (answer.isEmpty()) {
                return failChat(modelId, startedAt, extractCoreError(response, "EMPTY_MODEL_RESPONSE"));
            }

            return new ChatResponse(
                    modelId,
                    answer,
                    System.currentTimeMillis() - startedAt,
                    true,
                    "OK"
            );
        } catch (RestClientResponseException ex) {
            return failChat(
                    current.getId(),
                    startedAt,
                    "GENERATE_HTTP_" + ex.getStatusCode().value() + ": " + compact(ex.getResponseBodyAsString())
            );
        } catch (Exception ex) {
            return failChat(current.getId(), startedAt, "GENERATE_EXCEPTION: " + compact(ex.getMessage()));
        }
    }

    public String getLoadError() {
        return loadError;
    }

    private String pickModelForBootstrap() {
        List<ModelResponse> models = modelService.getModels();
        if (models.isEmpty()) {
            return null;
        }

        if (defaultModelId != null && !defaultModelId.isBlank()) {
            for (ModelResponse model : models) {
                if (defaultModelId.equals(model.getId())) {
                    return model.getId();
                }
            }
        }

        return models.get(0).getId();
    }

    private boolean isLoadSuccess(JsonNode response, String requestedModelId) {
        String status = response.path("status").asText("");
        String currentModel = response.path("currentModel").asText("");
        return "loaded".equalsIgnoreCase(status) || requestedModelId.equals(currentModel);
    }

    private String extractCoreError(JsonNode response, String fallback) {
        if (response == null) {
            return fallback;
        }

        String message = response.path("message").asText("").trim();
        if (!message.isEmpty()) {
            return message;
        }

        String error = response.path("error").asText("").trim();
        if (!error.isEmpty()) {
            return error;
        }

        String status = response.path("status").asText("").trim();
        if (!status.isEmpty()) {
            return status;
        }

        return fallback;
    }

    private ChatResponse failChat(String modelId, long startedAt, String message) {
        return new ChatResponse(
                modelId,
                null,
                System.currentTimeMillis() - startedAt,
                false,
                compact(message)
        );
    }

    private String compact(String text) {
        if (text == null || text.isBlank()) {
            return "UNKNOWN_ERROR";
        }
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
