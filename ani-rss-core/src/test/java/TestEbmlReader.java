import cn.hutool.core.lang.Assert;
import com.matthewn4444.ebml.EBMLReader;
import com.matthewn4444.ebml.subtitles.Subtitles;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
public class TestEbmlReader {
    @Test
    public void test() throws IOException {
        @Cleanup
        EBMLReader reader = new EBMLReader("/Users/wushuo/Movies/test/[LoliHouse] 蓝色管弦乐 S02E11.mkv");
        Assert.isTrue(reader.readHeader(), "文件格式错误");

        reader.readTracks();
        reader.readCues();

        for (int i = 0; i < reader.getCuesCount(); i++) {
            reader.readSubtitlesInCueFrame(i);
        }

        ArrayList<Subtitles> subtitles = reader.getSubtitles();
        for (Subtitles subtitle : subtitles) {
            String name = subtitle.getName();
            String language = subtitle.getLanguage();
            String presentableName = subtitle.getPresentableName();
            log.info("{} {} {}", name, language, presentableName);
        }
    }
}
