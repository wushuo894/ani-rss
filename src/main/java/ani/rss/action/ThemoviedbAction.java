package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Result;
import ani.rss.util.TmdbUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

@Auth
@Path("/tmdb")
public class ThemoviedbAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String s = request.getParam("method");
        if ("getThemoviedbName".equals(s)) {
            String name = request.getParam("name");
            String themoviedbName = TmdbUtil.getName(name);
            Result<String> result = new Result<String>()
                    .setCode(HttpStatus.HTTP_OK)
                    .setMessage("获取TMDB成功")
                    .setData(themoviedbName);
            if (StrUtil.isBlank(themoviedbName)) {
                result.setCode(HttpStatus.HTTP_INTERNAL_ERROR)
                        .setMessage("获取TMDB失败");
            }
            result(result);
        }
    }
}
