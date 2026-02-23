package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Ani;
import ani.rss.entity.Result;
import ani.rss.util.other.TmdbUtils;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wushuo.tmdb.api.entity.Tmdb;
import wushuo.tmdb.api.entity.TmdbGroup;

import java.util.List;

@RestController
public class ThemoviedbController {

    @Auth
    @Operation(summary = "获取TMDB标题")
    @PostMapping("/getThemoviedbName")
    public Result<Ani> getThemoviedbName(@RequestBody Ani ani) {
        String themoviedbName = TmdbUtils.getFinalName(ani);
        Result<Ani> result = new Result<Ani>()
                .setCode(HttpStatus.HTTP_OK)
                .setMessage("获取TMDB成功")
                .setData(ani.setThemoviedbName(themoviedbName));
        if (StrUtil.isBlank(themoviedbName)) {
            result.setCode(HttpStatus.HTTP_INTERNAL_ERROR)
                    .setMessage("获取TMDB失败");
        }
        return result;
    }

    @Auth
    @Operation(summary = "获取TMDB剧集组")
    @PostMapping("/getThemoviedbGroup")
    public Result<List<TmdbGroup>> getThemoviedbGroup(@RequestBody Ani ani) {
        Tmdb tmdb = ani.getTmdb();
        Assert.notNull(tmdb, "tmdb is null");
        Assert.notBlank(tmdb.getId(), "tmdb is null");
        List<TmdbGroup> tmdbGroup = TmdbUtils.getTmdbGroup(tmdb);
        return Result.success(tmdbGroup);
    }
}
