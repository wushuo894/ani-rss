package ani.rss.util.other;

import ani.rss.commons.GsonStatic;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class SourceSelectorUtil {

    public record SelectorData<T>(List<List<T>> regexList, Set<String> tags) {
    }

    public static <T> SelectorData<T> build(List<String> titles, BiFunction<String, String, T> itemBuilder) {
        List<String> patterns = ConfigUtil.CONFIG.getSourceSelectorPatterns();
        if (CollUtil.isEmpty(patterns)) {
            return new SelectorData<>(new ArrayList<>(), new HashSet<>());
        }

        Set<String> tags = new HashSet<>();
        List<List<T>> regexList = new ArrayList<>();
        for (String title : titles) {
            List<T> regexItems = new ArrayList<>();
            for (String regex : patterns) {
                if (StrUtil.isBlank(regex) || !ReUtil.contains(regex, title)) {
                    continue;
                }
                String label = ReUtil.get(regex, title, 0);
                label = StrUtil.upperFirst(label).toUpperCase();
                regexItems.add(itemBuilder.apply(label, regex));
                tags.add(label);
            }
            regexItems = CollUtil.distinct(regexItems, GsonStatic::toJson, true);
            regexList.add(regexItems);
        }

        regexList = CollUtil.distinct(regexList, GsonStatic::toJson, true);
        return new SelectorData<>(regexList, tags);
    }
}
