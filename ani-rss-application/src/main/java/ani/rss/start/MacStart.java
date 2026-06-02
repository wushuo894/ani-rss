package ani.rss.start;

import ani.rss.commons.MavenUtils;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class MacStart implements BaseStart {

    /**
     * 启用
     */
    @Override
    public void enable() {
        Optional<File> appOpt = getMacApp();

        if (appOpt.isEmpty()) {
            throw new IllegalStateException("macOS 仅支持以 ani-rss.app 方式运行时设置开机自启动");
        }

        File app = appOpt.get();

        String path = escapeAppleScript(app.getAbsolutePath());

        String createScript = """
                tell application "System Events"
                    make login item at end with properties {path:"%s", hidden:false}
                end tell
                """.formatted(path);
        RuntimeUtil.exec("/usr/bin/osascript", "-e", createScript);

        log.info("已添加 macOS 登录项 {}", app);
    }

    /**
     * 禁用
     */
    @Override
    public void disable() {
        Optional<File> appOpt = getMacApp();

        if (appOpt.isEmpty()) {
            return;
        }

        File app = appOpt.get();

        String path = escapeAppleScript(app.getAbsolutePath());
        String deleteScript = """
                tell application "System Events"
                    try
                        delete (login items whose path is "%s")
                    end try
                end tell
                """.formatted(path);
        RuntimeUtil.exec("/usr/bin/osascript", "-e", deleteScript);

        log.info("已删除 macOS 登录项 {}", app);
    }

    private static Optional<File> getMacApp() {
        File file = MavenUtils.getCurrentFile().getAbsoluteFile();
        while (Objects.nonNull(file)) {
            if (file.getName().endsWith(".app")) {
                return Optional.of(file);
            }
            file = file.getParentFile();
        }
        return Optional.empty();
    }

    private static String escapeAppleScript(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

}
