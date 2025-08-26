package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.Tmdb;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.util.*;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Optional;

public interface BaseNotification {

    /**
     * 发送通知
     *
     * @param notificationConfig
     * @param ani
     * @param text
     * @param notificationStatusEnum
     * @return
     */
    Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum);

    default String replaceNotificationTemplate(Ani ani, NotificationConfig notificationConfig, String text, NotificationStatusEnum notificationStatusEnum) {
        String notificationTemplate = notificationConfig.getNotificationTemplate();

        String comment = Opt.ofNullable(notificationConfig)
                .map(NotificationConfig::getComment)
                .filter(StrUtil::isNotBlank)
                .orElse("无备注");

        notificationTemplate = notificationTemplate.replace("${comment}", comment);

        return replaceNotificationTemplate(ani, notificationTemplate, text, notificationStatusEnum);
    }

    default String replaceNotificationTemplate(Ani ani, String notificationTemplate, String text, NotificationStatusEnum notificationStatusEnum) {
        notificationTemplate = notificationTemplate.replace("${text}", text);

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

        notificationTemplate = notificationTemplate.replace("${episode}",
                NumberFormatUtil.format(episode, 1, 0)
        );
        notificationTemplate = notificationTemplate.replace("${episodeFormat}", episodeFormat);

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
        notificationTemplate = notificationTemplate.replace("${seasonFormat}", seasonFormat);

        notificationTemplate = RenameUtil.replaceField(notificationTemplate, ani, list);

        String tmdbId = Optional.of(ani)
                .map(Ani::getTmdb)
                .map(Tmdb::getId)
                .filter(StrUtil::isNotBlank)
                .orElse("");
        notificationTemplate = notificationTemplate.replace("${tmdbid}", tmdbId);

        String tmdbUrl = "";
        if (StrUtil.isNotBlank(tmdbId)) {
            Boolean ova = Opt.ofNullable(ani)
                    .map(Ani::getOva)
                    .orElse(false);
            String type = ova ? "movie" : "tv";
            tmdbUrl = StrFormatter.format("https://www.themoviedb.org/{}/{}", type, tmdbId);
        }
        notificationTemplate = notificationTemplate.replace("${tmdburl}", tmdbUrl);

        String emoji = notificationStatusEnum.getEmoji();
        String action = notificationStatusEnum.getAction();

        notificationTemplate = notificationTemplate.replace("${emoji}", emoji);
        notificationTemplate = notificationTemplate.replace("${action}", action);

        String downloadPath = FilePathUtil.getAbsolutePath(TorrentUtil.getDownloadPath(ani));
        notificationTemplate = notificationTemplate.replace("${downloadPath}", downloadPath);

        if (notificationTemplate.contains("${jpTitle}")) {
            String jpTitle = RenameUtil.getJpTitle(ani);
            notificationTemplate = notificationTemplate.replace("${jpTitle}", jpTitle);
        }

        notificationTemplate = RenameUtil.replaceEpisodeTitle(notificationTemplate, episode, ani);

        if (!notificationTemplate.contains("${notification}")) {
            return notificationTemplate.trim();
        }

        Config config = ConfigUtil.CONFIG;
        String template = config.getNotificationTemplate();

        template = replaceNotificationTemplate(ani, template, text, notificationStatusEnum);

        notificationTemplate = notificationTemplate.replace("${notification}", template);

        return notificationTemplate.trim();
    }
}
