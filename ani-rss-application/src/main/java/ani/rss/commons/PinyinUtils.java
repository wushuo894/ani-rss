package ani.rss.commons;

import cn.hutool.core.util.ReUtil;
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


    /**
     * 拼音首字母
     *
     * @param str 文字
     * @return 首字母
     */
    public static String getPinyinInitialLetters(String str) {
        if (StrUtil.isBlank(str)) {
            return "0";
        }
        String pinyin = PinyinUtils.getPinyin(str);
        String s = pinyin.toUpperCase().substring(0, 1);
        if (ReUtil.isMatch("^\\d$", s)) {
            s = "0";
        } else if (!ReUtil.isMatch("^[a-zA-Z]$", s)) {
            s = "#";
        }
        return s;
    }
}
