package ani.rss.entity.torrent;

import ani.rss.entity.Config;
import ani.rss.enums.Aria2MethodEnum;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class Aria2RpcBody implements Serializable {
    private String id;
    @SerializedName("jsonrpc")
    private String jsonRpc;
    private String method;
    private List<Object> params;

    public static Aria2RpcBody getInstance(Aria2MethodEnum method) {
        Config config = ConfigUtil.CONFIG;
        String uuid = config.getUuid();
        String downloadToolPassword = config.getDownloadToolPassword();

        Aria2RpcBody aria2RpcBody = new Aria2RpcBody();
        List<Object> params = new ArrayList<>();
        aria2RpcBody
                .setId(uuid)
                .setJsonRpc("2.0")
                .setMethod("aria2." + method.name())
                .setParams(params);

        params.add("token:" + downloadToolPassword);
        return aria2RpcBody;
    }

    public static Aria2RpcBody addTorrent(File torrent, String savePath) {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.addTorrent);
        List<Object> params = aria2RpcBody.getParams();
        params.add(Base64.encode(torrent));
        params.add(List.of());
        params.add(Map.of(
                "dir", savePath
        ));
        return aria2RpcBody;
    }

    public static Aria2RpcBody addUri(String uri, String savePath) {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.addUri);
        List<Object> params = aria2RpcBody.getParams();
        params.add(List.of(uri));
        params.add(Map.of(
                "dir", savePath
        ));
        return aria2RpcBody;
    }

    public static Aria2RpcBody changeGlobalOption(String trackers) {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.changeGlobalOption);
        List<Object> params = aria2RpcBody.getParams();
        params.add(Map.of(
                "bt-tracker", trackers
        ));
        return aria2RpcBody;
    }

    public static Aria2RpcBody getGlobalStat() {
        return getInstance(Aria2MethodEnum.getGlobalStat);
    }

    public static Aria2RpcBody removeDownloadResult(String id) {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.removeDownloadResult);
        List<Object> params = aria2RpcBody.getParams();
        params.add(id);
        return aria2RpcBody;
    }

    public static Aria2RpcBody tellActive() {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.tellActive);
        List<Object> params = aria2RpcBody.getParams();
        params.add(List.of("gid", "totalLength", "completedLength", "status", "files", "bittorrent", "infoHash", "dir"));
        return aria2RpcBody;
    }

    public static Aria2RpcBody tellStopped() {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.tellStopped);
        List<Object> params = aria2RpcBody.getParams();
        params.add(-1);
        params.add(1000);
        params.add(List.of("gid", "totalLength", "completedLength", "status", "files", "bittorrent", "infoHash", "dir"));
        return aria2RpcBody;
    }

    public static Aria2RpcBody tellWaiting() {
        Aria2RpcBody aria2RpcBody = getInstance(Aria2MethodEnum.tellWaiting);
        List<Object> params = aria2RpcBody.getParams();
        params.add(-1);
        params.add(1000);
        params.add(List.of("gid", "totalLength", "completedLength", "status", "files", "bittorrent", "infoHash", "dir"));
        return aria2RpcBody;
    }
}
