package ani.rss.update;

import cn.hutool.core.io.FileUtil;

import java.io.File;

public class MacUpdate implements BaseUpdate {
    @Override
    public void update(File updateFile) {
        FileUtil.rename(updateFile, FileUtil.mainName(updateFile), true);

        System.exit(0);
    }
}
