package ani.rss.util.basic;

import ani.rss.entity.Config;
import ani.rss.entity.Log;
import ani.rss.list.FixedSizeLinkedList;
import ani.rss.util.other.ConfigUtil;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.http.HtmlUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
public class LogUtil {

    public static final List<Log> LOG_LIST = Collections.synchronizedList(new FixedSizeLinkedList<>());

    public static void loadLogback() {
        Config config = ConfigUtil.CONFIG;
        Boolean debug = config.getDebug();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            String s = ResourceUtil.readUtf8Str("logback-template.xml");
            s = s.replace("${config}", ConfigUtil.getConfigDir() + "/");
            s = s.replace("${level}", debug ? "debug" : "info");

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();

            byteArrayInputStream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
            configurator.doConfigure(byteArrayInputStream);

            Appender<ILoggingEvent> file = rootLogger.getAppender("FILE");
            file.clearAllFilters();
            file.addFilter(new AbstractMatcherFilter<>() {
                @Override
                public FilterReply decide(ILoggingEvent event) {
                    Instant instant = event.getInstant();
                    String date = DateUtil.format(new Date(instant.toEpochMilli()), DatePattern.NORM_DATETIME_PATTERN);
                    String level = event.getLevel().toString();
                    String loggerName = event.getLoggerName();
                    String formattedMessage = event.getFormattedMessage();
                    String threadName = event.getThreadName();
                    StringBuilder log = new StringBuilder(StrFormatter.format("{} {} [{}] {} - {}", date, level, threadName, loggerName, formattedMessage));
                    IThrowableProxy throwableProxy = event.getThrowableProxy();
                    addThrowableMsg(log, throwableProxy);
                    Log logEntity = new Log()
                            .setMessage(log.toString())
                            .setLevel(level)
                            .setLoggerName(loggerName)
                            .setThreadName(threadName);
                    synchronized (LOG_LIST) {
                        LOG_LIST.add(logEntity);
                    }
                    return FilterReply.NEUTRAL;
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.close(byteArrayInputStream);
        }
    }

    @SneakyThrows
    public static void addThrowableMsg(StringBuilder log, IThrowableProxy throwableProxy) {
        if (Objects.isNull(throwableProxy)) {
            return;
        }
        Config config = ConfigUtil.CONFIG;
        Boolean debug = config.getDebug();
        if (!debug) {
            return;
        }
        String className = throwableProxy.getClassName();
        String message = throwableProxy.getMessage();
        // Fix: Escape the message before appending to the log
        log.append(StrFormatter.format("\r\n{}: {}", className, HtmlUtil.escape(message)));
        StackTraceElementProxy[] stackTraceElementProxyArray = throwableProxy.getStackTraceElementProxyArray();
        if (Objects.isNull(stackTraceElementProxyArray)) {
            return;
        }
        for (StackTraceElementProxy stackTraceElementProxy : stackTraceElementProxyArray) {
            // Fix: Escape stack trace elements before appending
            log.append("\r\n\t").append(HtmlUtil.escape(stackTraceElementProxy.toString()));
        }
        IThrowableProxy cause = throwableProxy.getCause();
        if (Objects.isNull(cause)) {
            return;
        }
        addThrowableMsg(log, cause);
    }
}
