package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.util.AniUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
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
            String themoviedbName = AniUtil.getThemoviedbName(name);
            String yearReg = " (\\d+)$";
            if (ReUtil.contains(yearReg, name)) {
                themoviedbName = StrFormatter.format("{} ({})", themoviedbName, ReUtil.get(yearReg, name, 1));
            }
            resultSuccess(themoviedbName);
        }
    }
}
