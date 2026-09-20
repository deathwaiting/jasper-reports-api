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
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Creates the DataSource used during stress tests: a Hikari pool backed by a real
 * PostgreSQL instance started with Testcontainers. Because each report fill acquires
 * exactly one connection (see {@code JdbcReportGeneratorService}), the pool must handle
 * the concurrent queries emitted by the Gatling simulation.
 *
 * <p>Raising {@code max_connections}: the pool can grow to the configured
 * {@code stress.db-pool-size} while the stock Postgres default is 100 connections, so
 * the container is started with a larger limit to avoid connection rejections under load.
 *
 * <p>The optional per-connection latency remains available ({@code stress.db.latency.ms})
 * to inflate latencies beyond what the real database already provides; it defaults to 0.
 * Real Postgres now supplies the round-trip latency that the former embedded H2 setup
 * faked with {@code Thread.sleep}.
 *
 * <p>The pool is built explicitly (rather than wrapping the auto-configured bean) to
 * avoid a circular reference: this class declares the only {@code DataSource} in the
 * context, so injecting the auto-configured instance back in would be unresolvable.
 *
 * <p>Only active under the {@code stress-test} Spring profile.
 */
@Configuration
@Profile("stress-test")
@Slf4j
public class ArtificialLatencyDataSource {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:latest")
        .withCommand("postgres", "-c", "max_connections=1000");

    @Bean
    @Primary
    public DataSource stressingDataSource(DataSourceProperties properties,
                                          @Value("${dev.galal.jasper-rest-server.stress.db-latency-ms:0}") long latencyMs,
                                          @Value("${dev.galal.jasper-rest-server.stress.db-pool-size:300}") int maxPoolSize,
                                          @Value("${dev.galal.jasper-rest-server.stress.db-pool-min-idle:30}") int minIdle) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        properties.setUrl(POSTGRES.getJdbcUrl());
        properties.setUsername(POSTGRES.getUsername());
        properties.setPassword(POSTGRES.getPassword());

        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
        dataSource.setMaximumPoolSize(maxPoolSize);
        dataSource.setMinimumIdle(minIdle);
        log.info("Configured Hikari pool with max size [{}] and min idle [{}]",
                maxPoolSize, minIdle);
        log.info("Stress DataSource points at PostgreSQL {} ({})",
                POSTGRES.getDockerImageName(), POSTGRES.getJdbcUrl());
        if (latencyMs <= 0) {
            log.info("stress.db-latency-ms is [{}], relying on the real database latency", latencyMs);
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