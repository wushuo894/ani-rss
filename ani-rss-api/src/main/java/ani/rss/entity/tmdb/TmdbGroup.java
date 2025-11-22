package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * tmdb剧集组
 */
@Data
@Accessors(chain = true)
public class TmdbGroup implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * 剧集组名
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 类型名
     */
    @SerializedName(value = "typeName", alternate = "type_name")
    private String typeName;

    /**
     * 集数量
     */
    @SerializedName(value = "episodeCount", alternate = "episode_count")
    private String episodeCount;

    /**
     * 组数量
     */
    @SerializedName(value = "groupCount", alternate = "group_count")
    private String groupCount;

    /**
     * 描述
     */
    private String description;
}
