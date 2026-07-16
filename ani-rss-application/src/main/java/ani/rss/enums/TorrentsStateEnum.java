package ani.rss.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "种子状态")
public enum TorrentsStateEnum {
    @Schema(description = "未知状态")
    unknown,
    @Schema(description = "强制下载，忽略队列限制")
    forcedDL,
    @Schema(description = "种子正在下载，数据正在传输")
    downloading,
    @Schema(description = "强制获取元数据，忽略队列限制")
    forcedMetaDL,
    @Schema(description = "种子刚开始下载，正在获取元数据")
    metaDL,
    @Schema(description = "停滞中")
    stalledDL,
    @Schema(description = "强制上传，忽略队列限制")
    forcedUP,
    @Schema(description = "种子正在上传，数据正在传输")
    uploading,
    @Schema(description = "种子正在上传，但未建立任何连接")
    stalledUP,
    @Schema(description = "qBt启动时检查恢复数据")
    checkingResumeData,
    @Schema(description = "队列已启用，种子正在排队等待下载")
    queuedDL,
    @Schema(description = "队列已启用，种子正在排队等待上传")
    queuedUP,
    @Schema(description = "种子已完成下载，正在检查")
    checkingUP,
    @Schema(description = "与checkingUP相同，但种子尚未完成下载")
    checkingDL,
    @Schema(description = "种子已暂停且尚未完成下载 >= 5.0.0")
    stoppedDL,
    @Schema(description = "种子已暂停且尚未完成下载")
    pausedDL,
    @Schema(description = "种子已暂停且已完成下载 >= 5.0.0")
    stoppedUP,
    @Schema(description = "种子已暂停且已完成下载")
    pausedUP,
    @Schema(description = "种子正在移动到另一个位置")
    moving,
    @Schema(description = "种子数据文件缺失")
    missingFiles,
    @Schema(description = "发生了一些错误，适用于已暂停的种子文件")
    error,
    @Schema(description = "种子正在为下载分配磁盘空间")
    allocating
}
