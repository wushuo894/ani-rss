FROM alpine:edge
COPY target/ani-rss /usr/app/ani-rss
COPY docker/run.sh /run.sh
WORKDIR /usr/app
VOLUME /config
RUN apk update && \
    apk upgrade --no-cache && \
    apk add --no-cache bash ca-certificates su-exec tzdata libc6; \
    chmod +x /run.sh && \
    rm -rf /var/cache/apk/*
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
ENV PUID=0 PGID=0 UMASK=022
EXPOSE 7789
CMD ["/run.sh"]
