import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.io.FileUtil;

import java.io.File;

public class Test2 {
    public static void main(String[] args) {
        ConfigUtil.load();
        Ani ani = AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=3060&subgroupid=213");
        for (Item item : AniUtil.getItems(ani)) {
            System.out.println(item.getReName());
        }
        System.out.println(TorrentUtil.getDownloadPath(ani).toString().replace("\\", "/"));
        ConfigUtil.sync();

        for (File l : FileUtil.ls("Z:\\Media\\番剧")) {
            ani.setTitle(l.getName());
            System.out.println(TorrentUtil.getDownloadPath(ani));
        }

    }
}
