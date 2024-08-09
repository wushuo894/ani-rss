import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;

public class Test2 {
    public static void main(String[] args) {
        ConfigUtil.load();
        Ani ani = AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=3060&subgroupid=213");
        for (Item item : AniUtil.getItems(ani)) {
            System.out.println(item.getReName());
        }
    }
}
