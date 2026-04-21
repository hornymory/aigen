package com.hornymory.llm_api.service;

import com.hornymory.llm_api.model.Model;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoadModelService {
    private final ModelService modelService;
    private final AiService aiService;

    @Value("${ai.models-dir:models}")
    private String modelsDir;

    @PostConstruct
    public void init() {
        loadModelFromFolder();
        aiService.loadFirstAvailableModel();
    }

    public void loadModelFromFolder() {
        File folder = resolveModelsFolder();
        List<Model> loadedModels = new ArrayList<>();

        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            modelService.setModels(loadedModels);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf"));
        if (files == null) {
            modelService.setModels(loadedModels);
            return;
        }

        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            loadedModels.add(new Model(
                    file.getName(),
                    file.getAbsolutePath(),
                    false
            ));
        }

        modelService.setModels(loadedModels);
    }

    private File resolveModelsFolder() {
        List<String> candidates = new ArrayList<>();
        if (modelsDir != null && !modelsDir.isBlank()) {
            candidates.add(modelsDir);
        }
        candidates.add("models");
        candidates.add("../models");
        candidates.add("/models");

        for (String candidate : candidates) {
            File dir = new File(candidate);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }

        return new File(modelsDir == null || modelsDir.isBlank() ? "models" : modelsDir);
    }
}
