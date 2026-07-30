package ani.rss.entity.torrent;

import ani.rss.entity.Config;
import ani.rss.enums.TransmissionMethodEnum;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import com.google.gson.annotations.SerializedName;
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
    private String id;
    @SerializedName("jsonrpc")
    private String jsonRpc;
    private String method;
    private Map<String, Object> params;

    public static TransmissionRpcBody getInstance(TransmissionMethodEnum method) {
        Config config = ConfigUtil.CONFIG;
        String uuid = config.getUuid();

        TransmissionRpcBody transmissionRpcBody = new TransmissionRpcBody();
        transmissionRpcBody
                .setId(uuid)
                .setJsonRpc("2.0")
                .setMethod(method.getValue())
                .setParams(new HashMap<>());
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentAdd(List<String> tags, File torrent, String downloadDir) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentAdd);
        Map<String, Object> params = transmissionRpcBody.getParams();
        params.put("labels", tags);
        params.put("download_dir", downloadDir);
        params.put("metainfo", Base64.encode(torrent));
        params.put("filename", "");
        params.put("paused", false);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentAdd(List<String> tags, String magnet, String downloadDir) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentAdd);
        Map<String, Object> params = transmissionRpcBody.getParams();
        params.put("labels", tags);
        params.put("download_dir", downloadDir);
        params.put("metainfo", "");
        params.put("filename", magnet);
        params.put("paused", false);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentGet() {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentGet);
        Map<String, Object> params = transmissionRpcBody.getParams();

        List<String> fields = List.of(
                "name", "labels", "hash_string", "files",
                "is_finished", "id", "download_dir",
                "status", "total_size", "have_valid"
        );

        params.put("fields", fields);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentRemove(String hash, Boolean deleteLocalData) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentRemove);
        Map<String, Object> params = transmissionRpcBody.getParams();
        params.put("ids", List.of(hash));
        params.put("delete_local_data", deleteLocalData);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentRenamePath(String hash, String path, String name) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentRenamePath);
        Map<String, Object> params = transmissionRpcBody.getParams();
        params.put("ids", List.of(hash));
        params.put("path", path);
        params.put("name", name);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentSet(String hash, List<String> tags) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentSet);
        Map<String, Object> params = transmissionRpcBody.getParams();
        params.put("ids", List.of(hash));
        params.put("labels", tags);
        return transmissionRpcBody;
    }

    public static TransmissionRpcBody torrentSetLocation(String hash, String location) {
        TransmissionRpcBody transmissionRpcBody = getInstance(TransmissionMethodEnum.torrentSetLocation);
        Map<String, Object> params = transmissionRpcBody.getParams();
        params.put("ids", List.of(hash));
        params.put("location", location);
        return transmissionRpcBody;
    }

}
