package ani.rss.task;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.TorrentsTags;
import ani.rss.service.DownloadService;
import ani.rss.util.other.*;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FFmpeg 转码任务
 * 在下载完成后、重命名之前进行视频编码归一化处理
 */
@Slf4j
public class FfmpegTask extends Thread {

    private final AtomicBoolean loop;

    public FfmpegTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    @Override
    public void run() {
        super.setName("ffmpeg-task-thread");
        Config config = ConfigUtil.CONFIG;

        // 等待初始化完成
        cn.hutool.core.thread.ThreadUtil.sleep(5000);

        while (loop.get()) {
            Boolean ffmpegEnable = config.getFfmpegEnable();
            int sleepSeconds = config.getFfmpegSleepSeconds();

            if (!BooleanUtil.isTrue(ffmpegEnable)) {
                cn.hutool.core.thread.ThreadUtil.sleep(sleepSeconds * 1000L);
                continue;
            }

            // 检测 FFmpeg 可用性
            if (!FfmpegUtil.isAvailable(config.getFfmpegPath())) {
                log.warn("FFmpeg 不可用，请检查路径配置: {}", config.getFfmpegPath());
                cn.hutool.core.thread.ThreadUtil.sleep(sleepSeconds * 1000L);
                continue;
            }

            try {
                if (TorrentUtil.login()) {
                    processTorrents(config);
                }
            } catch (Exception e) {
                String message = ExceptionUtils.getMessage(e);
                log.error("FFmpeg 任务异常: {}", message, e);
            }

            cn.hutool.core.thread.ThreadUtil.sleep(sleepSeconds * 1000L);
        }
        log.info("{} 任务已停止", getName());
    }

