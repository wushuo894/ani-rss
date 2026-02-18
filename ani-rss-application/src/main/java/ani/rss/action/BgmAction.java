package ani.rss.action;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.lang.Opt;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.IOException;
import java.util.Objects;

/**
 * bgm
 */
@Auth
@Slf4j
@Path("/bgm")
public class BgmAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String type = request.getParam("type");
        switch (type) {
            case "search" -> {
                // 搜索
                String name = request.getParam("name");
                resultSuccess(BgmUtil.search(name));
            }
            case "getAniBySubjectId" -> {
                // 将指定id的BGM番剧转换为订阅
                String id = request.getParam("id");
                BgmInfo bgmInfo = BgmUtil.getBgmInfo(id, true);
                Ani ani = BgmUtil.toAni(bgmInfo, AniUtil.createAni());
                ani
                        .setCustomDownloadPath(true);
                resultSuccess(ani);
            }
            case "getTitle" -> {
                // 获取BGM标题
                Ani ani = getBody(Ani.class);
                Tmdb tmdb = ani.getTmdb();
                BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);
                resultSuccess(BgmUtil.getFinalName(bgmInfo, tmdb));
            }
            case "rate" -> {
                // 评分
                Ani ani = getBody(Ani.class);
                String subjectId = BgmUtil.getSubjectId(ani);
                Integer score = Opt.ofNullable(ani.getScore())
                        .map(Double::intValue)
                        .orElse(null);
                resultSuccess(result -> {
                    result.setData(BgmUtil.rate(subjectId, score))
                            .setMessage("保存评分成功");
                    if (Objects.isNull(score)) {
                        result.setMessage("");
                    }
                });
            }
            case "me" -> {
                // 获取当前BGM账号信息
                Long expiresDays = BgmUtil.getExpiresDays();
                JsonObject me = BgmUtil.me();
                me.addProperty("expires_days", expiresDays);
                resultSuccess(me);
            }
        }

    }
}
