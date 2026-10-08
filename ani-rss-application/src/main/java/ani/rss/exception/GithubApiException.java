package ani.rss.exception;

import lombok.Getter;

/**
 * GitHub 接口异常。
 */
@Getter
public class GithubApiException extends RuntimeException {
    private final int status;

    public GithubApiException(int status) {
        super(switch (status) {
            case 401 -> "GitHub Token 无效或已过期，请重新获取。";
            case 403 -> "GitHub 访问受限，请稍后重试。";
            default -> status >= 500 && status < 600
                    ? "GitHub 服务异常，请稍后重试。"
                    : "GitHub 请求失败，请检查网络或代理。";
        });
        this.status = status;
    }
}
