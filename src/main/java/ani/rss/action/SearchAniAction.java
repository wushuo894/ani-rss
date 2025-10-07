package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Ani;
import ani.rss.entity.PlayItem;
import ani.rss.entity.SearchAniItem;
import ani.rss.util.AniUtil;
import ani.rss.util.PlaylistUtil;
import ani.rss.util.ServerUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 搜索相关
 */
@Auth(type = {
        AuthType.IP_WHITE_LIST,
        AuthType.API_KEY
})
@Slf4j
@Path("/ani/search")
public class SearchAniAction implements BaseAction {

    public void get() {
        HttpServerRequest request = ServerUtil.REQUEST.get();
        String filterYear = request.getParam("year");
        String filterMonth = request.getParam("month");

        // 按拼音排序
        List<Ani> list = AniUtil.ANI_LIST;

        List<SearchAniItem> searchItems = list.parallelStream()
                .filter(ani -> StrUtil.isEmpty(filterYear) || filterYear.equals(ani.getYear().toString()))
                .filter(ani -> StrUtil.isEmpty(filterMonth) || filterMonth.equals(ani.getMonth().toString()))
                .map(ani -> {
                    String title = ani.getTitle();
                    String pinyin = PinyinUtil.getPinyin(title, "");
                    String pinyinInitials = PinyinUtil.getFirstLetter(title, "");

                    Integer year = ani.getYear();
                    Integer month = ani.getMonth();
                    Integer date = ani.getDate();

                    DateTime dateTime = DateUtil.parseDate(
                            StrFormatter.format("{}-{}-{}", year, month, date)
                    );
                    int week = DateUtil.dayOfWeek(dateTime) - 1;

                    File downloadPath = TorrentUtil.getDownloadPath(ani);
                    List<PlayItem> playItems = PlaylistUtil.getPlayItem(downloadPath);

                    SearchAniItem searchItem = new SearchAniItem()
                            .setId(ani.getId())
                            .setTitle(ani.getTitle())
                            .setJpTitle(ani.getJpTitle())
                            .setMikanTitle(ani.getMikanTitle())
                            .setBgmUrl(ani.getBgmUrl())
                            .setDate(ani.getDate())
                            .setYear(ani.getYear())
                            .setMonth(ani.getMonth())
                            .setSeason(ani.getSeason())
                            .setCurrentEpisodeNumber(ani.getCurrentEpisodeNumber())
                            .setTotalEpisodeNumber(ani.getTotalEpisodeNumber())
                            .setOva(ani.getOva())
                            .setImage(ani.getImage())
                            .setType(ani.getType())
                            .setScore(ani.getScore())
                            .setTmdb(ani.getTmdb())
                            .setSubgroup(ani.getSubgroup())
                            .setThemoviedbName(ani.getThemoviedbName())
                            .setPlaylist(playItems)
                            .setPinyin(pinyin)
                            .setPinyinInitials(pinyinInitials)
                            .setWeek(week);

                    if (searchItem.getBgmUrl() != null) {
                        Matcher matcher = PatternPool.get("/(\\d+)$").matcher(searchItem.getBgmUrl());
                        if (matcher.find()) {
                            searchItem.setBgmSubjectId(Integer.valueOf(matcher.group(1)));
                        }
                    }

                    return searchItem;
                }).toList();

        resultSuccess(searchItems);
    }

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String method = request.getMethod();
        if (method.equalsIgnoreCase("get")) {
            get();
        } else {
            response.send404("Not Found");
        }
    }
}
