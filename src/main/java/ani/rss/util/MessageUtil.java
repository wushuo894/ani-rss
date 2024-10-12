package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.msg.*;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.EnumUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MessageUtil {
    private static final ExecutorService EXECUTOR = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(128))
            .build();

    public static final Message mailMessage = new Mail();
    public static final Message telegramMessage = new Telegram();
    public static final Message webHookMessage = new WebHook();
    public static final Message serverChanMessage=new ServerChan();

    public static synchronized void send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        List<MessageEnum> messageList = config.getMessageList();
        if (Objects.nonNull(messageEnum)) {
            if (messageList.stream().noneMatch(it -> it.name().equalsIgnoreCase(messageEnum.name()))) {
                return;
            }
        }
        try {
            Boolean mail = config.getMail();
            if (mail) {
                EXECUTOR.execute(() -> mailMessage.send(config, ani, text));
            }

            Boolean telegram = config.getTelegram();
            if (telegram) {
                EXECUTOR.execute(() -> telegramMessage.send(config, ani, text));
            }

            Boolean webHook = config.getWebHook();
            if (webHook) {
                EXECUTOR.execute(() -> webHookMessage.send(config, ani, text));
            }

            Boolean serverChan =config.getServerChan();
            if (serverChan) {
                EXECUTOR.execute(()->serverChanMessage.send(config, ani, text));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
