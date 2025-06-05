package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.util.AfdianUtil;
import ani.rss.util.JellyfinUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class JellyfinRefresh implements Message {
    @Override
    public Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        Assert.isTrue(AfdianUtil.verifyExpirationTime(), "未解锁捐赠, 无法使用Jellyfin媒体库刷新");

        Long jellyfinDelayed = config.getJellyfinDelayed();
        if (jellyfinDelayed > 0) {
            ThreadUtil.sleep(jellyfinDelayed, TimeUnit.SECONDS);
        }
        try {
            JellyfinUtil.refresh(config);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
