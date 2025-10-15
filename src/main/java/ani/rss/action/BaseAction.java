package ani.rss.action;

import ani.rss.entity.Result;
import ani.rss.util.basic.GsonStatic;
import ani.rss.util.other.ServerUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public interface BaseAction extends Action {

    Logger logger = LoggerFactory.getLogger(BaseAction.class);

    static <T> void staticResult(Result<T> result) {
        HttpServerResponse response = ServerUtil.RESPONSE.get();
        if (Objects.isNull(response)) {
            logger.error("response is null");
            return;
        }
        result.setT(System.currentTimeMillis());
        response.setHeader(Header.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(Header.PRAGMA, "no-cache");
        response.setHeader("Expires", "0");
        response.setContentType("application/json; charset=utf-8");
        String json = GsonStatic.toJson(result);
        IoUtil.writeUtf8(response.getOut(), true, json);
    }

    default <T> T getBody(Class<T> tClass) {
        return GsonStatic.fromJson(ServerUtil.REQUEST.get().getBody(), tClass);
    }

    default <T> void resultSuccess() {
        result(Result.success());
    }

    default <T> void resultSuccess(Consumer<Result<Object>> consumer) {
        Result<Object> success = Result.success();
        consumer.accept(success);
        result(success);
    }

    default <T> void resultSuccess(T t) {
        result(Result.success(t));
    }

    default <T> void resultSuccessMsg(String t, Object... argArray) {
        result(Result.success().setMessage(StrFormatter.format(t, argArray)));
    }

    default <T> void resultError() {
        result(Result.error());
    }

    default <T> void resultError(Consumer<Result<Object>> consumer) {
        Result<Object> error = Result.error();
        consumer.accept(error);
        result(error);
    }

    default <T> void resultError(T t) {
        result(Result.error(t));
    }

    default <T> void resultErrorMsg(String t, Object... argArray) {
        result(Result.error().setMessage(StrFormatter.format(t, argArray)));
    }

    default <T> void result(Result<T> result) {
        staticResult(result);
    }
}
