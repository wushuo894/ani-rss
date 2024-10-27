package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
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

/**
 * 通知
 */
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
        Ani ani = Ani.bulidAni();
        ani.setBgmUrl("https://bgm.tv/subject/424883");
        BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);
        String image = bgmInfo.getImage();
        ani.setCover(AniUtil.saveJpg(image))
                .setImage(image)
                .setTitle("test")
                .setSeason(1)
                .setCurrentEpisodeNumber(2)
                .setTotalEpisodeNumber(12)
                .setScore(8.0)
                .setThemoviedbName("test")
                .setYear(2024)
                .setSubgroup("未知字幕组");
        Boolean test = message.send(config, ani, "test", null);
        if (test) {
            resultSuccess();
            return;
        }
        resultError();
    }
}
