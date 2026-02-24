package ani.rss.entity;

import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.enums.ServerChanTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@Schema(description = "通知配置")
public class NotificationConfig implements Serializable {
    /**
     * 启用
     */
    @Schema(description = "启用")
    private Boolean enable;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retry;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String comment;

    /**
     * 通知模版
     */
    @Schema(description = "通知模版")
    private String notificationTemplate;

    /**
     * 通知类型
     */
    @Schema(description = "通知类型")
    private NotificationTypeEnum notificationType;

    /**
     * 邮箱 smtp
     */
    @Schema(description = "邮箱 SMTP")
    private String mailSMTPHost;
    /**
     * 邮箱 端口
     */
    @Schema(description = "邮箱 端口")
    private Integer mailSMTPPort;
    /**
     * 邮箱 发件人
     */
    @Schema(description = "邮箱 发件人")
    private String mailFrom;
    /**
     * 邮箱 密码
     */
    @Schema(description = "邮箱 密码")
    private String mailPassword;
    /**
     * 邮箱 SSL
     */
    @Schema(description = "邮箱 SSL")
    private Boolean mailSSLEnable;
    /**
     * 邮箱 TLS
     */
    @Schema(description = "邮箱 TLS")
    private Boolean mailTLSEnable;
    /**
     * 邮箱 收件人
     */
    @Schema(description = "邮箱 收件人")
    private String mailAddressee;
    /**
     * 邮箱 发送图片
     */
    @Schema(description = "邮箱 发送图片")
    private Boolean mailImage;


    /**
     * server酱类型：server酱和server酱3
     */
    @Schema(description = "server酱类型")
    private ServerChanTypeEnum serverChanType;
    /**
     * server酱 sendKey
     */
    @Schema(description = "server酱 sendKey")
    private String serverChanSendKey;
    /**
     * server酱3 apiUrl
     */
    @Schema(description = "server酱3 apiUrl")
    private String serverChan3ApiUrl;
    /**
     * server酱 标题事件
     */
    @Schema(description = "server酱 标题事件")
    private Boolean serverChanTitleAction;


    /**
     * 系统通知
     */
    @Schema(description = "系统通知")
    private Boolean systemMsg;


    /**
     * telegram bot token
     */
    @Schema(description = "telegram bot token")
    private String telegramBotToken;
    /**
     * telegram chat_id
     */
    @Schema(description = "telegram chat_id")
    private String telegramChatId;
    /**
     * telegram topic id
     */
    @Schema(description = "telegram topic id")
    private Integer telegramTopicId;
    /**
     * telegram Api Host
     */
    @Schema(description = "telegram Api Host")
    private String telegramApiHost;
    /**
     * telegram 发送图片
     */
    @Schema(description = "telegram 发送图片")
    private Boolean telegramImage;
    /**
     * telegram 格式
     */
    @Schema(description = "telegram 格式")
    private String telegramFormat;


    /**
     * webHookMethod
     */
    @Schema(description = "WebHook 方法")
    private String webHookMethod;
    /**
     * webHookUrl
     */
    @Schema(description = "WebHook 地址")
    private String webHookUrl;
    /**
     * webHookHeader
     */
    @Schema(description = "WebHook Header")
    private String webHookHeader;
    /**
     * webHookBody
     */
    @Schema(description = "WebHook Body")
    private String webHookBody;


    /**
     * emby扫描媒体库
     */
    @Schema(description = "Emby 扫描媒体库")
    private Boolean embyRefresh;
    /**
     * emby地址
     */
    @Schema(description = "Emby 地址")
    private String embyHost;
    /**
     * emby api密钥
     */
    @Schema(description = "Emby API 密钥")
    private String embyApiKey;
    /**
     * emby扫描媒体库
     */
    @Schema(description = "Emby 扫描媒体库 ID 列表")
    private List<String> embyRefreshViewIds;
    /**
     * emby延迟扫描
     */
    @Schema(description = "Emby 延迟扫描")
    private Long embyDelayed;

    @Schema(description = "Shell 命令")
    private String shell;

    /**
     * 存活限制 秒
     */
    @Schema(description = "存活限制 秒")
    private Integer aliveLimit;

