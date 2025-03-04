#!/bin/bash

cd target

mkdir ani-rss
cp ani-rss-launcher.exe ani-rss

sudo apt update
sudo apt install zip unzip
wget https://github.com/ojdkbuild/ojdkbuild/releases/download/java-17-openjdk-17.0.3.0.6-1/java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64.zip
unzip java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64.zip
mv java-17-openjdk-17.0.3.0.6-1.jre.win.x86_64 ani-rss/jre
cp ../windows/* ani-rss
zip -r ani-rss.win.x86_64.zip ani-rss

md5sum ani-rss.win.x86_64.zip | awk '{print $1}' > ani-rss.win.x86_64.zip.md5