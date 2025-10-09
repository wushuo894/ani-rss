#!/bin/sh

path="./"
jar="ani-rss-jar-with-dependencies.jar"
jar_path=$path$jar

exec java -Xms60m -Xmx1g -Xss256k \
      -Xgcpolicy:gencon \
      -Xquickstart -Xcompressedrefs \
      -Xtune:virtualized \
      -XX:+UseStringDeduplication \
      -XX:-ShrinkHeapInSteps \
      -XX:TieredStopAtLevel=1 \
      -XX:+IgnoreUnrecognizedVMOptions \
      --add-opens=java.base/java.net=ALL-UNNAMED \
      --add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED \
      -jar $jar_path --port ${PORT}
