package dev.galal.jasperreports.rest.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ServerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class ServerInfoService {


    public record ServerInfo(String reportDirectory, List<String> reports){}

    @Value("${dev.galal.jasper-rest-server.reports-dir}")
    String reportsDir;

    public ServerInfo getServerInfo(){
        var reportsDirectory = Paths.get(reportsDir).toAbsolutePath();
        var reports = getReports(reportsDirectory);
        return new ServerInfo(reportsDirectory.toString(), reports);
    }

    private List<String> getReports(Path reportDir) {
        try(var walk = Files.walk(reportDir)) {
            return walk
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jrxml"))
                    .map(reportDir::relativize)
                    .sorted()
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
