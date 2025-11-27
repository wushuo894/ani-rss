package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.commons.ExceptionUtil;
import ani.rss.entity.About;
import ani.rss.util.other.UpdateUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


/**
 * 更新
 */
@Slf4j
@Auth
@Path("/update")
public class UpdateAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        About about = UpdateUtil.about();
        try {
            UpdateUtil.update(about);
            resultSuccessMsg("更新成功, 正在重启...");
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.info("更新失败 {}, {}", about.getLatest(), message);
            resultErrorMsg("更新失败 {}, {}", about.getLatest(), message);
        }
    }
}
