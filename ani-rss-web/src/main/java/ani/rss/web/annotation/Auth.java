package ani.rss.web.annotation;

import ani.rss.web.auth.enums.AuthType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 鉴权
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Auth {
    boolean value() default true;

    AuthType[] type() default {
            AuthType.IP_WHITE_LIST,
            AuthType.HEADER,
            AuthType.FORM
    };
}
