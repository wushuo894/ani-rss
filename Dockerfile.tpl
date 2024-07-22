FROM openjdk:11-jdk
COPY target/pornhub-download-jar-with-dependencies.jar /usr/app/pornhub-download-jar-with-dependencies.jar
COPY ffmpeg-release-amd64-static.tar.xz /usr/app/ffmpeg-release-amd64-static.tar.xz
WORKDIR /usr/app
VOLUME /config
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789
CMD ["java", "-jar", "ani-rss-jar-with-dependencies.jar"]
