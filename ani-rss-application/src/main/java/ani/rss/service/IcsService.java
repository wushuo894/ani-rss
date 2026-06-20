package ani.rss.service;

import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Slf4j
@Service
public class IcsService {

    private static final String LINE_SEPARATOR = "\r\n";

    private static final SimpleDateFormat ICS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private static final SimpleDateFormat ICS_DATE_FORMAT_ALL_DAY = new SimpleDateFormat("yyyyMMdd");

    static {
        ICS_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public String generateIcs() {
        StringBuilder ics = new StringBuilder();

        ics.append("BEGIN:VCALENDAR").append(LINE_SEPARATOR);
        ics.append("VERSION:2.0").append(LINE_SEPARATOR);
        ics.append("PRODID:-//ani-rss//CN").append(LINE_SEPARATOR);
        ics.append("CALSCALE:GREGORIAN").append(LINE_SEPARATOR);
        ics.append("METHOD:PUBLISH").append(LINE_SEPARATOR);
        ics.append("X-WR-CALNAME:ani-rss").append(LINE_SEPARATOR);
        ics.append("X-WR-TIMEZONE:Asia/Shanghai").append(LINE_SEPARATOR);
        ics.append("X-WR-CALDESC:ani-rss 动漫订阅日历").append(LINE_SEPARATOR);

        List<Ani> enabledAnis = AniUtil.ANI_LIST.stream()
                .filter(Ani::getEnable)
                .toList();

        for (Ani ani : enabledAnis) {
            ics.append(generateEvent(ani)).append(LINE_SEPARATOR);
        }

        ics.append("END:VCALENDAR").append(LINE_SEPARATOR);

        return ics.toString();
    }

    private String generateEvent(Ani ani) {
        StringBuilder event = new StringBuilder();

        event.append("BEGIN:VEVENT").append(LINE_SEPARATOR);

        String summary = buildSummary(ani);
        event.append("SUMMARY:").append(escapeIcsText(summary)).append(LINE_SEPARATOR);

        Boolean ova = ani.getOva();
        String bgmUrl = ani.getBgmUrl();
        Date releaseDate = ani.getReleaseDate();
        String dateStr = ICS_DATE_FORMAT_ALL_DAY.format(releaseDate);
        event.append("DTSTART;VALUE=DATE:").append(dateStr).append(LINE_SEPARATOR);

        Date endDate = DateUtil.offsetDay(releaseDate, 1);
        String endDateStr = ICS_DATE_FORMAT_ALL_DAY.format(endDate);
        event.append("DTEND;VALUE=DATE:").append(endDateStr).append(LINE_SEPARATOR);

        // 如果是剧场版/OVA，不添加重复规则
        if (!ova) {
            // 添加每周重复规则
            String rrule = buildRRule(ani);
            event.append("RRULE:").append(rrule).append(LINE_SEPARATOR);
        }

        String uid = generateUid(ani);
        event.append("UID:").append(uid).append(LINE_SEPARATOR);

        String dtStamp = ICS_DATE_FORMAT.format(new Date());
        event.append("DTSTAMP:").append(dtStamp).append(LINE_SEPARATOR);

        event.append("URL:").append(bgmUrl).append(LINE_SEPARATOR);

        String categories = buildCategories(ani);
        event.append("CATEGORIES:").append(categories).append(LINE_SEPARATOR);

        event.append("TRANSP:TRANSPARENT").append(LINE_SEPARATOR);

        event.append("END:VEVENT");

        return event.toString();
    }

    private String buildSummary(Ani ani) {
        String title = ani.getTitle();
        Integer season = ani.getSeason();
        return StrUtil.format("{} 第{}季", title, season);
    }

    private String buildCategories(Ani ani) {
        Boolean ova = ani.getOva();
        Integer season = ani.getSeason();

        return StrUtil.format("动漫,第{}季,{}", season, ova ? "剧场版/OVA" : "TV");
    }

    /**
     * 根据番剧的星期信息构建重复规则
     *
     * @param ani 番剧信息
     * @return RRULE字符串
     */
    private String buildRRule(Ani ani) {
        Date releaseDate = ani.getReleaseDate();

        // DateUtil.dayOfWeek返回1表示周日，7表示周六
        int dayOfWeek = DateUtil.dayOfWeek(releaseDate);

        String[] weekDays = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};

        // 获取ICS格式的星期
        String icsDay = weekDays[dayOfWeek - 1];

        // 构建RRULE：每周重复，直到番剧完结
        StringBuilder rrule = new StringBuilder();
        rrule.append("FREQ=WEEKLY");
        rrule.append(";BYDAY=").append(icsDay);

        Integer totalEpisodeNumber = ani.getTotalEpisodeNumber();
        if (totalEpisodeNumber > 0) {
            // 如果有总集数，计算结束日期（假设每周更新一集）
            Date untilDate = DateUtil.offsetWeek(releaseDate, totalEpisodeNumber - 1);
            String untilStr = ICS_DATE_FORMAT_ALL_DAY.format(untilDate);
            rrule.append(";UNTIL=").append(untilStr);
        } else {
            // 没有总集数则结束日期为一个月后
            Date untilDate = DateUtil.offsetMonth(DateUtil.beginOfDay(new Date()), 1);
            String untilStr = ICS_DATE_FORMAT_ALL_DAY.format(untilDate);
            rrule.append(";UNTIL=").append(untilStr);
        }

        return rrule.toString();
    }

    private String generateUid(ani.rss.entity.Ani ani) {
        return "ani-rss-" + ani.getId() + "@ani-rss";
    }

    private String escapeIcsText(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }

        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "")
                .trim();
    }
}