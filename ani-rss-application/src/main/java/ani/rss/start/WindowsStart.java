package ani.rss.start;

import ani.rss.commons.MavenUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class WindowsStart implements BaseStart {
    private static final String SHORTCUT_NAME = "ani-rss.lnk";

    /**
     * 启用
     */
    @Override
    public void enable() {
        File shortcut = getWindowsShortcut();

        File currentFile = MavenUtils.getCurrentFile().getAbsoluteFile();
        String extName = FileUtil.extName(currentFile);
        if (!"exe".equalsIgnoreCase(extName)) {
            throw new IllegalStateException("Windows 仅支持以 exe 方式运行时设置开机自启动");
        }

        File workingDirectory = currentFile.getParentFile();
        String targetPath = currentFile.getAbsolutePath();
        FileUtil.mkdir(shortcut.getParentFile());

        File vbs = null;
        try {
            vbs = File.createTempFile("ani-rss-autostart", ".vbs");
            String script = """
                    Set objShell = CreateObject("WScript.Shell")
                    Set objShortcut = objShell.CreateShortcut("%s")
                    objShortcut.TargetPath = "%s"
                    objShortcut.WorkingDirectory = "%s"
                    objShortcut.WindowStyle = 1
                    objShortcut.Description = "ani-rss"
                    objShortcut.Save
                    """.formatted(
                    escapeVbs(shortcut.getAbsolutePath()),
                    escapeVbs(targetPath),
                    escapeVbs(workingDirectory.getAbsolutePath())
            );
            FileUtil.writeUtf8String(script, vbs);
            String cscript = RuntimeUtil.execForStr("cscript", "//nologo", vbs.getAbsolutePath());
            log.debug("cscript: {}", cscript);
            log.info("已创建 Windows 启动快捷方式 {}", shortcut);
        } catch (Exception e) {
            throw new IllegalStateException("创建 Windows 启动快捷方式失败: " + e.getMessage(), e);
        } finally {
            FileUtil.del(vbs);
        }
    }

    /**
     * 禁用
     */
    @Override
    public void disable() {
        File shortcut = getWindowsShortcut();
        if (shortcut.exists()) {
            FileUtil.del(shortcut);
            log.info("已删除 Windows 启动快捷方式 {}", shortcut);
        }
    }

    private static File getWindowsShortcut() {
        String appData = SystemUtil.get("APPDATA");
        if (StrUtil.isBlank(appData)) {
            appData = FileUtil.getUserHomePath() + "/AppData/Roaming";
        }
        return new File(appData + "/Microsoft/Windows/Start Menu/Programs/Startup/" + SHORTCUT_NAME);
    }

    private static String escapeVbs(String value) {
        return value.replace("\"", "\"\"");
    }
}
