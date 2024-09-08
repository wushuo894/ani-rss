#!/bin/bash

mkdir target
native-image -H:+StaticExecutableWithDynamicLibC -march=compatibility -cp ./ani-rss-jar-with-dependencies.jar ani.rss.Main -o target/ani-rss --no-fallback