#!/bin/bash

if [ -d ../ani-rss-ui/dist ]; then
  if [ -d ./src/main/resources/dist ]; then
    rm -rf ./src/main/resources/dist
  fi
  mv ../ani-rss-ui/dist ./src/main/resources/dist
  echo "move dist ..."
fi

# build_info 位置
build_info_path=./src/main/resources/build_info

rm -rf ${build_info_path}
git rev-parse --short HEAD >> ${build_info_path}
git branch --show-current >> ${build_info_path}

# 更新程序位置
update_exe_path=./src/main/resources/ani-rss-update.exe

if [ ! -e ${update_exe_path} ]; then
  echo "下载 ani-rss-update.exe"
  curl -o ani-rss-update.exe https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe
  mv ani-rss-update.exe ${update_exe_path}
else
  echo -e "${YELLOW}已存在 ani-rss-update.exe${NC}"
fi
