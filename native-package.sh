#!/bin/bash
/bin/bash ./package.sh
cd target
native-image -cp ./ani-rss-jar-with-dependencies.jar ani.rss.Main -o ani-rss -O2 -H:-CheckToolchain -H:+StaticExecutableWithDynamicLibC -H:+ReportExceptionStackTraces --static --libc=musl
cd ..