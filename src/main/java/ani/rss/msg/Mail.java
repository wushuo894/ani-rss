package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.MyMailAccount;
import ani.rss.util.ExceptionUtil;
import cn.hutool.core.bean.BeanUtil;
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
