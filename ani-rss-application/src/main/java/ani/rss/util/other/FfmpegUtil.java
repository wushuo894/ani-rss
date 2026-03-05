package ani.rss.util.other;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Config;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
        private String formatLongName;
        private Long duration;
        private Long bitrate;
    }

    /**
     * 转码需求分析结果
     */
    @Data
    public static class TranscodeDecision {
        private boolean needVideoTranscode = false;
        private boolean needAudioTranscode = false;
        private boolean needRemux = false;

        public boolean needsProcessing() {
            return needVideoTranscode || needAudioTranscode || needRemux;
        }
    }

    /**
     * 检测 FFmpeg 是否可用
     *
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @return 是否可用
     */
    public static boolean isAvailable(String ffmpegPath) {
        return StrUtil.isNotBlank(getVersion(ffmpegPath));
    }

    /**
     * 获取 FFmpeg 版本信息
     *
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @return 版本字符串，不可用返回空字符串
     */
    public static String getVersion(String ffmpegPath) {
        if (StrUtil.isBlank(ffmpegPath)) {
            return "";
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String firstLine = reader.readLine();
                process.waitFor(5, TimeUnit.SECONDS);
                if (StrUtil.isNotBlank(firstLine)) {
                    return firstLine.trim();
                }
            }
        } catch (Exception e) {
            log.debug("FFmpeg 不可用: {} - {}", ffmpegPath, e.getMessage());
        }
        return "";
    }

    /**
     * 使用 FFprobe 探测视频信息
     *
     * @param file       视频文件
     * @param ffprobePath FFprobe 路径
     * @return 探测结果，失败返回 null
     */
    public static ProbeResult probeVideoInfo(File file, String ffprobePath) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
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
            pb.redirectErrorStream(false);
            Process process = pb.start();

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
            return null;
        }
    }

    /**
     * 解析 FFprobe JSON 输出
     */
    private static ProbeResult parseProbeOutput(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            ProbeResult result = new ProbeResult();

            // 解析 streams
            JsonArray streams = root.getAsJsonArray("streams");
            if (Objects.nonNull(streams)) {
                for (JsonElement streamElem : streams) {
                    JsonObject stream = streamElem.getAsJsonObject();
                    String codecType = getJsonString(stream, "codec_type");
                    if ("video".equals(codecType) && StrUtil.isBlank(result.getVideoCodec())) {
                        result.setVideoCodec(getJsonString(stream, "codec_name"));
                    } else if ("audio".equals(codecType) && StrUtil.isBlank(result.getAudioCodec())) {
                        result.setAudioCodec(getJsonString(stream, "codec_name"));
                    }
                }
            }

            // 解析 format
            JsonObject format = root.getAsJsonObject("format");
            if (Objects.nonNull(format)) {
                result.setFormatName(getJsonString(format, "format_name"));
                result.setFormatLongName(getJsonString(format, "format_long_name"));
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

    private static String getJsonString(JsonObject obj, String key) {
        JsonElement elem = obj.get(key);
        if (Objects.isNull(elem) || elem.isJsonNull()) {
            return "";
        }
        return elem.getAsString();
    }

    /**
     * 判断是否需要转码（白名单检测逻辑）
     * <p>
     * 白名单为空 → 不检测（跳过）
     * 源编码在白名单中 → 不需要转码
     * 源编码不在白名单中 → 需要转码
     *
     * @param probeResult 探测结果
     * @param config      配置
     * @return 转码决策
     */
    public static TranscodeDecision analyzeTranscodeNeeds(ProbeResult probeResult, Config config) {
        TranscodeDecision decision = new TranscodeDecision();

        if (Objects.isNull(probeResult)) {
            return decision;
        }

        // 视频编码检测
        List<String> acceptVideoCodecs = config.getFfmpegAcceptVideoCodecs();
        if (CollectionUtil.isNotEmpty(acceptVideoCodecs)) {
            String videoCodec = StrUtil.blankToDefault(probeResult.getVideoCodec(), "").toLowerCase();
            boolean accepted = acceptVideoCodecs.stream()
                    .map(String::toLowerCase)
                    .anyMatch(videoCodec::equals);
            if (!accepted) {
                decision.setNeedVideoTranscode(true);
                log.info("视频编码 [{}] 不在白名单中，需要转码", probeResult.getVideoCodec());
            }
        }

        // 音频编码检测
        List<String> acceptAudioCodecs = config.getFfmpegAcceptAudioCodecs();
        if (CollectionUtil.isNotEmpty(acceptAudioCodecs)) {
            String audioCodec = StrUtil.blankToDefault(probeResult.getAudioCodec(), "").toLowerCase();
            boolean accepted = acceptAudioCodecs.stream()
                    .map(String::toLowerCase)
                    .anyMatch(audioCodec::equals);
            if (!accepted) {
                decision.setNeedAudioTranscode(true);
                log.info("音频编码 [{}] 不在白名单中，需要转码", probeResult.getAudioCodec());
            }
        }

        // 容器格式检测
        List<String> acceptFormats = config.getFfmpegAcceptFormats();
        if (CollectionUtil.isNotEmpty(acceptFormats)) {
            // format_name 可能包含多个格式如 "matroska,webm"，需要拆分比较
            String formatName = StrUtil.blankToDefault(probeResult.getFormatName(), "").toLowerCase();
            String[] formatParts = formatName.split(",");
            boolean accepted = false;
            for (String part : formatParts) {
                String trimmed = part.trim();
                for (String accept : acceptFormats) {
                    if (trimmed.equals(accept.toLowerCase()) || isFormatMatch(trimmed, accept.toLowerCase())) {
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

        return decision;
    }

    /**
     * 格式别名匹配
     * ffprobe 返回的 format_name 可能与用户输入不同（如 matroska vs mkv）
     */
    private static boolean isFormatMatch(String probeFormat, String userFormat) {
        if ("matroska".equals(probeFormat) && "mkv".equals(userFormat)) return true;
        if ("mkv".equals(probeFormat) && "matroska".equals(userFormat)) return true;
        return false;
    }

    /**
     * 获取目标文件扩展名
     */
    public static String getTargetExtension(Config config, ProbeResult probeResult, File sourceFile) {
        String targetFormat = config.getFfmpegFormat();
        if (StrUtil.isNotBlank(targetFormat)) {
            return targetFormat.toLowerCase();
        }
        // 格式为空，保持原扩展名
        return FileUtil.extName(sourceFile).toLowerCase();
    }

    /**
     * 执行 FFmpeg 转码
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @param config     配置
     * @param decision   转码决策
     * @return 是否成功
     */
    public static boolean transcode(File sourceFile, File targetFile, Config config, TranscodeDecision decision) {
        String ffmpegPath = config.getFfmpegPath();
        String sourcePath = FileUtils.getAbsolutePath(sourceFile);
        String targetPath = FileUtils.getAbsolutePath(targetFile);

        // 确保目标目录存在
        FileUtil.mkdir(targetFile.getParentFile());

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(sourcePath);

        // 视频编码
        if (decision.isNeedVideoTranscode()) {
            String videoCodec = config.getFfmpegVideoCodec();
            if (StrUtil.isBlank(videoCodec)) {
                // 目标为空 → 保持原编码
                command.add("-c:v");
                command.add("copy");
            } else {
                command.add("-c:v");
                command.add(videoCodec);
                // CRF 和 preset 仅在实际重编码时生效
                Integer crf = config.getFfmpegCrf();
                if (Objects.nonNull(crf)) {
                    command.add("-crf");
                    command.add(String.valueOf(crf));
                }
                String preset = config.getFfmpegPreset();
                if (StrUtil.isNotBlank(preset)) {
                    command.add("-preset");
                    command.add(preset);
                }
            }
        } else {
            command.add("-c:v");
            command.add("copy");
        }

        // 音频编码
        if (decision.isNeedAudioTranscode()) {
            String audioCodec = config.getFfmpegAudioCodec();
            if (StrUtil.isBlank(audioCodec)) {
                command.add("-c:a");
                command.add("copy");
            } else {
                command.add("-c:a");
                command.add(audioCodec);
            }
        } else {
            command.add("-c:a");
            command.add("copy");
        }

        // 字幕处理
        String subtitleMode = config.getFfmpegSubtitleMode();
        String targetExt = FileUtil.extName(targetFile).toLowerCase();
        if ("remove".equals(subtitleMode)) {
            command.add("-sn");
        } else if ("mp4".equals(targetExt) || "m4v".equals(targetExt)) {
            // MP4 容器不支持 ASS/SSA 等字幕格式的直接 copy，移除字幕避免转码失败
            command.add("-sn");
        } else {
            command.add("-c:s");
            command.add("copy");
        }

        // 额外参数
        String extraArgs = config.getFfmpegExtraArgs();
        if (StrUtil.isNotBlank(extraArgs)) {
            for (String arg : extraArgs.trim().split("\\s+")) {
                if (StrUtil.isNotBlank(arg)) {
                    command.add(arg);
                }
            }
        }

        // 不覆盖已存在文件
        command.add("-n");
        command.add(targetPath);

        log.info("FFmpeg 转码命令: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出日志
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }

            boolean finished = process.waitFor(24, TimeUnit.HOURS);
            if (!finished) {
                process.destroyForcibly();
                log.error("FFmpeg 转码超时: {}", sourcePath);
                return false;
            }

            if (process.exitValue() != 0) {
                log.error("FFmpeg 转码失败, 退出码: {}, 文件: {}", process.exitValue(), sourcePath);
                // 清理失败的目标文件
                FileUtil.del(targetFile);
                return false;
            }

            log.info("FFmpeg 转码完成: {} -> {}", sourceFile.getName(), targetFile.getName());
            return true;
        } catch (Exception e) {
            log.error("FFmpeg 转码异常: {} - {}", sourcePath, e.getMessage(), e);
            FileUtil.del(targetFile);
            return false;
        }
    }

    /**
     * 复制文件到目标路径（源已符合要求时直接复制）
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
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
