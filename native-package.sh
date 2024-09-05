#!/bin/bash

# 创建一个工具存储目录
mkdir /usr/local/graal-aot-tools
# 进入到这个目录
cd /usr/local/graal-aot-tools
# 下载 x86_64-linux-musl 工具链
wget http://more.musl.cc/10/x86_64-linux-musl/x86_64-linux-musl-native.tgz
# 解压下载到的 musl 工具链包
tar -zxvf x86_64-linux-musl-native.tgz
# 复制文件到工作根目录
cp -r x86_64-linux-musl-native/* .
# 下载 zlib 依赖
wget https://zlib.net/current/zlib.tar.gz
# 解压 zlib 依赖源码包
tar -zxvf zlib.tar.gz

export TOOLCHAIN_DIR="/usr/local/graal-aot-tools"
export PATH="$TOOLCHAIN_DIR/bin:$PATH"
export CC="$TOOLCHAIN_DIR/bin/gcc"

ls -al
cd zlib-*
./configure --prefix=$TOOLCHAIN_DIR --static
make
make install

cd /home/runner/work/ani-rss/ani-rss

/bin/bash ./package.sh
cd /home/runner/work/ani-rss/ani-rss/target
native-image --gc=G1 --static --libc=musl --no-fallback -march=compatibility -cp ./ani-rss-jar-with-dependencies.jar ani.rss.Main -o ani-rss -O2 -H:-CheckToolchain -H:+StaticExecutableWithDynamicLibC -H:+ReportExceptionStackTraces
cd ..