package ani.rss.entity;

import ani.rss.commons.DateAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class BgmEpisodes implements Serializable {
    private List<BgmEpisode> data;
    private Integer total;
    private Integer limit;
    private Integer offset;

    @Data
    @Accessors(chain = true)
    public static class BgmEpisode implements Serializable {
        @JsonAdapter(DateAdapter.class)
        private Date airdate;
        private String name;
        @SerializedName(value = "nameCn", alternate = "name_cn")
        private String nameCn;
        private String duration;
        private String desc;
        private Integer ep;
        private Integer sort;
        private String id;
        @SerializedName(value = "subjectId", alternate = "subject_id")
        private Integer subjectId;
        private Integer comment;
        private Integer type;
        private Integer disc;
        @SerializedName(value = "durationSeconds", alternate = "duration_seconds")
        private Integer durationSeconds;
    }
}
