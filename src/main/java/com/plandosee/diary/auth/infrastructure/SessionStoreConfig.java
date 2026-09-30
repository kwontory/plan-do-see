package com.plandosee.diary.auth.infrastructure;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
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
                                              @Value("${spring.datasource.hikari.connection-init-sql:}") String initSql) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        dataSource.setPoolName("session-pool");
        dataSource.setMaximumPoolSize(Math.max(1, poolSize));
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(waitMillis);
        if (!initSql.isBlank()) {
            dataSource.setConnectionInitSql(initSql);
        }
        return dataSource;
    }

    @Bean(defaultCandidate = false)
    @SpringSessionTransactionManager
    public PlatformTransactionManager sessionTransactionManager(@SpringSessionDataSource DataSource sessionDataSource) {
        return new DataSourceTransactionManager(sessionDataSource);
    }
}
