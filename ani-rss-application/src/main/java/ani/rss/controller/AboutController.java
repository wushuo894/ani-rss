package ani.rss.controller;

import ani.rss.entity.About;
import ani.rss.entity.Result;
import ani.rss.util.other.UpdateUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AboutController {

    @Operation(summary = "查看关于信息")
    @PostMapping("/about")
    public Result<About> about() {
        return Result.success(UpdateUtil.about());
    }
}
