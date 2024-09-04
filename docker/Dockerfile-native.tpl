FROM alpine:edge as builder
COPY target/ani-rss /usr/app/ani-rss
COPY docker/run.sh /usr/app/run.sh
WORKDIR /usr/app
VOLUME /config
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789
RUN chmod +x /usr/app/run.sh
CMD ["/usr/app/run.sh"]
