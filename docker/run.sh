#!/bin/sh

export LANG=C.UTF-8
export LC_ALL=C.UTF-8

FOLDER="./"
JAR_FILE_NAME="ani-rss.jar"
JAR_FILE=$FOLDER$JAR_FILE_NAME

if [ ! -f $JAR_FILE ]; then
    URL="https://github.com/wushuo894/ani-rss/releases/latest/download/ani-rss.jar"
    wget -O $JAR_FILE $URL

    if [ $? -eq 0 ]; then
        echo "$JAR_FILE 下载成功！"
    else
        echo "$JAR_FILE 下载失败。"
    fi
fi

stop() {
  PID=$(pgrep -f "$JAR_FILE_NAME")
  if [ -n "$PID" ]; then
      echo "Stopping process $PID - $JAR_FILE_NAME"
      kill "$PID"
      wait "$PID"
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
      -jar $JAR_FILE&
    wait $!
    if [ $? -ne 0 ]; then
      break
    fi
done

exit 0
