package dev.galal.jasperreports.rest.config.stress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Populates the embedded database with a relatively large dataset so the stress
 * test exercises the report queries against a realistic row count instead of the
 * tiny seed data used by the integration tests.
 *
 * Only active under the {@code stress-test} Spring profile.
 */
@Component
@Profile("stress-test")
@Slf4j
@RequiredArgsConstructor
public class LargeDatasetGenerator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${dev.galal.jasper-rest-server.stress.dataset-size:50000}")
    private int datasetSize;

    @Override
    public void run(ApplicationArguments args) {
        var existingRows = countEmployees();
        if (existingRows >= datasetSize) {
            log.info("Employee table already has [{}] rows, no extra stress data generated", existingRows);
            return;
        }

        var generated = datasetSize - existingRows;
        insertBulk(generated);
        log.info("Generated [{}] extra employee rows, table size is now [{}]", generated, datasetSize);
    }

    private int countEmployees() {
        try {
            var count = jdbcTemplate.queryForObject("select count(*) from employee", Integer.class);
            return (count == null) ? 0 : count;
        } catch (Exception e) {
            log.warn("Failed to count employees, assuming empty table", e);
            return 0;
        }
    }

    private static final int BATCH_SIZE = 1000;

    private void insertBulk(int rows) {
        // Portable JDBC batch insert; avoids H2-specific functions (e.g. SYSTEM_RANGE),
        // so the stress database can be any embedded engine.
        var sql = """
                insert into employee (first_name, last_name, email, hire_date, job_title, salary, department_id)
                values (?, ?, ?, ?, ?, ?, ?)
                """;
        var start = datasetSize - rows;
        try (var connection = jdbcTemplate.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < rows; i++) {
                    int x = start + i;
                    statement.setString(1, "first" + x);
                    statement.setString(2, "last" + x);
                    statement.setString(3, "stress.user" + x + "@stress.test");
                    statement.setDate(4, java.sql.Date.valueOf(LocalDate.of(2000, 1, 1).plusDays(x % 10000)));
                    statement.setString(5, "Engineer " + (x % 20));
                    statement.setBigDecimal(6, BigDecimal.valueOf(50000 + (x % 100000)));
                    statement.setInt(7, x % 20 + 1);
                    statement.addBatch();
                    if ((i + 1) % BATCH_SIZE == 0) {
                        statement.executeBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new IllegalStateException("Failed to generate stress dataset", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to generate stress dataset", e);
        }
    }

    private static void rollbackQuietly(java.sql.Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            log.error("Rollback failed while generating stress dataset", rollbackError);
        }
    }
}