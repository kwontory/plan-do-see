package com.plandosee.diary.common.logging;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

/**
 * Registers the configured datasource and Flyway connection values with {@link LogSecretMasker} as soon as the
 * environment is known (before any bean connects), and again once the context is prepared so that properties added
 * by context initializers are covered too. Registered in {@code META-INF/spring.factories} because these events
 * fire before beans exist.
 */
public class DatasourceLogSecretsListener implements GenericApplicationListener {

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> type = eventType.getRawClass();
        return type != null && (ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(type)
                || ApplicationPreparedEvent.class.isAssignableFrom(type));
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent prepared) {
            register(prepared.getEnvironment());
        } else if (event instanceof ApplicationPreparedEvent prepared) {
            register(prepared.getApplicationContext().getEnvironment());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static void register(Environment env) {
        LogSecretMasker.registerDatasource(read(env, "spring.datasource.url"), read(env, "spring.datasource.username"),
                read(env, "spring.datasource.password"));
        LogSecretMasker.registerDatasource(read(env, "spring.flyway.url"), read(env, "spring.flyway.user"),
                read(env, "spring.flyway.password"));
    }

    private static String read(Environment env, String key) {
        try {
            return env.getProperty(key);
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            // e.g. DB_URL not set yet: nothing to mask, and the start-up failure is reported elsewhere.
            return null;
        }
    }
}
