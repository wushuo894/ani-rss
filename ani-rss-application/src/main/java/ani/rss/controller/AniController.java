package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.ListAni;
import ani.rss.entity.dto.IdDTO;
import ani.rss.entity.dto.ImportAniDataDTO;
import ani.rss.entity.dto.RssToAniDTO;
import ani.rss.entity.web.Result;
import ani.rss.service.AniService;
import ani.rss.service.DownloadService;
import ani.rss.util.other.AniUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class AniController extends BaseController {
    @Resource
    private AniService aniService;

    @Resource
    private DownloadService downloadService;

    @Auth
    @Operation(summary = "添加订阅")
    @PostMapping("/addAni")
    public Result<Void> addAni(@RequestBody Ani ani) {
        aniService.addAni(ani);
        return Result.success("添加订阅成功");
    }

    @Auth
    @Operation(summary = "修改订阅")
    @PostMapping("/setAni")
    public Result<Void> setAni(@RequestBody Ani ani) {
        aniService.setAni(ani);
        return Result.success("修改成功");
    }

    @Auth
    @Operation(summary = "删除订阅")
    @PostMapping("/deleteAni")
    public Result<Void> deleteAni(@RequestBody List<String> ids, @RequestParam("deleteFiles") Boolean deleteFiles) {
        aniService.deleteAni(ids, deleteFiles);
        return Result.success("删除订阅成功");
    }

    @Auth
    @Operation(summary = "订阅列表")
    @PostMapping("/listAni")
    public Result<ListAni> listAni() {
        ListAni listAni = aniService.listAni();
        return Result.success(listAni);
    }

    @Auth
    @Operation(summary = "更新总集数")
    @PostMapping("/updateTotalEpisodeNumber")
    public Result<Void> updateTotalEpisodeNumber(@RequestParam("force") Boolean force, @RequestBody List<String> ids) {
        aniService.updateTotalEpisodeNumber(force, ids);
        return Result.success("已开始更新总集数");
    }

    @Auth
    @Operation(summary = "批量 启用/禁用 订阅")
    @PostMapping("/batchEnable")
    public Result<Void> batchEnable(@RequestParam("value") Boolean value, @RequestBody List<String> ids) {
        aniService.batchEnable(value, ids);
        return Result.success("修改完成");
    }

    @Auth
    @Operation(summary = "刷新全部订阅")
    @PostMapping("/refreshAll")
    public Result<Void> refreshAll() {
        aniService.refreshAll();
        return Result.success("已开始刷新RSS");
    }

    @Auth
    @Operation(summary = "刷新订阅")
    @PostMapping("/refreshAni")
    public Result<Void> refreshAni(@RequestBody IdDTO dto) {
        aniService.refreshAni(dto);
        return Result.success("已开始刷新RSS");
    }

    @Auth
    @Operation(summary = "将RSS转换为订阅")
    @PostMapping("/rssToAni")
    public Result<Ani> rssToAni(@RequestBody RssToAniDTO dto) {
        try {
            Ani newAni = AniUtil.getAni(dto);
            return Result.success(newAni);
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            return Result.error("RSS解析失败 {}", message);
        }
    }

    @Auth
    @Operation(summary = "预览订阅")
    @PostMapping("/previewAni")
    public Result<Map<String, Object>> previewAni(@RequestBody Ani ani) {
        Map<String, Object> map = aniService.previewAni(ani);
        return Result.success(map);
    }

    @Auth
    @Operation(summary = "获取订阅的下载位置")
    @PostMapping("/downloadPath")
    public Result<Map<String, Object>> downloadPath(@RequestBody Ani ani) {
        Map<String, Object> map = aniService.downloadPath(ani);
        return Result.success(map);
    }

    @Auth
    @Operation(summary = "导入订阅")
    @PostMapping("/importAni")
    public Result<Void> importAni(@RequestBody ImportAniDataDTO dto) {
        aniService.importAni(dto);
        return Result.success("导入成功");
    }

    @Auth
    @Operation(summary = "刷新封面")
    @PostMapping("/refreshCover")
    public Result<String> refreshCover(@RequestBody Ani ani) {
        String s = AniUtil.saveCover(ani.getImage(), true);
        return Result.success(r -> r.setData(s));
    }

}
