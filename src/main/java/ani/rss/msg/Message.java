package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import ani.rss.util.TmdbUtil;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
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
        Double episode = 1.0;
        if (ReUtil.contains(StringEnum.SEASON_REG, text)) {
            episode = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, text, 2));
        }

        if (episode.intValue() == episode) {
            messageTemplate = messageTemplate.replace("${episode}", String.valueOf(episode.intValue()));
        } else {
            messageTemplate = messageTemplate.replace("${episode}", String.valueOf(episode));
        }


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

        for (Func1<Ani, Object> func1 : list) {
            try {
                String fieldName = LambdaUtil.getFieldName(func1);
                String s = StrFormatter.format("${{}}", fieldName);
                String v = func1.callWithRuntimeException(ani).toString();
                messageTemplate = messageTemplate.replace(s, v);
            } catch (Exception ignored) {
            }
        }

        String tmdbId = Optional.ofNullable(ani.getTmdb())
                .map(TmdbUtil.Tmdb::getId)
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

        return messageTemplate;
    }
}
