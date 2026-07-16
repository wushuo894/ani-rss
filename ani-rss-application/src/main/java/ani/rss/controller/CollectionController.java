package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.CollectionInfo;
import ani.rss.entity.Item;
import ani.rss.entity.web.Result;
import ani.rss.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class CollectionController extends BaseController {

    @Resource
    private CollectionService collectionService;

    @Auth
    @Operation(summary = "开始下载合集")
    @PostMapping("/startCollection")
    public Result<Void> startCollection(@RequestBody CollectionInfo collectionInfo) {
        collectionService.startCollection(collectionInfo);
        return Result.success("已经开始下载合集");
    }

    @Auth
    @Operation(summary = "合集预览")
    @PostMapping("/previewCollection")
    public Result<List<Item>> previewCollection(@RequestBody CollectionInfo collectionInfo) {
        List<Item> items = collectionService.previewCollection(collectionInfo);
        return Result.success(items);
    }

    @Auth
    @Operation(summary = "获取合集字幕组")
    @PostMapping("/getCollectionSubgroup")
    public Result<String> getCollectionSubgroup(@RequestBody CollectionInfo collectionInfo) {
        String subgroup = collectionService.getCollectionSubgroup(collectionInfo);

        Result<String> result = Result.success();
        result.setData(subgroup);
        return result;
    }
}
