#!/bin/bash

cd target

mkdir ani-rss
cp ani-rss-launcher.exe ani-rss

sudo apt update
sudo apt install zip unzip
wget https://github.com/ibmruntimes/semeru17-binaries/releases/download/jdk-17.0.14%2B7_openj9-0.49.0/ibm-semeru-open-jre_x64_windows_17.0.14_7_openj9-0.49.0.zip
unzip ibm-semeru-open-jre_x64_windows_17.0.14_7_openj9-0.49.0.zip
mv jdk-17.0.14+7-jre ani-rss/jre
cp ../windows/* ani-rss
zip -r ani-rss.win.x64.zip ani-rss

md5sum ani-rss.win.x64.zip | awk '{print $1}' > ani-rss.win.x64.zip.md5