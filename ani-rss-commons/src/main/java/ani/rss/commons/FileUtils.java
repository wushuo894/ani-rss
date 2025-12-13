package ani.rss.commons;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class FileUtils {

    /**
     * 获取绝对路径 并把 windows 狗日的 \ 转换为 /
     *
     * @param file
     * @return
     */
    public static String getAbsolutePath(File file) {
        String absolutePath = file.getPath();
        if (absolutePath.startsWith("/")) {
            // 已是绝对路径
            return normalize(absolutePath);
        }

        if (ReUtil.contains("^[A-z]:", absolutePath)) {
            // 已是绝对路径
            return normalize(absolutePath);
        }

        absolutePath = file.getAbsolutePath();
        return normalize(absolutePath);
    }

    /**
     * 获取绝对路径 并把 windows 狗日的 \ 转换为 /
     *
     * @param absolutePath
     * @return
     */
    public static String getAbsolutePath(String absolutePath) {
        if (absolutePath.startsWith("/")) {
            // 已是绝对路径
            return normalize(absolutePath);
        }

        if (ReUtil.contains("^[A-z]:", absolutePath)) {
            // 已是绝对路径
            return normalize(absolutePath);
        }

        absolutePath = new File(absolutePath).getAbsolutePath();
        return normalize(absolutePath);
    }

    public static String normalize(String path) {
        path = path.trim();
        String s = cn.hutool.core.io.FileUtil.normalize(path);
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * 获取文件列表 不会存在空指针问题
     *
     * @param path 文件夹位置
     * @return 文件列表
     */
    public static File[] listFiles(String path) {
        return listFiles(new File(path));
    }

    /**
     * 获取文件列表 不会存在空指针问题
     *
     * @param file 文件夹位置
     * @return 文件列表
     */
    public static File[] listFiles(File file) {
        if (Objects.isNull(file)) {
            return new File[0];
        }
        if (!file.exists()) {
            return new File[0];
        }
        if (file.isDirectory()) {
            return ObjectUtil.defaultIfNull(file.listFiles(), new File[0]);
        }
        return new File[0];
    }

    public static List<File> listFileList(String path) {
        return List.of(listFiles(path));
    }
}
