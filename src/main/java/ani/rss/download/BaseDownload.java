package ani.rss.download;

import ani.rss.entity.TorrentsInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.List;

public interface BaseDownload {
    Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    String tag = "ani-rss";

    List<String> videoFormat = List.of("mp4", "mkv", "avi", "wmv");

    List<String> subtitleFormat = List.of("ass", "ssa", "sub", "srt", "lyc");

    /**
     * 登录
     *
     * @return
     */
    Boolean login();

    /**
     * 获取任务列表
     *
     * @return
     */
    List<TorrentsInfo> getTorrentsInfos();

    /**
     * 下载
     *
     * @param name
     * @param savePath
     * @param torrentFile
     */
    Boolean download(String name, String savePath, File torrentFile);

    /**
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    void delete(TorrentsInfo torrentsInfo);

    /**
     * 重命名
     *
     * @param torrentsInfo
     * @param reName
     */
    void rename(TorrentsInfo torrentsInfo, String reName);
}
