FROM ubuntu:24.10
COPY target/ani-rss /usr/app/ani-rss
WORKDIR /usr/app
VOLUME /config
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789

CMD ["/usr/app/ani-rss"]
