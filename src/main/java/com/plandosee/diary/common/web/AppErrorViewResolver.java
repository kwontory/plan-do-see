package com.plandosee.diary.common.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webmvc.autoconfigure.error.DefaultErrorViewResolver;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

/**
 * Errors that reach the container error page (for example 413 when a form body is larger than
 * {@code server.tomcat.max-http-form-post-size}) always get an app error page. A template for the exact status
 * ({@code error/413}) or series ({@code error/4xx}) is used first, as Spring Boot does; otherwise any 4xx shows
 * {@code error/400} and any 5xx {@code error/500}, keeping the real status code. No framework fallback page.
 * Not created when the application runs without a web server (the data transfer command).
 */
@Component
@ConditionalOnWebApplication
public class AppErrorViewResolver implements ErrorViewResolver {

    private final DefaultErrorViewResolver conventions;

    public AppErrorViewResolver(ApplicationContext context, WebProperties webProperties) {
        this.conventions = new DefaultErrorViewResolver(context, webProperties.getResources());
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        ModelAndView view = conventions.resolveErrorView(request, status, model);
        if (view != null) {
            return view;
        }
        return new ModelAndView(status.is4xxClientError() ? "error/400" : "error/500", model, status);
    }
}
