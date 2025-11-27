package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.service.DownloadService;
import ani.rss.util.other.ItemsUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 预览订阅
 */
@Auth
@Path("/items")
public class ItemsAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        List<Item> items = ItemsUtil.getItems(ani);

        String downloadPath = DownloadService.getDownloadPath(ani);

        for (Item item : items) {
            item.setLocal(false);
            File torrent = TorrentUtil.getTorrent(ani, item);
            if (torrent.exists()) {
                item.setLocal(true);
                continue;
            }
            if (DownloadService.itemDownloaded(ani, item, false)) {
                item.setLocal(true);
            }
        }

        List<Integer> omitList = ItemsUtil.omitList(ani, items);

        resultSuccess(Map.of(
                "downloadPath", downloadPath,
                "items", items,
                "omitList", omitList
        ));
    }
}
