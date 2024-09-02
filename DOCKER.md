<img alt="mikan-pic.png" height="80" src="https://github.com/wushuo894/ani-rss/raw/master/image/mikan-pic.png"/>

# ANI-RSS

[GitHub](https://github.com/wushuo894/ani-rss)

默认 用户名: admin 密码: admin

### docker 部署

    docker run -d --name ani-rss -v /volume1/docker/ani-rss/config:/config -p 7789:7789 -e PORT="7789" -e CONFIG="/config" -e TZ=Asia/Shanghai --restart always wushuo894/ani-rss

| 参数     | 作用       | 默认值           |
|--------|----------|---------------|
| PORT   | 端口号      | 9877          |
| CONFIG | 配置文件存放位置 | /config       |
| TZ     | 时区       | Asia/Shanghai |

ps: 如果需要开启 文件已下载自动跳过 功能 请确保 qBittorrent 与本程序 docker 映射挂载路径一致