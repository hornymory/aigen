package com.hornymory.llm_api.controller;

import com.hornymory.llm_api.dto.ModelRequest;
import com.hornymory.llm_api.dto.ModelResponse;

import com.hornymory.llm_api.model.Model;
import com.hornymory.llm_api.service.AiService;
import com.hornymory.llm_api.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AiController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private AiService aiService;


    @GetMapping("/list")
    public List<ModelResponse> getModels(){
        return modelService.getModels();
    }

    @PostMapping("/set")
    public ModelResponse setModel(@RequestBody ModelRequest request) {
        Model model = modelService.findById(request.getId());

        if (model == null) {
            return new ModelResponse(null, null, false, "MODEL_NOT_FOUND");
        }

        Model previous = modelService.getCurrentModel();
        String previousId = previous != null ? previous.getId() : null;
        boolean previousLoaded = previous != null && previous.isLoaded();

        modelService.setCurrentModelById(request.getId());

        boolean loaded = aiService.loadCurrentModel();
        String loadMessage = aiService.getLoadError();

        ModelResponse current = modelService.getCurrentModelResponse();

        if (!loaded) {
            if (previousLoaded && previousId != null && !previousId.equals(request.getId())) {
                modelService.setCurrentModelById(previousId);
                boolean restored = aiService.loadCurrentModel();
                if (restored) {
                    ModelResponse restoredCurrent = modelService.getCurrentModelResponse();
                    return new ModelResponse(
                            restoredCurrent.getId(),
                            restoredCurrent.getPath(),
                            true,
                            "REQUESTED_MODEL_FAILED_RESTORED_PREVIOUS: " + loadMessage
                    );
                }
            }
            return new ModelResponse(
                    current.getId(),
                    current.getPath(),
                    false,
                    loadMessage
            );
        }

        return new ModelResponse(
                current.getId(),
                current.getPath(),
                true,
                "OK"
        );
    }
    @GetMapping("/current")
    public ModelResponse getCurrentModel(){
        return modelService.getCurrentModelResponse();
    }


}
