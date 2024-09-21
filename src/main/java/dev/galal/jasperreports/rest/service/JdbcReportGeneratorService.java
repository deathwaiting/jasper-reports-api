package dev.galal.jasperreports.rest.service;

import dev.galal.jasperreports.rest.config.exception.AppError;
import dev.galal.jasperreports.rest.service.ReportHandlers.ReportHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.JRFiller;
import net.sf.jasperreports.engine.fill.JasperReportSource;
import net.sf.jasperreports.engine.fill.SimpleJasperReportSource;
import net.sf.jasperreports.repo.RepositoryResourceContext;
import net.sf.jasperreports.repo.SimpleRepositoryResourceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static dev.galal.jasperreports.rest.config.cache.CacheConfig.REPORTS_CACHE;
import static dev.galal.jasperreports.rest.config.exception.AppError.REPORT_NOT_FOUND;
import static dev.galal.jasperreports.rest.service.ReportHandlers.getReportHandler;
import static net.sf.jasperreports.engine.JRPropertiesUtil.PROPERTY_PREFIX;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class JdbcReportGeneratorService {

    public record Report(String fileName, String mediaType, byte[] content){}

    @Value("${dev.galal.jasper-rest-server.reports-dir}")
    String reportsDir;

    private final DataSource dataSource;
    private final CacheManager cacheManager;

    public Report generate(String requestedReport, Map<String, String> params) {
        log.debug("Using classloader : " + this.getClass().getClassLoader().getName());

        var ext = getFileExtension(requestedReport);
        var reportHandler = getReportHandler(ext);
        var mediaType = reportHandler.mediaType();
        var report = doGenerateReport(requestedReport, reportHandler, params);

        return new Report(requestedReport, mediaType, report);
    }

    private byte[] doGenerateReport(String requestedReport, ReportHandler handler, Map<String, String> params) {
        try {
            var jasperReport = compileOrGetFromCache(requestedReport);
            var jrxmlPath = getJrxmlFilePath(requestedReport);
            var jasperPrint = fillReport(jasperReport, jrxmlPath,  new HashMap<>(params), dataSource.getConnection());
            return handler.exporter().apply(jasperPrint);
        } catch (SQLException | JRException e) {
            log.error("Failed to generate report", e);
            throw new RuntimeException(e);
        }
    }

    private JasperReport compileOrGetFromCache(String requestedReport) {
        try {
            return cacheManager.getCache(REPORTS_CACHE)
                    .get(requestedReport, () -> compileJrxmlFile(requestedReport));
        } catch(Cache.ValueRetrievalException e) {
            if(e.getCause() instanceof RuntimeException) {
                throw (RuntimeException)e.getCause();
            } else {
                log.error("Failed to compile report", e);
                throw AppError.of(INTERNAL_SERVER_ERROR, "Failed to compile report");
            }
        }
    }


    private JasperReport compileJrxmlFile(String requestedReport) throws JRException {
        var jrxmlReport = getJrxmlFilePath(requestedReport);
        if(!Files.exists(jrxmlReport)) {
            AppError.notAcceptable(REPORT_NOT_FOUND);
        }
        return JasperCompileManager.compileReport(jrxmlReport.toAbsolutePath().toString());
    }

    private Path getJrxmlFilePath(String requestedReport) {
        var jrxmlFile = replaceExtension(requestedReport, "jrxml");
        return Path.of(reportsDir).resolve(jrxmlFile);
    }


    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return ((dotIndex > 0)? fileName.substring(dotIndex + 1) : "No extension").toLowerCase();
    }

    public static String replaceExtension(String fileName, String newExt) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0)? fileName.substring(0, dotIndex) + "." + newExt : "No extension";
    }

    private static JasperPrint fillReport(JasperReport jasperReport, Path jrxmlPath, Map<String, Object> params, Connection connection) throws JRException {
        //JRFiller is used directly instead of JasperFillManager because the latter doesn't have a method that
        // generates JasperPrint that can handle resources with relative paths
        return JRFiller.fill(DefaultJasperReportsContext.getInstance(),
                getReportSource(jrxmlPath.toAbsolutePath(), jasperReport),
                params, connection);
    }

    private static JasperReportSource getReportSource(Path reportFile, JasperReport jasperReport)
    {
        //attempting resolve absolute paths as relative, that's what SimpleFileResolver(".") did
        RepositoryResourceContext fallbackContext = SimpleRepositoryResourceContext.of(".");
        SimpleRepositoryResourceContext reportContext = SimpleRepositoryResourceContext.of(
                reportFile.getParent().toAbsolutePath().toString(), fallbackContext);

        return SimpleJasperReportSource.from(jasperReport, reportFile.toAbsolutePath().toString(), reportContext);
    }
}
