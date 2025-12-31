#!/bin/bash

# build_info 位置
build_info_path=./src/main/resources/build_info

rm -rf ${build_info_path}
git rev-parse --short HEAD >> ${build_info_path}
git branch --show-current >> ${build_info_path}

# 更新程序位置
update_exe_path=./src/main/resources/ani-rss-update.exe

if [ ! -e ${update_exe_path} ]; then
  echo "${YELLOW}下载 ani-rss-update.exe${NC}"
  curl -L https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe -O ani-rss-update.exe
  if [ $? -eq 1 ]; then
    echo -e "${RED}下载失败 ani-rss-update.exe${NC}"
    exit 1
  fi
  mv ani-rss-update.exe ${update_exe_path}
else
  echo -e "${YELLOW}已存在 ani-rss-update.exe${NC}"
fi
