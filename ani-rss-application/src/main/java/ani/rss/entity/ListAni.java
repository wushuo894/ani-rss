package ani.rss.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class ListAni implements Serializable {

    private List<String> releaseDateList;

    private List<WeekAni> weekList;

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeekAni implements Serializable {
        private String weekLabel;
        private List<Ani> items;
    }
}
