use std::io::Write;
use std::path::PathBuf;

use ani_rss_client::{
    parse_key_value_args, AniRssClient, EndpointBody, EndpointCall, ENDPOINTS,
};
use anyhow::{bail, Context, Result};
use clap::{Parser, Subcommand, ValueEnum};
use serde_json::Value;

#[derive(Parser, Debug)]
#[command(name = "ani-rss-cli")]
#[command(about = "Rust CLI client for Ani-RSS API")]
struct Cli {
    #[arg(long, env = "ANI_RSS_BASE_URL", default_value = "http://127.0.0.1:7789")]
    base_url: String,

    #[arg(long, env = "ANI_RSS_TOKEN")]
    token: Option<String>,

    #[command(subcommand)]
    command: TopLevelCommand,
}

#[derive(Subcommand, Debug)]
enum TopLevelCommand {
    Auth {
        #[command(subcommand)]
        command: AuthCommand,
    },
    Endpoint {
        #[command(subcommand)]
        command: EndpointCommand,
    },
}

#[derive(Subcommand, Debug)]
enum AuthCommand {
    Login {
        #[arg(long)]
        username: String,
        #[arg(long)]
        password: String,
        #[arg(long)]
        ip: Option<String>,
        #[arg(long)]
        save_token: Option<PathBuf>,
        #[arg(long)]
        print_token: bool,
    },
    TestIpWhitelist {
        #[arg(long, value_enum, default_value_t = OutputFormat::Pretty)]
        output: OutputFormat,
    },
}

#[derive(Subcommand, Debug)]
enum EndpointCommand {
    List,
    Call {
        key: String,
        #[arg(long = "query")]
        query: Vec<String>,
        #[arg(long = "header")]
        header: Vec<String>,
        #[arg(long)]
        body_json: Option<String>,
        #[arg(long)]
        body_file: Option<PathBuf>,
        #[arg(long)]
        file: Option<PathBuf>,
        #[arg(long)]
        download: Option<PathBuf>,
        #[arg(long, value_enum, default_value_t = OutputFormat::Pretty)]
        output: OutputFormat,
    },
}

#[derive(ValueEnum, Clone, Debug, PartialEq, Eq)]
enum OutputFormat {
    Pretty,
    Json,
    Raw,
}

#[tokio::main]
async fn main() -> Result<()> {
    let cli = Cli::parse();
    run(cli).await
}

async fn run(cli: Cli) -> Result<()> {
    let mut client = AniRssClient::new(&cli.base_url).context("failed to initialize API client")?;
    if let Some(token) = cli.token {
        client.set_token(token);
    }

    match cli.command {
        TopLevelCommand::Auth { command } => match command {
            AuthCommand::Login {
                username,
                password,
                ip,
                save_token,
                print_token,
            } => {
                let token = client
                    .login(username, password, ip.as_deref())
                    .await
                    .context("login request failed")?;

                if let Some(path) = save_token {
                    if let Some(parent) = path.parent() {
                        std::fs::create_dir_all(parent)
                            .with_context(|| format!("failed to create token directory: {}", parent.display()))?;
                    }
                    std::fs::write(&path, &token)
                        .with_context(|| format!("failed to save token to {}", path.display()))?;
                    eprintln!("saved token to {}", path.display());
                }

                if print_token {
                    println!("{token}");
                } else {
                    println!(
                        "{}",
                        serde_json::to_string_pretty(&serde_json::json!({
                            "token": token,
                            "hint": "use --token or ANI_RSS_TOKEN for subsequent calls"
                        }))?
                    );
                }
            }
            AuthCommand::TestIpWhitelist { output } => {
                let result = client
                    .call_by_key("testIpWhitelist", EndpointCall::default())
                    .await
                    .context("testIpWhitelist request failed")?;
                render_output(&result.body, output)?;
            }
        },
        TopLevelCommand::Endpoint { command } => match command {
            EndpointCommand::List => {
                for endpoint in ENDPOINTS {
                    println!(
                        "{:<24} {:<6} {:<14} {}",
                        endpoint.key,
                        match endpoint.method {
                            ani_rss_client::HttpMethod::Get => "GET",
                            ani_rss_client::HttpMethod::Post => "POST",
                        },
                        endpoint.group,
                        endpoint.path
                    );
                }
            }
            EndpointCommand::Call {
                key,
                query,
                header,
                body_json,
                body_file,
                file,
                download,
                output,
            } => {
                let query = parse_key_value_args(&query)
                    .map_err(anyhow::Error::new)
                    .context("invalid --query argument, expected key=value")?;

                let extra_headers = parse_key_value_args(&header)
                    .map_err(anyhow::Error::new)
                    .context("invalid --header argument, expected key=value")?;

                let body = parse_body(body_json, body_file)?;

                let result = client
                    .call_by_key(
                        &key,
                        EndpointCall {
                            query,
                            body,
                            file_path: file,
                            extra_headers,
                        },
                    )
                    .await
                    .with_context(|| format!("endpoint call failed for key '{key}'"))?;

                if let Some(path) = download {
                    let bytes = result
                        .bytes()
                        .ok_or_else(|| anyhow::anyhow!("--download can only be used with binary responses"))?;
                    if let Some(parent) = path.parent() {
                        std::fs::create_dir_all(parent).with_context(|| {
                            format!("failed to create download directory: {}", parent.display())
                        })?;
                    }
                    std::fs::write(&path, bytes)
                        .with_context(|| format!("failed to write download file: {}", path.display()))?;
                    println!("saved {} bytes to {}", bytes.len(), path.display());
                } else {
                    render_output(&result.body, output)?;
                }
            }
        },
    }

    Ok(())
}

fn parse_body(body_json: Option<String>, body_file: Option<PathBuf>) -> Result<Option<Value>> {
    match (body_json, body_file) {
        (Some(_), Some(_)) => bail!("use either --body-json or --body-file, not both"),
        (Some(raw), None) => Ok(Some(serde_json::from_str(&raw).context("invalid JSON in --body-json")?)),
        (None, Some(path)) => {
            let content = std::fs::read_to_string(&path)
                .with_context(|| format!("failed to read JSON file: {}", path.display()))?;
            Ok(Some(
                serde_json::from_str(&content)
                    .with_context(|| format!("invalid JSON in {}", path.display()))?,
            ))
        }
        (None, None) => Ok(None),
    }
}

fn render_output(body: &EndpointBody, output: OutputFormat) -> Result<()> {
    match (body, output) {
        (EndpointBody::Json(value), OutputFormat::Pretty) => {
            println!("{}", serde_json::to_string_pretty(value)?);
        }
        (EndpointBody::Json(value), OutputFormat::Json | OutputFormat::Raw) => {
            println!("{}", serde_json::to_string(value)?);
        }
        (EndpointBody::Binary(bytes), OutputFormat::Raw) => {
            let mut stdout = std::io::stdout();
            stdout.write_all(bytes)?;
            stdout.flush()?;
        }
        (EndpointBody::Binary(bytes), OutputFormat::Pretty | OutputFormat::Json) => {
            match std::str::from_utf8(bytes) {
                Ok(text) => println!("{text}"),
                Err(_) => {
                    println!(
                        "{}",
                        serde_json::to_string_pretty(&serde_json::json!({
                            "kind": "binary",
                            "length": bytes.len(),
                        }))?
                    );
                }
            }
        }
    }
    Ok(())
}
