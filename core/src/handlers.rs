use axum::{Json, extract::State};
use serde::{Deserialize, Serialize};
use std::{fs, path::PathBuf};
use tracing::info;

use crate::{
    dto::*,
    error::AppError,
    llama_process::{build_llama_command, kill_child, wait_until_healthy},
    state::AppState,
};

#[derive(Debug, Serialize)]
struct OpenAiChatRequest {
    model: String,
    messages: Vec<ChatMessage>,
    max_tokens: u32,
    temperature: f32,
    top_p: f32,
    stream: bool,
}

#[derive(Debug, Serialize, Deserialize)]
struct ChatMessage {
    role: String,
    content: String,
}

#[derive(Debug, Deserialize)]
struct OpenAiChatResponse {
    choices: Vec<Choice>,
}

#[derive(Debug, Deserialize)]
struct Choice {
    message: ChoiceMessage,
    finish_reason: Option<String>,
}

#[derive(Debug, Deserialize)]
struct ChoiceMessage {
    content: String,
}

pub async fn health(State(state): State<AppState>) -> Result<Json<HealthResponse>, AppError> {
    let (current_model, last_working_model) = {
        let rt = state.runtime.lock().await;
        (rt.current_model.clone(), rt.last_working_model.clone())
    };

    let llama_server_running = state
        .http
        .get(format!("{}/health", state.config.llama_base_url()))
        .send()
        .await
        .map(|r| r.status().is_success())
        .unwrap_or(false);

    Ok(Json(HealthResponse {
        status: if current_model.is_none() || llama_server_running {
            "UP".into()
        } else {
            "DEGRADED".into()
        },
        llama_server_running,
        current_model,
        last_working_model,
    }))
}

pub async fn models(State(state): State<AppState>) -> Result<Json<ModelsResponse>, AppError> {
    let current_model = {
        let rt = state.runtime.lock().await;
        rt.current_model.clone()
    };

    let mut items = Vec::new();
    let dir = PathBuf::from(&state.config.models_dir);

    if dir.exists() {
        for entry in fs::read_dir(&dir).map_err(|e| AppError::Internal(e.to_string()))? {
            let entry = entry.map_err(|e| AppError::Internal(e.to_string()))?;
            let path = entry.path();

            if path.is_file() && path.extension().map(|e| e == "gguf").unwrap_or(false) {
                let name = path
                    .file_name()
                    .and_then(|n| n.to_str())
                    .unwrap_or_default()
                    .to_string();

                items.push(ModelInfo {
                    name: name.clone(),
                    path: path.to_string_lossy().to_string(),
                    loaded: current_model.as_deref() == Some(name.as_str()),
                });
            }
        }
    }

    items.sort_by(|a, b| a.name.cmp(&b.name));

    let last_working_model = {
        let rt = state.runtime.lock().await;
        rt.last_working_model.clone()
    };

    Ok(Json(ModelsResponse {
        models: items,
        current_model,
        last_working_model,
    }))
}

pub async fn load(
    State(state): State<AppState>,
    Json(req): Json<LoadRequest>,
) -> Result<Json<LoadResponse>, AppError> {
    let model_path = PathBuf::from(&state.config.models_dir).join(&req.model);
    if !model_path.exists() {
        return Err(AppError::NotFound(format!(
            "model '{}' not found in {}",
            req.model, state.config.models_dir
        )));
    }

    unload_internal(&state).await?;

    let ctx_size = req.ctx_size.unwrap_or(state.config.default_ctx_size);
    let threads = req.threads.unwrap_or(state.config.default_threads);
    let n_gpu_layers = req
        .n_gpu_layers
        .unwrap_or_else(|| state.config.default_n_gpu_layers.clone());

    let mut cmd = build_llama_command(&state.config, &model_path, ctx_size, threads, &n_gpu_layers);

    info!(
        "starting llama-server: model={}, ctx_size={}, threads={}, n_gpu_layers={}",
        req.model, ctx_size, threads, n_gpu_layers
    );

    let child = cmd
        .spawn()
        .map_err(|e| AppError::Internal(format!("failed to start llama-server: {}", e)))?;

    {
        let mut rt = state.runtime.lock().await;
        rt.child = Some(child);
        rt.current_model = Some(req.model.clone());
    }

    match wait_until_healthy(&state.http, &state.config.llama_base_url(), 60, 500).await {
        Ok(_) => {
            let mut rt = state.runtime.lock().await;
            rt.last_working_model = Some(req.model.clone());
        }
        Err(err) => {
            unload_internal(&state).await?;
            return Err(err);
        }
    }

    Ok(Json(LoadResponse {
        status: "loaded".into(),
        current_model: req.model,
        llama_url: state.config.llama_base_url(),
    }))
}

