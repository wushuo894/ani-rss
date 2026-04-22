use assert_cmd::Command;
use predicates::str::contains;
use tempfile::tempdir;
use wiremock::matchers::{body_string_contains, method, path, query_param};
use wiremock::{Mock, MockServer, ResponseTemplate};

#[test]
fn endpoint_list_prints_known_routes() {
    let mut cmd = Command::cargo_bin("ani-rss-cli").expect("binary");
    cmd.args(["endpoint", "list"])
        .assert()
        .success()
        .stdout(contains("listAni"))
        .stdout(contains("importConfig"))
        .stdout(contains("downloadLogs"));
}

#[tokio::test]
async fn auth_login_hashes_password_and_prints_token() {
    let server = MockServer::start().await;
    let expected_hash = format!("{:x}", md5::compute("p@ssw0rd"));

    Mock::given(method("POST"))
        .and(path("/api/login"))
        .and(body_string_contains("\"username\":\"alice\""))
        .and(body_string_contains(format!("\"password\":\"{expected_hash}\"")))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": 200,
            "message": "success",
            "data": "cli-token",
            "t": 0
        })))
        .mount(&server)
        .await;

    let mut cmd = Command::cargo_bin("ani-rss-cli").expect("binary");
    cmd.args([
        "--base-url",
        server.uri().as_str(),
        "auth",
        "login",
        "--username",
        "alice",
        "--password",
        "p@ssw0rd",
        "--print-token",
    ])
    .assert()
    .success()
    .stdout(contains("cli-token"));
}

#[tokio::test]
async fn endpoint_call_supports_query_parameters() {
    let server = MockServer::start().await;

    Mock::given(method("POST"))
        .and(path("/api/searchBgm"))
        .and(query_param("name", "darling"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": 200,
            "message": "success",
            "data": [{"id": 1}],
            "t": 0
        })))
        .mount(&server)
        .await;

    let mut cmd = Command::cargo_bin("ani-rss-cli").expect("binary");
    cmd.args([
        "--base-url",
        server.uri().as_str(),
        "endpoint",
        "call",
        "searchBgm",
        "--query",
        "name=darling",
        "--output",
        "json",
    ])
    .assert()
    .success()
    .stdout(contains("\"code\":200"));
}

#[tokio::test]
async fn endpoint_call_can_download_binary_payload() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/api/downloadLogs"))
        .respond_with(
            ResponseTemplate::new(200)
                .insert_header("content-type", "application/zip")
                .set_body_bytes(vec![1, 2, 3, 4, 5]),
        )
        .mount(&server)
        .await;

    let dir = tempdir().expect("temp dir");
    let output_file = dir.path().join("logs.zip");

    let mut cmd = Command::cargo_bin("ani-rss-cli").expect("binary");
    cmd.args([
        "--base-url",
        server.uri().as_str(),
        "endpoint",
        "call",
        "downloadLogs",
        "--download",
        output_file.to_str().expect("utf8 path"),
    ])
    .assert()
    .success()
    .stdout(contains("saved 5 bytes"));

    let bytes = std::fs::read(&output_file).expect("read output file");
    assert_eq!(bytes, vec![1, 2, 3, 4, 5]);
}
