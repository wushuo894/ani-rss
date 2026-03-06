# FFmpeg 转码流水线 — 架构与原理文档

## 概述

ani-rss 的 FFmpeg 转码功能在 BT 下载完成后、重命名前，对视频文件进行转码处理。
整个流水线通过 qBittorrent **标签（Tag）** 协调三个核心组件：`DownloadService`、`FfmpegTask`、`RenameTask`。

---

## 整体流程

```mermaid
flowchart TD
    A[RSS 发现新集] --> B[DownloadService.downloadAni]
    B --> C["qBittorrent.download<br/>paused=true 暂停状态添加"]
    C --> D["RenameTask<br/>重命名 API 调用<br/>S01E01.mkv"]
    D --> E[qBittorrent 开始下载]
    E --> F[下载完成<br/>进入 queuedUP/uploading 状态]
    F --> G["DownloadService.notification<br/>打 DOWNLOAD_COMPLETE 标签（幂等）"]
    G --> H{ffmpegEnable?}
    H -- 否 --> I[刮削 / 发送通知]
    H -- 是 --> J["FfmpegTask.submit<br/>入队 QUEUE"]
    J --> K["FfmpegTask<br/>工作线程处理"]
    K --> L["转码完成<br/>打 FFMPEG_DONE 标签"]
    L --> M{ffmpegSeeding?}
    M -- 做种 --> N["保留种子继续做种<br/>RenameTask 继续处理"]
    M -- 不做种 --> O{路径变化或格式变化?}
    O -- 是 --> P["TorrentUtil.delete<br/>删种 + 删缓存文件"]
    O -- 否 --> N
    N --> Q["RenameTask<br/>检测到 FFMPEG_DONE<br/>执行 rename"]
```



---

## qBittorrent 文件命名时机

```mermaid
flowchart LR
    A["ani-rss 调用<br/>download paused=true"] --> B["RenameTask<br/>通过 API 重命名文件<br/>[字幕组] 标题 S01E01.mkv"]
    B --> C["ani-rss 调用<br/>start 开始下载"]
    C --> D["qBittorrent 将文件<br/>下载到目标路径<br/>文件名已是最终形式"]
```



> **关键约定**：qBittorrent 在下载开始 **之前** 文件名已经是最终形式（如 `Title S01E01.mkv`）。
> 因此 `FfmpegUtil` 可直接使用 `FileUtil.mainName(sourceFile)` 作为输出文件名，无需从种子名推断。

---

## RenameTask 与 FfmpegTask 协调

```mermaid
flowchart TD
    subgraph RenameTask["RenameTask（轮询）"]
        R1["遍历 qBittorrent 种子列表"] --> R2{包含 DOWNLOAD_COMPLETE?}
        R2 -- 否 --> R1
        R2 -- 是 --> R3{ffmpegEnable?}
        R3 -- 否 --> R4[执行重命名]
        R3 -- 是 --> R5{包含 FFMPEG_DONE?}
        R5 -- 否 --> R6[跳过，等待下次轮询]
        R5 -- 是 --> R4
    end

    subgraph FfmpegTask["FfmpegTask（阻塞队列）"]
        F1["QUEUE.poll 等待任务"] --> F2[processOneTorrent]
        F2 --> F3["转码所有视频文件"]
        F3 --> F4["打 FFMPEG_DONE 标签"]
    end

    F4 -.-> R5
```



---

## FfmpegTask 详细处理流程

```mermaid
flowchart TD
    Start([工作线程启动]) --> A["findAniByDownloadPath<br/>反查订阅信息"]
    A --> B{订阅存在?}
    B -- 否 --> C["打 FFMPEG_DONE<br/>解除 RenameTask 阻塞"]
    B -- 是 --> D["resolveSourceDir<br/>确定源文件目录"]
    D --> E{目录存在?}
    E -- 否 --> F["等待 10s<br/>重新入队"]
    E -- 是 --> G["listFileList<br/>遍历顶层文件"]

    G --> H{文件类型}
    H -- 视频文件 --> I[processVideoFile]
    H -- 字幕文件 --> J["复制到 finalSavePath<br/>跟踪 allInPlace"]
    H -- 其他 --> K[跳过]

    I --> L{成功/跳过?}
    L -- 成功 --> M[successCount++]
    L -- 跳过 --> N[skipCount++]

    J --> O{复制成功?}
    O -- 是 --> P[subtitleCount++]
    O -- 否 --> N

    M --> Q{全部文件处理完成}
    N --> Q
    P --> Q

    Q --> R{successCount>0 且 skipCount==0?}
    R -- 是 --> S["打 FFMPEG_DONE"]
    R -- 否 --> T{successCount==0 且 skipCount==0?}
    T -- 是 --> U["打 FFMPEG_DONE<br/>防止 RenameTask 永久阻塞"]
    T -- 否 --> V["等待 10s<br/>重新入队（文件仍在下载）"]

    S --> W{ffmpegSeeding?}
    W -- 做种 --> X{原地转码?}
    X -- 是 --> End([结束])
    X -- 否 --> Y["TorrentUtil.delete<br/>删种 + 删缓存"]
    W -- 不做种 --> Z{diffPath || !allInPlace?}
    Z -- 是 --> Y
    Z -- 否 --> End
    Y --> End
```



