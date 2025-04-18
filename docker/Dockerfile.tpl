FROM ibm-semeru-runtimes:open-17-jre
COPY docker/run.sh /run.sh
COPY target/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /config
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE $PORT
RUN chmod +x /run.sh
CMD ["/run.sh"]
