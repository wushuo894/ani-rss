import ani.rss.entity.Mikan;
import ani.rss.util.ConfigUtil;
import ani.rss.util.MikanUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Test2 {
    public static void main(String[] args) {
        ConfigUtil.load();
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        System.out.println(gson.toJson(MikanUtil.list("",new Mikan.Season().setYear(2020).setSeason("春"))));
        System.out.println(gson.toJson(MikanUtil.getGroups("https://mikanani.me/Home/Bangumi/3360")));
    }
}
