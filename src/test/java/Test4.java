import ani.rss.download.Transmission;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;

import java.util.List;

public class Test4 {
    public static void main(String[] args) {
        ConfigUtil.CONFIG
                .setHost("http://192.168.5.4:9091")
                .setUsername("admin")
                .setPassword("admin")
                .setDownloadPath("1");
        Transmission transmission = new Transmission();
        transmission.login();
        List<TorrentsInfo> torrentsInfos = transmission.getTorrentsInfos();
        torrentsInfos.forEach(System.out::println);
    }
}
