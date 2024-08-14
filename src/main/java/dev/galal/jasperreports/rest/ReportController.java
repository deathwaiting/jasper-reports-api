package dev.galal.jasperreports.rest;

import dev.galal.jasperreports.rest.service.JdbcReportGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequestMapping(ReportController.REPORTS_URL)
@RequiredArgsConstructor
public class ReportController {
    public static final String REPORTS_URL = "/report";
    private final JdbcReportGeneratorService jdbcReportGen;

    @GetMapping(value = "/**", produces = APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getReport(HttpServletRequest request , @RequestParam Map<String, String> params) {
        var reportPath = URI.create(REPORTS_URL).relativize(URI.create(request.getServletPath()));
        var report = jdbcReportGen.generate(reportPath.toString(), params);

        var headers = new HttpHeaders();
        headers.set("Content-Type", report.mediaType());
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(report.fileName()) // Adjust the filename extension as needed
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(report.content());
    }
}
