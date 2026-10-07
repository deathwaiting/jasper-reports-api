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

import java.io.IOException;
import java.io.OutputStream;

import static dev.galal.jasperreports.rest.config.exception.AppError.REPORT_UNSUPPORTED;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;

public class ReportHandlers {

    @FunctionalInterface
    public interface ReportExporter {
        void export(JasperPrint print, OutputStream outputStream) throws JRException, IOException;
    }

    public record ReportHandler(String extension, String mediaType, ReportExporter exporter) {}

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


    private static void exportToXlsx(JasperPrint print, OutputStream outputStream) throws JRException {
        var configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(true);
        configuration.setIgnoreGraphics(false);

        var exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.setConfiguration(configuration);
        exporter.exportReport();
    }


    private static void exportToDocx(JasperPrint print, OutputStream outputStream) throws JRException {
        var exporter = new JRDocxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
    }

    private static void exportToCsv(JasperPrint print, OutputStream outputStream) throws JRException {
        var exporter = new JRCsvExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
        exporter.exportReport();
    }

    private static void exportTpPdf(JasperPrint print, OutputStream outputStream) throws JRException {
        JasperExportManager.exportReportToPdfStream(print, outputStream);
    }

    private static void exportToHtml(JasperPrint print, OutputStream outputStream) throws JRException {
        var exporter = new HtmlExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(outputStream));
        exporter.exportReport();
    }
}