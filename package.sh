#!/bin/bash

# 起始位置
base_path=$(pwd)

# ui 位置
ui_path=${base_path}/ani-rss-ui
# ani-rss-application 路径
application_path=${base_path}/ani-rss-application
# dist 位置
dist_path=${application_path}/src/main/resources/dist
# 更新程序位置
update_exe_path=${application_path}/src/main/resources/ani-rss-update.exe
# build_info 位置
build_info_path=${application_path}/src/main/resources/build_info
# target 位置
target_path=${application_path}/target

rm -rf ${build_info_path}
git rev-parse --short HEAD >> ${build_info_path}
git branch --show-current >> ${build_info_path}

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

cd ${ui_path}

mvn -B install -DskipTests --file pom.xml

if [ $? -eq 1 ]; then
  echo -e "${RED}web编译失败${NC}"
  exit 1
fi

echo -e "${GREEN}web编译完成${NC}"

if [ -d ${dist_path} ]; then
  echo -e "${YELLOW}清理 ${dist_path}${NC}"
  rm -rf ${dist_path}/*
else
  echo -e "${YELLOW}创建文件夹 ${dist_path}${NC}"
  mkdir -p ${dist_path}
fi

cp -r dist/* ${dist_path}

if [ ! -e ${update_exe_path} ]; then
  echo "下载 ani-rss-update.exe"
  wget https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe
  mv ani-rss-update.exe ${update_exe_path}
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



md5sum ${target_path}/ani-rss-jar-with-dependencies.jar | awk '{print $1}' > ${target_path}/ani-rss-jar-with-dependencies.jar.md5
md5sum ${target_path}/ani-rss-launcher.exe | awk '{print $1}' > ${target_path}/ani-rss-launcher.exe.md5

echo "md5"
echo "ani-rss-jar-with-dependencies.jar $(cat ${target_path}/ani-rss-jar-with-dependencies.jar.md5)"
echo "ani-rss-launcher.exe $(cat ${target_path}/ani-rss-launcher.exe.md5)"
