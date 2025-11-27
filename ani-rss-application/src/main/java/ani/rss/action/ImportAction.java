package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.dto.ImportAniDataDTO;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 导入订阅
 */
@Slf4j
@Auth
@Path("/ani/import")
public class ImportAction implements BaseAction {

    static final List<Ani> ANI_LIST = AniUtil.ANI_LIST;

    @Override
    @Synchronized("ANI_LIST")
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        ImportAniDataDTO dto = getBody(ImportAniDataDTO.class);
        List<Ani> aniList = dto.getAniList();
        if (aniList.isEmpty()) {
            resultErrorMsg("导入列表为空");
            return;
        }

        ImportAniDataDTO.Conflict conflict = dto.getConflict();

        for (Ani ani : aniList) {
            AniUtil.verify(ani);

            String title = ani.getTitle();
            int season = ani.getSeason();
            Optional<Ani> first = AniUtil.ANI_LIST.stream()
                    .filter(it -> it.getTitle().equals(title) && it.getSeason() == season)
                    .findFirst();

            if (first.isEmpty()) {
                String image = ani.getImage();
                String cover = AniUtil.saveJpg(image);
                ani.setCover(cover)
                        .setId(UUID.fastUUID().toString());
                ANI_LIST.add(ani);
                continue;
            }

            if (conflict == ImportAniDataDTO.Conflict.SKIP) {
                log.info("存在冲突，已跳过 {} 第{}季", title, season);
                continue;
            }

            log.info("存在冲突，已替换 {} 第{}季", title, season);
            String image = ani.getImage();
            String cover = AniUtil.saveJpg(image);
            ani.setCover(cover);

            String[] ignoreProperties = new String[]{"id", "currentEpisodeNumber", "lastDownloadTime"};
            BeanUtil.copyProperties(ani, first.get(), ignoreProperties);
        }

        AniUtil.sync();
        resultSuccessMsg("导入成功");
    }
}
