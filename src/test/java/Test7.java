import ani.rss.entity.Log;
import ani.rss.util.LogUtil;

public class Test7 {
    public static void main(String[] args) {
        for (int i = 0; i < 100000; i++) {
            LogUtil.LOGS.add(new Log());
        }
        System.out.println(LogUtil.LOGS.size());
    }
}
