import ani.rss.entity.Ani;
import ani.rss.entity.tmdb.Tmdb;
import ani.rss.service.ScrapeService;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TmdbUtil;

public class Test {
    public static void main(String[] args) {
        ConfigUtil.load();

        Tmdb tmdb = TmdbUtil.getTmdbTv("Re：从零开始的异世界生活");
        tmdb.setTmdbGroupId("641eb9d6b234b9007ac67063");

        Ani ani = Ani.createAni();
        ani.setTmdb(tmdb);
        ani.setTitle("Re：从零开始的异世界生活");
        ani.setSeason(2);

        try {
            ScrapeService.scrapeTv(ani, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
