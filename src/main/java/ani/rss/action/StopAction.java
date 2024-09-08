package ani.rss.action;

import ani.rss.annotation.Path;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
@Path("/stop")
public class StopAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String statusStr = request.getParam("status");
        int status = Integer.parseInt(statusStr);
        String s = List.of("重启", "关闭").get(status);
        log.info("正在 {}", s);
        resultSuccessMsg("正在 {}", s);
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(3000);
            System.exit(status);
        });
    }
}
