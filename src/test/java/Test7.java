import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;

import java.util.List;

public class Test7 {
    public static void main(String[] args) {
        ConfigUtil.load();
        AniUtil.load();
        Ani ani = AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=3354&subgroupid=370");
        List<Item> items = AniUtil.getItems(ani);
        for (Item item : items) {
            System.out.println(item.getReName());
        }
    }
}
