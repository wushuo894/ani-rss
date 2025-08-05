package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.util.AniUtil;
import ani.rss.util.GsonStatic;
import cn.hutool.core.lang.UUID;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonArray;
import lombok.Synchronized;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 导入订阅
 */
@Auth
@Path("/ani/import")
public class ImportAction implements BaseAction {

    static final List<Ani> ANI_LIST = AniUtil.ANI_LIST;

    @Override
    @Synchronized("ANI_LIST")
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        JsonArray jsonArray = getBody(JsonArray.class);
        List<Ani> anis = jsonArray.asList().stream()
                .map(it -> GsonStatic.fromJson(it, Ani.class))
                .toList();
        if (anis.isEmpty()) {
            resultErrorMsg("导入列表为空");
            return;
        }
        for (Ani ani : anis) {
            String title = ani.getTitle();
            Optional<Ani> first = AniUtil.ANI_LIST.stream()
                    .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                    .findFirst();
            if (first.isPresent()) {
                resultErrorMsg("订阅标题重复 {}", title);
                return;
            }
            AniUtil.verify(ani);
            String image = ani.getImage();
            String cover = AniUtil.saveJpg(image);
            ani.setCover(cover)
                    .setId(UUID.fastUUID().toString());
        }
        ANI_LIST.addAll(anis);
        AniUtil.sync();
        resultSuccessMsg("导入成功");
    }
}
