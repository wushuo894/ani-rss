package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.commons.MavenUtil;
import ani.rss.util.ServerUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 关闭或重启
 */
@Slf4j
@Auth
@Path("/stop")
public class StopAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String statusStr = request.getParam("status");
        int status = Integer.parseInt(statusStr);
        String s = List.of("重启", "关闭").get(status);
        log.info("正在{}", s);
        resultSuccessMsg("正在{}", s);
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(3000);
            File jar = MavenUtil.getJar();
            String extName = FileUtil.extName(jar);
            ServerUtil.stop();
            if ("exe".equals(extName) && status == 0) {
                log.info("正在重启 {}", jar.getName());
                RuntimeUtil.exec(jar.getName());
                System.exit(status);
                return;
            }
            System.exit(status);
        });
    }
}
