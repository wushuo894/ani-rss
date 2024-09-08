#!/bin/bash

while :
do
    java -jar -Xmx2g ani-rss-jar-with-dependencies.jar
    if [ $? -ne 0 ]; then
        break
    fi
done