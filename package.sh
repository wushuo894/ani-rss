#!/bin/bash

rm -rf ../src/main/resources/dist
mkdir -p src/main/resources/dist
cd ui
npm install pnpm -g
pnpm install
pnpm run build
if [ $? -eq 1 ]; then
  exit 1
fi
cp -r dist/* ../src/main/resources/dist
wget https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe
rm ../src/main/resources/ani-rss-update.exe
mv ani-rss-update.exe ../src/main/resources/ani-rss-update.exe
cd ..
mvn -B package -DskipTests --file pom.xml


md5sum target/ani-rss-jar-with-dependencies.jar | awk '{print $1}' > target/ani-rss-jar-with-dependencies.jar.md5
md5sum target/ani-rss-launcher.exe | awk '{print $1}' > target/ani-rss-launcher.exe.md5

