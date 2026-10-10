package ani.rss.commons;

import com.github.promeg.pinyinhelper.PinyinMapDict;

import java.util.HashMap;
import java.util.Map;

public class CustomPinyinMapDict extends PinyinMapDict {
    static final Map<String, String[]> MAPPING_MAP = new HashMap<>();

    static {
        MAPPING_MAP.put("重庆", new String[]{"CHONG", "QING"});
        MAPPING_MAP.put("重启", new String[]{"CHONG", "QI"});
        MAPPING_MAP.put("重回", new String[]{"CHONG", "HUI"});
        MAPPING_MAP.put("重生", new String[]{"CHONG", "SHENG"});
        MAPPING_MAP.put("重来", new String[]{"CHONG", "LAI"});
        MAPPING_MAP.put("调教", new String[]{"TIAO", "JIAO"});
    }

    @Override
    public Map<String, String[]> mapping() {
        return MAPPING_MAP;
    }
}
