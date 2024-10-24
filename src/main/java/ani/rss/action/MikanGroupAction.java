package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Mikan;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.MikanUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mikan字幕组
 */
@Auth
@Path("/mikan/group")
public class MikanGroupAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String url = request.getParam("url");
        List<Mikan.Group> groups = MikanUtil.getGroups(url);

        List<String> tagList = List.of(
                "1920x1080", "3840x2160", "1080p", "4k", "720p",
                "重制", "修复", "end", "抢先版",
                "繁", "简", "日",
                "cht", "chs", "hevc",
                "10bit", "h265", "h264",
                "内嵌", "外挂",
                "mp4", "mkv"
        );

        for (Mikan.Group group : groups) {
            Set<String> tags = new LinkedHashSet<>();
            group.setTags(tags);

            List<TorrentsInfo> items = group.getItems();
            for (TorrentsInfo item : items) {
                String name = item.getName().toLowerCase();
                for (String s : tagList) {
                    if (!name.contains(s)) {
                        continue;
                    }
                    tags.add(s);
                }
            }
        }
        resultSuccess(groups);
    }
}
