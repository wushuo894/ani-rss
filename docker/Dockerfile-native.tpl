FROM oraclelinux:7-slim
COPY target/ani-rss /usr/app/ani-rss
COPY docker/run.sh /run.sh
WORKDIR /usr/app
VOLUME /config
RUN chmod +x /run.sh
ENV PORT="7789"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7789

# Update the package repository and install necessary tools
RUN yum update -y && yum install -y wget

# Download and install GLIBC 2.34
RUN wget https://ftp.gnu.org/gnu/glibc/glibc-2.34.tar.gz
RUN tar -xzvf glibc-2.34.tar.gz
WORKDIR glibc-2.34
RUN mkdir build
WORKDIR build
RUN ../configure
RUN make
RUN make install

CMD ["/run.sh"]
