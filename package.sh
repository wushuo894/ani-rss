#!/bin/bash

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

cd ui

if [ -d ../src/main/resources/dist ]; then
  echo -e "${YELLOW}清理 src/main/resources/dist${NC}"
  rm -rf ../src/main/resources/dist/*
else
  echo -e "${YELLOW}创建文件夹 src/main/resources/dist${NC}"
  mkdir -p ../src/main/resources/dist
fi

if ! command -v java >/dev/null 2>&1; then
  echo -e "${YELLOW}正在安装 pnpm ...${NC}"
  npm install pnpm -g
fi

pnpm install
pnpm run build

if [ $? -eq 1 ]; then
  echo -e "${RED}web编译失败${NC}"
  exit 1
fi

echo -e "${GREEN}web编译完成${NC}"

cp -r dist/* ../src/main/resources/dist

if [ ! -e ../src/main/resources/ani-rss-update.exe ]; then
  echo "下载 ani-rss-update.exe"
  wget https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe
  mv ani-rss-update.exe ../src/main/resources/ani-rss-update.exe
else
  echo -e "${YELLOW}已存在 ani-rss-update.exe${NC}"
fi

cd ..
mvn -B package -DskipTests --file pom.xml

if [ $? -eq 1 ]; then
  echo -e "${RED}jar编译失败${NC}"
  exit 1
fi

echo -e "${GREEN}jar编译完成${NC}"

md5sum target/ani-rss-jar-with-dependencies.jar | awk '{print $1}' > target/ani-rss-jar-with-dependencies.jar.md5
md5sum target/ani-rss-launcher.exe | awk '{print $1}' > target/ani-rss-launcher.exe.md5

echo "md5"
echo "ani-rss-jar-with-dependencies.jar $(cat target/ani-rss-jar-with-dependencies.jar.md5)"
echo "target/ani-rss-launcher.exe $(cat target/ani-rss-launcher.exe.md5)"
