FROM alpine:edge
COPY target/ani-rss /usr/app/ani-rss
COPY docker/run.sh /run.sh
WORKDIR /usr/app
VOLUME /config
RUN chmod +x /run.sh
RUN ls / -al
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789
CMD ["/bin/sh","/run.sh"]
