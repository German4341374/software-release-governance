package com.portfolio.releasegovernance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywayCreatesSeedAndIdempotencyIndexes() {
        assertThat(jdbc.queryForObject("select count(*) from products", Long.class)).isGreaterThanOrEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from available_releases", Long.class)).isGreaterThanOrEqualTo(4L);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'uq_pending_approval'", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void auditLogIsAppendOnly() {
        assertThatThrownBy(() -> jdbc.update("delete from audit_events"))
                .hasMessageContaining("append-only");
    }
}
