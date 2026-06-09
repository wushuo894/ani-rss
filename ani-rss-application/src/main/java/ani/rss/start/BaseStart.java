package ani.rss.start;

import ani.rss.commons.MavenUtils;
import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;

public interface BaseStart {
    static Boolean isSupported() {
        MavenUtils.CurrentFile currentFile = MavenUtils.getCurrentFile();
        if (currentFile.isDirectory()) {
            return false;
        }

        OsInfo osInfo = SystemUtil.getOsInfo();
        return osInfo.isMac() || osInfo.isWindows();
    }

    static BaseStart getInstance() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isMac()) {
            return new MacStart();
        }

        if (osInfo.isWindows()) {
            return new WindowsStart();
        }

        throw new IllegalArgumentException("暂不支持该系统设置开机自启动 " + osInfo.getName());
    }

    default void sync() {
        Config config = ConfigUtil.CONFIG;
        Boolean autoStart = config.getAutoStart();
        if (autoStart) {
            enable();
            return;
        }
        disable();
    }

    /**
     * 启用
     */
    void enable();

    /**
     * 禁用
     */
    void disable();
}
