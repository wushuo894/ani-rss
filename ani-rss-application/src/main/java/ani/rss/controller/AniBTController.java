package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.AniBT;
import ani.rss.entity.dto.AniBTQueryDTO;
import ani.rss.entity.web.Result;
import ani.rss.service.AniBTService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AniBTController {

    @Resource
    private AniBTService aniBTService;

    @Auth
    @Operation(summary = "AniBT 番剧列表")
    @PostMapping("/aniBT")
    public Result<AniBT> aniBT(@RequestBody AniBTQueryDTO dto) {
        return Result.success(aniBTService.list(dto));
    }

    @Auth
    @PostMapping("/aniBTGroup")
    public Result<List<AniBT.Group>> aniBTGroup(@RequestParam("bgmId") String bgmId) {
        List<AniBT.Group> groups = aniBTService.getGroups(bgmId);
        return Result.success(groups);
    }
}
