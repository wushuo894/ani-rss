#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_TESTS="ani.rss.mcp.McpCatalogTests,ani.rss.controller.McpControllerWebTests,ani.rss.config.WebFilterTests"
TESTS="${ANI_RSS_MCP_TESTS:-$DEFAULT_TESTS}"

cd "$ROOT_DIR"

docker run --rm \
  --network host \
  -v "$ROOT_DIR":/workspace \
  -w /workspace \
  maven:3.9.11-eclipse-temurin-17 \
  mvn -pl ani-rss-application -am -Dfrontend.skip=true -Dtest="$TESTS" -Dsurefire.failIfNoSpecifiedTests=false test "$@"
