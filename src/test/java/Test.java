import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ItemsUtil;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        Ani ani = AniUtil.getAni("https://mikanime.tv/RSS/Bangumi?bangumiId=3360&subgroupid=370");
        List<Item> items = ItemsUtil.getItems(ani);
        items.forEach(System.out::println);
    }
}
