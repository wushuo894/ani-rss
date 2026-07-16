package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.web.ContentType;
import ani.rss.service.IcsService;
import cn.hutool.core.io.IoUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
public class IcsController extends BaseController {

    @Resource
    private IcsService icsService;

    @Auth
    @Operation(summary = "获取ICS日历")
    @GetMapping("/calendar.ics")
    public void getIcs(HttpServletResponse response) throws IOException {
        String icsContent = icsService.generateIcs();

        response.setContentType(ContentType.TEXT_CALENDAR);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=ani-rss-calendar.ics");
        setCacheControl(response, 3600);

        try (OutputStream outputStream = response.getOutputStream()) {
            IoUtil.writeUtf8(outputStream, true, icsContent);
        }

        log.debug("ICS日历已生成，包含{}字节", icsContent.length());
    }
}