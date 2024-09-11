FROM eclipse-temurin:11-jre
COPY docker/run.sh /run.sh
COPY target/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /config
ENV PORT=7789
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
ENV AUTO_UPDATE="false"
EXPOSE $PORT
RUN ln -s /opt/java/openjdk /usr/local/openjdk-11
RUN chmod +x /run.sh
CMD ["/run.sh"]
