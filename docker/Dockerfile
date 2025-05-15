FROM ibm-semeru-runtimes:open-17-jre

# Install latest su-exec
RUN  set -ex; \
     \
     curl -o /usr/local/bin/su-exec.c https://raw.githubusercontent.com/ncopa/su-exec/master/su-exec.c; \
     \
     fetch_deps='gcc libc-dev'; \
     apt-get update; \
     apt-get install -y --no-install-recommends $fetch_deps; \
     rm -rf /var/lib/apt/lists/*; \
     gcc -Wall \
         /usr/local/bin/su-exec.c -o/usr/local/bin/su-exec; \
     chown root:root /usr/local/bin/su-exec; \
     chmod 0755 /usr/local/bin/su-exec; \
     rm /usr/local/bin/su-exec.c; \
     \
     apt-get purge -y --auto-remove $fetch_deps

COPY docker/run.sh /run.sh
COPY docker/exec.sh /exec.sh
COPY target/ani-rss-jar-with-dependencies.jar /usr/app/ani-rss-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /config
ENV PUID=0 PGID=0 UMASK=022
ENV PORT=7789 CONFIG=/config TZ=Asia/Shanghai
EXPOSE $PORT
RUN chmod +x /exec.sh /run.sh
CMD ["/exec.sh"]
