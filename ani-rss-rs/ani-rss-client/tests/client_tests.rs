use std::collections::BTreeMap;

use ani_rss_client::{endpoint_by_key, AniRssClient, ClientError, EndpointBody, EndpointCall, ENDPOINTS};
use tempfile::NamedTempFile;
use wiremock::matchers::{body_string_contains, header_exists, method, path, query_param};
use wiremock::{Mock, MockServer, ResponseTemplate};

#[test]
fn endpoint_registry_contains_all_webui_keys() {
    let expected = [
        "config",
        "setConfig",
        "listAni",
        "addAni",
        "setAni",
        "deleteAni",
        "about",
        "update",
        "mikan",
        "mikanGroup",
        "refreshAll",
        "refreshAni",
        "rssToAni",
        "previewAni",
        "logs",
        "clearLogs",
        "getThemoviedbName",
        "getThemoviedbGroup",
        "testNotification",
        "newNotification",
        "getBgmTitle",
        "searchBgm",
        "testProxy",
        "torrentsInfos",
        "verifyNo",
        "tryOut",
        "updateTotalEpisodeNumber",
        "batchEnable",
        "importAni",
        "stop",
        "refreshCover",
        "rate",
        "setRate",
        "downloadPath",
        "scrape",
        "meBgm",
        "trackersUpdate",
        "getEmbyViews",
        "clearCache",
        "downloadLoginTest",
        "getTgUpdates",
        "login",
        "testIpWhitelist",
        "playList",
        "getSubtitles",
        "startCollection",
        "previewCollection",
        "getCollectionSubgroup",
        "getAniBySubjectId",
        "deleteTorrent",
        "importConfig",
        "exportConfig",
        "downloadLogs",
        "file",
        "upload",
        "customJs",
        "customCss",
        "mikanCover",
        "bgmOauthCallback",
    ];

    for key in expected {
        assert!(endpoint_by_key(key).is_some(), "missing endpoint key: {key}");
    }

    assert!(ENDPOINTS.len() >= expected.len());
}

#[tokio::test]
async fn login_hashes_password_and_sets_token() {
    let server = MockServer::start().await;
    let expected_hash = format!("{:x}", md5::compute("secret-password"));

    Mock::given(method("POST"))
        .and(path("/api/login"))
        .and(body_string_contains("\"username\":\"alice\""))
        .and(body_string_contains(format!("\"password\":\"{expected_hash}\"")))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": 200,
            "message": "success",
            "data": "token-123",
            "t": 0
        })))
        .mount(&server)
        .await;

    let mut client = AniRssClient::new(server.uri()).expect("client");
    let token = client
        .login("alice", "secret-password", None)
        .await
        .expect("login");

    assert_eq!(token, "token-123");
    assert_eq!(client.token(), Some("token-123"));
}

#[tokio::test]
async fn call_endpoint_with_query_and_json_response() {
    let server = MockServer::start().await;

    Mock::given(method("POST"))
        .and(path("/api/searchBgm"))
        .and(query_param("name", "darling"))
        .and(header_exists("authorization"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": 200,
            "message": "success",
            "data": [{"id": 1, "name": "DARLING in the FRANXX"}],
            "t": 0
        })))
        .mount(&server)
        .await;

    let client = AniRssClient::new(server.uri())
        .expect("client")
        .with_token("token-value");

    let mut query = BTreeMap::new();
    query.insert("name".to_string(), "darling".to_string());

    let result = client
        .call_by_key(
            "searchBgm",
            EndpointCall {
                query,
                ..EndpointCall::default()
            },
        )
        .await
        .expect("call");

    let json = result.json().expect("json");
    assert_eq!(json["code"], 200);
    assert_eq!(json["data"][0]["id"], 1);
}

#[tokio::test]
async fn multipart_upload_is_supported() {
    let server = MockServer::start().await;

    Mock::given(method("POST"))
        .and(path("/api/importConfig"))
        .and(header_exists("content-type"))
        .and(body_string_contains("backup-body"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": 200,
            "message": "导入成功",
            "t": 0
        })))
        .mount(&server)
        .await;

    let mut temp = NamedTempFile::new().expect("temp file");
    std::io::Write::write_all(&mut temp, b"backup-body").expect("write temp file");

    let client = AniRssClient::new(server.uri()).expect("client");
    client
        .call_by_key(
            "importConfig",
            EndpointCall {
                file_path: Some(temp.path().to_path_buf()),
                ..EndpointCall::default()
            },
        )
        .await
        .expect("multipart call");
}

#[tokio::test]
async fn binary_download_returns_bytes() {
    let server = MockServer::start().await;

    Mock::given(method("GET"))
        .and(path("/api/downloadLogs"))
        .respond_with(
            ResponseTemplate::new(200)
                .insert_header("content-type", "application/zip")
                .set_body_bytes(vec![0, 1, 2, 3, 4]),
        )
        .mount(&server)
        .await;

    let client = AniRssClient::new(server.uri()).expect("client");
    let result = client
        .call_by_key("downloadLogs", EndpointCall::default())
        .await
        .expect("download");

    match result.body {
        EndpointBody::Binary(bytes) => assert_eq!(bytes, vec![0, 1, 2, 3, 4]),
        EndpointBody::Json(_) => panic!("expected binary payload"),
    }
}

#[tokio::test]
async fn api_error_is_returned_for_non_2xx_code_field() {
    let server = MockServer::start().await;

    Mock::given(method("POST"))
        .and(path("/api/config"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": 500,
            "message": "boom",
            "t": 0
        })))
        .mount(&server)
        .await;

    let client = AniRssClient::new(server.uri()).expect("client");
    let err = client
        .call_by_key("config", EndpointCall::default())
        .await
        .expect_err("should return API error");

    match err {
        ClientError::ApiError { code, message } => {
            assert_eq!(code, 500);
            assert_eq!(message, "boom");
        }
        other => panic!("unexpected error: {other:?}"),
    }
}
