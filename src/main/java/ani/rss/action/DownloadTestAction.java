package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.download.BaseDownload;
import ani.rss.entity.Config;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

/**
 * 测试下载工具
 */
@Auth
@Path("/downloadLoginTest")
public class DownloadTestAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Config config = getBody(Config.class);
        ConfigUtil.format(config);
        String download = config.getDownloadToolType();
        Class<Object> loadClass = ClassUtil.loadClass("ani.rss.download." + download);
        BaseDownload baseDownload = (BaseDownload) ReflectUtil.newInstance(loadClass);
        Boolean login = baseDownload.login(config);
        if (login) {
            resultSuccessMsg("登录成功");
            return;
        }
        resultErrorMsg("登录失败");
    }
}
