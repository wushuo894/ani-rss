# 使用文档

## qBittorrent 设置

![Xnip2024-09-08_04-57-51.jpg](https://github.com/wushuo894/ani-rss/raw/master/image/Xnip2024-09-08_04-57-51.jpg)

### 拼音首字母

用于整理番剧到A-Z文件夹中
如:

    ├─A
    │  └─安达与岛村
    │      └─S01
    │              安达与岛村 S01E01.mp4
    │              安达与岛村 S01E02.mp4
    │              安达与岛村 S01E03.mp4
    │
    ├─B
    │  ├─BanG Dream! It's MyGO!!!!!
    │  │  └─S01
    │  │          BanG Dream! It's MyGO!!!!! S01E01.mp4
    │  │          BanG Dream! It's MyGO!!!!! S01E02.mp4
    │  │          BanG Dream! It's MyGO!!!!! S01E03.mp4

### 同时下载数量限制

防止同时下载任务过多导致qb卡死

强烈建议视性能设置, 推荐 1-2

## 基本设置

![Xnip2024-09-08_04-58-04.jpg](https://github.com/wushuo894/ani-rss/raw/master/image/Xnip2024-09-08_04-58-04.jpg)

### RSS间隔(分钟)

RSS更新检查的间隔，单位 分钟

### 自动重命名

自动命名视频与字幕让其易于刮削

如:

    2024-09-01 13:29:06.865 [rss-task-thread] INFO  ani.rss.util.TorrentUtil - 添加下载 Wonderful 光之美少女！ S01E31
    2024-09-01 13:29:06.866 [rss-task-thread] INFO  ani.rss.util.TorrentUtil - 下载种子 Wonderful 光之美少女！ S01E31
    2024-09-01 13:29:46.352 [rename-task-thread] INFO  ani.rss.util.TorrentUtil - 重命名 [FLsnow][Wonderful_Precure！][31][1080P]/[FLsnow][Wonderful_Precure！][31][1080P].mkv ==> Wonderful 光之美少女！ S01E31.mkv
    2024-09-01 13:29:46.362 [rename-task-thread] INFO  ani.rss.util.TorrentUtil - 重命名 [FLsnow][Wonderful_Precure！][31][1080P]/[FLsnow][Wonderful_Precure！][31][1080P].cht.ass ==> Wonderful 光之美少女！ S01E31.cht.ass
    2024-09-01 13:29:46.365 [rename-task-thread] INFO  ani.rss.util.TorrentUtil - 重命名 [FLsnow][Wonderful_Precure！][31][1080P]/[FLsnow][Wonderful_Precure！][31][1080P].chs.ass ==> Wonderful 光之美少女！ S01E31.chs.ass
    2024-09-01 13:38:49.392 [rename-task-thread] INFO  ani.rss.util.TorrentUtil - 删除已完成任务 Wonderful 光之美少女！ S01E31

### 重命名间隔(分钟)

单位 分钟

### 自动跳过

自动检测季度文件夹下是否已经下载某集

支持的命名:

    ├─A
    │  └─安达与岛村
    │      ├─S1
    │      │       安达与岛村 S1E1.mp4
    │      ├─S01
    │      │       安达与岛村 S01E02.mp4
    │      ├─Season 1
    │      │       S1E3.mp4
    │      └─Season 01
    │              S01E04.mp4
    │              安达与岛村(2020) S1E5.mp4
    │              安达与岛村(2020) S01E06.mp4

PS: 此选项必须启用 自动重命名。确保 qBittorrent 与 ani-rss 的 docker 映射挂载路径一致

示例：
![Xnip2024-09-07_13-40-34.jpg](https://github.com/wushuo894/ani-rss/raw/master/image/Xnip2024-09-07_13-40-34.jpg)
![Xnip2024-09-07_13-40-08.jpg](https://github.com/wushuo894/ani-rss/raw/master/image/Xnip2024-09-07_13-40-08.jpg)
确保 qBittorrent 与 ani-rss 的 docker 映射挂载路径一致

### 自动删除已完成任务

当qb中已下载并完成做种后删除任务，不会删除本地文件

### 自动推断剧集偏移

当添加RSS时会根据最小集数计算出集数偏移

如 无职转生 第二季 是从 第 0 集开始的 则自动识别偏移为 1

如 我推的孩子 第二季 有些字幕组是从第 12 集开始的 则自动识别偏移为 -11

主要看你的个人喜好决定是否开启

### 自动禁用订阅

根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅

### DEBUG

可以看到 debug 级别的日志

    2024-09-03 14:26:01 INFO ani.rss.util.TorrentUtil - 已下载 深夜冲击 S01E01
    2024-09-03 14:26:01 INFO ani.rss.util.TorrentUtil - 下载种子 深夜冲击 S01E01
    2024-09-03 14:26:02 DEBUG ani.rss.util.TorrentUtil - 本地文件已存在 深夜冲击 S01E01
    2024-09-03 14:26:02 DEBUG ani.rss.util.TorrentUtil - {
    "title": "[ANi] Mayonaka Punch /  深夜 Punch - 02 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
    "reName": "深夜冲击 S01E02",
    "torrent": "https://mikanani.me/Download/20240715/1ef6e3168b72cef9a30edb9b97490158629ba7d0.torrent",
    "episode": 2
    }
    2024-09-03 14:26:02 INFO ani.rss.util.TorrentUtil - 已下载 深夜冲击 S01E02
    2024-09-03 14:26:02 INFO ani.rss.util.TorrentUtil - 下载种子 深夜冲击 S01E02
    2024-09-03 14:26:03 DEBUG ani.rss.util.TorrentUtil - 本地文件已存在 深夜冲击 S01E02
    2024-09-03 14:26:03 DEBUG ani.rss.util.TorrentUtil - {
    "title": "[ANi] Mayonaka Punch /  深夜 Punch - 03 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
    "reName": "深夜冲击 S01E03",
    "torrent": "https://mikanani.me/Download/20240722/ec1ec2faf5356d4b363e42d99b799d31450bc34d.torrent",
    "episode": 3
    }

## 代理设置

主要用与访问 mikan

## 登录设置

设置网页的账号与密码

## 邮件通知

当有新番开始下载时向邮箱发送通知

![Xnip2024-09-08_04-58-16.jpg](https://github.com/wushuo894/ani-rss/raw/master/image/Xnip2024-09-08_04-58-16.jpg)

### QQ邮箱设置示例

    SMTP地址: smtp.qq.com
    SMTP端口: 465
    发件人邮箱: [qq号]@qq.com
    密码: xxxx
    SSL: 开启
    收件人邮箱: [qq号]@qq.com

### QQ邮箱密码:

QQ邮箱 - 常规 - 第三方服务 - IMAP/SMTP服务

开启并 生成授权码

## 关于剧场版

若 `剧场版保存位置` 为空则使用 `保存位置`

由于剧场版命名与文件结构各异, 故并不会使用重命名功能


[快速开始](START.md) | [Docker 部署](DOCKER.md)
