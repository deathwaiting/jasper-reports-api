package dev.galal.jasperreports.rest.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.ProblemDetail.forStatus;

public class AppError {

    public static final String REPORT_UNSUPPORTED = "error.report.unsupported-ext";
    public static final String REPORT_NOT_FOUND = "error.report.not-found";

    public static ErrorResponseException of(HttpStatus status, String errorMsgCode) {
        return  new ErrorResponseException(status, forStatus(status), new RuntimeException(), errorMsgCode, null);
    }

    public static void notAcceptable(String errorMsgCode) {
        trigger(NOT_ACCEPTABLE, errorMsgCode);
    }

    public static void trigger(HttpStatus status, String errorMsgCode) {
        throw of(status, errorMsgCode);
    }

}
