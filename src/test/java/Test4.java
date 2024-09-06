import ani.rss.util.ConfigUtil;
import ani.rss.util.MailUtils;

public class Test4 {
    public static void main(String[] args) {
        ConfigUtil.load();
        MailUtils.send("test");
    }
}
