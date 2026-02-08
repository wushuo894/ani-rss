package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
public class OpenListFileInfo implements Serializable {
    private String name;
    private Long size;

    @SerializedName(value = "isDir",alternate = "is_dir")
    private Boolean isDir;
    private Date modified;
    private Date created;
    private String path;
}
