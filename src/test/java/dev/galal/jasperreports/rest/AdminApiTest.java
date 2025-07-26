package dev.galal.jasperreports.rest;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static dev.galal.jasperreports.rest.Utils.givenAuthenticated;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class AdminApiTest {

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

    @Test
    void getNonExistingReport() {
        var response =
                givenAuthenticated()
                        .when()
                        .get("/admin/server/info")
                        .andReturn();
        response.then()
                .statusCode(200)
                .and()
                .body(jsonEquals("""
                        {
                             "reportDirectory": "${json-unit.ignore}",
                             "reports": [
                                 "book/book.jrxml",
                                 "book/emp-report.jrxml",
                                 "emp-report.jrxml",
                                 "inner-dir/emp-report.jrxml",
                                 "sub-report/emp-report.jrxml",
                                 "sub-report/report.jrxml"
                             ]
                         }
                        """));
    }
}
