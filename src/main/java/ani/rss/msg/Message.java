package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public interface Message {

    Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    Boolean send(Config config, Ani ani, String text);
}
