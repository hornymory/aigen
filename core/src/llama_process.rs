use std::path::Path;

use crate::{config::AppConfig, error::AppError};
use tokio::{
    process::{Child, Command},
    time::{sleep, Duration},
};
use tracing::{info, warn};

pub fn build_llama_command(
    config: &AppConfig,
    model_path: &Path,
    ctx_size: u32,
    threads: u32,
    n_gpu_layers: &str,
) -> Command {
    let mut cmd = Command::new(&config.llama_server_bin);

    cmd.arg("-m")
        .arg(model_path)
        .arg("--host")
        .arg(&config.llama_host)
        .arg("--port")
        .arg(config.llama_port.to_string())
        .arg("-c")
        .arg(ctx_size.to_string())
        .arg("-t")
        .arg(threads.to_string())
        .arg("--n-gpu-layers")
        .arg(n_gpu_layers)
        .stdout(std::process::Stdio::inherit())
        .stderr(std::process::Stdio::inherit());
    cmd
}
pub async fn wait_until_healthy(
    http: &reqwest::Client,
    base_url: &str,
    retries: u32,
    delay_ms: u64,
) -> Result<(), AppError> {
    for _ in 0..retries {
        let url = format!("{}/health", base_url);
        match http.get(&url).send().await {
            Ok(resp) if resp.status().is_success() => {
                info!("llama-server is healthy at {}", url);
                return Ok(());
            }
            Ok(resp) => {
                warn!("llama-server returned status {}", resp.status());
            }
            Err(err) => {
                warn!("llama-server health check failed: {}", err);
            }
        }

        sleep(Duration::from_millis(delay_ms)).await;
    }

    Err(AppError::Upstream(
        "llama-server did not become healthy in time".into(),
    ))
}
pub async fn kill_child(child: &mut Child) {
    if let Err(err) = child.kill().await {
        warn!("failed to kill llama-server child: {}", err);
    }
}
