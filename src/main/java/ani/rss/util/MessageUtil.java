package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.msg.Mail;
import ani.rss.msg.Message;
import ani.rss.msg.Telegram;
import ani.rss.msg.WebHook;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageUtil {
    public static final Message mailMessage = new Mail();
    public static final Message telegramMessage = new Telegram();
    public static final Message webHookMessage = new WebHook();

    public static synchronized void send(Config config, Ani ani, String text) {
        try {
            Boolean mail = config.getMail();
            if (mail) {
                ThreadUtil.execute(() -> mailMessage.send(config, ani, text));
            }

            Boolean telegram = config.getTelegram();
            if (telegram) {
                ThreadUtil.execute(() -> telegramMessage.send(config, ani, text));
            }

            Boolean webHook = config.getWebHook();
            if (webHook) {
                ThreadUtil.execute(() -> webHookMessage.send(config, ani, text));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
