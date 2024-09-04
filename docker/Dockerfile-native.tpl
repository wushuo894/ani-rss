FROM oraclelinux:7-slim
COPY target/ani-rss /usr/app/ani-rss
COPY docker/run.sh /run.sh
WORKDIR /usr/app
VOLUME /config
RUN chmod +x /run.sh
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
ENV PUID=0 PGID=0 UMASK=022
EXPOSE 7789
CMD ["/run.sh"]
