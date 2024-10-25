package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Objects;

public interface Message {

    Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum);

    default String replaceMessageTemplate(Ani ani, String messageTemplate, String text) {
        messageTemplate = messageTemplate.replace("${text}", text);

        if (Objects.nonNull(ani)) {
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
                    Ani::getTotalEpisodeNumber
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
        }

        // 集数
        double episode = 1.0;
        if (ReUtil.contains(StringEnum.SEASON_REG, text)) {
            episode = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, text, 2));
        }
        messageTemplate = messageTemplate.replace("${episode}", String.valueOf(episode));
        return messageTemplate;
    }
}
