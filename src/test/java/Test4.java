import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;

public class Test4 {
    public static void main(String[] args) {
        ConfigUtil.load();
        AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=3353&subgroupid=203");
    }
}
