package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class TorrentsInfo implements Serializable {
    private String id;

    /**
     * hash
     */
    private String hash;

    /**
     * 名称
     */
    private String name;

    /**
     * 状态
     */
    private State state;

    /**
     * 标签
     */
    private String tags;

    /**
     * 大小
     */
    private String sizeStr;

    /**
     * 时间
     */
    private String dateStr;

    /**
     * 下载位置
     */
    private String downloadDir;

    /**
     * 文件列表
     */
    private List<String> files;

    public enum State {
        /**
         * 校验恢复数据
         */
        checkingResumeData,
        /**
         * 正在检验磁盘文件
         */
        checkingDisk,
        /**
         * [F] 下载中
         */
        forcedDL,
        /**
         * 停滞中
         */
        stalledDL,
        /**
         * 已暂停
         */
        pausedDL,
        /**
         * 队列中
         */
        queuedDL,
        /**
         * 下载中
         */
        downloading,
        /**
         * 做种中
         */
        stalledUP,
        /**
         * 错误
         */
        error,
        /**
         * 上传中
         */
        uploading,
        /**
         * 已完成
         */
        pausedUP,
        /**
         * [F]元数据
         */
        forcedMetaDownload,
        /**
         * 元数据
         */
        metaDownload,
        /**
         * 缺失文件
         */
        missingFiles,
        /**
         * 未知
         */
        unknown
    }
}


