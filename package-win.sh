#!/bin/bash


install_zip() {
  if command -v zip unzip >/dev/null 2>&1; then
    return
  fi

  echo -e "${YELLOW}正在安装 zip unzip...${NC}"
  if command -v apt >/dev/null 2>&1; then
    apt update -qq && apt install -y zip unzip
  elif command -v yum >/dev/null 2>&1; then
    yum update -y && yum install -y zip unzip
  elif command -v brew >/dev/null 2>&1; then
    brew update && brew install zip unzip
  else
    echo -e "${RED}不存在支持的包管理器${NC}"
    exit 1
  fi
  echo "${GREEN}安装完成 zip unzip${NC}"
}

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

if [ ! -f target/ani-rss-launcher.exe ]; then
  bash package.sh
fi

cd target

if [ -d ani-rss ]; then
  echo -e "${YELLOW}清理文件夹 ani-rss${NC}"
  rm -rf ani-rss
fi

mkdir ani-rss
cp ani-rss-launcher.exe ani-rss

install_zip

if [ ! -f java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64.zip ]; then
  wget https://github.com/ojdkbuild/ojdkbuild/releases/download/java-17-openjdk-17.0.3.0.6-1/java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64.zip
  if [ $? -eq 1 ]; then
    echo -e "${RED}JRE下载失败${NC}"
    exit 1
  fi

  echo -e "${GREEN}JRE下载成功${NC}"
else
  echo -e "${YELLOW}JRE已存在${NC}"
fi

unzip java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64.zip
mv java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64 ani-rss/jre
cp ../windows/* ani-rss
zip -r ani-rss.win.x86_64.zip ani-rss

echo -e "${GREEN}打包完成 ani-rss.win.x86_64.zip${NC}"

md5sum ani-rss.win.x86_64.zip | awk '{print $1}' > ani-rss.win.x86_64.zip.md5

echo "md5"
echo "ani-rss.win.x86_64.zip $(cat ani-rss.win.x86_64.zip.md5)"
