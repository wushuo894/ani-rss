import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ItemsUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

public class Test5 {
    public static void main(String[] args) {
        String s = "(.*|\\[.*])( -? \\d+|\\[\\d+]|\\[\\d+.?[vV]\\d]|第\\d+[话話集]|\\[第?\\d+[话話集]]|\\[\\d+.?END]|[Ee][Pp]?\\d+)(.*)";
        String itemTitle = "[ANi] 关于我转生变成史莱姆这档事 第三季 - 65.5 [1080P][Baha][WEB-DL][AAC AVC][CHT].mp4";
        String e = ReUtil.get(s, itemTitle, 2);
        String episode = ReUtil.get("\\d+", e, 0);
        if (StrUtil.isBlank(episode)) {
            return;
        }
        String string = ReUtil.get(s, itemTitle, 3);
        if (string.startsWith(".5")) {
            string = episode + string;
            System.out.println(string);
            if (itemTitle.endsWith(string)) {
                System.out.println(".5");
                return;
            }
        }
        ConfigUtil.load();
        Ani ani = AniUtil.getAni("https://mikanani.me/RSS/Bangumi?bangumiId=3341&subgroupid=583");
        List<Item> items = ItemsUtil.getItems(ani);
        for (Item item : items) {
            System.out.println(item);
        }
    }
}
