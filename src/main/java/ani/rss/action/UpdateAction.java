package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.About;
import ani.rss.util.UpdateUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Path("/update")
public class UpdateAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        About about = UpdateUtil.about();
        try {
            UpdateUtil.update(about);
            resultSuccessMsg("更新成功, 正在重启...");
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug(e.getMessage(), e);
            log.info("更新失败 {}", about.getLatest());
            resultErrorMsg("更新失败 {}", about.getLatest());
        }
    }
}
