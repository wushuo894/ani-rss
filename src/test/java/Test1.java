import cn.hutool.core.util.ReUtil;

public class Test1 {
    public static void main(String[] args) {
        String s = "(.*|\\[.*])( -? \\d+|\\[\\d+]|\\[\\d+.?[vV]\\d]|第\\d+[话話集]|\\[第?\\d+[话話集]]|\\[\\d+.?END]|[Ee][Pp]?\\d+)(.*)";
        String title = "[LoliHouse] 我的妻子没有感情 / Boku no Tsuma wa Kanjou ga Nai - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]";
        String e = ReUtil.get(s, title, 2);
        String episode = ReUtil.get("\\d+", e, 0);
        System.out.println(episode);
    }
}
