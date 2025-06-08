package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.util.AniUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LastDownload implements Message {

    @Override
    public Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        log.info("更新下载时间 {}",text);
        ani.setLastDownload(System.currentTimeMillis());

        AniUtil.ANI_LIST.stream().filter(v->v.getId().equals(ani.getId()))
        .findFirst().ifPresent(v->{
            log.info("同步下载时间 {} {}",ani.getLastDownload(),text);

            v.setLastDownload(ani.getLastDownload());
            AniUtil.sync();
        });
        return Boolean.TRUE;
    }

}