    /**
     * 文件移动目标位置
     */
    @Schema(description = "文件移动目标位置")
    private String fileMoveTarget;

    /**
     * 文件移动目标位置 OVA
     */
    @Schema(description = "文件移动目标位置 OVA")
    private String fileMoveOvaTarget;

    /**
     * 文件移动时删除旧的同集视频
     */
    @Schema(description = "文件移动时删除旧的同集视频")
    private Boolean fileMoveDeleteOldEpisode;

    /**
     * OpenList Host
     */
    @Schema(description = "OpenList Host")
    private String openListUploadHost;

    /**
     * OpenList ApiKey
     */
    @Schema(description = "OpenList ApiKey")
    private String openListUploadApiKey;

    /**
     * OpenList 上传位置
     */
    @Schema(description = "OpenList 上传位置")
    private String openListUploadPath;

    /**
     * OpenList OVA/剧场版 上传位置
     */
    @Schema(description = "OpenList OVA/剧场版 上传位置")
    private String openListUploadOvaPath;

    /**
     * 上传完成后删除本地文件
     */
    @Schema(description = "上传完成后删除本地文件")
    private Boolean openListUploadDeleteLocalFile;

    /**
     * 删除同及文件
     */
    @Schema(description = "删除同集文件")
    private Boolean openListUploadDeleteOldEpisode;

    /**
     * 通知 状态
     */
    @Schema(description = "通知状态")
    private List<NotificationStatusEnum> statusList;

    /**
     * 顺序
     */
    @Schema(description = "顺序")
    private Long sort;

    public static NotificationConfig createNotificationConfig() {
        NotificationConfig notificationConfig = new NotificationConfig();

        notificationConfig
                .setEnable(true)
                .setRetry(3)
                .setSort(10L)
                .setNotificationType(NotificationTypeEnum.TELEGRAM)
                .setNotificationTemplate("${notification}")
                .setComment("")
                .setStatusList(List.of(
                        NotificationStatusEnum.DOWNLOAD_START,
                        NotificationStatusEnum.OMIT,
                        NotificationStatusEnum.ERROR
                ));

        // 邮箱
        notificationConfig
                .setMailSMTPHost("smtp.qq.com")
                .setMailSMTPPort(465)
                .setMailFrom("")
                .setMailPassword("")
                .setMailAddressee("")
                .setMailImage(true)
                .setMailSSLEnable(true)
                .setMailTLSEnable(false);

        // telegram
        notificationConfig
                .setTelegramChatId("")
                .setTelegramBotToken("")
                .setTelegramApiHost("https://api.telegram.org")
                .setTelegramImage(true)
                .setTelegramTopicId(-1)
                .setTelegramFormat("");

        // webhook
        notificationConfig
                .setWebHookBody("")
                .setWebHookUrl("")
                .setWebHookHeader("")
                .setWebHookMethod("POST");

        // server-chan
        notificationConfig
                .setServerChanType(ServerChanTypeEnum.SERVER_CHAN)
                .setServerChanSendKey("")
                .setServerChan3ApiUrl("")
                .setServerChanTitleAction(true);

        // emby
        notificationConfig
                .setEmbyRefresh(false)
                .setEmbyApiKey("")
                .setEmbyRefreshViewIds(new ArrayList<>())
                .setEmbyDelayed(0L);

        // SHELL
        notificationConfig.setShell("")
                .setAliveLimit(10);

        // FileMove
        notificationConfig
                .setFileMoveTarget("/CD2/115/Media/番剧/${title}/Season ${season}")
                .setFileMoveOvaTarget("/CD2/115/Media/剧场版/${title}")
                .setFileMoveDeleteOldEpisode(false);

        // OpenList
        notificationConfig
                .setOpenListUploadHost("http://127.0.0.1:5244")
                .setOpenListUploadApiKey("")
                .setOpenListUploadPath("/115/Media/番剧/${title}/Season ${season}")
                .setOpenListUploadOvaPath("/115/Media/剧场版/${title}")
                .setOpenListUploadDeleteLocalFile(false)
                .setOpenListUploadDeleteOldEpisode(false);

        return notificationConfig;
    }


}
