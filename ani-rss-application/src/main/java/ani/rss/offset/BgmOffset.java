package ani.rss.offset;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmEpisodes;
import ani.rss.util.other.BgmUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BgmOffset implements BaseOffset {
    private final Map<Ani, Integer> map = new HashMap<>();

    @Override
    public Integer getMinEpisode(Ani ani) {
        if (map.containsKey(ani)) {
            return map.get(ani);
        }

        String subjectId = BgmUtil.getSubjectId(ani);
        List<BgmEpisodes.BgmEpisode> episodes = BgmUtil.getEpisodes(subjectId, 0);
        int minEpisode = episodes
                .stream()
                .mapToInt(BgmEpisodes.BgmEpisode::getSort)
                .min()
                .orElse(1);
        map.put(ani, minEpisode);
        return minEpisode;
    }
}
