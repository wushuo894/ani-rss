package ani.rss.service;

import ani.rss.commons.FileUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.download.BaseDownload;
import ani.rss.entity.Config;
import ani.rss.entity.GitInfo;
import ani.rss.entity.Login;
import ani.rss.entity.ProxyTest;
import ani.rss.entity.web.Result;
import ani.rss.entity.web.ResultCode;
import ani.rss.start.BaseStart;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

@Slf4j
@Service
public class ConfigService {

    @Resource
    private AfdianService afdianService;

    @Resource
    private ClearService clearService;

    @Resource
    private TaskService taskService;

    @Resource
    private GitProperties gitProperties;

    public Config config() {
        String version = MavenUtils.getVersion();
        Config config = ObjectUtil.clone(ConfigUtil.CONFIG);
        config.getLogin().setPassword("");
        config.setVersion(version)
                .setGitInfo(getGitInfo())
                .setVerifyExpirationTime(afdianService.verifyExpirationTime());
        return config;
    }

    public GitInfo getGitInfo() {
        return new GitInfo()
                .setBranch(gitProperties.getBranch())
                .setShortCommitId(gitProperties.getShortCommitId())
                .setCommitId(gitProperties.getCommitId());
    }

    public void setConfig(Config newConfig) {
        Config config = ConfigUtil.CONFIG;
        Login login = config.getLogin();
        String username = login.getUsername();
        String password = login.getPassword();
        Integer renameSleepSeconds = config.getRenameSleepSeconds();
        Integer sleep = config.getRssSleepMinutes();
        String download = config.getDownloadToolType();
        Boolean autoStart = config.getAutoStart();

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
                throw new IllegalArgumentException("代理参数不完整");
            }
        }

        ConfigUtil.sync();
        Integer newRenameSleepSeconds = config.getRenameSleepSeconds();
        Integer newSleep = config.getRssSleepMinutes();
        Boolean newAutoStart = config.getAutoStart();

        // 时间间隔发生改变，重启任务
        if (
                !Objects.equals(newSleep, sleep) ||
                        !Objects.equals(newRenameSleepSeconds, renameSleepSeconds)
        ) {
            taskService.restart();
        }
        // 下载工具发生改变
        if (!download.equals(config.getDownloadToolType())) {
            TorrentUtil.loadDownloadTool();
        }
        // 开机自启发生改变
        if (!newAutoStart.equals(autoStart)) {
            if (BaseStart.isSupported()) {
                BaseStart instance = BaseStart.getInstance();
                instance.sync();
            }
        }
    }

    public String clearCache() {
        File configDir = ConfigUtil.getConfigDir();
        String configDirStr = FileUtils.getAbsolutePath(configDir);

        Long size = clearService.clearCover();

        // 清理 mikan 预览封面
        FileUtil.del(configDirStr + "/img");

        return FileUtils.formatSize(size, true);
    }

    public ProxyTest testProxy(String url, Config config) {
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
                    .setCode(ResultCode.HTTP_INTERNAL_ERROR);
        }

        long end = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());
        proxyTest.setTime(end - start);
        return proxyTest;
    }

    public Boolean downloadLoginTest(Config config) {
        ConfigUtil.format(config);
        String download = config.getDownloadToolType();
        Class<BaseDownload> loadClass = ClassUtil.loadClass("ani.rss.download." + download);
        BaseDownload baseDownload = SpringUtil.getBean(loadClass);
        return baseDownload.login(true, config);
    }


}
