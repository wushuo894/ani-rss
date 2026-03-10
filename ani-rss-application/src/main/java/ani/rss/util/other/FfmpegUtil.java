package ani.rss.util.other;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Config;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * FFmpeg 工具类
 * 用于视频信息探测、编码检测、转码等操作
 */
@Slf4j
public class FfmpegUtil {

    /**
     * 视频探测结果
     */
    @Data
    public static class ProbeResult {
        private String videoCodec;
        private String audioCodec;
        private String formatName;
        private Long duration;
        private Long bitrate;
        /** 视频宽度（像素） */
        private int width;
        /** 视频高度（像素） */
        private int height;
        /** 视频帧率（fps），null 表示未知 */
        private Double frameRate;
        /** 源文件是否包含字幕流 */
        private boolean hasSubtitles;
    }

    /**
     * 转码需求分析结果
     */
    @Data
    public static class TranscodeDecision {
        private boolean needVideoTranscode = false;
        private boolean needAudioTranscode = false;
        private boolean needRemux = false;
        /** 是否将内封字幕烧录到视频画面（转封装为 MP4 时使用） */
        private boolean hardSub = false;
        /** 是否需要缩放分辨率 */
        private boolean needResize = false;
        /** 目标分辨率（FFmpeg scale 参数，如 -2:1080） */
        private String targetScale;
        /** 是否需要调整帧率 */
        private boolean needFrameRateChange = false;
        /** 目标帧率字符串（如 24、30） */
        private String targetFrameRate;
        /** 是否需要降低码率 */
        private boolean needBitrateReduce = false;
        /** 目标视频码率（如 4000k、2M） */
        private String targetBitrate;
        /**
         * 需要重编码时使用的视频编码器（由 analyzeTranscodeNeeds 计算）
         * 优先使用用户配置，未配置时回退到与源编码对应的编码器
         */
        private String effectiveVideoEncoder;

        /** 是否需要进行任何处理 */
        public boolean needsProcessing() {
            return needVideoTranscode || needAudioTranscode || needRemux || hardSub
                    || needResize || needFrameRateChange || needBitrateReduce;
        }

        /** 是否需要视频重编码（与 -c:v copy 互斥） */
        public boolean needVideoReencode() {
            return needVideoTranscode || hardSub || needResize || needFrameRateChange || needBitrateReduce;
        }
    }

    // =========================================================================
    // 公共工具
    // =========================================================================

    /**
     * 检测 FFmpeg 是否可用
     */
    public static boolean isAvailable(String ffmpegPath) {
        return StrUtil.isNotBlank(getVersion(ffmpegPath));
    }

