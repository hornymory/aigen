# syntax=docker/dockerfile:1.7

FROM rust:1.86-bookworm AS core-builder

WORKDIR /app

COPY Cargo.toml Cargo.lock ./
COPY src ./src

RUN cargo build --release --locked

FROM ghcr.io/ggml-org/llama.cpp:server-vulkan-b8840

WORKDIR /app

COPY --from=core-builder /app/target/release/core /usr/local/bin/core

ENV RUST_CORE_HOST=0.0.0.0
ENV RUST_CORE_PORT=8081
ENV MODELS_DIR=/models
ENV LLAMA_SERVER_BIN=/app/llama-server
ENV LLAMA_HOST=127.0.0.1
ENV LLAMA_PORT=8091
ENV LLAMA_CTX_SIZE=4096
ENV LLAMA_THREADS=8
ENV LLAMA_N_GPU_LAYERS=999

EXPOSE 8081 8091

ENTRYPOINT ["core"]
