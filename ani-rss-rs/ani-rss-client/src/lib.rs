use std::collections::BTreeMap;
use std::path::PathBuf;

use reqwest::header::{HeaderName, HeaderValue, CONTENT_TYPE};
use reqwest::{multipart, Client, StatusCode, Url};
use serde_json::{json, Map, Value};
use thiserror::Error;

pub mod endpoint;

pub use endpoint::{endpoint_by_key, EndpointDef, HttpMethod, ENDPOINTS};

#[derive(Debug, Error)]
pub enum ClientError {
    #[error("invalid base URL: {0}")]
    InvalidBaseUrl(String),
    #[error("unknown endpoint key: {0}")]
    UnknownEndpoint(String),
    #[error("multipart endpoint requires a file path")]
    MissingMultipartFile,
    #[error("invalid header name: {0}")]
    InvalidHeaderName(String),
    #[error("invalid header value for {name}: {value}")]
    InvalidHeaderValue { name: String, value: String },
    #[error("io error: {0}")]
    Io(#[from] std::io::Error),
    #[error("http client error: {0}")]
    Http(#[from] reqwest::Error),
    #[error("invalid JSON response: {0}")]
    Json(#[from] serde_json::Error),
    #[error("api error ({code}): {message}")]
    ApiError { code: i64, message: String },
    #[error("http status {status}: {body}")]
    HttpStatusError { status: u16, body: String },
    #[error("missing auth token in login response")]
    MissingLoginToken,
}

#[derive(Debug, Clone, Default)]
pub struct EndpointCall {
    pub query: BTreeMap<String, String>,
    pub body: Option<Value>,
    pub file_path: Option<PathBuf>,
    pub extra_headers: BTreeMap<String, String>,
}

#[derive(Debug, Clone)]
pub enum EndpointBody {
    Json(Value),
    Binary(Vec<u8>),
}

#[derive(Debug, Clone)]
pub struct EndpointResult {
    pub endpoint: &'static EndpointDef,
    pub status: StatusCode,
    pub content_type: Option<String>,
    pub body: EndpointBody,
}

impl EndpointResult {
    pub fn json(&self) -> Option<&Value> {
        match &self.body {
            EndpointBody::Json(value) => Some(value),
            EndpointBody::Binary(_) => None,
        }
    }

    pub fn bytes(&self) -> Option<&[u8]> {
        match &self.body {
            EndpointBody::Json(_) => None,
            EndpointBody::Binary(bytes) => Some(bytes),
        }
    }
}

#[derive(Debug, Clone)]
pub struct AniRssClient {
    base_url: Url,
    token: Option<String>,
    client: Client,
}

impl AniRssClient {
    pub fn new(base_url: impl AsRef<str>) -> Result<Self, ClientError> {
        let raw = base_url.as_ref().trim();
        let normalized = if raw.ends_with('/') {
            raw.to_string()
        } else {
            format!("{raw}/")
        };

        let base_url = Url::parse(&normalized).map_err(|_| ClientError::InvalidBaseUrl(raw.to_string()))?;

        Ok(Self {
            base_url,
            token: None,
            client: Client::builder().build()?,
        })
    }

    pub fn with_token(mut self, token: impl Into<String>) -> Self {
        self.token = Some(token.into());
        self
    }

    pub fn set_token(&mut self, token: impl Into<String>) {
        self.token = Some(token.into());
    }

    pub fn clear_token(&mut self) {
        self.token = None;
    }

    pub fn token(&self) -> Option<&str> {
        self.token.as_deref()
    }

    pub async fn login(
        &mut self,
        username: impl AsRef<str>,
        password_plaintext: impl AsRef<str>,
        ip: Option<&str>,
    ) -> Result<String, ClientError> {
        let payload = json!({
            "username": username.as_ref(),
            "password": format!("{:x}", md5::compute(password_plaintext.as_ref())),
            "ip": ip.unwrap_or_default(),
        });

        let result = self
            .call_by_key(
                "login",
                EndpointCall {
                    body: Some(payload),
                    ..EndpointCall::default()
                },
            )
            .await?;

        let token = result
            .json()
            .and_then(|value| value.get("data"))
            .and_then(Value::as_str)
            .map(ToOwned::to_owned)
            .ok_or(ClientError::MissingLoginToken)?;

        self.set_token(token.clone());
        Ok(token)
    }

    pub async fn call_by_key(&self, key: &str, call: EndpointCall) -> Result<EndpointResult, ClientError> {
        let endpoint = endpoint_by_key(key).ok_or_else(|| ClientError::UnknownEndpoint(key.to_string()))?;
        self.call_endpoint(endpoint, call).await
    }

    pub async fn call_endpoint(
        &self,
        endpoint: &'static EndpointDef,
        call: EndpointCall,
    ) -> Result<EndpointResult, ClientError> {
        let mut url = self
            .base_url
            .join(endpoint.path.trim_start_matches('/'))
            .map_err(|_| ClientError::InvalidBaseUrl(self.base_url.to_string()))?;

        if !call.query.is_empty() {
            {
                let mut pairs = url.query_pairs_mut();
                for (key, value) in &call.query {
                    pairs.append_pair(key, value);
                }
            }
        }

        let mut request_builder = match endpoint.method {
            HttpMethod::Get => self.client.get(url),
            HttpMethod::Post => self.client.post(url),
        };

        if let Some(token) = &self.token {
            request_builder = request_builder.header("Authorization", token);
        }

        for (name, value) in &call.extra_headers {
            let header_name = HeaderName::try_from(name.as_str())
                .map_err(|_| ClientError::InvalidHeaderName(name.to_string()))?;
            let header_value = HeaderValue::try_from(value.as_str()).map_err(|_| ClientError::InvalidHeaderValue {
                name: name.to_string(),
                value: value.to_string(),
            })?;
            request_builder = request_builder.header(header_name, header_value);
        }

        if endpoint.multipart {
            let file_path = call.file_path.ok_or(ClientError::MissingMultipartFile)?;
            let file_name = file_path
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("upload.bin")
                .to_string();
            let file_bytes = std::fs::read(&file_path)?;
            let file_part = multipart::Part::bytes(file_bytes).file_name(file_name);
            let mut form = multipart::Form::new().part("file", file_part);

            if let Some(Value::Object(fields)) = call.body {
                for (key, value) in fields {
                    form = form.text(key, scalar_to_form_value(&value));
                }
            }

            request_builder = request_builder.multipart(form);
        } else if let Some(body) = call.body {
            request_builder = request_builder.json(&body);
        }

        let response = request_builder.send().await?;
        let status = response.status();
        let headers = response.headers().clone();
        let content_type = headers
            .get(CONTENT_TYPE)
            .and_then(|value| value.to_str().ok())
            .map(ToOwned::to_owned);

        let payload_bytes = response.bytes().await?.to_vec();

        if looks_like_json(content_type.as_deref()) {
            let value: Value = serde_json::from_slice(&payload_bytes)?;
            validate_api_code(&value)?;

            if !status.is_success() {
                return Err(ClientError::HttpStatusError {
                    status: status.as_u16(),
                    body: value.to_string(),
                });
            }

            return Ok(EndpointResult {
                endpoint,
                status,
                content_type,
                body: EndpointBody::Json(value),
            });
        }

        if !status.is_success() {
            return Err(ClientError::HttpStatusError {
                status: status.as_u16(),
                body: String::from_utf8_lossy(&payload_bytes).to_string(),
            });
        }

        Ok(EndpointResult {
            endpoint,
            status,
            content_type,
            body: EndpointBody::Binary(payload_bytes),
        })
    }
}

fn scalar_to_form_value(value: &Value) -> String {
    match value {
        Value::Null => String::new(),
        Value::Bool(v) => v.to_string(),
        Value::Number(v) => v.to_string(),
        Value::String(v) => v.clone(),
        Value::Array(_) | Value::Object(_) => value.to_string(),
    }
}

fn looks_like_json(content_type: Option<&str>) -> bool {
    match content_type {
        Some(content_type) => {
            let lower = content_type.to_ascii_lowercase();
            lower.contains("application/json") || lower.contains("+json")
        }
        None => false,
    }
}

fn validate_api_code(value: &Value) -> Result<(), ClientError> {
    let code = value.get("code").and_then(Value::as_i64);
    if let Some(code) = code {
        if !(200..300).contains(&code) {
            let message = value
                .get("message")
                .and_then(Value::as_str)
                .unwrap_or("unknown error")
                .to_string();
            return Err(ClientError::ApiError { code, message });
        }
    }
    Ok(())
}

pub fn parse_key_value_args(items: &[String]) -> Result<BTreeMap<String, String>, ClientError> {
    let mut map = BTreeMap::new();
    for item in items {
        let (key, value) = item
            .split_once('=')
            .ok_or_else(|| ClientError::InvalidHeaderName(item.clone()))?;
        map.insert(key.to_string(), value.to_string());
    }
    Ok(map)
}

pub fn object_from_map(map: BTreeMap<String, String>) -> Value {
    let object: Map<String, Value> = map
        .into_iter()
        .map(|(key, value)| (key, Value::String(value)))
        .collect();
    Value::Object(object)
}
