package ani.rss.update;

import ani.rss.commons.FileUtils;
import ani.rss.commons.MavenUtils;

import java.io.File;
import java.nio.file.Path;

public class DesktopUpdate implements BaseUpdate {
    /**
     * 暂存已校验的 JAR，随后退出进程，让桌面端在文件释放后完成更新。
     */
    @Override
    public void update(File updateFile) {
        File currentFile = MavenUtils.getCurrentFile().getFile();
        Path pendingFile = Path.of(currentFile.getPath() + ".pending");
        FileUtils.move(updateFile.toPath(), pendingFile);
        System.exit(0);
    }
}
