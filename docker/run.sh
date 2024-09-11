#!/bin/bash

update="$AUTO_UPDATE"
path="./"
jar="ani-rss-jar-with-dependencies.jar"
jar_path=$path$jar

if [ ! -f "$jar_path" ] && [ "$update" ]; then
    echo "正在更新……"
    version=$(curl -Ls https://github.com/wushuo894/ani-rss/releases/latest | grep -o '<h1 data-view-component="true" class="d-inline mr-3">[^<]*</h1>' | sed 's/<[^>]*>//g')
    url="https://github.com/wushuo894/ani-rss/releases/download/$version/ani-rss-jar-with-dependencies.jar"

    if ! wget -O "$jar_path" "$url"
    then
        echo "$jar_path 下载成功！"
    else
        echo "$jar_path 下载失败。"
    fi
fi


pid=$(ps -ef | grep java |  grep "$jar" | awk '{print $2}')
if [ -n "$pid" ]; then
    echo "Stopping process $pid - $jar"
    kill "$pid"
fi

while :
do
    if ! java -jar -Xmx2g $jar_path --port "$PORT"
    then
        break
    fi
done