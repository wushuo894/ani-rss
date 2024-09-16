import ani.rss.entity.Ani;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;

public class Test7 {
    public static void main(String[] args) {
        ConfigUtil.load();
        AniUtil.load();
        for (Ani ani : AniUtil.ANI_LIST) {
            AniUtil.getBangumiInfo(ani, false, false);
        }
        AniUtil.sync();
    }
}
