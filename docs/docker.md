<img alt="mikan-pic.png" height="80" src="https://docs.wushuo.top/image/mikan-pic.png"/>

## ani-rss

![Docker Image Size (tag)](https://img.shields.io/docker/image-size/wushuo894/ani-rss/latest)
[![Docker Pulls](https://img.shields.io/docker/pulls/wushuo894/ani-rss)](https://hub.docker.com/r/wushuo894/ani-rss)
[![qq-qun](https://img.shields.io/static/v1?label=QQ%E7%BE%A4&message=171563627&color=blue)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=_EKAkxs6Ld4fWcMNAbUQzcp4tv20vjVH&authKey=KG3GAsZfKQosbAWkks%2FbEj0LCGwxoeLJ3DTU0loHkGdHLqHYgJNv3%2BmSERmYt47b&noverify=0&group_code=171563627)
[![GitHub](https://img.shields.io/badge/-GitHub-181717?logo=github)](https://github.com/wushuo894/ani-rss)

[使用文档](https://docs.wushuo.top/docs)

![Xnip2024-10-15_20-41-53.jpg](https://docs.wushuo.top/image/Xnip2024-10-15_20-41-53.jpg)

默认 用户名: admin 密码: admin

| 参数     | 作用       | 默认值           |
|--------|----------|---------------|
| PORT   | 端口号      | 7789          |
| CONFIG | 配置文件存放位置 | /config       |
| TZ     | 时区       | Asia/Shanghai |

ps: 如果需要开启 文件已下载自动跳过 功能 请确保 qBittorrent 与本程序 docker 映射挂载路径一致

### 视频教程

[阿里云盘](https://www.alipan.com/s/eqt2XLZJThu)

<iframe width="560" height="315" src="https://www.youtube.com/embed/y9-mgvnSnxs?si=CCz_58LaZu3mbpr5" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>

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

## Docker Compose 部署

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

### Container Manager 部署

![Xnip2024-09-26_20-25-16.jpg](https://docs.wushuo.top/image/Xnip2024-09-26_20-25-16.jpg)

在注册表搜索 **ani-rss** 选择第一个 wushuo894/ani-rss 并下载

![Xnip2024-09-26_20-26-21.jpg](https://docs.wushuo.top/image/Xnip2024-09-26_20-26-21.jpg)

下载后点击运行

![Xnip2024-09-26_20-38-32.jpg](https://docs.wushuo.top/image/Xnip2024-09-26_20-38-32.jpg)

修改容器名

![Xnip2024-09-26_20-28-32.jpg](https://docs.wushuo.top/image/Xnip2024-09-26_20-28-32.jpg)

配置端口 默认7789
设置 配置文件存放位置 /config
设置 番剧存放位置(和你的QB保持一致,/Media 仅作示例) /Media

![Xnip2024-09-26_20-39-43.jpg](https://docs.wushuo.top/image/Xnip2024-09-26_20-39-43.jpg)

确认配置无误后点击完成

通过 **http://[ip]:7789** 即可访问

默认 用户名:admin 密码:admin

<hr style="height: 1px;">
<div>
<img src="https://docs.wushuo.top/image/tr.png" alt="transmission" width="60">
<img src="https://docs.wushuo.top/image/qb.png" alt="qbittorrent" width="60">
<img src="https://docs.wushuo.top/image/aria2.png" alt="qbittorrent" width="60">

<p>支持 <strong>Transmission</strong> <strong>qBittorrent</strong> <strong>Aria2</strong></p>
</div>

### 相关文章:

[猫猫博客 Docker 部署 ani-rss 实现自动追番](https://catcat.blog/docker-ani-rss.html)

[从零开始的NAS生活 第四回：ANI-RSS，自动追番！](https://www.wtsss.fun/archives/qhaQ3M7v)

[自动化追番计划](http://jinghuashang.cn/posts/8f622332.html)

[ANI-RSS：自动追番新姿势！](https://www.himiku.com/archives/ani-rss.html)
