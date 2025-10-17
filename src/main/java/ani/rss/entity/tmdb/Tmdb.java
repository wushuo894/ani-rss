package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * tmdb
 */
@Data
@Accessors(chain = true)
public class Tmdb implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * 名称
     */
    @SerializedName(value = "name", alternate = "title")
    private String name;

    /**
     * 原名
     */
    @SerializedName(value = "originalName", alternate = {"original_name", "original_title"})
    private String originalName;

    /**
     * 剧集组id
     */
    @SerializedName(value = "tmdbGroupId", alternate = "tmdb_group_id")
    private String tmdbGroupId;

    /**
     * 概述
     */
    private String overview;

    /**
     * 平均评分
     */
    @SerializedName(value = "voteAverage", alternate = "vote_average")
    private String voteAverage;

    /**
     * 评分人数
     */
    @SerializedName(value = "voteCount", alternate = "vote_count")
    private String voteCount;

    /**
     * 封面
     */
    @SerializedName(value = "posterPath", alternate = "poster_path")
    private String posterPath;

    /**
     * 背景图
     */
    @SerializedName(value = "backdropPath", alternate = "backdrop_path")
    private String backdropPath;

    /**
     * 成人
     */
    private Boolean adult;

    /**
     * 原语言
     */
    @SerializedName(value = "originalLanguage", alternate = "original_language")
    private String originalLanguage;

    /**
     * 原产地
     */
    @SerializedName(value = "originCountry", alternate = "origin_country")
    private List<String> originCountry;

    /**
     * 首映日期
     */
    @SerializedName(value = "date", alternate = {"first_air_date", "release_date"})
    private Date date;

    /**
     * 种类
     */
    @SerializedName(value = "genreIds", alternate = "genre_ids")
    private List<Integer> genreIds;

    /**
     * 主页
     */
    private String homepage;

    /**
     * 种类
     */
    private List<TmdbGenres> genres;

    /**
     * 演职人员
     */
    private List<TmdbCredit> credits;
}
