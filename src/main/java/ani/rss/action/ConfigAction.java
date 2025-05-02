package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.*;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * 设置
 */
@Auth
@Path("/config")
public class ConfigAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        String method = req.getMethod();
        if (method.equals("GET")) {
            String version = MavenUtil.getVersion();
            Config config = ObjectUtil.clone(ConfigUtil.CONFIG);
            config.getLogin().setPassword("");
            config.setVersion(version)
                    .setVerifyExpirationTime(AfdianUtil.verifyExpirationTime());
            resultSuccess(config);
            return;
        }

        if (!method.equals("POST")) {
            return;
        }
        Config config = ConfigUtil.CONFIG;
        Login login = config.getLogin();
        String username = login.getUsername();
        String password = login.getPassword();
        Double renameSleep = config.getRenameSleep();
        Integer sleep = config.getSleep();
        Integer gcSleep = config.getGcSleep();
        String download = config.getDownload();
        BeanUtil.copyProperties(
                getBody(Config.class)
                        .setExpirationTime(null)
                        .setOutTradeNo(null)
                        .setTryOut(null),
                config,
                CopyOptions
                        .create()
                        .setIgnoreNullValue(true)
        );
        String host = config.getHost();
        if (StrUtil.isNotBlank(host)) {
            if (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }
        }
        if (StrUtil.isNotBlank(host)) {
            if (!ReUtil.contains("http(s*)://", host)) {
                host = "http://" + host;
            }
        }
        config.setHost(host);

        Boolean proxy = ObjectUtil.defaultIfNull(config.getProxy(), false);
        if (proxy) {
            String proxyHost = config.getProxyHost();
            Integer proxyPort = config.getProxyPort();
            if (StrUtil.isBlank(proxyHost) || Objects.isNull(proxyPort)) {
                resultErrorMsg("代理参数不完整");
                return;
            }
        }
        String loginPassword = config.getLogin().getPassword();
        // 密码未发生修改
        if (StrUtil.isBlank(loginPassword)) {
            config.getLogin().setPassword(password);
        }
        String loginUsername = config.getLogin().getUsername();
        if (StrUtil.isBlank(loginUsername)) {
            config.getLogin().setUsername(username);
        }

        // 下载地址后面不要带 斜杠
        String downloadPath = FilePathUtil.getAbsolutePath(config.getDownloadPath());
        String ovaDownloadPath = FilePathUtil.getAbsolutePath(config.getOvaDownloadPath());
        if (downloadPath.endsWith("/")) {
            downloadPath = downloadPath.substring(0, downloadPath.length() - 1);
        }
        if (ovaDownloadPath.endsWith("/")) {
            ovaDownloadPath = ovaDownloadPath.substring(0, ovaDownloadPath.length() - 1);
        }
        config.setDownloadPath(downloadPath)
                .setOvaDownloadPath(ovaDownloadPath);

        String telegramApiHost = config.getTelegramApiHost();
        if (telegramApiHost.endsWith("/")) {
            telegramApiHost = telegramApiHost.substring(0, telegramApiHost.length() - 1);
        }
        config.setTelegramApiHost(telegramApiHost);

        ConfigUtil.sync();
        Double newRenameSleep = config.getRenameSleep();
        Integer newSleep = config.getSleep();
        Integer newGcSleep = config.getGcSleep();

        // 时间间隔发生改变，重启任务
        if (
                !Objects.equals(newSleep, sleep) ||
                        !Objects.equals(newRenameSleep, renameSleep) ||
                        !Objects.equals(newGcSleep, gcSleep)
        ) {
            TaskUtil.restart();
        }
        // 下载工具发生改变
        if (!download.equals(config.getDownload())) {
            TorrentUtil.load();
        }

        resultSuccessMsg("修改成功");
    }
}
