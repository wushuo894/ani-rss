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
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FFmpeg 转码任务
 * 在下载完成通知后对视频进行转码处理，支持并行转码多个任务
 */
@Slf4j
public class FfmpegTask extends Thread {

    private final AtomicBoolean loop;

    public FfmpegTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    /**
     * 转码任务队列，由下载完成通知（DownloadService.notification）提交
     */
    public static final BlockingQueue<TorrentsInfo> QUEUE = new LinkedBlockingQueue<>();

    /**
     * 当前正在处理的任务（key: 种子名, value: 任务状态）
     */
    public static final ConcurrentHashMap<String, ActiveTask> ACTIVE_TASKS = new ConcurrentHashMap<>();

    /**
     * 当前活跃转码任务的状态信息
     */
    @Data
    public static class ActiveTask {
        private volatile String file;
        private volatile int progress;
    }

    /** 执行实际转码的线程池 */
    private ExecutorService executor;

    /** 下载完成监听器引用，用于线程停止时注销 */
    private Consumer<TorrentsInfo> downloadCompleteListener;

    @Override
    public void run() {
        setName("ffmpeg-task-thread");
        Config config = ConfigUtil.CONFIG;

        // 注册下载完成监听器，实现与 DownloadService 的解耦
        // DownloadService 无需感知 FfmpegTask，由此处主动订阅事件
        downloadCompleteListener = torrentsInfo -> {
            if (!BooleanUtil.isTrue(config.getFfmpegEnable())) {
                return;
            }
            if (!torrentsInfo.getTags().contains(TorrentsTags.ANI_RSS.getValue())) {
                return;
            }
            QUEUE.offer(torrentsInfo);
        };
        DownloadService.addDownloadCompleteListener(downloadCompleteListener);

        // 等待初始化完成
        ThreadUtil.sleep(5000);

        // 应用重启后恢复未完成的任务
        recoverPending(config);

        // 创建线程池（无界缓存池，并发数由 ACTIVE_TASKS.size() 限制）
        AtomicInteger workerIndex = new AtomicInteger(0);
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("ffmpeg-worker-" + workerIndex.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        try {
            while (loop.get()) {
                Boolean ffmpegEnable = config.getFfmpegEnable();
                int sleepSeconds = Objects.requireNonNullElse(config.getFfmpegSleepSeconds(), 30);
                int maxConcurrent = Objects.requireNonNullElse(config.getFfmpegMaxConcurrent(), 1);
                if (maxConcurrent < 1) maxConcurrent = 1;

                if (!BooleanUtil.isTrue(ffmpegEnable)) {
                    ThreadUtil.sleep(sleepSeconds * 1000L);
                    continue;
                }

                // 当前活跃任务已达上限时，等待空出槽位
                if (ACTIVE_TASKS.size() >= maxConcurrent) {
                    ThreadUtil.sleep(1000L);
                    continue;
                }

                // 检测 FFmpeg / FFprobe 可用性
                if (!FfmpegUtil.isAvailable(config.getFfmpegPath())) {
                    log.warn("FFmpeg 不可用，请检查路径配置: {}", config.getFfmpegPath());
                    ThreadUtil.sleep(sleepSeconds * 1000L);
                    continue;
                }
                if (!FfmpegUtil.isAvailable(config.getFfprobePath())) {
                    log.warn("FFprobe 不可用，请检查路径配置: {}", config.getFfprobePath());
                    ThreadUtil.sleep(sleepSeconds * 1000L);
                    continue;
                }

                try {
                    // 阻塞等待新任务（超时后重新检查状态）
                    TorrentsInfo torrentsInfo = QUEUE.poll(sleepSeconds, TimeUnit.SECONDS);
                    if (torrentsInfo == null || !loop.get()) {
                        continue;
                    }
                    if (!TorrentUtil.login()) {
                        log.warn("FFmpeg: 连接下载器失败，任务重新入队: {}", torrentsInfo.getName());
                        QUEUE.offer(torrentsInfo);
                        ThreadUtil.sleep(sleepSeconds * 1000L);
                        continue;
                    }
                    // 提交到线程池并行处理
                    final TorrentsInfo task = torrentsInfo;
                    // Bug fix: 在提交前先占位，避免主循环下次检查 size() 时竞态条件超限
                    String taskName = task.getName();
                    ActiveTask activeTask = new ActiveTask();
                    ACTIVE_TASKS.put(taskName, activeTask);
                    executor.submit(() -> {
                        try {
                            processOneTorrent(task, config, activeTask);
                        } catch (Exception e) {
                            log.error("FFmpeg 处理失败: {} - {}", task.getName(), ExceptionUtils.getMessage(e), e);
                            // 异常导致 FFMPEG_DONE 未能标记，兜底标记以解除 RenameTask 阻塞
                            TorrentUtil.addTags(task, TorrentsTags.FFMPEG_DONE.getValue());
                        } finally {
                            ACTIVE_TASKS.remove(taskName);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("FFmpeg 任务异常: {}", ExceptionUtils.getMessage(e), e);
                }
            }
        } finally {
            // 停止时注销监听器，防止重启后重复注册
            DownloadService.removeDownloadCompleteListener(downloadCompleteListener);
            // 停止时中断所有正在运行的工作线程（FFmpeg 进程会在 transcode() 的 catch 中被 destroyForcibly）
            executor.shutdownNow();
            ACTIVE_TASKS.clear();
            // 清空静态队列，避免重启后 recoverPending() 与残留队列重复入队
            QUEUE.clear();
            log.info("{} 任务已停止", getName());
        }
    }

    /**
     * 应用重启后的恢复扫描：
     * 查找 qBittorrent 中带有 ANI_RSS + DOWNLOAD_COMPLETE 标签且下载目录仍存在的任务，重新入队。
     * 若下载目录已被删除，则说明转码已完成（转码源目录已清理），跳过。
     */
    private void recoverPending(Config config) {
        if (!BooleanUtil.isTrue(config.getFfmpegEnable())) {
            return;
        }
        try {
            if (!TorrentUtil.login()) {
                return;
            }
            List<TorrentsInfo> pending = TorrentUtil.getTorrentsInfos().stream()
                    .filter(t -> t.getTags().contains(TorrentsTags.ANI_RSS.getValue()))
                    .filter(t -> t.getTags().contains(TorrentsTags.DOWNLOAD_COMPLETE.getValue()))
                    // 已打 FFMPEG_DONE 标签：转码已完成，无需重新入队
                    .filter(t -> !t.getTags().contains(TorrentsTags.FFMPEG_DONE.getValue()))
                    .filter(t -> {
                        // 若下载目录已不存在，说明转码源目录已清理，跳过
                        File dir = new File(t.getDownloadDir());
                        return dir.exists() && dir.isDirectory();
                    })
                    .toList();
            if (!pending.isEmpty()) {
                log.info("FFmpeg: 应用重启，恢复 {} 个未完成任务", pending.size());
                pending.forEach(QUEUE::offer);
            }
        } catch (Exception e) {
            log.warn("FFmpeg: 恢复任务失败: {}", e.getMessage());
        }
    }

    /**
     * 处理单个种子的转码（在工作线程中执行）
     *
     * @return true 如果进行了处理
     */
    private boolean processOneTorrent(TorrentsInfo torrentsInfo, Config config, ActiveTask activeTask) {
        String name = torrentsInfo.getName();
        String downloadDir = torrentsInfo.getDownloadDir();
        String ffprobePath = config.getFfprobePath();

        // 反查对应的订阅
        Optional<Ani> aniOpt = DownloadService.findAniByDownloadPath(torrentsInfo);
        if (aniOpt.isEmpty()) {
            log.warn("FFmpeg: 未能找到对应订阅（可能已被删除），标记 FFMPEG_DONE 以解除 RenameTask 阻塞: {}", name);
            TorrentUtil.addTags(torrentsInfo, TorrentsTags.FFMPEG_DONE.getValue());
            return false;
        }

        Ani ani = aniOpt.get();

        // 计算最终保存路径（原始下载路径，不受 ffmpegEnable 影响）
        String finalSavePath = DownloadService.getDownloadPath(ani, config);

        log.info("FFmpeg 开始处理: {} -> {}", name, finalSavePath);

        // 查找实际的源文件目录：优先 qBittorrent 报告的路径，其次尝试转码目标目录和原始路径
        File downloadDirFile = resolveSourceDir(downloadDir, ani, config);
        if (Objects.isNull(downloadDirFile)) {
            log.warn("FFmpeg: 源文件目录均不存在，任务重新入队: qbt={}", downloadDir);
            ThreadUtil.sleep(10_000L);
            QUEUE.offer(torrentsInfo);
            return false;
        }

        int successCount = 0;
        int skipCount = 0;
        int subtitleCount = 0;
        // 跟踪是否全部为原地转码（源文件即最终文件，由 qBittorrent 继续管理）
        boolean allInPlace = true;

        List<File> files = FileUtils.listFileList(downloadDirFile.getAbsolutePath());
        for (File file : files) {
            if (!loop.get()) {
                return false;
            }
            if (!file.isFile()) {
                continue;
            }
            // 跳过原地转码临时文件（崩溃遗留或当前正在写入的中间文件）
            if (file.getName().startsWith(".ffmpeg_")) {
                continue;
            }
            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                continue;
            }

            if (FileUtils.isVideoFormat(extName)) {
                boolean[] inPlace = {true};
                boolean success = processVideoFile(file, finalSavePath, config, ffprobePath, activeTask, inPlace);
                if (!inPlace[0]) {
                    allInPlace = false;
                }
                if (success) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } else if (FileUtils.isSubtitleFormat(extName)) {
                // qBittorrent 已在下载前完成重命名，file.getName() 即为最终集数名（如 Title S01E01.zh.ass）
                File targetFile = new File(finalSavePath, file.getName());
                if (!file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                    allInPlace = false;
                }
                if (FfmpegUtil.copyFile(file, targetFile)) {
                    subtitleCount++;
                } else {
                    skipCount++;
                }
            }
        }

        log.info("FFmpeg 统计: {} - 成功:{} 字幕:{} 跳过:{}", name, successCount, subtitleCount, skipCount);

        if ((successCount > 0 || subtitleCount > 0) && skipCount == 0) {
            log.info("FFmpeg 处理完成: {}", name);

            // 标记 FFMPEG_DONE，通知 RenameTask 可以继续处理
            TorrentUtil.addTags(torrentsInfo, TorrentsTags.FFMPEG_DONE.getValue());

            Boolean seeding = config.getFfmpegSeeding();
            if (!BooleanUtil.isTrue(seeding)) {
                // 不做种：满足以下任一条件时，通过 qBittorrent 删除种子并清理源文件：
                // 1. 转码源目录与最终保存路径不同（配置了 ffmpegOutputPath）
                // 2. 存在独立输出文件（格式转换导致源文件与输出文件不同，如 MKV→MP4）
                boolean diffPath = !downloadDirFile.getAbsolutePath()
                        .equals(new File(finalSavePath).getAbsolutePath());
                if (diffPath || !allInPlace) {
                    TorrentUtil.delete(torrentsInfo, true, true);
                    log.info("FFmpeg: 不做种，已删除种子及缓存文件: {}", name);
                } else {
                    log.debug("FFmpeg: 原地转码（源文件即最终文件），跳过删除，RenameTask 将处理后续: {}", name);
                }
            }
        } else if (successCount == 0 && skipCount == 0) {
            // 未找到可处理的视频文件（目录为空、文件在子目录等情况）
            // 仍需标记 FFMPEG_DONE，否则 RenameTask 将永久跳过该种子
            log.debug("FFmpeg: 未发现可处理的视频文件，标记 FFMPEG_DONE 以防 RenameTask 阻塞: {}", name);
            TorrentUtil.addTags(torrentsInfo, TorrentsTags.FFMPEG_DONE.getValue());
        } else {
            log.info("FFmpeg: 部分文件跳过（可能仍在下载），任务重新入队: {}", name);
            ThreadUtil.sleep(10_000L);
            QUEUE.offer(torrentsInfo);
        }

        return successCount > 0;
    }

    /**
     * 处理单个视频文件
     */
    private boolean processVideoFile(File sourceFile, String finalSavePath, Config config,
                                     String ffprobePath, ActiveTask activeTask,
                                     boolean[] outInPlace) {
        FfmpegUtil.ProbeResult probeResult = FfmpegUtil.probeVideoInfo(sourceFile, ffprobePath);
        if (Objects.isNull(probeResult)) {
            log.warn("FFmpeg: 无法探测视频信息（文件可能仍在下载），跳过: {}", sourceFile.getName());
            return false;
        }

        log.info("FFmpeg 探测: {} - 视频:{} 音频:{} 格式:{} 字幕:{}",
                sourceFile.getName(),
                probeResult.getVideoCodec(),
                probeResult.getAudioCodec(),
                probeResult.getFormatName(),
                probeResult.isHasSubtitles());

        FfmpegUtil.TranscodeDecision decision = FfmpegUtil.analyzeTranscodeNeeds(probeResult, config);
        String targetExt = FfmpegUtil.getTargetExtension(config, sourceFile);

        // qBittorrent 在开始下载前已完成文件重命名，此处直接使用源文件名（如 Title S01E01.mkv）
        String baseName = FileUtil.mainName(sourceFile);

        File targetFile = new File(finalSavePath, baseName + "." + targetExt);
        // 源文件与目标文件路径相同（转码输出目录未配置且格式不变时发生）
        boolean isSameFile = sourceFile.getAbsolutePath().equals(targetFile.getAbsolutePath());
        if (!isSameFile) {
            outInPlace[0] = false;
        }

        decision.setHardSub(FfmpegUtil.shouldHardSub(config, probeResult, targetExt));

        activeTask.setFile(sourceFile.getName());
        activeTask.setProgress(0);

        if (!decision.needsProcessing()) {
            if (isSameFile) {
                // 编码和格式均符合要求，文件已在目标位置，无需操作
                log.info("FFmpeg: 编码/格式符合要求，文件已就位: {}", sourceFile.getName());
                activeTask.setProgress(100);
                return true;
            }
            log.info("FFmpeg: 编码/格式符合要求，直接复制: {}", sourceFile.getName());
            boolean ok = FfmpegUtil.copyFile(sourceFile, targetFile);
            if (ok) activeTask.setProgress(100);
            return ok;
        }

        // 源文件即目标文件时，先转码到临时文件再替换（原地转码）
        // 临时文件以 .ffmpeg_ 为前缀保持原扩展名，确保 FFmpeg 能正确识别输出格式
        File actualTarget = isSameFile
                ? new File(targetFile.getParent(), ".ffmpeg_" + targetFile.getName())
                : targetFile;

        if (!isSameFile && targetFile.exists()) {
            log.info("FFmpeg: 目标文件已存在，跳过: {}", targetFile.getName());
            activeTask.setProgress(100);
            return true;
        }

        // 清理上次崩溃遗留的临时文件
        if (isSameFile && actualTarget.exists()) {
            FileUtil.del(actualTarget);
        }

        log.info("FFmpeg: 开始转码 {} -> {} (视频转码:{} 音频转码:{} 转封装:{} 烧录字幕:{} 缩放:{} 调帧率:{} 降码率:{})",
                sourceFile.getName(), targetFile.getName(),
                decision.isNeedVideoTranscode(),
                decision.isNeedAudioTranscode(),
                decision.isNeedRemux(),
                decision.isHardSub(),
                decision.isNeedResize(),
                decision.isNeedFrameRateChange(),
                decision.isNeedBitrateReduce());

        long durationMs = Objects.requireNonNullElse(probeResult.getDuration(), 0L);
        boolean ok = FfmpegUtil.transcode(sourceFile, actualTarget, config, decision, durationMs,
                pct -> activeTask.setProgress(pct));
        if (ok && isSameFile) {
            FileUtil.del(sourceFile);
            FileUtil.rename(actualTarget, targetFile.getName(), true);
        }
        return ok;
    }

    /**
     * 按优先级查找实际存在的源文件目录
     * 1. qBittorrent 报告的 downloadDir
     * 2. 转码输出目录（ffmpegOutputPath 重定向后的路径）
     * 3. 原始下载路径
     *
     * @return 存在的目录，全部不存在返回 null（下次轮询重试）
     */
    private File resolveSourceDir(String downloadDir, Ani ani, Config config) {
        File dir = new File(downloadDir);
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }

        String transcodePath = DownloadService.getActualDownloadPath(ani, config);
        dir = new File(transcodePath);
        if (dir.exists() && dir.isDirectory()) {
            log.info("FFmpeg: 在转码目标目录找到文件: {}", transcodePath);
            return dir;
        }

        String originalPath = DownloadService.getDownloadPath(ani, config);
        dir = new File(originalPath);
        if (dir.exists() && dir.isDirectory()) {
            log.info("FFmpeg: 在原始下载路径找到文件: {}", originalPath);
            return dir;
        }

        return null;
    }

}
