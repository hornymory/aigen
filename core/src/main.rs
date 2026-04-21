mod config;
mod dto;
mod error;
mod handlers;
mod llama_process;
mod state;

use axum::{
    Router,
    routing::{get, post},
};
use tower_http::{
    cors::{Any, CorsLayer},
    trace::TraceLayer,
};
use tracing::info;

use crate::{config::AppConfig, state::AppState};

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            std::env::var("RUST_LOG")
                .unwrap_or_else(|_| "rust_core=info,tower_http=info,axum=info".into()),
        )
        .init();

    let config = AppConfig::from_env();
    let app_state = AppState::new(config.clone());

    let app = Router::new()
        .route("/health", get(handlers::health))
        .route("/models", get(handlers::models))
        .route("/load", post(handlers::load))
        .route("/generate", post(handlers::generate))
        .route("/unload", post(handlers::unload))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        )
        .layer(TraceLayer::new_for_http())
        .with_state(app_state);

    let bind_addr = config.bind_addr();
    let listener = tokio::net::TcpListener::bind(&bind_addr)
        .await
        .expect("failed to bind");

    info!("rust-core listening on {}", bind_addr);

    axum::serve(listener, app).await.expect("server failed");
}

