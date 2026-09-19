package ani.rss.util.other;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentInfo;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 磁力链接元数据解析工具。
 */
public final class MagnetTorrentUtil {

    private static final int FETCH_TIMEOUT_SECONDS = 60;
    private static final List<String> DEFAULT_TRACKERS = List.of(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.stealth.si:80/announce"
    );

    private MagnetTorrentUtil() {
    }

    /**
     * 使用 jlibtorrent 获取磁力元数据并缓存为种子文件。
     *
     * @param magnet 磁力链接
     * @return 可供合集预览和下载使用的种子文件
     * @throws IOException 临时目录或种子缓存写入失败
     */
    public static synchronized File resolve(String magnet) throws IOException {
        Assert.isTrue(StrUtil.startWithIgnoreCase(magnet, "magnet:?"), "磁力链接格式错误");
        magnet = appendDefaultTrackers(magnet);

        File cacheDir = new File(ConfigUtil.getConfigDir(), "cache/magnet");
        FileUtil.mkdir(cacheDir);
        File torrentFile = new File(cacheDir, SecureUtil.sha256(magnet) + ".torrent");
        if (torrentFile.isFile() && torrentFile.length() > 0) {
            return torrentFile;
        }

        SessionManager sessionManager = new SessionManager();
        Path tempDir = Files.createTempDirectory("ani-rss-magnet-");
        try {
            // fetchMagnet 只获取元数据，不下载种子包含的文件。
            sessionManager.start();
            byte[] torrentData = sessionManager.fetchMagnet(
                    magnet,
                    FETCH_TIMEOUT_SECONDS,
                    tempDir.toFile()
            );
            Assert.notNull(torrentData, "获取磁力链接元数据超时，请检查链接、Tracker 或网络");

            // 写入缓存前先校验返回内容确实是有效的种子元数据。
            TorrentInfo.bdecode(torrentData);
            FileUtil.writeBytes(torrentData, torrentFile);
            return torrentFile;
        } finally {
            // SessionManager 持有原生线程，使用结束后必须主动释放。
            sessionManager.stop();
            FileUtil.del(tempDir.toFile());
        }
    }

    /**
     * 为磁力链接追加用于发现 Peer 的默认 Tracker。
     *
     * @param magnet 原始磁力链接
     * @return 包含默认 Tracker 的磁力链接
     */
    private static String appendDefaultTrackers(String magnet) {
        StringBuilder result = new StringBuilder(magnet);
        for (String tracker : DEFAULT_TRACKERS) {
            String encodedTracker = URLEncoder.encode(tracker, StandardCharsets.UTF_8);
            if (magnet.contains("tr=" + tracker) || magnet.contains("tr=" + encodedTracker)) {
                continue;
            }

            // Tracker 必须作为独立的 tr 查询参数进行 URL 编码。
            result.append('&').append("tr=").append(encodedTracker);
        }
        return result.toString();
    }
}
