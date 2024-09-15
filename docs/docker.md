<img alt="mikan-pic.png" height="80" src="https://docs.wushuo.top/image/mikan-pic.png"/>

## ani-rss

![Docker Image Size (tag)](https://img.shields.io/docker/image-size/wushuo894/ani-rss/latest)
[![Docker Pulls](https://img.shields.io/docker/pulls/wushuo894/ani-rss)](https://hub.docker.com/r/wushuo894/ani-rss)
[![qq-qun](https://img.shields.io/static/v1?label=QQ%E7%BE%A4&message=171563627&color=blue)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=_EKAkxs6Ld4fWcMNAbUQzcp4tv20vjVH&authKey=KG3GAsZfKQosbAWkks%2FbEj0LCGwxoeLJ3DTU0loHkGdHLqHYgJNv3%2BmSERmYt47b&noverify=0&group_code=171563627)
[![GitHub](https://img.shields.io/badge/-GitHub-181717?logo=github)](https://github.com/wushuo894/ani-rss)

[使用文档](https://docs.wushuo.top/docs)

![Xnip2024-09-08_04-57-29.jpg](https://docs.wushuo.top/image/Xnip2024-09-08_04-57-29.jpg)

默认 用户名: admin 密码: admin

| 参数     | 作用       | 默认值           |
|--------|----------|---------------|
| PORT   | 端口号      | 7789          |
| CONFIG | 配置文件存放位置 | /config       |
| TZ     | 时区       | Asia/Shanghai |

ps: 如果需要开启 文件已下载自动跳过 功能 请确保 qBittorrent 与本程序 docker 映射挂载路径一致

### Docker 部署

    docker run -d \
    --name ani-rss \
    -v /volume1/docker/ani-rss/config:/config \
    -v /volume2/Media/:/Media \
    -p 7789:7789 \
    -e PORT="7789" \
    -e CONFIG="/config" \
    -e TZ=Asia/Shanghai \
    --restart always \
    wushuo894/ani-rss

## Docker Compose部署

创建docker-compose.yml文件，内容如下

```yaml
version: "3"
services:
  ani-rss:
    container_name: ani-rss
    volumes:
      - /volume1/docker/ani-rss/config:/config
      - /volume2/Media/:/Media
    ports:
      - 7789:7789
    environment:
      - PORT=7789
      - CONFIG=/config
      - TZ=Asia/Shanghai
    restart: always
    image: wushuo894/ani-rss
```

启动容器

```shell
docker compose up -d
```


<hr style="height: 1px;">
<div>
<img src="https://docs.wushuo.top/image/tr.png" alt="transmission" width="60">
<img src="https://docs.wushuo.top/image/qb.png" alt="qbittorrent" width="60">
<img src="https://docs.wushuo.top/image/aria2.png" alt="qbittorrent" width="60">

<p>支持 <strong>Transmission</strong> <strong>qBittorrent</strong> <strong>Aria2</strong></p>
</div>

[猫猫博客 Docker 部署 ani-rss 实现自动追番](https://catcat.blog/docker-ani-rss.html)