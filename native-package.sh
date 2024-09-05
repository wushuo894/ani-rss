#!/bin/bash
/bin/bash ./package.sh
cd /home/runner/work/ani-rss/ani-rss/target
native-image -march=compatibility -cp ./ani-rss-jar-with-dependencies.jar ani.rss.Main -o ani-rss
cd ..