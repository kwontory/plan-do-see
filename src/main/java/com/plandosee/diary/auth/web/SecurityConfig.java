package com.plandosee.diary.auth.web;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Who may open what (ADR-33, ADR-34).
 * <ul>
 *   <li>Public: GET/POST /login and /signup, static files (/css, /js, /fonts, /favicon.ico), error dispatches and
 *       GET /session/keepalive (it answers 401 itself when nobody is logged in). Everything else, / included, needs
 *       login; without it a request is redirected (302) to /login. Another person's data stays 404 through the owner
 *       condition of every query.</li>
 *   <li>Form login POST /login (loginId, password) through LocalLoginAuthenticationProvider only (its own
 *       ProviderManager without a parent, so a failed try is checked and recorded once); success goes to / or back to
 *       the form page kept after an ended session (ReturnAfterLogin), failure to /login with one flash code
 *       (AuthFlash). No request is remembered for after login (no session for
 *       anonymous requests). A successful sign-up logs in the same way (SessionLogin).</li>
 *   <li>POST /logout deletes the server session (Spring Session JDBC) and goes to /login.</li>
 *   <li>CSRF: Spring Security token stored in the session, required on every POST/PUT/PATCH/DELETE (hidden {@code _csrf}
 *       field, added to th:action forms automatically); SameOriginWriteFilter still checks the Origin first. A write
 *       without any session (it ended) goes to /login instead of 403 (SessionEndedHandler, ADR-38).
 *       HiddenHttpMethodFilter runs before this chain, so a PUT or DELETE form is checked the same way.</li>
 *   <li>The session id changes at login (session fixation). Response headers come from SecurityHeadersFilter
 *       (ADR-26, ADR-32), so Spring Security writes none of its own.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String[] PUBLIC_PATHS = {"/login", "/signup", "/css/**", "/js/**", "/fonts/**", "/favicon.ico",
            "/error", KeepAliveController.PATH};

    /** Where the logged-in security context is kept: the server session (shared with SessionLogin). */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    /** CSRF tokens in the server session (shared with SessionLogin, which renews the token at sign-up login). */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SecurityFilterChain appSecurity(HttpSecurity http, LocalLoginAuthenticationProvider loginProvider,
                                           AuthFlash authFlash, ReturnAfterLogin returnAfterLogin,
                                           SessionEndedHandler sessionEnded, SecurityContextRepository contexts,
                                           CsrfTokenRepository csrfTokens) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .authenticationManager(new ProviderManager(loginProvider))
                .securityContext(context -> context.securityContextRepository(contexts))
                .formLogin(form -> form
                        .loginPage(AuthFlash.LOGIN_PATH)
                        .loginProcessingUrl(AuthFlash.LOGIN_PATH)
                        .usernameParameter(AuthFlash.LOGIN_ID)
                        .passwordParameter("password")
                        .successHandler(returnAfterLogin)
                        .failureHandler(authFlash))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(authFlash))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .exceptionHandling(errors -> errors.accessDeniedHandler(sessionEnded))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .headers(headers -> headers.disable());
        return http.build();
    }
}
