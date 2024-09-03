# Docs

## qBittorrent 设置

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

## 基本设置

### 间隔(分钟)

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

### 文件已下载自动跳过

自动检测季度文件夹下是否已经下载某集
支持的命名:

    ├─A
    │  └─安达与岛村
    │      ├─S1
    │      │       安达与岛村 S01E01.mp4
    │      ├─S01
    │      │       安达与岛村 S01E02.mp4
    │      ├─Season 1
    │      │       安达与岛村 S01E03.mp4
    │      └─Season 01
    │              安达与岛村 S01E04.mp4

ps: 此选项必须启用 自动重命名。确保 qBittorrent 与本程序 docker 映射挂载路径一致

### 自动删除已完成任务

当qb中已下载并完成做种后删除任务，不会删除本地文件

### 自动推断剧集偏移

当添加RSS时会根据最小集数计算出集数偏移

如 无职转生 第二季 是从 第 0 集开始的 则自动识别偏移为 1

如 我推的孩子 第二季 有些字幕组是从第 12 集开始的 则自动识别偏移为 -11

主要看你的个人喜好决定是否开启

### DEBUG

可以看到 debug 级别的日志

## 代理设置

主要用与访问 mikan

## 登录设置

设置网页的账号与密码

## 邮件通知

当有新番开始下载时向邮箱发送通知

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