---

## 单个视频文件转码决策

```mermaid
flowchart TD
    A["processVideoFile<br/>源文件"] --> B["FfmpegUtil.probeVideoInfo<br/>ffprobe 探测"]
    B --> C{探测成功?}
    C -- 否 --> D["skipCount++<br/>文件可能仍在下载"]
    C -- 是 --> E["analyzeTranscodeNeeds<br/>分析转码需求"]
    E --> F["getTargetExtension<br/>确定目标格式"]
    F --> G["构造 targetFile 路径<br/>finalSavePath + baseName + ext"]

    G --> H{isSameFile?}
    H -- 否 --> I["outInPlace = false"]
    H -- 是 --> J["outInPlace 保持"]

    I --> K["shouldHardSub<br/>是否烧录字幕"]
    J --> K
    K --> L{needsProcessing?}
    L -- 否且isSameFile --> M["文件已就位<br/>直接返回 true"]
    L -- 否且不同路径 --> N["FfmpegUtil.copyFile<br/>直接复制"]
    L -- 是 --> O{isSameFile?}
    O -- 是 --> P["actualTarget =<br/>.ffmpeg_ + 原文件名<br/>原地转码临时文件"]
    O -- 否 --> Q["actualTarget = targetFile"]

    P --> R{目标临时文件已存在?}
    R -- 是 --> S["删除旧临时文件<br/>清理崩溃遗留"]
    R -- 否 --> T["FfmpegUtil.transcode<br/>执行转码"]
    S --> T
    Q --> T

    T --> U{转码成功?}
    U -- 是且isSameFile --> V["删除源文件<br/>重命名临时文件→最终文件"]
    U -- 是且不同路径 --> W["直接使用 targetFile"]
    U -- 否 --> X[返回 false]
    V --> Y[返回 true]
    W --> Y
```



---

## 转码需求分析（analyzeTranscodeNeeds）

```mermaid
flowchart TD
    A[ProbeResult + Config] --> B{acceptVideoCodecs 非空?}
    B -- 是 --> C{源视频编码在白名单?}
    C -- 否 --> D[needVideoTranscode = true]
    C -- 是 --> E[视频编码 OK]
    B -- 否 --> E

    A --> F{acceptAudioCodecs 非空?}
    F -- 是 --> G{源音频编码在白名单?}
    G -- 否 --> H[needAudioTranscode = true]
    G -- 是 --> I[音频编码 OK]
    F -- 否 --> I

    A --> J{acceptFormats 非空?}
    J -- 是 --> K{源格式在白名单?}
    K -- 否 --> L[needRemux = true]
    K -- 是 --> M[容器格式 OK]
    J -- 否 --> M

    A --> N{acceptResolutions 非空?}
    N -- 是 --> O{源分辨率在白名单?}
    O -- 否 --> P[needResize = true]
    N -- 否 --> Q[分辨率 OK]

    A --> R{acceptFrameRates 非空?}
    R -- 是 --> S{"源帧率与白名单匹配?<br/>±0.5fps 容差"}
    S -- 否 --> T[needFrameRateChange = true]
    R -- 否 --> U[帧率 OK]

    A --> V{acceptMaxBitrate > 0?}
    V -- 是 --> W{源码率 > 阈值?}
    W -- 是 --> X[needBitrateReduce = true]
    V -- 否 --> Y[码率 OK]

    D --> Z["TranscodeDecision<br/>needsProcessing = 任意一项为 true"]
    H --> Z
    L --> Z
    P --> Z
    T --> Z
    X --> Z
```



---

## 字幕处理策略

```mermaid
flowchart TD
    A["目标格式确定后"] --> B{targetExt?}

    B -- "mkv / ts / 其他" --> C{subtitleMode?}
    C -- copy --> D["-c:s copy<br/>保留字幕轨"]
    C -- remove --> E["-sn 移除字幕"]

    B -- "mp4 / m4v" --> F{hardSub == true<br/>且有内封字幕?}
    F -- 是 --> G["-vf subtitles=path<br/>烧录到画面<br/>-sn 移除轨道"]
    F -- 否 --> H{subtitleMode?}
    H -- copy --> I["-c:s mov_text<br/>MP4 兼容文字字幕"]
    H -- remove --> E

    G --> J["注意：位图字幕<br/>PGS/DVDSUB<br/>会导致失败"]
    I --> K["注意：位图字幕<br/>PGS/DVDSUB<br/>会被自动丢弃"]
```



