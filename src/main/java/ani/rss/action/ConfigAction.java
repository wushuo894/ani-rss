package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TaskUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.Objects;

@Auth
@Path("/config")
public class ConfigAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        String method = req.getMethod();
        if (method.equals("GET")) {
            Config config = ObjectUtil.clone(ConfigUtil.CONFIG);
            config.getLogin().setPassword("");
            resultSuccess(config);
            return;
        }

        if (!method.equals("POST")) {
            return;
        }
        Config config = ConfigUtil.CONFIG;
        Login login = config.getLogin();
        String password = login.getPassword();
        Integer renameSleep = config.getRenameSleep();
        Integer sleep = config.getSleep();
        String download = config.getDownload();
        BeanUtil.copyProperties(gson.fromJson(req.getBody(), Config.class), config);
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

        // 下载地址后面不要带 斜杠
        String downloadPath = config.getDownloadPath().replace("\\", "/");
        String ovaDownloadPath = config.getOvaDownloadPath().replace("\\", "/");
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
        Integer newRenameSleep = config.getRenameSleep();
        Integer newSleep = config.getSleep();

        // 时间间隔发生改变，重启任务
        if (!Objects.equals(newSleep, sleep) ||
                !Objects.equals(newRenameSleep, renameSleep)) {
            TaskUtil.restart();
        }
        // 下载工具发生改变
        if (!download.equals(config.getDownload())) {
            TorrentUtil.load();
        }

        resultSuccessMsg("修改成功");
    }
}
