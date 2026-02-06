package ani.rss.entity;

import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.enums.ServerChanTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class NotificationConfig implements Serializable {
    /**
     * 启用
     */
    private Boolean enable;

    /**
     * 重试次数
     */
    private Integer retry;

    /**
     * 备注
     */
    private String comment;

    /**
     * 通知模版
     */
    private String notificationTemplate;

    /**
     * 通知类型
     */
    private NotificationTypeEnum notificationType;

    /**
     * 邮箱 smtp
     */
    private String mailSMTPHost;
    /**
     * 邮箱 端口
     */
    private Integer mailSMTPPort;
    /**
     * 邮箱 发件人
     */
    private String mailFrom;
    /**
     * 邮箱 密码
     */
    private String mailPassword;
    /**
     * 邮箱 SSL
     */
    private Boolean mailSSLEnable;
    /**
     * 邮箱 TLS
     */
    private Boolean mailTLSEnable;
    /**
     * 邮箱 收件人
     */
    private String mailAddressee;
    /**
     * 邮箱 发送图片
     */
    private Boolean mailImage;


    /**
     * server酱类型：server酱和server酱3
     */
    private ServerChanTypeEnum serverChanType;
    /**
     * server酱 sendKey
     */
    private String serverChanSendKey;
    /**
     * server酱3 apiUrl
     */
    private String serverChan3ApiUrl;
    /**
     * server酱 标题事件
     */
    private Boolean serverChanTitleAction;


    /**
     * 系统通知
     */
    private Boolean systemMsg;


    /**
     * telegram bot token
     */
    private String telegramBotToken;
    /**
     * telegram chat_id
     */
    private String telegramChatId;
    /**
     * telegram topic id
     */
    private Integer telegramTopicId;
    /**
     * telegram Api Host
     */
    private String telegramApiHost;
    /**
     * telegram 发送图片
     */
    private Boolean telegramImage;
    /**
     * telegram 格式
     */
    private String telegramFormat;


    /**
     * webHookMethod
     */
    private String webHookMethod;
    /**
     * webHookUrl
     */
    private String webHookUrl;
    /**
     * webHookHeader
     */
    private String webHookHeader;
    /**
     * webHookBody
     */
    private String webHookBody;


    /**
     * emby扫描媒体库
     */
    private Boolean embyRefresh;
    /**
     * emby地址
     */
    private String embyHost;
    /**
     * emby api密钥
     */
    private String embyApiKey;
    /**
     * emby扫描媒体库
     */
    private List<String> embyRefreshViewIds;
    /**
     * emby延迟扫描
     */
    private Long embyDelayed;

    private String shell;

    /**
     * 存活限制 秒
     */
    private Integer aliveLimit;

    /**
     * 文件移动目标位置
     */
    private String fileMoveTarget;

    /**
     * 文件移动目标位置 OVA
     */
    private String fileMoveOvaTarget;

    /**
     * 文件移动时删除旧的同集视频
     */
    private Boolean fileMoveDeleteOldEpisode;

    /**
     * 通知 状态
     */
    private List<NotificationStatusEnum> statusList;


    public static NotificationConfig createNotificationConfig() {
        NotificationConfig notificationConfig = new NotificationConfig();

        notificationConfig
                .setEnable(true)
                .setRetry(3)
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

        notificationConfig.setShell("")
                .setAliveLimit(10);

        notificationConfig
                .setFileMoveTarget("/CD2/115/Media/番剧/${title}/Season ${season}")
                .setFileMoveOvaTarget("/CD2/115/Media/剧场版/${title}")
                .setFileMoveDeleteOldEpisode(false);

        return notificationConfig;
    }


}
