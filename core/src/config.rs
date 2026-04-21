use std::env;

#[derive(Clone, Debug)]
pub struct AppConfig {
    pub bind_host: String,
    pub bind_port: u16,

    pub models_dir: String,

    pub llama_server_bin: String,
    pub llama_host: String,
    pub llama_port: u16,

    pub default_ctx_size: u32,
    pub default_threads: u32,
    pub default_n_gpu_layers: String,

    pub upstream_timeout_secs: u64,
}

impl AppConfig {
    pub fn from_env() -> Self {
        Self {
            bind_host: env::var("RUST_CORE_HOST").unwrap_or_else(|_| "0.0.0.0".into()),
            bind_port: env::var("RUST_CORE_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(8081),

            models_dir: env::var("MODELS_DIR").unwrap_or_else(|_| "/models".into()),

            llama_server_bin: env::var("LLAMA_SERVER_BIN")
                .unwrap_or_else(|_| "/usr/local/bin/llama-server".into()),
            llama_host: env::var("LLAMA_HOST").unwrap_or_else(|_| "127.0.0.1".into()),
            llama_port: env::var("LLAMA_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(8091),

            default_ctx_size: env::var("LLAMA_CTX_SIZE")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(4096),

            default_threads: env::var("LLAMA_THREADS")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(8),

            default_n_gpu_layers: env::var("LLAMA_N_GPU_LAYERS").unwrap_or_else(|_| "0".into()),

            upstream_timeout_secs: env::var("UPSTREAM_TIMEOUT_SECS")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(180),
        }
    }
    pub fn llama_base_url(&self) -> String {
        format!("http://{}:{}", self.llama_host, self.llama_port)
    }
    pub fn bind_addr(&self) -> String {
        format!("{}:{}", self.bind_host, self.bind_port)
    }
}
