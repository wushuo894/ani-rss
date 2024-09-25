#!/bin/bash

port="7789"
path="./"
jar="ani-rss-jar-with-dependencies.jar"
jar_path=$path$jar

if [ ! -f $jar_path ]; then
    version=$(curl -Ls https://github.com/wushuo894/ani-rss/releases/latest | grep -o '<h1 data-view-component="true" class="d-inline mr-3">[^<]*</h1>' | sed 's/<[^>]*>//g')
    url="https://github.com/wushuo894/ani-rss/releases/download/$version/ani-rss-jar-with-dependencies.jar"
    wget -O $jar_path $url

    if [ $? -eq 0 ]; then
        echo "$jar_path 下载成功！"
    else
        echo "$jar_path 下载失败。"
    fi
fi

stop() {
  pid=$(ps -ef | grep java | grep "$jar" | awk '{print $2}')
  if [ -n "$pid" ]; then
      echo "Stopping process $pid - $jar"
      kill "$pid"
  fi
}

sigterm_handler() {
    stop
    exit 143
}

trap 'sigterm_handler' SIGTERM

while :
do
    java -jar -Xmx2g $jar_path --port $port
    if [ $? -ne 0 ]; then
        break
    fi
done