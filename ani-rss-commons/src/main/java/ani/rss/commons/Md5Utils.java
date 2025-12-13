package ani.rss.commons;

import cn.hutool.core.util.StrUtil;

public class Md5Utils {
    /**
     * 校验MD5文本
     *
     * @param md5 文本
     * @return 结果
     */
    public static boolean isValidMD5(String md5) {
        if (StrUtil.isBlank(md5)) {
            return false;
        }
        return md5.matches("^[a-fA-F0-9]{32}$");
    }

}
