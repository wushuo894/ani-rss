package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.msg.Message;
import cn.hutool.core.bean.DynaBean;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MessageUtil {
    private static final ExecutorService EXECUTOR_SERVICE = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(256))
            .build();

    public static synchronized void send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        List<MessageEnum> messageList = config.getMessageList();

        if (!messageList.contains(messageEnum)) {
            return;
        }

        if (!AfdianUtil.verifyExpirationTime()) {
            if (MessageEnum.COMPLETED == messageEnum) {
                log.warn("未解锁捐赠, 无法使用订阅完结通知");
                return;
            }
            if (MessageEnum.ALIST_UPLOAD == messageEnum) {
                log.warn("未解锁捐赠, 无法使用Alist上传通知");
                return;
            }
        }

        Boolean isMessage = Opt.ofNullable(ani)
                .map(Ani::getMessage)
                .orElse(true);

        if (!isMessage) {
            // 未开启此订阅通知
            return;
        }

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

            Message message = (Message) ReflectUtil.newInstance(aClass);
            EXECUTOR_SERVICE.execute(() -> {
                try {
                    message.send(config, ani, text, messageEnum);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }
}
