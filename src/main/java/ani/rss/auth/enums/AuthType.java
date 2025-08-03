package ani.rss.auth.enums;

import ani.rss.auth.fun.ApiKey;
import ani.rss.auth.fun.Form;
import ani.rss.auth.fun.Header;
import ani.rss.auth.fun.IpWhitelist;
import cn.hutool.http.server.HttpServerRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * 可以扩展出 api_key cookie 校验
 */
@AllArgsConstructor
public enum AuthType {
    HEADER(Header.class),
    FORM(Form.class),
    API_KEY(ApiKey.class),
    IP_WHITE_LIST(IpWhitelist.class);

    @Getter
    private final Class<? extends Function<HttpServerRequest, Boolean>> clazz;
}
