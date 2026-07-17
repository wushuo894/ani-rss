package ani.rss.entity.torrent;

import ani.rss.enums.TransmissionMethodEnum;
import cn.hutool.core.codec.Base64;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class TransmissionRpcBody implements Serializable {
    private String tag;
    private String method;
    private Map<String, Object> arguments;

    public static TransmissionRpcBody getInstance(TransmissionMethodEnum method) {
        TransmissionRpcBody transmissionRpcBody = new TransmissionRpcBody();
        transmissionRpcBody.setTag("");
        transmissionRpcBody.setMethod(method.getValue());
        transmissionRpcBody.setArguments(new HashMap<>());
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentAdd(List<String> tags, File torrent, String downloadDir) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentAdd);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();
        arguments.put("labels", tags);
        arguments.put("download-dir", downloadDir);
        arguments.put("metainfo", Base64.encode(torrent));
        arguments.put("filename", "");
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentAdd(List<String> tags, String magnet, String downloadDir) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentAdd);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();
        arguments.put("labels", tags);
        arguments.put("download-dir", downloadDir);
        arguments.put("metainfo", "");
        arguments.put("filename", magnet);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentGet() {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentGet);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();

        List<String> fields = List.of(
                "name", "labels", "hashString", "files",
                "isFinished", "isStalled", "id", "downloadDir",
                "status", "totalSize", "haveValid"
        );

        arguments.put("fields", fields);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentRemove(String id, Boolean deleteLocalData) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentAdd);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();
        arguments.put("ids", List.of(id));
        arguments.put("delete-local-data", deleteLocalData);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentRenamePath(String id, String path, String name) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentRenamePath);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();
        arguments.put("id", id);
        arguments.put("path", path);
        arguments.put("name", name);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentSet(String id, List<String> tags) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentSet);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();
        arguments.put("ids", List.of(id));
        arguments.put("labels", tags);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentSetLocation(String id, String location) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentSetLocation);
        Map<String, Object> arguments = transmissionRpcBody.getArguments();
        arguments.put("ids", List.of(id));
        arguments.put("location", location);
        return transmissionRpcBody;
    }

}
