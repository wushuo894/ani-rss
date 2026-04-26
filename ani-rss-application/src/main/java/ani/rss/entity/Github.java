package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class Github implements Serializable {

    @Data
    @Accessors(chain = true)
    public static class Release implements Serializable {
        private String id;

        @SerializedName(value = "nodeId", alternate = "node_id")
        private String nodeId;

        @SerializedName(value = "tagName", alternate = "tag_name")
        private String tagName;

        @SerializedName(value = "targetCommitish", alternate = "target_commitish")
        private String targetCommitish;

        private String name;

        private String url;

        @SerializedName(value = "assetsUrl", alternate = "assets_url")
        private String assetsUrl;

        @SerializedName(value = "uploadUrl", alternate = "upload_url")
        private String uploadUrl;

        @SerializedName(value = "htmlUrl", alternate = "html_url")
        private String htmlUrl;

        private Boolean draft;

        private Boolean immutable;

        private Boolean prerelease;

        @SerializedName(value = "createdAt", alternate = "created_at")
        private Date createdAt;

        @SerializedName(value = "updatedAt", alternate = "updated_at")
        private Date updatedAt;

        @SerializedName(value = "publishedAt", alternate = "published_at")
        private Date publishedAt;

        private Author author;

        private List<Assets> assets;

        private String body;

        private String message;
    }

    @Data
    @Accessors(chain = true)
    public static class Assets implements Serializable {
        private String url;

        private String id;

        @SerializedName(value = "nodeId", alternate = "node_id")
        private String nodeId;

        private String name;

        private String label;

        private Uploader uploader;

        @SerializedName(value = "contentType", alternate = "content_type")
        private String contentType;

        private String state;

        private Long size;

        private String digest;

        @SerializedName(value = "downloadCount", alternate = "download_count")
        private Integer downloadCount;

        @SerializedName(value = "createdAt", alternate = "created_at")
        private Date createdAt;

        @SerializedName(value = "updatedAt", alternate = "updated_at")
        private Date updatedAt;

        @SerializedName(value = "browserDownloadUrl", alternate = "browser_download_url")
        private String browserDownloadUrl;
    }

    @Data
    @Accessors(chain = true)
    public static class Uploader implements Serializable {
        private String login;

        private String id;

        @SerializedName(value = "nodeId", alternate = "node_id")
        private String nodeId;

        @SerializedName(value = "avatarUrl", alternate = "avatar_url")
        private String avatarUrl;

        @SerializedName(value = "gravatarId", alternate = "gravatar_id")
        private String gravatarId;

        private String url;

        @SerializedName(value = "htmlUrl", alternate = "html_url")
        private String htmlUrl;

        @SerializedName(value = "followersUrl", alternate = "followers_url")
        private String followersUrl;

        @SerializedName(value = "followingUrl", alternate = "following_url")
        private String followingUrl;

        @SerializedName(value = "gistsUrl", alternate = "gists_url")
        private String gistsUrl;

        @SerializedName(value = "starredUrl", alternate = "starred_url")
        private String starredUrl;

        @SerializedName(value = "subscriptionsUrl", alternate = "subscriptions_url")
        private String subscriptionsUrl;

        @SerializedName(value = "organizationsUrl", alternate = "organizations_url")
        private String organizationsUrl;

        @SerializedName(value = "reposUrl", alternate = "repos_url")
        private String reposUrl;

        @SerializedName(value = "eventsUrl", alternate = "events_url")
        private String eventsUrl;

        @SerializedName(value = "receivedEventsUrl", alternate = "received_events_url")
        private String receivedEventsUrl;

        private String type;

        @SerializedName(value = "userViewType", alternate = "user_view_type")
        private String userViewType;

        @SerializedName(value = "siteAdmin", alternate = "site_admin")
        private Boolean siteAdmin;
    }

    @Data
    @Accessors(chain = true)
    public static class Author implements Serializable {
        private String login;

        private String id;

        @SerializedName(value = "nodeId", alternate = "node_id")
        private String nodeId;

        @SerializedName(value = "avatarUrl", alternate = "avatar_url")
        private String avatarUrl;

        @SerializedName(value = "gravatarId", alternate = "gravatar_id")
        private String gravatarId;

        private String url;

        @SerializedName(value = "htmlUrl", alternate = "html_url")
        private String htmlUrl;

        @SerializedName(value = "followersUrl", alternate = "followers_url")
        private String followersUrl;

        @SerializedName(value = "followingUrl", alternate = "following_url")
        private String followingUrl;

        @SerializedName(value = "gistsUrl", alternate = "gists_url")
        private String gistsUrl;

        @SerializedName(value = "starredUrl", alternate = "starred_url")
        private String starredUrl;

        @SerializedName(value = "subscriptionsUrl", alternate = "subscriptions_url")
        private String subscriptionsUrl;

        @SerializedName(value = "organizationsUrl", alternate = "organizations_url")
        private String organizationsUrl;

        @SerializedName(value = "reposUrl", alternate = "repos_url")
        private String reposUrl;

        @SerializedName(value = "eventsUrl", alternate = "events_url")
        private String eventsUrl;

        @SerializedName(value = "receivedEventsUrl", alternate = "received_events_url")
        private String receivedEventsUrl;

        private String type;

        @SerializedName(value = "userViewType", alternate = "user_view_type")
        private String userViewType;

        @SerializedName(value = "siteAdmin", alternate = "site_admin")
        private Boolean siteAdmin;
    }
}
