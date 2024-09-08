<img alt="mikan-pic.png" height="80" src="https://docs.wushuo.top/image/mikan-pic.png"/>

## 快速开始

### 安装 java

推荐使用 jdk11 或 jdk17
如果已经有了可以跳过

##### windows

    winget install 'OpenJDK 11'

##### linux

    sudo apt update
    sudo apt install openjdk-11-jdk

#### macOS

    brew update
    brew install openjdk@11

### 运行

下载最新的 **ani-rss-jar-with-dependencies.jar** [链接](https://github.com/wushuo894/ani-rss/releases/latest)

    java -jar -Xmx2g ./ani-rss-jar-with-dependencies.jar --port 7789

#### 脚本运行 支持自动重启

    wget https://github.com/wushuo894/ani-rss/raw/master/docker/run.sh
    sudo chmod -R 777 ./run.sh
    sudo bash ./run.sh

通过 **http://[ip]:7789** 访问

<a href="docs">使用文档</a>
|
<a href="docker">Docker部署</a>

