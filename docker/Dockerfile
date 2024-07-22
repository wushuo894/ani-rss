FROM wushuo894/openj9:open-21-jre

COPY docker/run.sh /run.sh
COPY docker/exec.sh /exec.sh
COPY target/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /config
ENV PUID=0 PGID=0 UMASK=022
ENV PORT=7789 CONFIG=/config TZ=Asia/Shanghai
EXPOSE $PORT
RUN chmod +x /exec.sh /run.sh
CMD ["/exec.sh"]
