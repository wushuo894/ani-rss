package ani.rss.action;

import ani.rss.entity.PlayItem;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;
import com.matthewn4444.ebml.EBMLReader;
import com.matthewn4444.ebml.subtitles.Subtitles;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频操作
 */
@Auth
@Slf4j
@Path("/playitem")
public class PlayItemAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        JsonObject jsonObject = getBody(JsonObject.class);
        String type = jsonObject.get("type").getAsString();
        String file = jsonObject.get("file").getAsString();
        if ("getSubtitles".equalsIgnoreCase(type)) {
            getSubtitles(file);
            return;
        }

        resultErrorMsg("未知操作");
    }

    /**
     * 获取内封字幕并返回到客户端
     *
     * @param file
     * @throws IOException
     */
    public void getSubtitles(String file) throws IOException {
        Assert.notBlank(file);

        if (Base64.isBase64(file)) {
            file = Base64.decodeStr(file);
        }

        List<PlayItem.Subtitles> subtitlesList = new ArrayList<>();

        String extName = FileUtil.extName(file);
        if (StrUtil.isBlank(extName)) {
            resultSuccess(subtitlesList);
            return;
        }

        if (!"mkv".equals(extName)) {
            resultSuccess(subtitlesList);
            return;
        }

        Assert.isTrue(FileUtil.exist(file), "视频文件不存在");

        @Cleanup
        EBMLReader reader = new EBMLReader(file);
        if (!reader.readHeader()) {
            resultSuccess(subtitlesList);
            return;
        }
        reader.readTracks();
        reader.readCues();

        for (int i = 0; i < reader.getCuesCount(); i++) {
            reader.readSubtitlesInCueFrame(i);
        }

        List<Subtitles> subtitles = reader.getSubtitles();
        for (Subtitles subtitle : subtitles) {
            String name = subtitle.getName();
            String presentableName = subtitle.getPresentableName();
            String contents = subtitle.getContentsToVTT();
            PlayItem.Subtitles sub = new PlayItem.Subtitles();
            sub.setContent(contents)
                    .setName(name)
                    .setHtml(presentableName)
                    .setUrl("")
                    .setType("vtt");
            subtitlesList.add(sub);
        }

        resultSuccess(subtitlesList);
    }
}
