package ani.rss.action;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Mikan;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.other.MikanUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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

        List<String> regexItemList = List.of(
                "1920[Xx]1080", "3840[Xx]2160", "1080[Pp]", "4[Kk]", "720[Pp]",
                "繁", "简", "日",
                "cht|Cht|CHT", "chs|Chs|CHS", "hevc|Hevc|HEVC",
                "10bit|10Bit|10BIT", "h265|H265", "h264|H264",
                "内嵌", "内封", "外挂",
                "mp4|MP4", "mkv|MKV"
        );

        for (Mikan.Group group : groups) {
            Set<String> tags = new HashSet<>();
            List<List<Mikan.RegexItem>> regexList = new ArrayList<>();
            List<TorrentsInfo> items = group.getItems();
            for (TorrentsInfo item : items) {
                String name = item.getName();
                List<Mikan.RegexItem> regexItems = new ArrayList<>();
                for (String regex : regexItemList) {
                    if (!ReUtil.contains(regex, name)) {
                        continue;
                    }
                    String label = ReUtil.get(regex, name, 0);
                    label = label.toUpperCase();
                    Mikan.RegexItem regexItem = new Mikan.RegexItem(label, regex);
                    regexItems.add(regexItem);
                    tags.add(label);
                }
                regexItems = CollUtil.distinct(regexItems, GsonStatic::toJson, true);
                regexList.add(regexItems);
            }

            regexList = CollUtil.distinct(regexList, GsonStatic::toJson, true);
            group.setRegexList(regexList)
                    .setTags(tags);
        }
        resultSuccess(groups);
    }
}
