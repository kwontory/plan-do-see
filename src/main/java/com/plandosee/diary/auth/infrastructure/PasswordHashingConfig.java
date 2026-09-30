package com.plandosee.diary.auth.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.plandosee.diary.auth.domain.AuthRules;

/** bcrypt from Spring Security (ADR-34): a new random salt per hash, cost AuthRules.BCRYPT_STRENGTH. */
@Configuration
public class PasswordHashingConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(AuthRules.BCRYPT_STRENGTH);
    }
}
