package ani.rss.msg;

import ani.rss.entity.Config;

public interface Message {

    Boolean send(Config config, String text);
}
