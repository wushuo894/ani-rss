import ani.rss.download.Aria2;
import ani.rss.download.BaseDownload;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;

import java.util.List;

public class Test4 {
    public static void main(String[] args) {
        ConfigUtil.CONFIG
                .setHost("http://192.168.5.4:6800")
                .setUsername("")
                .setPassword("12345")
                .setDownloadPath("1");
        BaseDownload baseDownload = new Aria2();
        System.out.println(baseDownload.login(ConfigUtil.CONFIG));
        List<TorrentsInfo> torrentsInfos = baseDownload.getTorrentsInfos();
        torrentsInfos.forEach(System.out::println);
    }
}
