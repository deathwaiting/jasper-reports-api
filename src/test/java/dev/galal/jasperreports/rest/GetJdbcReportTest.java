package dev.galal.jasperreports.rest;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Slf4j
public class GetJdbcReportTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";

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
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                            .get("/report/emp-report.pdf")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("application/pdf"));

        saveReportToTmpFile(response, "pdf");
    }


    @Test
    void getXlsxReport() throws IOException {
        var response =
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/emp-report.xlsx")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        saveReportToTmpFile(response, "xlsx");
    }

    @Test
    void getHtmlReport() throws IOException {
        var response =
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/emp-report.html")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("text/html"));

        saveReportToTmpFile(response, "html");
    }


    @Test
    void getCsvReport() throws IOException {
        var response =
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/emp-report.csv")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("text/plain"));

        saveReportToTmpFile(response, "csv");
    }


    @Test
    void getDocxReport() throws IOException {
        var response =
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/emp-report.docx")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .contentType(equalTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        saveReportToTmpFile(response, "docx");
    }


    @Test
    void getNonExistingReport() {
        var response =
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/NOT_THERE.pdf")
                        .andReturn();
        response.then()
                .statusCode(406)
                .and()
                .body(jsonEquals("""
                        {
                            "type": "about:blank",
                            "title": "Not Acceptable",
                            "status": 406,
                            "detail": "Report Not Found",
                            "instance": "/report/NOT_THERE.pdf"
                        }
                        """));
    }


    @Test
    void getNonSupportedExtension() {
        var response =
                givenAuthenticated()
                        .queryParam("dep_id", "1")
                        .when()
                        .get("/report/emp-report.mp3")
                        .andReturn();
        response.then()
                .statusCode(406)
                .and()
                .body(jsonEquals("""
                        {
                            "type": "about:blank",
                            "title": "Not Acceptable",
                            "status": 406,
                            "detail": "Unsupported Extension",
                            "instance": "/report/NOT_THERE.pdf"
                        }
                        """));
    }

    private static void saveReportToTmpFile(Response response, String ext) throws IOException {
        var fileContent = response.asByteArray();
        var tmpFile = Files.createTempFile("jasperreport-", "." + ext);
        Files.write(tmpFile, fileContent);
        log.info(">>>>>>>> Report downloaded to:  " + tmpFile);
    }


    private boolean dataExists(Connection conn) throws SQLException {
        var stmt = conn.createStatement();
        var result = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'EMPLOYEE';\n");
        result.first();
        return result.getInt(1) > 0;
    }


    private static RequestSpecification givenAuthenticated() {
        return given().auth().basic(USERNAME, PASSWORD);
    }

}