    /**
     * 获取 FFmpeg 版本信息，不可用返回空字符串
     */
    public static String getVersion(String ffmpegPath) {
        if (StrUtil.isBlank(ffmpegPath)) {
            return "";
        }
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String firstLine = reader.readLine();
                while (reader.readLine() != null) {
                }
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return "";
                }
                if (StrUtil.isNotBlank(firstLine)) {
                    return firstLine.trim();
                }
            }
        } catch (Exception e) {
            log.debug("FFmpeg 不可用: {} - {}", ffmpegPath, e.getMessage());
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return "";
    }

    // =========================================================================
    // 探测
    // =========================================================================

    /**
     * 使用 FFprobe 探测视频信息，失败返回 null
     */
    public static ProbeResult probeVideoInfo(File file, String ffprobePath) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        Process process = null;
        try {
            String filePath = FileUtils.getAbsolutePath(file);
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_streams",
                    "-show_format",
                    filePath
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                output = sb.toString();
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("FFprobe 超时: {}", filePath);
                return null;
            }

            if (process.exitValue() != 0) {
                log.error("FFprobe 执行失败, 退出码: {}, 文件: {}", process.exitValue(), filePath);
                return null;
            }

            return parseProbeOutput(output);
        } catch (Exception e) {
            log.error("FFprobe 探测失败: {} - {}", file.getName(), e.getMessage());
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            return null;
        }
    }

    private static ProbeResult parseProbeOutput(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            ProbeResult result = new ProbeResult();

            JsonArray streams = root.getAsJsonArray("streams");
            if (Objects.nonNull(streams)) {
                for (JsonElement streamElem : streams) {
                    JsonObject stream = streamElem.getAsJsonObject();
                    String codecType = getJsonString(stream, "codec_type");
                    if ("video".equals(codecType) && StrUtil.isBlank(result.getVideoCodec())) {
                        result.setVideoCodec(getJsonString(stream, "codec_name"));
                        result.setWidth(getJsonInt(stream, "width"));
                        result.setHeight(getJsonInt(stream, "height"));
                        // 帧率（r_frame_rate 格式为 "num/den"，如 "30000/1001"）
                        String rFrameRate = getJsonString(stream, "r_frame_rate");
                        if (StrUtil.isNotBlank(rFrameRate)) {
                            result.setFrameRate(parseFrameRate(rFrameRate));
                        }
                    } else if ("audio".equals(codecType) && StrUtil.isBlank(result.getAudioCodec())) {
                        result.setAudioCodec(getJsonString(stream, "codec_name"));
                    } else if ("subtitle".equals(codecType)) {
                        result.setHasSubtitles(true);
                    }
                }
            }

            JsonObject format = root.getAsJsonObject("format");
            if (Objects.nonNull(format)) {
                result.setFormatName(getJsonString(format, "format_name"));
                String duration = getJsonString(format, "duration");
                if (StrUtil.isNotBlank(duration)) {
                    try {
                        result.setDuration((long) (Double.parseDouble(duration) * 1000));
                    } catch (NumberFormatException ignored) {
                    }
                }
                String bitrate = getJsonString(format, "bit_rate");
                if (StrUtil.isNotBlank(bitrate)) {
                    try {
                        result.setBitrate(Long.parseLong(bitrate));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("解析 FFprobe 输出失败: {}", e.getMessage());
            return null;
        }
    }

    /** 解析帧率字符串（如 "30000/1001"、"24"），返回 fps，失败返回 null */
    private static Double parseFrameRate(String rateStr) {
        if (StrUtil.isBlank(rateStr) || "0/0".equals(rateStr)) {
            return null;
        }
        String[] parts = rateStr.split("/");
        if (parts.length == 2) {
            try {
                long num = Long.parseLong(parts[0].trim());
                long den = Long.parseLong(parts[1].trim());
                if (den == 0) return null;
                return (double) num / den;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Double.parseDouble(rateStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getJsonString(JsonObject obj, String key) {
        JsonElement elem = obj.get(key);
        if (Objects.isNull(elem) || elem.isJsonNull()) {
            return "";
        }
        return elem.getAsString();
    }

    private static int getJsonInt(JsonObject obj, String key) {
        JsonElement elem = obj.get(key);
        if (Objects.isNull(elem) || elem.isJsonNull()) {
            return 0;
        }
        try {
            return elem.getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================================
    // 转码决策分析
    // =========================================================================

    /**
     * 判断是否需要转码（白名单检测逻辑）
     * <p>
     * 白名单为空 → 不检测（跳过）
     * 源属性在白名单中 → 不需要处理
     * 源属性不在白名单中 → 需要处理
     */
    public static TranscodeDecision analyzeTranscodeNeeds(ProbeResult probeResult, Config config) {
        TranscodeDecision decision = new TranscodeDecision();

        if (Objects.isNull(probeResult)) {
            return decision;
        }

        // --- 视频编码检测 ---
        List<String> acceptVideoCodecs = config.getFfmpegAcceptVideoCodecs();
        String videoCodec = StrUtil.blankToDefault(probeResult.getVideoCodec(), "").toLowerCase();
        if (CollectionUtil.isNotEmpty(acceptVideoCodecs) && StrUtil.isNotBlank(videoCodec)) {
            boolean accepted = acceptVideoCodecs.stream()
                    .map(String::toLowerCase)
                    .anyMatch(videoCodec::equals);
            if (!accepted) {
                String targetVideoCodec = StrUtil.blankToDefault(config.getFfmpegVideoCodec(), "").toLowerCase();
                if (StrUtil.isNotBlank(targetVideoCodec) && isCodecEquivalent(videoCodec, targetVideoCodec)) {
                    log.info("视频编码 [{}] 与目标编码 [{}] 等价，跳过视频转码", probeResult.getVideoCodec(), targetVideoCodec);
                } else {
                    decision.setNeedVideoTranscode(true);
                    log.info("视频编码 [{}] 不在白名单中，需要转码", probeResult.getVideoCodec());
                }
            }
        }

        // --- 音频编码检测 ---
        List<String> acceptAudioCodecs = config.getFfmpegAcceptAudioCodecs();
        String audioCodec = StrUtil.blankToDefault(probeResult.getAudioCodec(), "").toLowerCase();
        if (CollectionUtil.isNotEmpty(acceptAudioCodecs) && StrUtil.isNotBlank(audioCodec)) {
            boolean accepted = acceptAudioCodecs.stream()
                    .map(String::toLowerCase)
                    .anyMatch(audioCodec::equals);
            if (!accepted) {
                String targetAudioCodec = StrUtil.blankToDefault(config.getFfmpegAudioCodec(), "").toLowerCase();
                if (StrUtil.isNotBlank(targetAudioCodec) && isCodecEquivalent(audioCodec, targetAudioCodec)) {
                    log.info("音频编码 [{}] 与目标编码 [{}] 等价，跳过音频转码", probeResult.getAudioCodec(), targetAudioCodec);
                } else {
                    decision.setNeedAudioTranscode(true);
                    log.info("音频编码 [{}] 不在白名单中，需要转码", probeResult.getAudioCodec());
                }
            }
        }

        // --- 容器格式检测 ---
        List<String> acceptFormats = config.getFfmpegAcceptFormats();
        String formatName = StrUtil.blankToDefault(probeResult.getFormatName(), "").toLowerCase();
        if (CollectionUtil.isNotEmpty(acceptFormats) && StrUtil.isNotBlank(formatName)) {
            List<String> acceptFormatsLower = acceptFormats.stream().map(String::toLowerCase).toList();
            boolean accepted = false;
            for (String part : formatName.split(",")) {
                String trimmed = part.trim();
                for (String accept : acceptFormatsLower) {
                    if (trimmed.equals(accept) || isFormatMatch(trimmed, accept)) {
                        accepted = true;
                        break;
                    }
                }
                if (accepted) break;
            }
            if (!accepted) {
                decision.setNeedRemux(true);
                log.info("容器格式 [{}] 不在白名单中，需要转封装", probeResult.getFormatName());
            }
        }

        // --- 分辨率检测 ---
        List<String> acceptResolutions = config.getFfmpegAcceptResolutions();
        int sourceHeight = probeResult.getHeight();
        if (CollectionUtil.isNotEmpty(acceptResolutions) && sourceHeight > 0) {
            boolean accepted = acceptResolutions.stream()
                    .anyMatch(r -> parseResolutionHeight(r) == sourceHeight);
            if (!accepted) {
                String targetScale = StrUtil.blankToDefault(config.getFfmpegTargetResolution(), "");
                if (StrUtil.isNotBlank(targetScale)) {
                    decision.setNeedResize(true);
                    decision.setTargetScale(targetScale);
                    log.info("分辨率 [{}x{}] 不在白名单中，将缩放至: {}", probeResult.getWidth(), sourceHeight, targetScale);
                } else {
                    log.debug("分辨率 [{}x{}] 不在白名单中，但未配置目标分辨率，跳过", probeResult.getWidth(), sourceHeight);
                }
            }
        }

        // --- 帧率检测 ---
        List<String> acceptFrameRates = config.getFfmpegAcceptFrameRates();
        Double sourceFrameRate = probeResult.getFrameRate();
        if (CollectionUtil.isNotEmpty(acceptFrameRates) && sourceFrameRate != null) {
            boolean accepted = acceptFrameRates.stream().anyMatch(r -> {
                try {
                    return Math.abs(sourceFrameRate - Double.parseDouble(r)) < 0.5;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
            if (!accepted) {
                String targetFps = StrUtil.blankToDefault(config.getFfmpegTargetFrameRate(), "");
                if (StrUtil.isNotBlank(targetFps)) {
                    decision.setNeedFrameRateChange(true);
                    decision.setTargetFrameRate(targetFps);
                    log.info("帧率 [{}fps] 不在白名单中，将转换至: {}fps", sourceFrameRate, targetFps);
                } else {
                    log.debug("帧率 [{}fps] 不在白名单中，但未配置目标帧率，跳过", sourceFrameRate);
                }
            }
        }

        // --- 码率检测 ---
        Integer acceptMaxBitrate = config.getFfmpegAcceptMaxBitrate();
        Long sourceBitrate = probeResult.getBitrate();
        if (acceptMaxBitrate != null && acceptMaxBitrate > 0 && sourceBitrate != null) {
            long sourceBitrateKbps = sourceBitrate / 1000;
            if (sourceBitrateKbps > acceptMaxBitrate) {
                String targetBitrate = StrUtil.blankToDefault(config.getFfmpegTargetBitrate(), "");
                if (StrUtil.isNotBlank(targetBitrate)) {
                    decision.setNeedBitrateReduce(true);
                    decision.setTargetBitrate(targetBitrate);
                    log.info("码率 [{}kbps] 超过阈值 [{}kbps]，将限制至: {}", sourceBitrateKbps, acceptMaxBitrate, targetBitrate);
                } else {
                    log.debug("码率 [{}kbps] 超过阈值，但未配置目标码率，跳过", sourceBitrateKbps);
                }
            }
        }

        // --- 计算实际使用的视频编码器 ---
        // 优先使用用户配置的目标编码；未配置时，当需要重编码时回退到源编码对应的编码器
        String configuredEncoder = StrUtil.blankToDefault(config.getFfmpegVideoCodec(), "");
        if (StrUtil.isNotBlank(configuredEncoder)) {
            decision.setEffectiveVideoEncoder(configuredEncoder);
        } else {
            // 用源编码对应的编码器（如 hevc → libx265），保持编码格式不变
            decision.setEffectiveVideoEncoder(getEncoder(videoCodec));
        }

        return decision;
    }

    /**
     * 将分辨率描述字符串解析为视频高度（像素）
     * 支持：4K/2160、2K/1440、1080p/1080、720p/720、480p/480、360p/360 及纯数字
     */
    private static int parseResolutionHeight(String resolution) {
        if (StrUtil.isBlank(resolution)) return -1;
        String s = resolution.toLowerCase().replace("p", "").trim();
        switch (s) {
            case "4k":  case "uhd":  return 2160;
            case "2k":  case "qhd":  return 1440;
            case "1080":             return 1080;
            case "720":              return 720;
            case "480":              return 480;
            case "360":              return 360;
            default:
                try { return Integer.parseInt(s); }
                catch (NumberFormatException e) { return -1; }
        }
    }

    /**
     * 将 ffprobe 返回的编解码器名称映射为对应的 ffmpeg 编码器名称
     * 例：hevc → libx265，h264 → libx264，未知编码保持原名
     */
    public static String getEncoder(String probeCodec) {
        if (StrUtil.isBlank(probeCodec)) {
            return "libx264";
        }
        String alias = CODEC_ALIAS.get(probeCodec.toLowerCase());
        return alias != null ? alias : probeCodec.toLowerCase();
    }

    /**
     * 判断是否需要将内封字幕烧录到视频画面
     * 条件：ffmpegHardSub 开启 + 目标格式为 mp4/m4v + 源文件含字幕流
     */
    public static boolean shouldHardSub(Config config, ProbeResult probeResult, String targetExt) {
        if (!BooleanUtil.isTrue(config.getFfmpegHardSub())) {
            return false;
        }
        if (!("mp4".equals(targetExt) || "m4v".equals(targetExt))) {
            return false;
        }
        return probeResult != null && probeResult.isHasSubtitles();
    }

    /**
     * 获取目标文件扩展名（优先使用用户配置，否则保持原扩展名）
     */
    public static String getTargetExtension(Config config, File sourceFile) {
        String targetFormat = config.getFfmpegFormat();
        if (StrUtil.isNotBlank(targetFormat)) {
            return targetFormat.toLowerCase();
        }
        return FileUtil.extName(sourceFile).toLowerCase();
    }

    // =========================================================================
    // 格式 / 编解码器别名
    // =========================================================================

    /**
     * 格式别名匹配（ffprobe 返回名与用户输入名的映射）
     */
    private static boolean isFormatMatch(String probeFormat, String userFormat) {
        return ("matroska".equals(probeFormat) && "mkv".equals(userFormat))
                || ("mkv".equals(probeFormat) && "matroska".equals(userFormat))
                || ("mpegts".equals(probeFormat) && "ts".equals(userFormat))
                || ("ts".equals(probeFormat) && "mpegts".equals(userFormat));
    }

    /**
     * 编解码器等价匹配（双向）
     * ffprobe 名 ↔ ffmpeg 编码器名，如 hevc ↔ libx265
     */
    private static final Map<String, String> CODEC_ALIAS = Map.of(
            "hevc", "libx265",
            "h264", "libx264",
            "av1", "libsvtav1",
            "vp9", "libvpx-vp9",
            "vp8", "libvpx",
            "mp3", "libmp3lame",
            "opus", "libopus",
            "vorbis", "libvorbis"
    );

    /**
     * 支持命名预设（ultrafast/fast/medium/slow 等）的编码器白名单
     * libvpx-vp9 不支持 -preset；libsvtav1 仅支持数字预设 0-13
     */
    private static final java.util.Set<String> NAMED_PRESET_ENCODERS = java.util.Set.of("libx264", "libx265");

    private static boolean isCodecEquivalent(String a, String b) {
        if (a.equals(b)) return true;
        if ("av1".equals(a) && "libaom-av1".equals(b)) return true;
        if ("av1".equals(b) && "libaom-av1".equals(a)) return true;
        String aliasOfA = CODEC_ALIAS.get(a);
        if (aliasOfA != null && aliasOfA.equals(b)) return true;
        String aliasOfB = CODEC_ALIAS.get(b);
        return aliasOfB != null && aliasOfB.equals(a);
    }

    // =========================================================================
    // 转码执行
    // =========================================================================

    /**
     * 对 FFmpeg subtitles 滤镜的文件路径进行转义
     * filtergraph 语法中：\\ : ' [ ] 均需转义
     */
    private static String escapeSubtitlePath(String path) {
        return path
                .replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("'", "\\'")
                .replace(",", "\\,")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }

    public static boolean transcode(File sourceFile, File targetFile, Config config, TranscodeDecision decision) {
        return transcode(sourceFile, targetFile, config, decision, 0, null);
    }

    /**
     * 执行 FFmpeg 转码（带进度回调）
     *
     * @param sourceFile       源文件
     * @param targetFile       目标文件
     * @param config           配置
     * @param decision         转码决策
     * @param durationMs       视频总时长（毫秒），用于计算进度；0 表示不上报
     * @param progressCallback 进度回调（0-100），可为 null
     * @return 是否成功
     */
    public static boolean transcode(File sourceFile, File targetFile, Config config, TranscodeDecision decision,
                                    long durationMs, Consumer<Integer> progressCallback) {
        String ffmpegPath = config.getFfmpegPath();
        String sourcePath = FileUtils.getAbsolutePath(sourceFile);
        String targetPath = FileUtils.getAbsolutePath(targetFile);

        FileUtil.mkdir(targetFile.getParentFile());

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(sourcePath);

        // === 视频编码 ===
        if (decision.needVideoReencode()) {
            String videoCodec = StrUtil.blankToDefault(decision.getEffectiveVideoEncoder(), "libx264");
            command.add("-c:v");
            command.add(videoCodec);
            // CRF（质量优先模式）
            Integer crf = config.getFfmpegCrf();
            if (Objects.nonNull(crf)) {
                command.add("-crf");
                command.add(String.valueOf(crf));
            }
            // 编码预设（仅 libx264/libx265 支持命名预设；libvpx-vp9 不支持 -preset；libsvtav1 仅支持数字预设）
            String preset = config.getFfmpegPreset();
            if (StrUtil.isNotBlank(preset) && NAMED_PRESET_ENCODERS.contains(videoCodec)) {
                command.add("-preset");
                command.add(preset);
            }
            // 目标码率（码率模式）
            if (decision.isNeedBitrateReduce() && StrUtil.isNotBlank(decision.getTargetBitrate())) {
                command.add("-b:v");
                command.add(decision.getTargetBitrate());
            }
        } else {
            command.add("-c:v");
            command.add("copy");
        }

        // === 视频滤镜（分辨率缩放 + 字幕烧录，可组合） ===
        List<String> vfFilters = new ArrayList<>();
        if (decision.isNeedResize() && StrUtil.isNotBlank(decision.getTargetScale())) {
            vfFilters.add("scale=" + decision.getTargetScale());
        }
        if (decision.isHardSub()) {
            vfFilters.add("subtitles=" + escapeSubtitlePath(sourcePath));
        }
        if (!vfFilters.isEmpty()) {
            command.add("-vf");
            command.add(String.join(",", vfFilters));
        }

        // === 帧率 ===
        if (decision.isNeedFrameRateChange() && StrUtil.isNotBlank(decision.getTargetFrameRate())) {
            command.add("-r");
            command.add(decision.getTargetFrameRate());
        }

        // === 音频编码 ===
        String audioCodec = config.getFfmpegAudioCodec();
        if (decision.isNeedAudioTranscode() && StrUtil.isNotBlank(audioCodec)) {
            command.add("-c:a");
            command.add(audioCodec);
        } else {
            command.add("-c:a");
            command.add("copy");
        }

        // === 字幕处理 ===
        String subtitleMode = config.getFfmpegSubtitleMode();
        String targetExt = FileUtil.extName(targetFile).toLowerCase();
        if (decision.isHardSub()) {
            // 已烧录到画面，去除字幕流
            command.add("-sn");
        } else if ("remove".equals(subtitleMode)) {
            command.add("-sn");
        } else if ("mp4".equals(targetExt) || "m4v".equals(targetExt)) {
            // MP4 容器不支持直接 copy ASS/SSA/PGS 等字幕流
            // subtitleMode=copy 时尝试转为 mov_text（文字字幕可用，复杂样式会丢失；位图字幕会报错）
            // 其他模式（或未配置）则直接丢弃，避免 FFmpeg 因格式不兼容报错
            if ("copy".equals(subtitleMode)) {
                command.add("-c:s");
                command.add("mov_text");
            } else {
                command.add("-sn");
            }
        } else {
            command.add("-c:s");
            command.add("copy");
        }

        // === 额外参数 ===
        String extraArgs = config.getFfmpegExtraArgs();
        if (StrUtil.isNotBlank(extraArgs)) {
            for (String arg : extraArgs.trim().split("\\s+")) {
                if (StrUtil.isNotBlank(arg)) {
                    command.add(arg);
                }
            }
        }

        command.add("-n");
        command.add(targetPath);

        log.info("FFmpeg 转码命令: {}", String.join(" ", command));

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                    if (progressCallback != null && durationMs > 0) {
                        long timeMs = parseTimeMs(line);
                        if (timeMs >= 0) {
                            int pct = (int) Math.min(100, timeMs * 100 / durationMs);
                            progressCallback.accept(pct);
                        }
                    }
                }
            }

            boolean finished = process.waitFor(24, TimeUnit.HOURS);
            if (!finished) {
                process.destroyForcibly();
                log.error("FFmpeg 转码超时: {}", sourcePath);
                FileUtil.del(targetFile);
                return false;
            }

            if (process.exitValue() != 0) {
                log.error("FFmpeg 转码失败, 退出码: {}, 文件: {}", process.exitValue(), sourcePath);
                FileUtil.del(targetFile);
                return false;
            }

            log.info("FFmpeg 转码完成: {} -> {}", sourceFile.getName(), targetFile.getName());
            if (progressCallback != null) {
                progressCallback.accept(100);
            }
            return true;
        } catch (Exception e) {
            log.error("FFmpeg 转码异常: {} - {}", sourcePath, e.getMessage(), e);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            FileUtil.del(targetFile);
            return false;
        }
    }

    /**
     * 解析 FFmpeg 输出行中的 time= 字段，返回毫秒数；解析失败返回 -1
     */
    private static long parseTimeMs(String line) {
        int idx = line.indexOf("time=");
        if (idx < 0) {
            return -1;
        }
        String timePart = line.substring(idx + 5).split("\\s")[0];
        if (timePart.startsWith("N/A")) {
            return -1;
        }
        try {
            String[] parts = timePart.split(":");
            if (parts.length != 3) {
                return -1;
            }
            long h = Long.parseLong(parts[0]);
            long m = Long.parseLong(parts[1]);
            double s = Double.parseDouble(parts[2]);
            return (h * 3600 + m * 60) * 1000 + (long) (s * 1000);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 复制文件到目标路径（目标已存在时跳过）
     *
     * @return 是否成功
     */
    public static boolean copyFile(File sourceFile, File targetFile) {
        try {
            FileUtil.mkdir(targetFile.getParentFile());
            if (targetFile.exists()) {
                log.debug("目标文件已存在, 跳过复制: {}", targetFile.getName());
                return true;
            }
            FileUtil.copy(sourceFile, targetFile, false);
            log.info("文件复制完成: {} -> {}", sourceFile.getName(), targetFile.getName());
            return true;
        } catch (Exception e) {
            log.error("文件复制失败: {} - {}", sourceFile.getName(), e.getMessage(), e);
            return false;
        }
    }
}
