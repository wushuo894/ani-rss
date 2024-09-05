FROM scratch
COPY target/ani-rss-x86_64-linux-musl /ani-rss
WORKDIR /
VOLUME /config
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789

CMD ["/ani-rss"]
