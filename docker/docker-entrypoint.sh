#!/bin/sh
set -e

path="./"
jar="ani-rss-jar-with-dependencies.jar"
jar_path=$path$jar

if [ ! -f $jar_path ]; then
    url="https://github.com/wushuo894/ani-rss/releases/latest/download/ani-rss-jar-with-dependencies.jar"
    wget -O $jar_path $url

    if [ $? -eq 0 ]; then
        echo "$jar_path 下载成功！"
    else
        echo "$jar_path 下载失败。"
        exit 1
    fi
fi

chown -R ${PUID}:${PGID} /usr/app

umask ${UMASK}

exec gosu "$PUID:$PGID" "$@"