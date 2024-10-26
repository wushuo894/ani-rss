import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ItemsUtil;

public class Test7 {
    public static void main(String[] args) {
        Ani ani = AniUtil.getAni("https://mikan.wushuo.top/RSS/Bangumi?bangumiId=3436&subgroupid=583");
        ani.setUrl("https://nyaa.si/?page=rss&q=[GM-Team][国漫][剑来]&c=0_0&f=0");
        for (Item item : ItemsUtil.getItems(ani)) {
            System.out.println(item.getPubDate());
        }
    }
}
