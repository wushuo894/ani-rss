#!/bin/bash

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

mvn -B package -DskipTests --file pom.xml

if [ $? -eq 1 ]; then
  echo -e "${RED}jar编译失败${NC}"
  exit 1
fi

echo -e "${GREEN}jar编译完成${NC}"
