package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import ani.rss.msg.Message;
import cn.hutool.core.bean.DynaBean;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MessageUtil {
    private static final Map<String, ExecutorService> SERVICE_MAP = new HashMap<>();

    @Synchronized("SERVICE_MAP")
    public static synchronized void send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        List<MessageEnum> messageList = config.getMessageList();
        if (Objects.nonNull(messageEnum)) {
            if (messageList.stream().noneMatch(it -> it.name().equalsIgnoreCase(messageEnum.name()))) {
                return;
            }
        }

        String messageTemplate = config.getMessageTemplate();
        messageTemplate = messageTemplate.replace("${text}", text);

        if (Objects.nonNull(ani)) {
            List<Func1<Ani, Object>> list = List.of(
                    Ani::getTitle,
                    Ani::getScore,
                    Ani::getYear,
                    Ani::getMonth,
                    Ani::getDate,
                    Ani::getThemoviedbName,
                    Ani::getBgmUrl,
                    Ani::getCustomEpisode,
                    Ani::getTotalEpisodeNumber
            );

            for (Func1<Ani, Object> func1 : list) {
                try {
                    String fieldName = LambdaUtil.getFieldName(func1);
                    String s = StrFormatter.format("${{}}", fieldName);
                    String v = func1.callWithRuntimeException(ani).toString();
                    messageTemplate = messageTemplate.replace(s, v);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 集数
        double episode = 1.0;
        if (ReUtil.contains(StringEnum.SEASON_REG, text)) {
            episode = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, text, 2));
        }
        messageTemplate = messageTemplate.replace("${episode}", String.valueOf(episode));

        Set<Class<?>> classes = ClassUtil.scanPackage("ani.rss.msg");
        DynaBean dynaBean = DynaBean.create(config);
        for (Class<?> aClass : classes) {
            if (aClass.isInterface()) {
                continue;
            }
            String name = aClass.getSimpleName();
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
            Object b = dynaBean.get(name);
            if (Objects.isNull(b)) {
                continue;
            }
            if (!(b instanceof Boolean)) {
                continue;
            }
            if (!(Boolean) b) {
                continue;
            }

            ExecutorService executor = SERVICE_MAP.get(name);
            if (Objects.isNull(executor)) {
                executor = ExecutorBuilder.create()
                        .setCorePoolSize(1)
                        .setMaxPoolSize(1)
                        .setWorkQueue(new LinkedBlockingQueue<>(64))
                        .build();
                SERVICE_MAP.put(name, executor);
            }

            try {
                Message message = (Message) ReflectUtil.newInstance(aClass);
                String finalMessageTemplate = messageTemplate;
                executor.execute(() -> message.send(config, ani, messageEnum, finalMessageTemplate));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
