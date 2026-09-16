package dev.galal.jasperreports.rest.config.stress;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Creates the DataSource used during stress tests: a Hikari pool backing a
 * {@link DelegatingDataSource} that adds an artificial delay on every
 * connection acquisition. Because each report fill acquires exactly one
 * connection (see {@code JdbcReportGeneratorService}), this mimics the
 * query round-trip latency of a real database when the embedded H2 responds
 * too quickly for a meaningful stress test.
 *
 * <p>The pool is built explicitly (rather than wrapping the auto-configured
 * bean) to avoid a circular reference: this class declares the only
 * {@code DataSource} in the context, so injecting the auto-configured
 * instance back in would be unresolvable.
 *
 * <p>Only active under the {@code stress-test} Spring profile.
 */
@Configuration
@Profile("stress-test")
@Slf4j
public class ArtificialLatencyDataSource {

    @Bean
    @Primary
    public DataSource stressingDataSource(DataSourceProperties properties,
                                          @Value("${dev.galal.jasper-rest-server.stress.db-latency-ms:0}") long latencyMs,
                                          @Value("${dev.galal.jasper-rest-server.stress.db-pool-size:200}") int maxPoolSize,
                                          @Value("${dev.galal.jasper-rest-server.stress.db-pool-min-idle:25}") int minIdle) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
        dataSource.setMaximumPoolSize(maxPoolSize);
        dataSource.setMinimumIdle(minIdle);
        log.info("Configured Hikari pool with max size [{}] and min idle [{}]", maxPoolSize, minIdle);
        if (latencyMs <= 0) {
            log.warn("stress.db-latency-ms is [{}], no artificial DB latency will be applied", latencyMs);
            return dataSource;
        }
        log.info("Applying artificial DB latency of [{}] ms per connection acquisition", latencyMs);
        return new DelegatingDataSource(dataSource) {
            @Override
            public Connection getConnection() throws SQLException {
                sleepQuietly(latencyMs);
                return super.getConnection();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                sleepQuietly(latencyMs);
                return super.getConnection(username, password);
            }
        };
    }

    private static void sleepQuietly(long latencyMs) {
        if (latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while applying artificial DB latency", e);
            }
        }
    }
}