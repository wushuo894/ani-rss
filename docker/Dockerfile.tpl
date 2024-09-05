FROM eclipse-temurin:11-jre
COPY target/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /config
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789
RUN ln -s /opt/java/openjdk /usr/local/openjdk-11
CMD ["java", "-jar", "ani-rss-jar-with-dependencies.jar"]
