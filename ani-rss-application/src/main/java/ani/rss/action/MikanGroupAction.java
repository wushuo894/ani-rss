package ani.rss.action;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Mikan;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.other.MikanUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.ArrayList;
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
                "繁", "简", "日",
                "cht", "chs", "hevc",
                "10bit", "h265", "h264",
                "内嵌", "内封", "外挂",
                "mp4", "mkv"
        );


        for (Mikan.Group group : groups) {
            List<List<String>> matchList = new ArrayList<>();
            Set<String> tags = new LinkedHashSet<>();
            group.setTags(tags);

            List<TorrentsInfo> items = group.getItems();
            for (TorrentsInfo item : items) {
                String name = item.getName();
                List<String> match = new ArrayList<>();
                for (String s : tagList) {
                    if (name.contains(s)) {
                        tags.add(s);
                        match.add(s);
                        continue;
                    }
                    if (name.contains(s.toUpperCase())) {
                        tags.add(s.toUpperCase());
                        match.add(s.toUpperCase());
                    }
                }
                match = CollUtil.distinct(match);
                matchList.add(match);
            }

            matchList = CollUtil.distinct(matchList, GsonStatic::toJson, true);
            group.setMatchList(matchList);
        }
        resultSuccess(groups);
    }
}
