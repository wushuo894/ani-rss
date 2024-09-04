#!/bin/bash
/bin/bash ./package.sh
cd target
pwd
ls -al
native-image -cp ./ani-rss-jar-with-dependencies.jar ani.rss.Main -o ani-rss