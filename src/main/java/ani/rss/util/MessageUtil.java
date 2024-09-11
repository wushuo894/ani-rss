package ani.rss.util;

import ani.rss.entity.Config;

public class MessageUtil {
    public static synchronized void send(Config config, String text) {
        Boolean mail = config.getMail();
        if (mail) {
            MailUtils.send(config, text);
        }

        Boolean telegram = config.getTelegram();
        if (telegram) {
            TelegramUtil.send(config, text);
        }

    }
}
