package dev.galal.jasperreports.rest;

import dev.galal.jasperreports.rest.service.ServerInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
@ConditionalOnProperty(name = "dev.galal.jasper-rest-server.admin.api.enabled", havingValue = "true")
public class AdminController {

    private final ServerInfoService service;

    @GetMapping("/server/info")
    public ServerInfoService.ServerInfo getInfo() {
        return service.getServerInfo();
    }
}
