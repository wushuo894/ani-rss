package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;

public interface Message {

    Boolean send(Config config, Ani ani, String text);
}
