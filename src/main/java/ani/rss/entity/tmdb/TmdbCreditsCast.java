package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 演职人员
 */
@Data
@Accessors(chain = true)
public class TmdbCreditsCast implements Serializable {
    private Integer id;

    private String name;

    @SerializedName(value = "originalName", alternate = "original_name")
    private String originalName;

    private Double popularity;

    @SerializedName(value = "profilePath", alternate = "profile_path")
    private String profilePath;

    private String character;

    @SerializedName(value = "creditId", alternate = "credit_id")
    private String creditId;

    private String order;

    /**
     * 成人
     */
    private Boolean adult;

    /**
     * 性别
     */
    public Integer gender;

    @SerializedName(value = "knownForDepartment", alternate = "known_for_department")
    private String knownForDepartment;
}
