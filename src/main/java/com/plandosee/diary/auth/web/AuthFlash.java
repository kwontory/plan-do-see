package com.plandosee.diary.auth.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import com.plandosee.diary.auth.domain.AuthCodes;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.web.FlashMessages;

/**
 * Redirects of the Spring Security filters (outside Spring MVC) with the same flash keys as the controllers
 * (FlashMessages): a failed login and a logout both go to GET /login.
 * <ul>
 *   <li>Login failure: {@code flashKey} = {@link AuthCodes#LOGIN_FAILED} for every reason, the same 302 and
 *       Location; {@code loginId} = the typed login id (trimmed, at most {@value #LOGIN_ID_ECHO_MAX} characters) so the
 *       form can show it again. The password is never kept.</li>
 *   <li>Logout: {@code flashKey} = {@link AuthCodes#FLASH_LOGGED_OUT}.</li>
 * </ul>
 */
@Component
public class AuthFlash implements AuthenticationFailureHandler, LogoutSuccessHandler {

    public static final String LOGIN_PATH = "/login";
    /** Flash attribute: the typed login id after a failed login. */
    public static final String LOGIN_ID = "loginId";
    static final int LOGIN_ID_ECHO_MAX = 100;

    /** Same kind of manager Spring MVC uses (flash maps in the HTTP session, here the Spring Session). */
    private final FlashMapManager flashMapManager = new SessionFlashMapManager();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        FlashMap flash = new FlashMap();
        flash.put(FlashMessages.KEY, AuthCodes.LOGIN_FAILED);
        String typed = TextInput.normalize(request.getParameter(LOGIN_ID));
        if (typed != null) {
            flash.put(LOGIN_ID, TextInput.truncate(typed, LOGIN_ID_ECHO_MAX));
        }
        redirectToLogin(flash, request, response);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        redirectToLogin(AuthCodes.FLASH_LOGGED_OUT, request, response);
    }

    /** 302 to /login with the flash key. */
    public void redirectToLogin(String flashKey, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        FlashMap flash = new FlashMap();
        flash.put(FlashMessages.KEY, flashKey);
        redirectToLogin(flash, request, response);
    }

    private void redirectToLogin(FlashMap flash, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String target = request.getContextPath() + LOGIN_PATH;
        flash.setTargetRequestPath(target);
        flashMapManager.saveOutputFlashMap(flash, request, response);
        response.sendRedirect(response.encodeRedirectURL(target));
    }
}
