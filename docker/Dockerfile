FROM eclipse-temurin:25-jdk-alpine AS jre-builder

# 使用 jlink 创建一个只包含必要模块的自定义 JRE
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.sql,java.sql.rowset,java.xml,jdk.httpserver,jdk.naming.dns,jdk.unsupported \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2 \
    --output /custom-jre

COPY target/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar

ARG gosuVer=1.19

# 处理jar文件，并下载gosu
RUN apk update && apk add --no-cache 7zip ca-certificates wget && \
    7zz x -o/tmp/ /usr/app/ani-rss-jar-with-dependencies.jar && \
    7zz d /usr/app/ani-rss-jar-with-dependencies.jar ani-rss-update.exe org/sqlite/native && \
    ARCH=$(apk --print-arch) && \
    cp /tmp/org/sqlite/native/Linux-Musl/${ARCH}/libsqlitejdbc.so /custom-jre/lib/ && \
    case $ARCH in \
        x86_64) gosuArch="amd64" ;; \
        aarch64) gosuArch="arm64" ;; \
        *) echo "Unsupported architecture: $ARCH" && exit 1 ;; \
    esac && \
    wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/${gosuVer}/gosu-${gosuArch}" && \
    chmod +x /usr/local/bin/gosu && \
    gosu --version


FROM alpine:latest

# 从 jre-builder 阶段拷贝自定义JRE、处理好的jar、gosu
COPY --from=jre-builder /custom-jre /usr/lib/jvm/custom-jre
COPY --from=jre-builder /usr/local/bin/gosu /usr/local/bin/gosu
COPY --from=jre-builder /usr/app/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar

# 启动脚本
COPY docker/run.sh /run.sh
COPY docker/docker-entrypoint.sh /docker-entrypoint.sh

RUN apk add --no-cache ca-certificates wget && \
    chmod +x /docker-entrypoint.sh /run.sh

WORKDIR /usr/app
VOLUME /config
ENV JAVA_HOME=/usr/lib/jvm/custom-jre
ENV PATH="$JAVA_HOME/bin:$PATH"
ENV PUID=0 PGID=0 UMASK=022
ENV PORT=7789 CONFIG=/config TZ=Asia/Shanghai
EXPOSE $PORT
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/run.sh"]
