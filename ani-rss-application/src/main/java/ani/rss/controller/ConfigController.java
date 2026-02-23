package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.FileUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.config.CronConfig;
import ani.rss.entity.*;
import ani.rss.service.ClearService;
import ani.rss.service.TaskService;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AfdianUtil;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final CronConfig cronConfig;

    /**
     * 构建信息
     */
    public String buildInfo() {
        String buildInfo = "";
        try {
            buildInfo = ResourceUtil.readUtf8Str("build_info");
        } catch (Exception ignored) {
        }
        return buildInfo;
    }

    @Auth
    @Operation(summary = "获取设置")
    @PostMapping("/config")
    public Result<Config> config() {
        String version = MavenUtils.getVersion();
        String buildInfo = buildInfo();
        Config config = ObjectUtil.clone(ConfigUtil.CONFIG);
        config.getLogin().setPassword("");
        config.setVersion(version)
                .setBuildInfo(buildInfo)
                .setVerifyExpirationTime(AfdianUtil.verifyExpirationTime());
        return Result.success(config);
    }

    @Auth
    @Operation(summary = "修改设置")
    @PostMapping("/setConfig")
    public Result<Void> setConfig(@RequestBody Config newConfig) {
        Config config = ConfigUtil.CONFIG;
        Login login = config.getLogin();
        String username = login.getUsername();
        String password = login.getPassword();
        Integer renameSleepSeconds = config.getRenameSleepSeconds();
        Integer sleep = config.getRssSleepMinutes();
        String download = config.getDownloadToolType();

        newConfig.setExpirationTime(null)
                .setOutTradeNo(null)
                .setTryOut(null);

        CopyOptions copyOptions = CopyOptions
                .create()
                .setIgnoreNullValue(true);

        BeanUtil.copyProperties(
                newConfig,
                config,
                copyOptions
        );

        String loginPassword = config.getLogin().getPassword();
        // 密码未发生修改
        if (StrUtil.isBlank(loginPassword)) {
            config.getLogin().setPassword(password);
        }
        String loginUsername = config.getLogin().getUsername();
        if (StrUtil.isBlank(loginUsername)) {
            config.getLogin().setUsername(username);
        }

        Boolean proxy = config.getProxy();
        if (proxy) {
            String proxyHost = config.getProxyHost();
            Integer proxyPort = config.getProxyPort();
            if (StrUtil.isBlank(proxyHost) || Objects.isNull(proxyPort)) {
                return Result.error("代理参数不完整");
            }
        }

        ConfigUtil.sync();
        Integer newRenameSleepSeconds = config.getRenameSleepSeconds();
        Integer newSleep = config.getRssSleepMinutes();

        // 时间间隔发生改变，重启任务
        if (
                !Objects.equals(newSleep, sleep) ||
                        !Objects.equals(newRenameSleepSeconds, renameSleepSeconds)
        ) {
            TaskService.restart();
        }
        // 下载工具发生改变
        if (!download.equals(config.getDownloadToolType())) {
            TorrentUtil.load();
        }

        return Result.success("修改成功");
    }

    @Auth
    @Operation(summary = "清理缓存")
    @PostMapping("/clearCache")
    public Result<Void> clearCache() {
        File configDir = ConfigUtil.getConfigDir();
        String configDirStr = FileUtils.getAbsolutePath(configDir);

        Set<String> covers = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getCover)
                .map(s -> FileUtils.getAbsolutePath(new File(configDirStr + "/files/" + s)))
                .collect(Collectors.toSet());

        FileUtil.mkdir(configDirStr + "/files");
        FileUtil.mkdir(configDirStr + "/img");

        Set<File> files = FileUtil.loopFiles(configDirStr + "/files")
                .stream()
                .filter(file -> {
                    String fileName = FileUtils.getAbsolutePath(file);
                    return !covers.contains(fileName);
                }).collect(Collectors.toSet());
        long filesSize = files.stream()
                .mapToLong(File::length)
                .sum();
        long imgSize = FileUtil.size(new File(configDirStr + "/img"));

        long sumSize = filesSize + imgSize;

        if (sumSize < 1) {
            return Result.success("清理完成, 共清理{}MB", 0);
        }

        for (File file : files) {
            FileUtil.del(file);
            ClearService.clearParentFile(file);
        }

        FileUtil.del(configDirStr + "/img");

        String mb = NumberUtil.decimalFormat("0.00", sumSize / 1024.0 / 1024.0);

        return Result.success("清理完成, 共清理{}MB", mb);
    }

    @Auth
    @Operation(summary = "更新trackers")
    @PostMapping("/trackersUpdate")
    public Result<Void> trackersUpdate(@RequestBody Config config) {
        cronConfig.updateTrackers(config);
        return Result.success();
    }

    @Auth
    @Operation(summary = "代理测试")
    @PostMapping("/testProxy")
    public Result<ProxyTest> testProxy(@RequestParam("url") String url, @RequestBody Config config) {
        url = Base64.decodeStr(url);

        log.info(url);

        HttpRequest httpRequest = HttpReq.get(url);
        HttpReq.setProxy(httpRequest, config);

        ProxyTest proxyTest = new ProxyTest();
        Result<ProxyTest> result = Result.success(proxyTest);

        long start = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());
        try {
            httpRequest
                    .then(res -> {
                        int status = res.getStatus();
                        proxyTest.setStatus(status);

                        String title = Jsoup.parse(res.body())
                                .title();
                        result.setMessage(StrFormatter.format("测试成功 {}", title));
                    });
        } catch (Exception e) {
            result.setMessage(e.getMessage())
                    .setCode(HttpStatus.HTTP_INTERNAL_ERROR);
        }

        long end = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());
        proxyTest.setTime(end - start);
        return result;
    }
}
