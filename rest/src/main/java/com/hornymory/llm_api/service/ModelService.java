package com.hornymory.llm_api.service;

import com.hornymory.llm_api.dto.ModelResponse;
import com.hornymory.llm_api.model.Model;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class ModelService {

    private final List<Model> models = new ArrayList<>();
    private Model currentModel;

    public synchronized List<ModelResponse> getModels() {
        List<ModelResponse> response = new ArrayList<>();

        for (Model model : models) {
            response.add(new ModelResponse(
                    model.getId(),
                    model.getPath(),
                    model.isLoaded(),
                    "OK"
            ));
        }

        return response;
    }

    public synchronized void setModels(List<Model> newModels) {
        models.clear();
        models.addAll(newModels);
    }

    public synchronized ModelResponse getCurrentModelResponse() {
        if (currentModel == null) {
            return new ModelResponse(
                    null,
                    null,
                    false,
                    "NO_MODEL_SELECTED"
            );
        }

        return new ModelResponse(
                currentModel.getId(),
                currentModel.getPath(),
                currentModel.isLoaded(),
                "OK"
        );
    }
    public synchronized Model getCurrentModel() {
        if (currentModel == null) {
            return null;
        }

        return new Model(
                currentModel.getId(),
                currentModel.getPath(),
                currentModel.isLoaded()
        );
    }

    public synchronized void setCurrentModelById(String id){
        Model selected = null;
        for (Model model : models) {
            model.setLoaded(false);
            if (model.getId().equals(id)) {
                selected = model;
            }
        }
        currentModel = selected;
    }

    public synchronized void setCurrentModelLoaded(boolean loaded) {
        if (currentModel == null) {
            return;
        }

        for (Model model : models) {
            if (model.getId().equals(currentModel.getId())) {
                model.setLoaded(loaded);
            } else if (loaded) {
                model.setLoaded(false);
            }
        }
        currentModel.setLoaded(loaded);
    }
    public synchronized Model findById(String id){
        for(Model model : models){
            if(model.getId().equals(id)){
                return model;
            }
        }
        return null;
    }

    public synchronized void clearCurrentModel(){
        if(currentModel!=null){
            currentModel.setLoaded(false);
        }
        currentModel=null;
    }


}
