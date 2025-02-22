#!/bin/bash

port="7789"
path="./"
jdk_version="17"
java_path="/usr/bin/java"
jar="ani-rss-jar-with-dependencies.jar"
jar_path=$path$jar

if [ ! -f $jar_path ]; then
    url="https://github.com/wushuo894/ani-rss/releases/latest/download/ani-rss-jar-with-dependencies.jar"
    wget -O $jar_path $url

    if [ $? -eq 0 ]; then
        echo "$jar_path 下载成功！"
    else
        echo "$jar_path 下载失败。"
    fi
fi

stop() {
  pid=$(ps -ef | grep java | grep "$jar" | awk '{print $2}')
  if [ -n "$pid" ]; then
      echo "Stopping process $pid - $jar"
      kill "$pid"
  fi
}

stop

sigterm_handler() {
    stop
    exit 0
}

trap 'sigterm_handler' SIGTERM

update_jdk(){
    get_jdk_version() {
        $java_path -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}'
    }

    check_jdk_version() {
        current_jdk_version=$(get_jdk_version)
        echo "需要JDK版本为 $jdk_version"
        echo "当前JDK版本为 $current_jdk_version"
        if [ "$current_jdk_version" -lt "$jdk_version" ]; then
            return 1
        fi
        return 0
    }

    download_jdk(){
    local jdk_version="$1"
        set -e
        echo "尝试下载JDK $jdk_version"

        local arch=$(uname -m)
        local jdk_url
        if [ "$arch" == "x86_64" ]; then
            jdk_url="https://mirrors.huaweicloud.com/openjdk/${jdk_version}/openjdk-${jdk_version}_linux-x64_bin.tar.gz"
        else
            jdk_url="https://mirrors.huaweicloud.com/openjdk/${jdk_version}/openjdk-${jdk_version}_linux-aarch64_bin.tar.gz"
        fi

        rm -rf "./jdk"
        mkdir -p "./jdk"

        tar_file="./jdk/jdk-$jdk_version.tar.gz"
        if ! wget -O "$tar_file" "$jdk_url"; then
            echo "下载JDK $jdk_version 失败，请检查网络连接或URL是否正确。" >&2
            return 1
        fi

        if ! tar -zxvf "$tar_file" -C "./jdk"; then
            echo "解压JDK $jdk_version 失败，请检查压缩包是否损坏。" >&2
            return 1
        fi

        rm "$tar_file"
        mv -f "./jdk/jdk-$jdk_version" "./jdk/$jdk_version"

        java_path="./jdk/$jdk_version/bin/java"
        
        if check_jdk_version; then
            echo "JDK $jdk_version 下载成功"
            return 0
        else
            echo "下载的JDK版本不符合要求"
            exit 1
        fi
    }

    check_local_jdk() {
        local_jdk_dir="./jdk/$jdk_version"
        if [ -f "$local_jdk_dir/bin/java" ]; then
            echo "在目录中找到jdk，使用该jdk"
            java_path="$local_jdk_dir/bin/java"
            echo "java_path: $java_path"
            if check_jdk_version; then
                echo "JDK版本符合要求"
            else
                echo "JDK版本不符合要求"
                download_jdk "$jdk_version"
            fi
        else
            echo "未在当前目录找到jdk，下载并安装"
            download_jdk "$jdk_version"
        fi
    }

    if ! command -v java &> /dev/null; then
        echo "环境中未找到 java 命令"
        check_local_jdk
    else
        echo "环境中已找到 java 命令，检查版本"
        java_path=$(which java)
        if check_jdk_version; then
            echo "JDK版本符合要求"
        else
            echo "JDK版本不符合要求"
            check_local_jdk
        fi
    fi
}

while :
do
    update_jdk
    $java_path -Xms50m -Xmx1g -Xss512k -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ShrinkHeapInSteps -jar $jar_path --port $port &
    wait $!
    if [ $? -ne 0 ]; then
        break
    fi
done

exit 0
