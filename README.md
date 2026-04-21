# 🚀 LLM Inference Gateway (Rust + Java + llama.cpp)

## 📖 Overview

This project is a **self-hosted LLM inference service** built on top of llama.cpp.

It provides a REST API for running local language models (GGUF format) with **GPU acceleration only**.

The system uses a **two-layer architecture**:
- 🦀 Rust service — low-level inference engine
- ☕ Java service — external/public API layer

---

## 🏗️ Architecture


```
Client → Java API → Rust API → llama.cpp → GPU
```


### 🦀 Rust API
- Direct integration with llama.cpp
- Loads models **into GPU memory**
- Handles:
  - text generation
  - model switching
  - inference parameters

⚠️ CPU inference is **not supported**

### ☕ Java API
- Public-facing REST API
- Acts as a gateway
- Simplifies interaction with the model

---

## ⚙️ Features

- Local LLM inference (no external APIs)
- GGUF model support (e.g. Q4 quantization)
- **GPU-only execution**
- REST API for:
  - text generation
  - model management
- Docker-based deployment

---

## 🧰 Tech Stack

### 🦀 Rust

- axum
- tokio
- tower
- serde / serde_json
- reqwest
- tracing / tracing-subscriber 
- thiserror
- uuid


### ☕ Java (Spring Boot)

- Spring Boot 3 (Java 21)
- Spring Web — REST API
- Spring Validation — request validation
- Spring Actuator — monitoring
- Lombok — boilerplate reduction  


## 📦 Requirements
- Docker
- GPU with Vulkan support (required)
- Vulkan-capable host setup for containers (`/dev/dri` access in Docker)
- NVIDIA users: NVIDIA driver + NVIDIA Container Toolkit
- GGUF model (e.g. q4.gguf)

---

## 🚦 Quick Start

1. Put one or more `.gguf` files into `./models`
2. (Optional) set default model in env:
   - `AI_CORE_DEFAULT_MODEL=your-model-file.gguf`
3. Start services:
   ```bash
   docker compose up --build -d
   ```
4. Check health:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/health
   ```

---

## 🌐 Service Ports

- `8080` — Java REST API (public/client-facing)
- `8081` — Rust core API (internal core control + generation)
- `8091` — `llama-server` process port behind Rust core (debug/internal use)

---

## 🤖 Model Selection Behavior

- Model identifier is the **exact GGUF filename**, including extension  
  Example: `gemma-3-1b-it-f16.gguf`
- Java startup auto-load behavior:
  - if `AI_CORE_DEFAULT_MODEL` exists in model list, it is loaded
  - otherwise Java tries to load the first available `.gguf` model

---

## 🔌 Endpoints

### 🦀 Rust Endpoints (core, port `8081`)

- `GET /health`  
  Health + runtime state (`status`, `llamaServerRunning`, `currentModel`, `lastWorkingModel`)

- `GET /models`  
  List `.gguf` models from `MODELS_DIR`

- `POST /load`  
  `model` must be exact filename from `MODELS_DIR` (including `.gguf`)
  
  Body:
  ```json
  {
    "model": "gemma-3-1b-it-f16.gguf",
    "ctxSize": 4096,
    "threads": 8,
    "nGpuLayers": "999"
  }
  ```

- `POST /generate`  
  Body:
  ```json
  {
    "prompt": "Hello",
    "maxTokens": 256,
    "temperature": 0.7,
    "topP": 0.95,
    "model": "gemma-3-1b-it-f16.gguf",
    "system": "You are helpful assistant"
  }
  ```

- `POST /unload`  
  Unloads currently running model

### ☕ Java Endpoints (public API, port `8080`)

- `GET /api/list`  
  Returns available models known by Java layer

- `POST /api/set`  
  `id` must be exact `.gguf` filename from models list
  
  Body:
  ```json
  {
    "id": "yourmodel.gguf"
  }
  ```
  Sets current model and attempts to load it in Rust core

- `GET /api/current`  
  Returns current selected model

- `POST /chat`  
  Body:
  ```json
  {
    "message": "Hi GGUF!"
  }
  ```
  Sends user message to currently selected model via core
