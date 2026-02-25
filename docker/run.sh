#!/bin/sh

export LANG=C.UTF-8
export LC_ALL=C.UTF-8

path="./"
jar="ani-rss.jar"
jar_path=$path$jar

if [ ! -f $jar_path ]; then
    url="https://github.com/wushuo894/ani-rss/releases/latest/download/ani-rss.jar"
    wget -O $jar_path $url

    if [ $? -eq 0 ]; then
        echo "$jar_path 下载成功！"
    else
        echo "$jar_path 下载失败。"
    fi
fi

stop() {
  pid=$(pgrep -f "$jar")
  if [ -n "$pid" ]; then
      echo "Stopping process $pid - $jar"
      kill "$pid"
      wait "$pid"
  fi
}

stop

sigterm_handler() {
    stop
}

trap 'sigterm_handler' 15

while :
do
    java -Xms60m -Xmx1g -Xss256k \
      -Dfile.encoding=UTF-8 \
      -Xgcpolicy:gencon \
      -Xshareclasses:none \
      -Xquickstart -Xcompressedrefs \
      -Xtune:virtualized \
      -XX:+UseStringDeduplication \
      -XX:-ShrinkHeapInSteps \
      -XX:TieredStopAtLevel=1 \
      -XX:+IgnoreUnrecognizedVMOptions \
      -XX:+UseCompactObjectHeaders \
      --enable-native-access=ALL-UNNAMED \
      --add-opens=java.base/java.net=ALL-UNNAMED \
      --add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED \
      -jar $jar_path&
    wait $!
    if [ $? -ne 0 ]; then
      break
    fi
done

exit 0
