package com.plandosee.diary.auth.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebConfig implements WebMvcConfigurer {

    private final AccountModelInterceptor accountModel;

    public AuthWebConfig(AccountModelInterceptor accountModel) {
        this.accountModel = accountModel;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accountModel);
    }
}