---

## 路径计算逻辑

```mermaid
flowchart TD
    A["getDownloadPath<br/>原始最终路径<br/>如 /Media/番剧/Title/Season 1"] --> B{ffmpegEnable<br/>且 ffmpegOutputPath 非空?}
    B -- 是 --> C["getRelativeSubPath<br/>计算公共前缀后的子路径<br/>如 番剧/Title/Season 1"]
    C --> D["getActualDownloadPath<br/>= ffmpegOutputPath + subPath<br/>如 /Media/transcode/番剧/Title/Season 1"]
    B -- 否 --> E["getActualDownloadPath<br/>= 原始路径（原地处理）"]

    D --> F["BT 客户端实际下载目录"]
    E --> F
    A --> G["FfmpegTask 转码输出目录<br/>finalSavePath"]
```



**路径计算示例：**


| ffmpegOutputPath   | getDownloadPath      | getActualDownloadPath          |
| ------------------ | -------------------- | ------------------------------ |
| 空                  | `/Media/番剧/Title/S1` | `/Media/番剧/Title/S1`（原地）       |
| `/Media/transcode` | `/Media/番剧/Title/S1` | `/Media/transcode/番剧/Title/S1` |
| `/Media/transcode` | `/Media/517057`      | `/Media/transcode/517057`      |


---

## 应用重启恢复（recoverPending）

```mermaid
flowchart TD
    A[应用启动] --> B["ThreadUtil.sleep 5s<br/>等待初始化"]
    B --> C{ffmpegEnable?}
    C -- 否 --> D[跳过恢复]
    C -- 是 --> E["TorrentUtil.getTorrentsInfos<br/>获取全部种子"]
    E --> F["过滤条件：<br/>含 ANI_RSS 标签<br/>含 DOWNLOAD_COMPLETE 标签<br/>不含 FFMPEG_DONE 标签<br/>下载目录仍存在"]
    F --> G{有未完成任务?}
    G -- 是 --> H["pending.forEach<br/>QUEUE::offer<br/>重新入队"]
    G -- 否 --> I[无需恢复]
    H --> J[进入主循环]
    I --> J
```



---

## FFmpeg 命令构造（transcode）

```mermaid
flowchart TD
    A["transcode<br/>sourceFile, targetFile, config, decision, durationMs"] --> B["ffmpeg -i sourceFile"]
    B --> C{needVideoTranscode?}
    C -- 是 --> D["-c:v videoCodec<br/>-crf N -preset P"]
    C -- 否 --> E["-c:v copy"]
    D --> F
    E --> F

    F{needAudioTranscode?}
    F -- 是 --> G["-c:a audioCodec"]
    F -- 否 --> H["-c:a copy"]
    G --> I
    H --> I

    I["字幕参数<br/>按 shouldHardSub + targetExt 决定"] --> J

    J{needResize?} -- 是 --> K["-vf scale=targetResolution"]
    J -- 否 --> L

    K --> L{needFrameRateChange?}
    L -- 是 --> M["-r targetFrameRate"]
    L -- 否 --> N

    M --> N{needBitrateReduce?}
    N -- 是 --> O["-b:v targetBitrate"]
    N -- 否 --> P

    O --> P["ffmpegExtraArgs 追加"]
    P --> Q["targetFile<br/>-y 覆盖"]
    Q --> R["启动进程<br/>解析 time= 更新进度"]
    R --> S{exitCode == 0?}
    S -- 是 --> T["return true"]
    S -- 否 --> U["destroyForcibly<br/>return false"]
```



---

## 配置项说明


