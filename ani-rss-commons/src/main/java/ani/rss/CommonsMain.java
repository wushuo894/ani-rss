package ani.rss;

import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class CommonsMain {
    public static void main(String[] args) {
        Set<Class<?>> classSet = ClassUtil.scanPackage("ani.rss.commons");
        for (Class<?> clazz : classSet) {
            if (clazz.isMemberClass()) {
                continue;
            }
            log.info(clazz.getName());
        }
    }
}
