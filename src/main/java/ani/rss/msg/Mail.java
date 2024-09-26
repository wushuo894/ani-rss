package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.MyMailAccount;
import ani.rss.util.ExceptionUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Mail implements Message {
    @Override
    public Boolean send(Config config, Ani ani, String text) {
        Boolean mail = config.getMail();
        MyMailAccount myMailAccount = config.getMailAccount();
        String mailAddressee = config.getMailAddressee();

        if (!mail) {
            return false;
        }
        String from = myMailAccount.getFrom();
        String host = myMailAccount.getHost();
        String pass = myMailAccount.getPass();
        Assert.notBlank(from, "发件人邮箱 为空");
        Assert.notBlank(host, "SMTP地址 为空");
        Assert.notBlank(pass, "密码 为空");
        Assert.notBlank(mailAddressee, "收件人 为空");

        MailAccount mailAccount = new MailAccount();
        BeanUtil.copyProperties(myMailAccount, mailAccount);
        mailAccount.setUser(from)
                .setFrom(StrFormatter.format("ani-rss <{}>", from))
                .setAuth(true);
        try {
            MailUtil.send(mailAccount, List.of(mailAddressee), "ani-rss", text, false);
            return true;
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            return false;
        }
    }
}
