package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Tmdb;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import ani.rss.util.FilePathUtil;
import ani.rss.util.NumberFormatUtil;
import ani.rss.util.RenameUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface Message {

    Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum);

    default String replaceMessageTemplate(Ani ani, String messageTemplate, String text, MessageEnum messageEnum) {
        messageTemplate = messageTemplate.replace("${text}", text);
        // 集数
        double episode = 1.0;
        if (ReUtil.contains(StringEnum.SEASON_REG, text)) {
            episode = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, text, 2));
        }

        String episodeFormat = String.format("%02d", (int) episode);

        // .5
        boolean is5 = episode != (int) episode;

        if (is5) {
            episodeFormat = episodeFormat + ".5";
        }

        messageTemplate = messageTemplate.replace("${episode}",
                NumberFormatUtil.format(episode, 1, 0)
        );
        messageTemplate = messageTemplate.replace("${episodeFormat}", episodeFormat);

        if (Objects.isNull(ani)) {
            return messageTemplate;
        }

        List<Func1<Ani, Object>> list = List.of(
                Ani::getTitle,
                Ani::getScore,
                Ani::getSeason,
                Ani::getYear,
                Ani::getMonth,
                Ani::getDate,
                Ani::getThemoviedbName,
                Ani::getBgmUrl,
                Ani::getCurrentEpisodeNumber,
                Ani::getTotalEpisodeNumber,
                Ani::getSubgroup
        );

        int season = ani.getSeason();
        String seasonFormat = String.format("%02d", season);
        messageTemplate = messageTemplate.replace("${seasonFormat}", seasonFormat);

        messageTemplate = RenameUtil.replaceField(messageTemplate, ani, list);

        String tmdbId = Optional.ofNullable(ani.getTmdb())
                .map(Tmdb::getId)
                .orElse("");
        messageTemplate = messageTemplate.replace("${tmdbid}", tmdbId);

        String tmdbUrl = "";
        if (StrUtil.isNotBlank(tmdbId)) {
            String type = ani.getOva() ? "movie" : "tv";
            tmdbUrl = StrFormatter.format("https://www.themoviedb.org/{}/{}", type, tmdbId);
        }
        messageTemplate = messageTemplate.replace("${tmdburl}", tmdbUrl);

        String emoji = messageEnum.getEmoji();
        String action = messageEnum.getAction();

        messageTemplate = messageTemplate.replace("${emoji}", emoji);
        messageTemplate = messageTemplate.replace("${action}", action);

        String downloadPath = FilePathUtil.getAbsolutePath(TorrentUtil.getDownloadPath(ani).get(0));
        messageTemplate = messageTemplate.replace("${downloadPath}", downloadPath);

        if (messageTemplate.contains("${jpTitle}")) {
            String jpTitle = RenameUtil.getJpTitle(ani);
            messageTemplate = messageTemplate.replace("${jpTitle}", jpTitle);
        }

        messageTemplate = RenameUtil.replaceEpisodeTitle(messageTemplate, episode, ani);

        return messageTemplate;
    }
}
