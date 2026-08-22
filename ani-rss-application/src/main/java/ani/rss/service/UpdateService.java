package ani.rss.service;

import ani.rss.cache.CacheUtils;
import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.entity.About;
import ani.rss.entity.UpdateInfo;
import ani.rss.update.BaseUpdate;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateService {

    private final GithubService githubService;

    /**
     * 关于
     *
     * @return 关于信息
     */
    public synchronized About about() {
        String key = "github#releases-latest";

        About cacheAbout = CacheUtils.get(key);

        if (Objects.nonNull(cacheAbout)) {
            return cacheAbout;
        }

        String version = MavenUtils.getVersion();

        About about = (About) new About()
                .setVersion(version)
                .setUpdate(false)
                .setAutoUpdate(false)
                .setLatest("")
                .setMarkdownBody("");
        try {
            MavenUtils.CurrentFile currentFile = MavenUtils.getCurrentFile();

            String filename = currentFile.isJar() ? "ani-rss.jar" : "ani-rss.exe";

            UpdateInfo updateInfo = githubService.getUpdateInfo("wushuo894", "ani-rss", filename, version);

            BeanUtil.copyProperties(updateInfo, about, "version");
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error("检测更新失败 {}", message);
            log.error(message, e);
        }
        // 缓存一分钟
        CacheUtils.put(key, about, 1000 * 60);
        return about;
    }

    /**
     * 更新程序
     *
     * @param about 关于信息
     */
    public synchronized void update(About about) {
        Boolean update = about.getUpdate();
        if (!update) {
            return;
        }

        MavenUtils.CurrentFile currentFile = MavenUtils.getCurrentFile();

        Assert.isTrue(currentFile.isFile(), "不支持更新");

        BaseUpdate baseUpdate = BaseUpdate.getInstance();

        File updateFile = baseUpdate.downloadUpdateFile(about);

        ThreadUtil.execute(() -> {
            try {
                baseUpdate.update(updateFile);
            } catch (Exception e) {
                log.error("更新时遇到错误: {}", e.getMessage(), e);
            }
        });
    }
}
