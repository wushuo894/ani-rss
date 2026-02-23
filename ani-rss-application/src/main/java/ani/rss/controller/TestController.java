package ani.rss.controller;

import ani.rss.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Operation(summary = "测试权限")
    @PostMapping("/test")
    public Result<Void> test() {
        return Result.success();
    }
}
