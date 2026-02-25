package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.auth.fun.IpWhitelist;
import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.entity.About;
import ani.rss.entity.Global;
import ani.rss.entity.Result;
import ani.rss.util.other.UpdateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@Slf4j
@RestController
public class AboutController extends BaseController {

    @Auth
    @Operation(summary = "查看关于信息")
    @PostMapping("/about")
    public Result<About> about() {
        return Result.success(UpdateUtil.about());
    }

    @Auth
    @Operation(summary = "停止服务")
    @PostMapping("/stop")
    public Result<Void> stop(@RequestParam("status") Integer status) {
        String s = List.of("重启", "关闭").get(status);
        log.info("正在{}", s);
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(3000);
            File jar = MavenUtils.getJar();
            String extName = FileUtil.extName(jar);
            if ("exe".equals(extName) && status == 0) {
                log.info("正在重启 {}", jar.getName());
                RuntimeUtil.exec(jar.getName());
                System.exit(status);
                return;
            }
            System.exit(status);
        });
        return Result.success("正在{}", s);
    }

    @Auth
    @Operation(summary = "更新")
    @PostMapping("/update")
    public Result<Void> update() {
        About about = UpdateUtil.about();
        try {
            UpdateUtil.update(about);
            return Result.success("更新成功, 正在重启...");
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.info("更新失败 {}, {}", about.getLatest(), message);
            return Result.success("更新失败 {}, {}", about.getLatest(), message);
        }
    }

    private final IpWhitelist ipWhitelist = new IpWhitelist();

    @Operation(summary = "IP白名单测试")
    @PostMapping("/testIpWhitelist")
    public Result<Void> testIpWhitelist() {
        HttpServletRequest request = Global.REQUEST.get();
        Boolean b = ipWhitelist.apply(request);
        if (b) {
            return Result.success();
        }
        return Result.error();
    }
}
