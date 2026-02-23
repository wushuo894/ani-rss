package ani.rss.controller;

import ani.rss.auth.fun.IpWhitelist;
import ani.rss.entity.Global;
import ani.rss.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final IpWhitelist ipWhitelist = new IpWhitelist();

    @Operation(summary = "IP白名单测试")
    @PostMapping("/test")
    public Result<Void> test() {
        HttpServletRequest request = Global.REQUEST.get();
        Boolean b = ipWhitelist.apply(request);
        if (b) {
            return Result.success();
        }
        return Result.error();
    }
}
