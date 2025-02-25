package ani.rss.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.Synchronized;

import java.io.File;


/**
 * 对MD5增加线程安全
 */
public class Md5Util {
    private static final MD5 MD5 = new MD5();


    /**
     * 生成文件摘要
     *
     * @param data 被摘要数据
     * @return 摘要
     */
    @Synchronized("MD5")
    public static String digestHex(String data) {
        return MD5.digestHex(data);
    }

    /**
     * 生成摘要，并转为16进制字符串<br>
     *
     * @param data 被摘要数据
     * @return 摘要
     */
    @Synchronized("MD5")
    public static String digestHex(byte[] data) {
        return MD5.digestHex(data);
    }

    /**
     * 生成文件摘要，并转为16进制字符串<br>
     * 使用默认缓存大小，见 {@link IoUtil#DEFAULT_BUFFER_SIZE}
     *
     * @param file 被摘要文件
     * @return 摘要
     */
    @Synchronized("MD5")
    public static String digestHex(File file) {
        return MD5.digestHex(file);
    }

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
