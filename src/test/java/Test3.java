import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.List;

public class Test3 {
    public static void main(String[] args) {
        int season = 1;
        File ff = new File("Z:\\Downloads\\会长是女仆大人\\S01");
        String title = ff.getParentFile().getName();
        File[] files = FileUtil.ls(ff.toString());
        for (File file : files) {
            String name = file.getName();
            String s = "(.*|\\[.*])( -? \\d+|\\[\\d+]|\\[\\d+.?[vV]\\d]|第\\d+[话話集]|\\[第?\\d+[话話集]]|\\[\\d+.?END]|[Ee][Pp]?\\d+)(.*)";
            String e = ReUtil.get(s, name, 2);
            String episode = ReUtil.get("\\d+", e, 0);
            if (StrUtil.isBlank(episode)) {
                episode = ReUtil.get(" \\d{2}", name, 0).trim();
            }
            String reName = StrFormatter.format("{} S{}E{}",
                    title,
                    String.format("%02d", season),
                    String.format("%02d", Integer.parseInt(episode)));

            String ext = FileUtil.extName(name);
            if (StrUtil.isBlank(ext)) {
                continue;
            }
            String newPath = reName;
            if (List.of("mp4", "mkv", "avi", "wmv").contains(ext.toLowerCase())) {
                newPath = newPath + "." + ext;
            } else if (List.of("ass", "ssa", "sub", "srt", "lyc").contains(ext.toLowerCase())) {
                s = FileUtil.extName(FileUtil.mainName(name));
                if (StrUtil.isNotBlank(s)) {
                    newPath = newPath + "." + s;
                }
                newPath = newPath + "." + ext;
            } else {
                continue;
            }

            System.out.println(name);
            System.out.println(newPath);
            FileUtil.rename(file, newPath, false);
        }
    }
}
