# ani-rss-rs

Rust workspace for Ani-RSS API automation.

## Crates

- `ani-rss-client`: reusable API client (endpoint catalog + generic dispatcher)
- `ani-rss-cli`: command line client

## Build & Test

```bash
cd ani-rss-rs
cargo test --workspace
```

## CLI Quick Start

List all supported endpoint keys:

```bash
cargo run -p ani-rss-cli -- endpoint list
```

Login and print token:

```bash
cargo run -p ani-rss-cli -- \
  --base-url http://127.0.0.1:7789 \
  auth login --username admin --password your-password --print-token
```

Call an endpoint by key:

```bash
cargo run -p ani-rss-cli -- \
  --base-url http://127.0.0.1:7789 \
  --token YOUR_TOKEN \
  endpoint call listAni
```

Call with query arguments:

```bash
cargo run -p ani-rss-cli -- \
  --base-url http://127.0.0.1:7789 \
  --token YOUR_TOKEN \
  endpoint call searchBgm --query name=darling
```

Download binary response:

```bash
cargo run -p ani-rss-cli -- \
  --base-url http://127.0.0.1:7789 \
  --token YOUR_TOKEN \
  endpoint call downloadLogs --download ./logs.zip
```

Upload multipart file (`importConfig` / `upload`):

```bash
cargo run -p ani-rss-cli -- \
  --base-url http://127.0.0.1:7789 \
  --token YOUR_TOKEN \
  endpoint call importConfig --file ./ani-rss.backup.zip
```