| 配置键                       | 类型      | 默认值       | 说明                  |
| ------------------------- | ------- | --------- | ------------------- |
| `ffmpegEnable`            | Boolean | false     | 转码总开关               |
| `ffmpegPath`              | String  | `ffmpeg`  | ffmpeg 可执行文件路径      |
| `ffprobePath`             | String  | `ffprobe` | ffprobe 可执行文件路径     |
| `ffmpegOutputPath`        | String  | `""`      | 转码目标目录，空=原地转码       |
| `ffmpegAcceptVideoCodecs` | List    | `[]`      | 视频编码白名单，空=不检测       |
| `ffmpegVideoCodec`        | String  | `""`      | 目标视频编码，空=copy       |
| `ffmpegAcceptAudioCodecs` | List    | `[]`      | 音频编码白名单，空=不检测       |
| `ffmpegAudioCodec`        | String  | `""`      | 目标音频编码，空=copy       |
| `ffmpegAcceptFormats`     | List    | `[]`      | 容器格式白名单，空=不检测       |
| `ffmpegFormat`            | String  | `""`      | 目标容器格式，空=保持原格式      |
| `ffmpegCrf`               | Integer | 23        | CRF 质量参数（0-51）      |
| `ffmpegPreset`            | String  | `medium`  | 编码预设                |
| `ffmpegSubtitleMode`      | String  | `copy`    | 字幕处理方式（copy/remove） |
| `ffmpegHardSub`           | Boolean | false     | MP4 烧录内封字幕          |
| `ffmpegSeeding`           | Boolean | true      | 转码后继续做种             |
| `ffmpegMaxConcurrent`     | Integer | 1         | 最大并发转码数             |
| `ffmpegSleepSeconds`      | Integer | 30        | 队列空闲等待时间（秒）         |
| `ffmpegAcceptResolutions` | List    | `[]`      | 分辨率白名单（如 1080p）     |
| `ffmpegTargetResolution`  | String  | `""`      | 目标分辨率（如 -2:1080）    |
| `ffmpegAcceptFrameRates`  | List    | `[]`      | 帧率白名单（如 24、30）      |
| `ffmpegTargetFrameRate`   | String  | `""`      | 目标帧率                |
| `ffmpegAcceptMaxBitrate`  | Integer | 0         | 最大可接受码率（kbps，0=不检测） |
| `ffmpegTargetBitrate`     | String  | `""`      | 目标码率（如 4000k、2M）    |


---

## 边界情况与错误处理


| 场景                    | 处理方式                                                   |
| --------------------- | ------------------------------------------------------ |
| 订阅已被删除                | 打 `FFMPEG_DONE`，解除 RenameTask 阻塞，跳过处理                  |
| 源文件目录不存在              | 等待 10s 后重新入队，下次轮询重试                                    |
| ffprobe 探测失败（文件仍在写入）  | `skipCount++`，任务重新入队                                   |
| 目录内无可处理视频文件           | 打 `FFMPEG_DONE`，防止 RenameTask 永久阻塞                     |
| 部分文件跳过（仍在下载）          | 等待 10s 后整个任务重新入队                                       |
| FFmpeg 进程崩溃/被中断       | 兜底 `catch` 块打 `FFMPEG_DONE`，防止死锁                       |
| 应用重启，任务未完成            | `recoverPending()` 重新扫描并入队                             |
| 原地转码临时文件遗留            | 转码前检测 `.ffmpeg_` 前缀文件并删除                               |
| 已存在目标文件（非原地）          | 直接跳过，返回 `true`                                         |
| 连接下载器失败               | 等待后将任务重新入队                                             |
| ffmpegCachePath（旧配置键） | `migrateOldFfmpegCachePath()` 自动迁移到 `ffmpegOutputPath` |


---

## Docker 部署说明

FFmpeg **不内置**于 Docker 镜像，有两种方案：

**方案一：系统包（推荐简单场景）**

在 `Dockerfile` 中启用注释行：

```dockerfile
RUN apk add --no-cache ffmpeg
```

**方案二：挂载静态编译版本（推荐生产环境）**

```yaml
# docker-compose.yml
volumes:
  - /path/to/static-ffmpeg:/usr/local/bin/ffmpeg:ro
  - /path/to/static-ffprobe:/usr/local/bin/ffprobe:ro
```

在 ani-rss 配置中设置：

- FFmpeg 路径：`/usr/local/bin/ffmpeg`
- FFprobe 路径：`/usr/local/bin/ffprobe`

---

## 关键代码位置索引


| 功能                   | 文件                     | 方法                                       |
| -------------------- | ---------------------- | ---------------------------------------- |
| 转码任务入队               | `DownloadService.java` | `notification()`                         |
| 转码主循环                | `FfmpegTask.java`      | `run()`                                  |
| 单种子处理                | `FfmpegTask.javagit`   | `processOneTorrent()`                    |
| 单视频文件处理              | `FfmpegTask.java`      | `processVideoFile()`                     |
| 源目录查找                | `FfmpegTask.java`      | `resolveSourceDir()`                     |
| 重启恢复                 | `FfmpegTask.java`      | `recoverPending()`                       |
| 视频信息探测               | `FfmpegUtil.java`      | `probeVideoInfo()`                       |
| 转码需求分析               | `FfmpegUtil.java`      | `analyzeTranscodeNeeds()`                |
| 执行转码                 | `FfmpegUtil.java`      | `transcode()`                            |
| 字幕烧录判断               | `FfmpegUtil.java`      | `shouldHardSub()`                        |
| 下载路径计算               | `DownloadService.java` | `getDownloadPath()`                      |
| 实际下载路径               | `DownloadService.java` | `getActualDownloadPath()`                |
| 配置加载与迁移              | `ConfigUtil.java`      | `load()` / `migrateOldFfmpegCachePath()` |
| RenameTask FFmpeg 门控 | `RenameTask.java`      | `rename()` 中的 FFMPEG_DONE 检查             |


