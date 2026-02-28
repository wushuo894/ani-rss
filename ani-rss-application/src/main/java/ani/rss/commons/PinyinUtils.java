package ani.rss.commons;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinEngine;
import cn.hutool.extra.pinyin.engine.tinypinyin.TinyPinyinEngine;

public class PinyinUtils {

    public static final PinyinEngine ENGINE = new TinyPinyinEngine();

    public static PinyinEngine getEngine() {
        return ENGINE;
    }

    public static String getPinyin(String str) {
        return getPinyin(str, StrUtil.SPACE);
    }

    public static String getPinyin(String str, String separator) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        return getEngine().getPinyin(str, separator);
    }

    public static String getFirstLetter(String str, String separator) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        return getEngine().getFirstLetter(str, separator);
    }
}
