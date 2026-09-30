package com.plandosee.diary.auth.web;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.plandosee.diary.auth.application.LoginService;
import com.plandosee.diary.auth.domain.AuthenticatedUser;

/**
 * Form login (POST /login) through LoginService. The client address is the container's remote address
 * (WebAuthenticationDetails = request.getRemoteAddr(), set by Tomcat's RemoteIpValve behind a trusted proxy); no
 * forwarded header is read here. Every failure is the same BadCredentialsException. The result holds only the
 * person's id (AuthenticatedUser) and no credentials.
 */
@Component
public class LocalLoginAuthenticationProvider implements AuthenticationProvider {

    private final LoginService loginService;

    public LocalLoginAuthenticationProvider(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String loginId = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials instanceof String text ? text : null;
        String remoteAddr = authentication.getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress() : null;
        AuthenticatedUser user = loginService.authenticate(loginId, password, remoteAddr)
                .orElseThrow(() -> new BadCredentialsException("login failed"));
        return UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
