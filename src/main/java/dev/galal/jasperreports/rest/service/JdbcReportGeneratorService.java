package dev.galal.jasperreports.rest.service;

import dev.galal.jasperreports.rest.config.exception.AppError;
import dev.galal.jasperreports.rest.service.ReportHandlers.ReportHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static dev.galal.jasperreports.rest.config.exception.AppError.REPORT_NOT_FOUND;
import static dev.galal.jasperreports.rest.service.ReportHandlers.getReportHandler;

@Service
@RequiredArgsConstructor
@Slf4j
public class JdbcReportGeneratorService {

    public record Report(String fileName, String mediaType, byte[] content){}

    @Value("${dev.galal.jasper-rest-server.reports-dir}")
    String reportsDir;

    private final DataSource dataSource;

    public Report generate(String requestedReport, Map<String, String> params) {
        log.debug("Using classloader : " + this.getClass().getClassLoader().getName());
        var jrxmlFile = replaceExtension(requestedReport, "jrxml");
        var reportPath = Path.of(reportsDir).resolve(jrxmlFile);
        if(!Files.exists(reportPath)) {
            AppError.notAcceptable(REPORT_NOT_FOUND);
        }
        var ext = getFileExtension(requestedReport);
        var reportHandler = getReportHandler(ext);
        var mediaType = reportHandler.mediaType();
        var report = doGenerateReport(reportPath, reportHandler, params);

        return new Report(requestedReport, mediaType, report);
    }

    private byte[] doGenerateReport(Path filePath, ReportHandler handler, Map<String, String> params) {
        try {
            var jasperReport = JasperCompileManager.compileReport(filePath.toAbsolutePath().toString());
            var jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(params), dataSource.getConnection());
            return handler.exporter().apply(jasperPrint);
        } catch (SQLException | JRException e) {
            log.error("Failed to generate report", e);
            throw new RuntimeException(e);
        }
    }


    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return ((dotIndex > 0)? fileName.substring(dotIndex + 1) : "No extension").toLowerCase();
    }

    public static String replaceExtension(String fileName, String newExt) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0)? fileName.substring(0, dotIndex) + "." + newExt : "No extension";
    }
}
