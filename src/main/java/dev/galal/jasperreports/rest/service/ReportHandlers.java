package dev.galal.jasperreports.rest.service;

import dev.galal.jasperreports.rest.config.exception.AppError;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.ImmutableMap;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;

import static dev.galal.jasperreports.rest.config.exception.AppError.REPORT_UNSUPPORTED;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;

public class ReportHandlers {

    public record ReportHandler(String extension, String mediaType, Function<JasperPrint,byte[]> exporter) {}

    private static final ReportHandler pdfHandler = new ReportHandler("pdf", "application/pdf", ReportHandlers::exportTpPdf);
    private static final ReportHandler xlsxhandler =
            new ReportHandler("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ReportHandlers::exportToXlsx);

    private static final ReportHandler csvHandler = new ReportHandler("csv", "text/plain", ReportHandlers::exportToCsv);

    private static final ReportHandler docxHandler = new ReportHandler("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ReportHandlers::exportToDocx);

    private static final ReportHandler htmlHandler = new ReportHandler("html", "text/html", ReportHandlers::exportToHtml);
    private static final ImmutableMap<String,ReportHandler> MediaTypes =
            Lists.immutable.of(pdfHandler, xlsxhandler, csvHandler, docxHandler, htmlHandler)
                    .toImmutableMap(ReportHandler::extension, it -> it);

    public static ReportHandler getReportHandler(String ext) {
        return ofNullable(MediaTypes.get(ext))
                .orElseThrow(ReportHandlers::getUnsupportedExtension);
    }


    private static ErrorResponseException getUnsupportedExtension() {
        return AppError.of(NOT_ACCEPTABLE, REPORT_UNSUPPORTED);
    }


    private static byte[] exportToXlsx(JasperPrint print) {
        // Configure the XLSX exporter
        var configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(true);
        configuration.setIgnoreGraphics(false);

        var exporter = new JRXlsxExporter();
        try (var byteArrayOutputStream = new ByteArrayOutputStream()){
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException | JRException e) {
            throw new RuntimeException(e);
        }
    }


    private static byte[] exportToDocx(JasperPrint print) {
        var exporter = new JRDocxExporter();
        try (var byteArrayOutputStream = new ByteArrayOutputStream()){
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));
            exporter.exportReport();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException | JRException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] exportToCsv(JasperPrint print) {
        var exporter = new JRCsvExporter();
        try (var byteArrayOutputStream = new ByteArrayOutputStream()){
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleWriterExporterOutput(byteArrayOutputStream));
            exporter.exportReport();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException | JRException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] exportTpPdf(JasperPrint print) {
        try {
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] exportToHtml(JasperPrint print) {
        var exporter = new HtmlExporter();
        try (var byteArrayOutputStream = new ByteArrayOutputStream()){
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleHtmlExporterOutput(byteArrayOutputStream));
            exporter.exportReport();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException | JRException e) {
            throw new RuntimeException(e);
        }
    }
}