pub async fn generate(
    State(state): State<AppState>,
    Json(req): Json<GenerateRequest>,
) -> Result<Json<GenerateResponse>, AppError> {
    if req.prompt.trim().is_empty() {
        return Err(AppError::BadRequest("prompt must not be empty".into()));
    }

    let mut fallback_used = false;

    if let Some(model) = &req.model {
        let current_model = {
            let rt = state.runtime.lock().await;
            rt.current_model.clone()
        };

        if current_model.as_deref() != Some(model.as_str()) {
            let _ = load(
                State(state.clone()),
                Json(LoadRequest {
                    model: model.clone(),
                    ctx_size: None,
                    threads: None,
                    n_gpu_layers: None,
                }),
            )
            .await?;
        }
    }

    let current_model = {
        let rt = state.runtime.lock().await;
        rt.current_model.clone()
    };

    let active_model = if let Some(m) = current_model {
        m
    } else {
        let last_working_model = {
            let rt = state.runtime.lock().await;
            rt.last_working_model.clone()
        };

        match last_working_model {
            Some(model) => {
                fallback_used = true;
                let _ = load(
                    State(state.clone()),
                    Json(LoadRequest {
                        model: model.clone(),
                        ctx_size: None,
                        threads: None,
                        n_gpu_layers: None,
                    }),
                )
                .await?;
                model
            }
            None => {
                return Err(AppError::BadRequest(
                    "no model loaded and no fallback model available".into(),
                ));
            }
        }
    };

    let mut messages = Vec::new();

    if let Some(system) = req.system {
        if !system.trim().is_empty() {
            messages.push(ChatMessage {
                role: "system".into(),
                content: system,
            });
        }
    }

    messages.push(ChatMessage {
        role: "user".into(),
        content: req.prompt,
    });

    let payload = OpenAiChatRequest {
        model: "local-model".into(),
        messages,
        max_tokens: req.max_tokens.unwrap_or(256),
        temperature: req.temperature.unwrap_or(0.7),
        top_p: req.top_p.unwrap_or(0.95),
        stream: false,
    };

    let response = state
        .http
        .post(format!(
            "{}/v1/chat/completions",
            state.config.llama_base_url()
        ))
        .json(&payload)
        .send()
        .await
        .map_err(|e| AppError::Upstream(format!("failed to call llama-server: {}", e)))?;

    if !response.status().is_success() {
        let body = response.text().await.unwrap_or_default();
        return Err(AppError::Upstream(format!(
            "llama-server returned non-success status: {}",
            body
        )));
    }

    let parsed: OpenAiChatResponse = response
        .json()
        .await
        .map_err(|e| AppError::Upstream(format!("invalid llama-server response: {}", e)))?;

    let first = parsed
        .choices
        .first()
        .ok_or_else(|| AppError::Upstream("llama-server returned empty choices".into()))?;

    Ok(Json(GenerateResponse {
        model: active_model,
        output: first.message.content.clone(),
        finish_reason: first.finish_reason.clone(),
        fallback_used,
    }))
}

pub async fn unload(State(state): State<AppState>) -> Result<Json<UnloadResponse>, AppError> {
    unload_internal(&state).await?;
    Ok(Json(UnloadResponse {
        status: "unloaded".into(),
    }))
}

async fn unload_internal(state: &AppState) -> Result<(), AppError> {
    let mut rt = state.runtime.lock().await;

    if let Some(mut child) = rt.child.take() {
        kill_child(&mut child).await;
    }

    rt.current_model = None;
    Ok(())
}
