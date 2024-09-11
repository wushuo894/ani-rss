package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.MyMailAccount;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MailUtils {
    public static synchronized void send(Config config,String text) {
        Boolean mail = config.getMail();
        MyMailAccount myMailAccount = config.getMailAccount();
        String mailAddressee = config.getMailAddressee();

        if (!mail) {
            return;
        }
        String from = myMailAccount.getFrom();
        MailAccount mailAccount = new MailAccount();
        BeanUtil.copyProperties(myMailAccount, mailAccount);
        mailAccount.setUser(from)
                .setFrom(StrFormatter.format("ani-rss <{}>", from))
                .setAuth(true);
        ThreadUtil.execute(() -> {
            try {
                MailUtil.send(mailAccount, List.of(mailAddressee), "ani-rss", text, false);
            } catch (Exception e) {
                log.error(e.getMessage());
                log.debug(e.getMessage(), e);
            }
        });
    }
}
