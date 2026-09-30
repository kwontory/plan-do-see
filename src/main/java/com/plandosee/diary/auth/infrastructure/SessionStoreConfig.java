package com.plandosee.diary.auth.infrastructure;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.SQLErrorCodesFactory;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.SpringSessionTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Session JDBC gets its own small connection pool on the same database (same URL, user, password, session time
 * limits). Every request of a logged-in person reads and writes its session; with one shared pool, a full application
 * pool would make that fail before the controller runs, so the "busy, try again" notices (form input kept, button
 * flash) could not be shown. Neither bean is a default candidate: the application DataSource and transaction manager
 * stay the ones Spring Boot creates, and only Spring Session (by its qualifiers) uses these.
 */
@Configuration
public class SessionStoreConfig {

    @Bean(defaultCandidate = false)
    @SpringSessionDataSource
    public HikariDataSource sessionDataSource(DataSourceProperties properties,
                                              @Value("${app.session-pool.size:2}") int poolSize,
                                              @Value("${spring.datasource.hikari.connection-timeout:500}") long waitMillis,
                                              @Value("${spring.datasource.hikari.minimum-idle:0}") int minimumIdle,
                                              @Value("${spring.datasource.hikari.idle-timeout:60000}") long idleMillis,
                                              @Value("${spring.datasource.hikari.max-lifetime:600000}") long lifetimeMillis,
                                              @Value("${spring.datasource.hikari.connection-init-sql:}") String initSql) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        dataSource.setPoolName("session-pool");
        int size = Math.max(1, poolSize);
        dataSource.setMaximumPoolSize(size);
        // Same connection budget rules as the application pool (ADR-39): idle connections are given back, the pool
        // never fails the start-up when the database is full or away (a request then gets the busy notice).
        dataSource.setMinimumIdle(Math.min(Math.max(0, minimumIdle), size));
        dataSource.setIdleTimeout(idleMillis);
        dataSource.setMaxLifetime(lifetimeMillis);
        dataSource.setInitializationFailTimeout(-1);
        dataSource.setConnectionTimeout(waitMillis);
        // Spring Session's JdbcTemplate would otherwise open a connection at start-up only to learn the database
        // product for its error codes; the database is known (ADR-39).
        SQLErrorCodesFactory.getInstance().registerDatabase(dataSource, "PostgreSQL");
        if (!initSql.isBlank()) {
            dataSource.setConnectionInitSql(initSql);
        }
        return dataSource;
    }

    /**
     * Given explicitly so Spring Session does not open a database connection during start-up only to ask whether the
     * database is Oracle (it is PostgreSQL: plain LOB handling). One remote connection less per container start, and
     * no start-up failure when the pooler is full (ADR-39).
     */
    @Bean(name = "springSessionLobHandler", defaultCandidate = false)
    @SuppressWarnings("deprecation")
    public org.springframework.jdbc.support.lob.LobHandler springSessionLobHandler() {
        return new org.springframework.jdbc.support.lob.DefaultLobHandler();
    }

    @Bean(defaultCandidate = false)
    @SpringSessionTransactionManager
    public PlatformTransactionManager sessionTransactionManager(@SpringSessionDataSource DataSource sessionDataSource) {
        return new DataSourceTransactionManager(sessionDataSource);
    }
}
