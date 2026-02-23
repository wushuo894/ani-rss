package ani.rss.notification;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.extra.mail.JakartaMailUtil;
import cn.hutool.extra.mail.MailAccount;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 邮箱
 */
@Slf4j
public class MailNotification implements BaseNotification {
    /**
     * 测试
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     */
    @Override
    public void test(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        send(notificationConfig, ani, text, notificationStatusEnum);
    }

    /**
     * 发送通知
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     * @return 是否成功
     */
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String mailFrom = notificationConfig.getMailFrom();
        String mailSMTPHost = notificationConfig.getMailSMTPHost();
        String mailPassword = notificationConfig.getMailPassword();
        Integer mailSMTPPort = notificationConfig.getMailSMTPPort();
        String mailAddressee = notificationConfig.getMailAddressee();
        Boolean mailImage = notificationConfig.getMailImage();
        Boolean mailSSLEnable = notificationConfig.getMailSSLEnable();
        Boolean mailTLSEnable = notificationConfig.getMailTLSEnable();

        Assert.notBlank(mailFrom, "发件人邮箱 为空");
        Assert.notBlank(mailSMTPHost, "SMTP地址 为空");
        Assert.notBlank(mailPassword, "密码 为空");
        Assert.notBlank(mailAddressee, "收件人 为空");

        MailAccount mailAccount = new MailAccount()
                .setUser(mailFrom)
                .setFrom(StrFormatter.format("ani-rss <{}>", mailFrom))
                .setPass(mailPassword)
                .setHost(mailSMTPHost)
                .setPort(mailSMTPPort)
                .setSslEnable(mailSSLEnable)
                .setStarttlsEnable(mailTLSEnable)
                .setAuth(true);

        String notificationTemplate = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        notificationTemplate = notificationTemplate.replace("\n", "<br/>");

        if (mailImage) {
            String image = ani.getImage();
            notificationTemplate += StrFormatter.format("<br/><img src=\"{}\"/>", image);
        }

        String title = ani.getTitle();

        title = text.length() > 200 ? title : text;

        try {
            JakartaMailUtil.send(mailAccount, List.of(mailAddressee), title, notificationTemplate, true);
            return true;
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            return false;
        }
    }
}
