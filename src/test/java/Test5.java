import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;

import java.util.List;

public class Test5 {
    public static void main(String[] args) {
        ConfigUtil.load();
        Ani ani = AniUtil.getAni("https://mikanime.tv/RSS/Bangumi?bangumiId=3384&subgroupid=615");
        List<Item> items = AniUtil.getItems(ani);
        for (Item item : items) {
            System.out.println(item);
        }
    }
}
