import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.msg.Telegram;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;

public class Test6 {
    public static void main(String[] args) {
        ConfigUtil.load();
        Config config = ConfigUtil.CONFIG;
        Ani ani = AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=3394&subgroupid=583");
        Telegram telegram = new Telegram();
        Boolean test = telegram.send(config, ani, "test", null);
        System.out.println(test);
    }
}
