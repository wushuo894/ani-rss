<div align="center">
<img alt="mikan-pic.png" height="80" src="https://github.com/wushuo894/ani-rss/raw/master/image/mikan-pic.png"/>
<h1 align="center" style="margin-top: 0">ANI-RSS</h1>
<p align="center"><strong>自动订阅下载蜜柑RSS动漫</strong></p>

![GitHub License](https://img.shields.io/github/license/wushuo894/ani-rss)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/wushuo894/ani-rss/maven.yml?branch=master)](https://github.com/wushuo894/ani-rss/actions/workflows/maven.yml)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/wushuo894/ani-rss?color=blue&label=download&sort=semver)](https://github.com/wushuo894/ani-rss/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/wushuo894/ani-rss/total?color=blue&label=github%20downloads)](https://github.com/wushuo894/ani-rss/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/wushuo894/ani-rss)](https://hub.docker.com/r/wushuo894/ani-rss)

</div>

[使用文档](DOCS.md)

### 实现功能

- [x] 自动下载
- [x] 自动识别季数
- [x] 自动重命名
- [x] 文件已下载自动跳过
- [x] 自动删除已完成任务
- [x] 自定义 qBittorrent 设置
- [x] RSS 关键字过滤
- [x] 集数偏移
- [x] 自动识别集数偏移
- [x] 自定义间隔
- [x] 适配移动端
- [x] 支持拼音排序与搜索
- [x] 支持自定义代理

![screenshot.jpg](https://github.com/wushuo894/ani-rss/raw/master/image/screenshot.jpg)

默认 用户名: admin 密码: admin

### docker 部署

    docker run -d --name ani-rss -v /volume1/docker/ani-rss/config:/config -p 7789:7789 -e PORT="7789" -e CONFIG="/config" -e TZ=Asia/Shanghai --restart always wushuo894/ani-rss

| 参数     | 作用       | 默认值           |
|--------|----------|---------------|
| PORT   | 端口号      | 9877          |
| CONFIG | 配置文件存放位置 | /config       |
| TZ     | 时区       | Asia/Shanghai |

ps: 如果需要开启 文件已下载自动跳过 功能 请确保 qBittorrent 与本程序 docker 映射挂载路径一致

### 蜜柑

已被墙

[https://mikanani.me/](https://mikanime.tv/)

未被墙

[https://mikanime.tv/](https://mikanime.tv/)
