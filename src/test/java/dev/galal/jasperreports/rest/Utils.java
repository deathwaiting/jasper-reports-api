package dev.galal.jasperreports.rest;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

import static io.restassured.RestAssured.given;


@Slf4j
public class Utils {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";

    public static void saveReportToTmpFile(Response response, String ext) throws IOException {
        var fileContent = response.asByteArray();
        var tmpFile = Files.createTempFile("jasperreport-", "." + ext);
        Files.write(tmpFile, fileContent);
        log.info(">>>>>>>> Report downloaded to:  " + tmpFile);
    }


    public static boolean dataExists(Connection conn) throws SQLException {
        var stmt = conn.createStatement();
        var result = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'EMPLOYEE';\n");
        result.first();
        return result.getInt(1) > 0;
    }


    public static RequestSpecification givenAuthenticated() {
        return given().auth().basic(USERNAME, PASSWORD);
    }
}
