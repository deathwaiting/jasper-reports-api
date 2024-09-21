package dev.galal.jasperreports.rest;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.galal.jasperreports.rest.Utils.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("security-off")

public class SecurityOffTest {
    @Autowired
    DataSource dataSource;

    @LocalServerPort
    int port;


    @BeforeAll
    public static void setup() {
        RestAssured.config = RestAssured.config()
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails()
                        .enablePrettyPrinting(true));
    }

    @BeforeEach
    void init() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @BeforeEach
    void setUp() throws Exception {
        try (var conn = dataSource.getConnection()) {
            if(dataExists(conn)) {
                return;
            }
            var sqlFile = Path.of("src/test/resources/sql/sample-emp-data.sql");
            var sql = Files.readString(sqlFile);
            var stmt = conn.createStatement();
            stmt.execute(sql);
        }
    }


    @Test
    void getPdfReport() throws IOException {
        var response =
                given()
                    .queryParam("dep_id", "1")
                    .when()
                    .get("/report/inner-dir/emp-report.pdf")
                    .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("application/pdf"));

        saveReportToTmpFile(response, "pdf");
    }


    @Test
    void getBookPdfReport() throws IOException {
        var response =
                given()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/book/book.pdf?JR_force_compile=true")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("application/pdf"));

        saveReportToTmpFile(response, "pdf");
    }


    @Test
    void getSubReportPdfReport() throws IOException {
        var response =
                given()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/sub-report/report.pdf")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("application/pdf"));

        saveReportToTmpFile(response, "pdf");
    }
}
