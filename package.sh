#!/bin/bash

mkdir -p src/main/resources/dist
cd ui
npm install pnpm -g
pnpm install
pnpm run build
cp -r dist/* ../src/main/resources/dist
wget https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe
cp ani-rss-update.exe ../src/main/resources/ani-rss-update.exe
cd ..
mvn -B package -DskipTests --file pom.xml
