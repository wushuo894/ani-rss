package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 邮箱
 */
@Slf4j
public class MailNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Config config = ConfigUtil.CONFIG;
        String template = config.getNotificationTemplate();
        template = replaceNotificationTemplate(ani, template, text, notificationStatusEnum);

        String notificationTemplate = notificationConfig.getNotificationTemplate();
        notificationTemplate = notificationTemplate.replace("${notification}", template);
        notificationTemplate = replaceNotificationTemplate(ani, notificationTemplate, text, notificationStatusEnum);

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

        notificationTemplate = notificationTemplate.replace("\n", "<br/>");

        if (mailImage) {
            String image = "https://docs.wushuo.top/null.png";

            if (Objects.nonNull(ani)) {
                image = ani.getImage();
            }

            notificationTemplate += StrFormatter.format("<br/><img src=\"{}\"/>", image);
        }

        try {
            MailUtil.send(mailAccount, List.of(mailAddressee), text.length() > 200 ? ani.getTitle() : text, notificationTemplate, true);
            return true;
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            return false;
        }
    }
}
