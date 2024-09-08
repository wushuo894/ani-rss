#!/bin/bash

jar="ani-rss-jar-with-dependencies.jar"

pid=$(pgrep -f "$jar")
if [ -n "$pid" ]; then
    echo "Stopping process $pid - $jar"
    kill "$pid"
fi

while :
do
    java -jar -Xmx2g $jar
    if [ $? -ne 0 ]; then
        break
    fi
done