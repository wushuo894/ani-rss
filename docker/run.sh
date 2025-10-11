#!/bin/bash

port="7789"
path="./"
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

while :
do
    java -Xms60m -Xmx1g -Xss256k \
      -Xgcpolicy:gencon \
      -Xshareclasses:none \
      -Xquickstart -Xcompressedrefs \
      -Xtune:virtualized \
      -XX:+UseStringDeduplication \
      -XX:-ShrinkHeapInSteps \
      -XX:TieredStopAtLevel=1 \
      -XX:+IgnoreUnrecognizedVMOptions \
      --enable-native-access=ALL-UNNAMED \
      --add-opens=java.base/java.net=ALL-UNNAMED \
      --add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED \
      -jar $jar_path --port $port &
    wait $!
    if [ $? -ne 0 ]; then
        break
    fi
done

exit 0
