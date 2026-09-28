package com.plandosee.diary.common.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Spring Boot's console pattern with {@link LogSecretMasker} applied to the whole rendered line: message, MDC,
 * exception messages and every "Caused by" in the stack trace. Wired in {@code logback-spring.xml}.
 */
public class MaskingPatternLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return LogSecretMasker.mask(super.doLayout(event));
    }
}
