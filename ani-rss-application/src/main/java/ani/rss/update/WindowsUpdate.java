package ani.rss.update;

import ani.rss.commons.FileUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.entity.Global;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class WindowsUpdate implements BaseUpdate {
    @Override
    public void update(File updateFile) {
        File currentFile = MavenUtils.getCurrentFile();
        String filename = "ani-rss-update.exe";
        File updateExe = new File(updateFile.getParent(), filename);
        FileUtil.del(updateExe);

        try (InputStream stream = ResourceUtil.getStream(filename)) {
            FileUtil.writeFromStream(stream, updateExe, true);

            String exe = updateExe.toString();
            String source = FileUtils.getAbsolutePath(updateFile);
            String target = FileUtils.getAbsolutePath(currentFile);

            // 过滤掉 --gui, 因为 exe 本身会自动添加此参数
            String args = Global.ARGS.stream()
                    .filter(s -> !"--gui".equals(s))
                    .collect(Collectors.joining(" "));

            List<String> strings = new ArrayList<>();
            strings.add(exe);
            strings.add("--source=" + source);
            strings.add("--target=" + target);
            if (StrUtil.isNotBlank(args)) {
                strings.add("--args=" + args);
            }
            String[] array = ArrayUtil.toArray(strings, String.class);
            RuntimeUtil.exec(array);
            System.exit(0);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
