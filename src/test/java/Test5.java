import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;

import java.util.List;

public class Test5 {
    public static void main(String[] args) {
        Ani ani = AniUtil.getAni("https://nyaa.si/?page=rss&q=Ani+%E6%88%91%E6%8E%A8%E7%9A%84%E5%AD%A9%E5%AD%90&c=0_0&f=0", "", "nyaa");
        List<Item> items = AniUtil.getItems(ani);
        for (Item item : items) {
            System.out.println(item);
        }
        ani = AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=2995&subgroupid=639");
        items = AniUtil.getItems(ani);
        for (Item item : items) {
            System.out.println(item);
        }
    }
}
