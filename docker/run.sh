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

if [ -z "$JAVA_OPTS" ]; then
  export JAVA_OPTS="-Xms64m -Xmx512m -Xss256k -XX:+UseG1GC"
fi

echo "JAVA_OPTS=$JAVA_OPTS"

while :
do
    java $JAVA_OPTS \
      -XX:+UseStringDeduplication \
      -XX:+UseCompactObjectHeaders \
      -XX:TieredStopAtLevel=1 \
      -XX:+IgnoreUnrecognizedVMOptions \
      --enable-native-access=ALL-UNNAMED \
      --add-opens=java.base/java.net=ALL-UNNAMED \
      --add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED \
      -Dfile.encoding=UTF-8 \
      -jar $JAR_FILE&
    wait $!
    if [ $? -ne 0 ]; then
      break
    fi
done

exit 0
