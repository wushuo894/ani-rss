#!/bin/bash
wget https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-17.0.9/graalvm-community-jdk-17.0.9_linux-x64_bin.tar.gz

tar -xvf graalvm-community-jdk-17.0.9_linux-x64_bin.tar.gz
export PATH="$PATH:/home/runner/work/ani-rss/ani-rss/graalvm-community-openjdk-17.0.9+9.1/bin"

native-image -H:+StaticExecutableWithDynamicLibC -march=compatibility -cp ./ani-rss-jar-with-dependencies.jar ani.rss.Main -o /target/ani-rss