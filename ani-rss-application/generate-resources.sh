#!/bin/bash

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

cd $(dirname $0)/src/main/resources

# build.info
BUILD_INFO_FILE="build.info"

rm -rf ${BUILD_INFO_FILE}
git rev-parse --short HEAD >> ${BUILD_INFO_FILE}
git branch --show-current >> ${BUILD_INFO_FILE}

echo "$(cat ${BUILD_INFO_FILE})"

# 更新程序
UPDATE_EXE_FILE="ani-rss-update.exe"

if [ ! -e ${UPDATE_EXE_FILE} ]; then
  echo -e "${YELLOW}下载 ${UPDATE_EXE_FILE}${NC}"
  curl -L https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe -o ${UPDATE_EXE_FILE}
  if [ $? -eq 1 ]; then
    echo -e "${RED}下载失败 ${UPDATE_EXE_FILE}${NC}"
    exit 1
  fi
  echo -e "${GREEN}下载完成 ${UPDATE_EXE_FILE}${NC}"
else
  echo -e "${YELLOW}已存在 ${UPDATE_EXE_FILE}${NC}"
fi
