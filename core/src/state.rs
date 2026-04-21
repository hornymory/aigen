use crate::config::AppConfig;
use std::sync::Arc;
use tokio::{process::Child, sync::Mutex};

pub struct RuntimeState {
    pub child: Option<Child>,
    pub current_model: Option<String>,
    pub last_working_model: Option<String>,
}
impl RuntimeState {
    pub fn new() -> Self {
        Self {
            child: None,
            current_model: None,
            last_working_model: None,
        }
    }
}

#[derive(Clone)]
pub struct AppState {
    pub config: AppConfig,
    pub http: reqwest::Client,
    pub runtime: Arc<Mutex<RuntimeState>>,
}

impl AppState {
    pub fn new(config: AppConfig) -> Self {
        let http = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(config.upstream_timeout_secs))
            .build()
            .expect("failed to build reqwest client");

        Self {
            config,
            http,
            runtime: Arc::new(Mutex::new(RuntimeState::new())),
        }
    }
}
