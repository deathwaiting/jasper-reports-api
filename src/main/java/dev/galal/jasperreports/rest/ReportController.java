package dev.galal.jasperreports.rest;

import dev.galal.jasperreports.rest.service.JdbcReportGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final JdbcReportGeneratorService jdbcReportGen;

    @GetMapping(value = "/report/{report_file}", produces = APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getReport(@PathVariable("report_file") String reportFile,
                                            @RequestParam Map<String, String> params) {
        var report = jdbcReportGen.generate(reportFile, params);

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
