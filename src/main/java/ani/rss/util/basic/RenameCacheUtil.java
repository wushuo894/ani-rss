package ani.rss.util.basic;


import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.SqlConnRunner;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

/**
 * 重命名缓存
 */
@Slf4j
public class RenameCacheUtil {
    private static final String TABLE_NAME = "RENAME_CACHES";
    private static Connection connection;
    private static SqlConnRunner sqlConnRunner;

    private static synchronized void connection() {
        if (Objects.nonNull(connection)) {
            return;
        }

        File configDir = ConfigUtil.getConfigDir();
        String absolutePath = MyFileUtil.getAbsolutePath(configDir);
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(StrFormatter.format("jdbc:sqlite:{}/database.db", absolutePath));

            Statement statement = connection.createStatement();

            String sql = StrFormatter.format("CREATE TABLE IF NOT EXISTS {} (K TEXT PRIMARY KEY, V TEXT)", TABLE_NAME);
            statement.execute(sql);

            sqlConnRunner = DbUtil.newSqlConnRunner(connection);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> DbUtil.close(connection)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static synchronized void put(String key, String object) {
        remove(key);
        log.debug("put => key: {}, object: {}", key, object);
        try {
            sqlConnRunner.insert(connection,
                    new Entity(TABLE_NAME)
                            .set("K", key)
                            .set("V", object)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized String get(String key) {
        connection();
        log.debug("get => key: {}", key);
        try {
            List<Entity> list = sqlConnRunner.find(connection,
                    new Entity(TABLE_NAME)
                            .set("K", key)
            );
            if (list.isEmpty()) {
                return null;
            }
            return (String) list.get(0).get("V");
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static synchronized void remove(String key) {
        connection();
        try {
            int i = sqlConnRunner.del(connection,
                    new Entity(TABLE_NAME)
                            .set("K", key)
            );
            if (i > 0) {
                log.debug("remove => key: {}", key);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
