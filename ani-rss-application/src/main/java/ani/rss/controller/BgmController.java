package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.*;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.Opt;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wushuo.tmdb.api.entity.Tmdb;

import java.util.List;
import java.util.Map;

@RestController
public class BgmController extends BaseController {

    @Auth
    @Operation(summary = "搜索BGM条目")
    @PostMapping("/searchBgm")
    public Result<List<BgmInfo>> searchBgm(@RequestParam("name") String name) {
        List<BgmInfo> search = BgmUtil.search(name);
        return Result.success(search);
    }

    @Auth
    @Operation(summary = "将指定id的BGM番剧转换为订阅")
    @PostMapping("/getAniBySubjectId")
    public Result<Ani> getAniBySubjectId(@RequestParam("id") String id) {
        BgmInfo bgmInfo = BgmUtil.getBgmInfo(id, true);
        Ani ani = BgmUtil.toAni(bgmInfo, AniUtil.createAni());
        ani
                .setCustomDownloadPath(true);
        return Result.success(ani);
    }

    @Auth
    @Operation(summary = "获取BGM标题")
    @PostMapping("/getBgmTitle")
    public Result<String> getBgmTitle(@RequestBody Ani ani) {
        Tmdb tmdb = ani.getTmdb();
        BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);
        String finalName = BgmUtil.getFinalName(bgmInfo, tmdb);
        return Result.success(r -> r.setData(finalName));
    }

    @Auth
    @Operation(summary = "获取评分")
    @PostMapping("/rate")
    public Result<Integer> rate(@RequestBody Ani ani) {
        String subjectId = BgmUtil.getSubjectId(ani);
        Integer rate = BgmUtil.rate(subjectId, null);
        return Result.success(rate);
    }

    @Auth
    @Operation(summary = "进行评分")
    @PostMapping("/setRate")
    public Result<Integer> setRate(@RequestBody Ani ani) {
        String subjectId = BgmUtil.getSubjectId(ani);
        Integer score = Opt.ofNullable(ani.getScore())
                .map(Double::intValue)
                .orElse(null);

        Integer rate = BgmUtil.rate(subjectId, score);
        return Result.success(rate).setMessage("保存评分成功");
    }

    @Auth
    @Operation(summary = "获取当前BGM账号信息")
    @PostMapping("/meBgm")
    public Result<BgmMe> meBgm() {
        int expiresDays = BgmUtil.getExpiresDays();
        BgmMe me = BgmUtil.me();
        me.setExpiresDays(expiresDays);
        return Result.success(me);
    }

    @Auth
    @Operation(summary = "BGM授权回调")
    @PostMapping("/bgm/oauth/callback")
    public Result<Void> callback(@RequestParam("code") String code) {
        Config config = ConfigUtil.CONFIG;
        String bgmAppID = config.getBgmAppID();
        String bgmAppSecret = config.getBgmAppSecret();
        String bgmRedirectUri = config.getBgmRedirectUri();

        Map<String, String> map = Map.of(
                "grant_type", "authorization_code",
                "client_id", bgmAppID,
                "client_secret", bgmAppSecret,
                "code", code,
                "redirect_uri", bgmRedirectUri
        );

        HttpReq.post("https://bgm.tv/oauth/access_token")
                .body(GsonStatic.toJson(map))
                .then(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    String accessToken = jsonObject.get("access_token").getAsString();
                    String refreshToken = jsonObject.get("refresh_token").getAsString();
                    config.setBgmToken(accessToken)
                            .setBgmRefreshToken(refreshToken);
                });
        ConfigUtil.sync();
        return Result.success("授权成功, 现在你可以关闭此窗口");
    }
}
