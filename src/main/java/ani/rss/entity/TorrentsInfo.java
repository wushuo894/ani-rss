package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TorrentsInfo {
    private String hash;
    private String name;
    private State state;
    private Long progress;

    public enum State {
        checkingResumeData,
        /**
         * 停滞中
         */
        stalledDL,
        /**
         * 下载中
         */
        downloading,
        /**
         * 做种中
         */
        stalledUP,
        /**
         * 已完成
         */
        pausedUP
    }
}


