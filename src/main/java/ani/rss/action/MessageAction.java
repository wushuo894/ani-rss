package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.msg.Message;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

@Auth
@Path("/message")
public class MessageAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String s = request.getParam("type");
        if (StrUtil.isBlank(s)) {
            resultError();
            return;
        }
        Config config = getBody(Config.class);
        Class<Object> loadClass = ClassUtil.loadClass("ani.rss.msg." + s);
        Message message = (Message) ReflectUtil.newInstance(loadClass);
        Boolean test = message.send(config, "test");
        if (test) {
            resultSuccess();
            return;
        }
        resultError();
    }
}