    /**
     * 处理所有需要转码的种子
     */
    private void processTorrents(Config config) {
        List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
        Integer maxConcurrent = config.getFfmpegMaxConcurrent();
        AtomicInteger currentCount = new AtomicInteger(0);

        for (TorrentsInfo torrentsInfo : torrentsInfos) {
            if (!loop.get()) {
                return;
            }

            List<String> tags = torrentsInfo.getTags();

            // 仅处理有 ani-rss 标签的任务
            if (!tags.contains(TorrentsTags.ANI_RSS.getValue())) {
                continue;
            }

            // 已经处理过
            if (tags.contains(TorrentsTags.FFMPEG_DONE.getValue())) {
                continue;
            }

            // 必须已下载完成（直接检查种子状态，不依赖 DOWNLOAD_COMPLETE 标签）
            if (!isDownloadComplete(torrentsInfo.getState())) {
                continue;
            }

            // 并发数限制
            if (maxConcurrent > 0 && currentCount.get() >= maxConcurrent) {
                log.debug("FFmpeg 已达最大并发数 {}", maxConcurrent);
                break;
            }

            try {
                boolean processed = processOneTorrent(torrentsInfo, config);
                if (processed) {
                    currentCount.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("FFmpeg 处理失败: {} - {}", torrentsInfo.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 处理单个种子的转码
     *
     * @return true 如果进行了处理
     */
    private boolean processOneTorrent(TorrentsInfo torrentsInfo, Config config) {
        String name = torrentsInfo.getName();
        String downloadDir = torrentsInfo.getDownloadDir();
        String ffprobePath = config.getFfprobePath();

        // 反查对应的订阅
        Optional<Ani> aniOpt = DownloadService.findAniByDownloadPath(torrentsInfo);
        if (aniOpt.isEmpty()) {
            log.debug("FFmpeg: 未能获取番剧对象: {}", name);
            // 非 ani-rss 管理的任务，直接标记跳过
            TorrentUtil.addTags(torrentsInfo, TorrentsTags.FFMPEG_DONE.getValue());
            return false;
        }

        Ani ani = aniOpt.get();

        // 计算最终保存路径（非缓存路径，即关闭 FFmpeg 时的原始路径）
        String finalSavePath = getOriginalDownloadPath(ani, config);

        log.info("FFmpeg 开始处理: {} -> {}", name, finalSavePath);

        // 查找实际的源文件目录：优先 qBittorrent 报告的路径，其次尝试缓存路径和原始路径
        File downloadDirFile = resolveSourceDir(downloadDir, ani, config);
        if (Objects.isNull(downloadDirFile)) {
            log.warn("FFmpeg: 源文件目录均不存在，等待下次重试: qbt={}", downloadDir);
            return false;
        }

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        List<File> files = FileUtils.listFileList(downloadDirFile.getAbsolutePath());
        for (File file : files) {
            if (!loop.get()) {
                return false;
            }
            if (!file.isFile()) {
                continue;
            }
            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                continue;
            }

            if (FileUtils.isVideoFormat(extName)) {
                boolean success = processVideoFile(file, finalSavePath, config, ffprobePath);
                if (success) {
                    successCount++;
                } else {
                    // processVideoFile 返回 false：FFprobe 失败（文件可能还在下载）或转码失败
                    skipCount++;
                }
            } else if (FileUtils.isSubtitleFormat(extName)) {
                File targetFile = new File(finalSavePath, file.getName());
                FfmpegUtil.copyFile(file, targetFile);
            }
        }

        log.info("FFmpeg 统计: {} - 成功:{} 跳过:{} 失败:{}", name, successCount, skipCount, failCount);

        if (successCount > 0 && skipCount == 0) {
            // 全部成功且无跳过 → 标记完成
            TorrentUtil.addTags(torrentsInfo, TorrentsTags.FFMPEG_DONE.getValue());
            log.info("FFmpeg 处理完成: {}", name);

            Boolean seeding = config.getFfmpegSeeding();
            if (!BooleanUtil.isTrue(seeding)) {
                TorrentUtil.DOWNLOAD.delete(torrentsInfo, true);
                log.info("FFmpeg: 不做种，已删除种子和缓存文件: {}", name);
            }
        } else if (successCount == 0 && skipCount == 0) {
            log.debug("FFmpeg: 未发现可处理的视频文件: {}", name);
        } else {
            log.info("FFmpeg: 部分文件跳过（可能仍在下载），等待下次重试: {}", name);
        }

        return successCount > 0;
    }

    /**
     * 处理单个视频文件
     */
    private boolean processVideoFile(File sourceFile, String finalSavePath, Config config, String ffprobePath) {
        // 探测视频信息
        FfmpegUtil.ProbeResult probeResult = FfmpegUtil.probeVideoInfo(sourceFile, ffprobePath);
        if (Objects.isNull(probeResult)) {
            log.warn("FFmpeg: 无法探测视频信息（文件可能仍在下载），跳过: {}", sourceFile.getName());
            return false;
        }

        log.info("FFmpeg 探测: {} - 视频:{} 音频:{} 格式:{}",
                sourceFile.getName(),
                probeResult.getVideoCodec(),
                probeResult.getAudioCodec(),
                probeResult.getFormatName());

        // 分析转码需求
        FfmpegUtil.TranscodeDecision decision = FfmpegUtil.analyzeTranscodeNeeds(probeResult, config);

        if (!decision.needsProcessing()) {
            // 不需要任何处理，直接复制
            log.info("FFmpeg: 编码/格式符合要求，直接复制: {}", sourceFile.getName());
            String targetExt = FfmpegUtil.getTargetExtension(config, probeResult, sourceFile);
            String baseName = FileUtil.mainName(sourceFile);
            File targetFile = new File(finalSavePath, baseName + "." + targetExt);
            return FfmpegUtil.copyFile(sourceFile, targetFile);
        }

        // 需要转码/转封装
        String targetExt = FfmpegUtil.getTargetExtension(config, probeResult, sourceFile);
        String baseName = FileUtil.mainName(sourceFile);
        File targetFile = new File(finalSavePath, baseName + "." + targetExt);

        if (targetFile.exists()) {
            log.info("FFmpeg: 目标文件已存在，跳过: {}", targetFile.getName());
            return true;
        }

        log.info("FFmpeg: 开始转码 {} -> {} (视频转码:{} 音频转码:{} 转封装:{})",
                sourceFile.getName(), targetFile.getName(),
                decision.isNeedVideoTranscode(),
                decision.isNeedAudioTranscode(),
                decision.isNeedRemux());

        return FfmpegUtil.transcode(sourceFile, targetFile, config, decision);
    }

    /**
     * 按优先级查找实际存在的源文件目录
     * 1. qBittorrent 报告的 downloadDir
     * 2. FFmpeg 缓存路径（getActualDownloadPath）
     * 3. 原始下载路径（getDownloadPath，无缓存重定向）
     *
     * @return 存在的目录，全部不存在返回 null（下次轮询重试）
     */
    private File resolveSourceDir(String downloadDir, Ani ani, Config config) {
        // 1. qBittorrent 报告的路径
        File dir = new File(downloadDir);
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }

        // 2. 缓存路径
        String cachePath = DownloadService.getActualDownloadPath(ani, config);
        dir = new File(cachePath);
        if (dir.exists() && dir.isDirectory()) {
            log.info("FFmpeg: 在缓存路径找到文件: {}", cachePath);
            return dir;
        }

        // 3. 原始路径（兼容 FFmpeg 启用前已下载的种子）
        String originalPath = getOriginalDownloadPath(ani, config);
        dir = new File(originalPath);
        if (dir.exists() && dir.isDirectory()) {
            log.info("FFmpeg: 在原始路径找到文件: {}", originalPath);
            return dir;
        }

        return null;
    }

    /**
     * 获取原始下载路径（不经过 FFmpeg 缓存路径重定向）
     * 用于确定转码后的最终保存位置
     */
    private String getOriginalDownloadPath(Ani ani, Config config) {
        // 临时关闭 FFmpeg 以获取原始路径
        Boolean originalEnable = config.getFfmpegEnable();
        try {
            config.setFfmpegEnable(false);
            return DownloadService.getDownloadPath(ani, config);
        } finally {
            config.setFfmpegEnable(originalEnable);
        }
    }

    private static final Set<TorrentsInfo.State> COMPLETED_STATES = Set.of(
            TorrentsInfo.State.queuedUP,
            TorrentsInfo.State.uploading,
            TorrentsInfo.State.stalledUP,
            TorrentsInfo.State.pausedUP,
            TorrentsInfo.State.stoppedUP
    );

    private boolean isDownloadComplete(TorrentsInfo.State state) {
        if (Objects.isNull(state)) {
            return false;
        }
        return COMPLETED_STATES.contains(state);
    }
}
