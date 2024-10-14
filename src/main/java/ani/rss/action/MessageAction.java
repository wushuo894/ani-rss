package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.BigInfo;
import ani.rss.entity.Config;
import ani.rss.msg.Message;
import ani.rss.util.AniUtil;
import ani.rss.util.BgmUtil;
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
        Ani ani = new Ani();
        ani.setBgmUrl("https://bgm.tv/subject/424883");
        BigInfo bgmInfo = BgmUtil.getBgmInfo(ani);
        String image = bgmInfo.getImage();
        ani.setCover(AniUtil.saveJpg(image));
        Boolean test = message.send(config, ani, null, "test");
        if (test) {
            resultSuccess();
            return;
        }
        resultError();
    }
}
