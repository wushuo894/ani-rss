import ani.rss.util.FilePathUtil;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        System.out.println(FilePathUtil.getAbsolutePath(new File("D:\\Media\\番剧\\魔女与使魔\\Season 1\\[ANi] 魔女与使魔 S01E03.mp4").getPath()));
        System.out.println(FilePathUtil.getAbsolutePath("D:\\Media\\番剧\\魔女与使魔\\Season 1\\[ANi] 魔女与使魔 S01E03.mp4"));
    }
}
