#!/bin/bash

while :
do
    java -jar ani-rss-jar-with-dependencies.jar
    if [ $? -ne 0 ]; then
        break
    fi
done