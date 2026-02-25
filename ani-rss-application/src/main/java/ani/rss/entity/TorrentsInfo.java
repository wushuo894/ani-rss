package ani.rss.entity;

import cn.hutool.core.util.NumberUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

/**
 * 种子信息
 */
@Data
@Accessors(chain = true)
@Schema(description = "种子信息")
public class TorrentsInfo implements Serializable {
    @Schema(description = "id")
    private String id;

    /**
     * hash
     */
    @Schema(description = "hash")
    private String hash;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private State state;

    /**
     * 标签
     */
    @Schema(description = "标签")
    private List<String> tags;

    /**
     * 磁链
     */
    @Schema(description = "磁链")
    private String magnet;

    /**
     * 已下载的大小
     */
    @Schema(description = "已下载的大小")
    private Long completed;

    /**
     * 大小
     */
    @Schema(description = "大小")
    private Long size;

    /**
     * 进度
     */
    @Schema(description = "进度")
    private Double progress;

    /**
     * 大小
     */
    @Schema(description = "大小(字符串)")
    private String sizeStr;

    /**
     * 时间
     */
    @Schema(description = "时间")
    private String dateStr;

    /**
     * 下载位置
     */
    @Schema(description = "下载位置")
    private String downloadDir;

    /**
     * 种子地址
     */
    @Schema(description = "种子地址")
    private String torrent;

    /**
     * 文件列表
     */
    @Schema(description = "文件列表")
    private Supplier<List<String>> files;

    public TorrentsInfo progress(long completed, long size) {
        if (size < 1) {
            size = 1;
            this.setProgress(0.0);
        } else {
            this.setProgress(
                    NumberUtil.round((completed * 1.0 / size) * 100, 2).doubleValue()
            );
        }

        this.setCompleted(completed);
        this.setSize(size);
        this.setSizeStr(NumberUtil.roundStr((size * 1.0) / 1024 / 1024, 2));
        return this;
    }

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
        stoppedDL,
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
         * 排队中(上传)
         */
        queuedUP,
        /**
         * 已完成
         */
        pausedUP,
        stoppedUP,
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


