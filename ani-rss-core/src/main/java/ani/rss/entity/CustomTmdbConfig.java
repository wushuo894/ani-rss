package ani.rss.entity;

import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.util.StrUtil;
import wushuo.tmdb.api.entity.TmdbConfig;

public class CustomTmdbConfig extends TmdbConfig {

    public final static Config CONFIG = ConfigUtil.CONFIG;

    public CustomTmdbConfig() {
        super("");
    }

    @Override
    public String getTmdbApi() {
        return CONFIG.getTmdbApi();
    }

    @Override
    public String getTmdbApiKey() {
        String tmdbApiKey = CONFIG.getTmdbApiKey();
        tmdbApiKey = StrUtil.blankToDefault(tmdbApiKey, "450e4f651e1c93e31383e20f8e731e5f");
        return tmdbApiKey;
    }

    @Override
    public String getTmdbLanguage() {
        return CONFIG.getTmdbLanguage();
    }

    @Override
    public Boolean getTmdbAnime() {
        return CONFIG.getTmdbAnime();
    }

    @Override
    public Boolean getProxy() {
        return CONFIG.getProxy();
    }

    @Override
    public String getProxyHost() {
        return CONFIG.getProxyHost();
    }

    @Override
    public Integer getProxyPort() {
        return CONFIG.getProxyPort();
    }

    @Override
    public String getProxyUsername() {
        return CONFIG.getProxyUsername();
    }

    @Override
    public String getProxyPassword() {
        return CONFIG.getProxyPassword();
    }
}
