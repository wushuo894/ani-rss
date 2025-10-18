import ani.rss.entity.Ani;
import ani.rss.entity.tmdb.Tmdb;
import ani.rss.service.ScrapeService;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TmdbUtil;
import org.junit.Test;

/**
 * 刮削测试
 */
public class ScrapeTest {

    /**
     * 刮削电影
     */
    @Test
    public void scrapeMovie() {
        ConfigUtil.load();

        Tmdb tmdb = TmdbUtil.getTmdbMovie("你的名字");

        Ani ani = Ani.createAni();
        ani.setTmdb(tmdb);
        ani.setTitle("你的名字");
        ani.setOva(true);
        ani.setSeason(1);

        try {
            ScrapeService.scrapeMovie(ani, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 刮削电视剧
     */
    @Test
    public void scrapeTv() {
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
