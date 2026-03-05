FROM wushuo894/eclipse-temurin:25-jre-alpine

COPY docker/run.sh /run.sh
COPY docker/exec.sh /exec.sh
COPY ani-rss-application/target/ani-rss.jar /usr/app/ani-rss.jar
WORKDIR /usr/app
VOLUME /config
ENV PUID=0 PGID=0 UMASK=022
ENV SERVER_PORT=7789 CONFIG=/config TZ=Asia/Shanghai SWAGGER_ENABLED=false
EXPOSE $SERVER_PORT
RUN apk add --no-cache ffmpeg \
    && sed -i 's/\r$//' /exec.sh /run.sh \
    && chmod +x /exec.sh /run.sh
CMD ["/exec.sh"]
