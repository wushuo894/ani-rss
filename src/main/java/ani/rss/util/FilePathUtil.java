package ani.rss.util;

import cn.hutool.core.io.FileUtil;

import java.io.File;

public class FilePathUtil {

    /**
     * 获取绝对路径 并把 windows 狗日的 \ 转换为 /
     *
     * @param file
     * @return
     */
    public static String getAbsolutePath(File file) {
        String absolutePath = file.getAbsolutePath();
        return FileUtil.normalize(absolutePath);
    }

    /**
     * 获取绝对路径 并把 windows 狗日的 \ 转换为 /
     *
     * @param file
     * @return
     */
    public static String getAbsolutePath(String file) {
        return FileUtil.getAbsolutePath(file);
    }
}
