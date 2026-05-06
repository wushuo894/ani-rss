package ani.rss.commons;

import ani.rss.entity.GroupRegex;
import ani.rss.entity.GroupRegex.RegexItem;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GroupRegexUtils {

    private final static List<String> REGEX_LIST = List.of(
            "1920[Xx]1080", "3840[Xx]2160",
            "1080[Pp]", "720[Pp]", "4[Kk]",
            "繁", "简", "日",
            "内嵌", "内封", "外挂",
            "cht|Cht|CHT", "chs|Chs|CHS",
            "avc|Avc|AVC", "hevc|Hevc|HEVC",
            "h264|H264", "h265|H265",
            "10bit|10Bit|10BIT",
            "mp4|MP4", "mkv|MKV"
    );

    @Synchronized("REGEX_LIST")
    public static <T> GroupRegex toGroupRegx(List<T> list, Function<T, String> getFun) {
        List<String> titles = list.stream()
                .map(getFun)
                .distinct()
                .toList();

        List<List<RegexItem>> regexList = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        for (String title : titles) {
            List<RegexItem> regexItems = new ArrayList<>();
            for (String regex : REGEX_LIST) {
                if (!ReUtil.contains(regex, title)) {
                    continue;
                }
                String tag = ReUtil.get(regex, title, 0);

                RegexItem regexItem = new RegexItem();
                regexItem.setRegex(regex)
                        .setLabel(tag);

                regexItems.add(regexItem);

                if (tags.size() < 5 && !tags.contains(tag)) {
                    tags.add(tag);
                }
            }
            if (regexItems.isEmpty()) {
                continue;
            }
            regexItems = CollUtil.distinct(regexItems, GsonStatic::toJson, true);
            regexList.add(regexItems);
        }

        regexList = CollUtil.distinct(regexList, GsonStatic::toJson, true);

        return new GroupRegex()
                .setRegexList(regexList)
                .setTags(tags);
    }
}
