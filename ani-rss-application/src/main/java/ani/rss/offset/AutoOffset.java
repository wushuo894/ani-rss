package ani.rss.offset;

import ani.rss.entity.Ani;

public class AutoOffset implements BaseOffset {
    @Override
    public Integer getMinEpisode(Ani ani) {
        return 1;
    }
}
