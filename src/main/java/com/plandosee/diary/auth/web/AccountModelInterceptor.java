package com.plandosee.diary.auth.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.plandosee.diary.auth.application.AccountService;
import com.plandosee.diary.auth.domain.AuthenticatedUser;
import com.plandosee.diary.auth.domain.DraftOwnerKey;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.web.PageAccountModel;

/**
 * Shell attributes of every rendered page of a logged-in person (absent when nobody is logged in):
 * {@value #DRAFT_OWNER_KEY} (String, DraftOwnerKey: 32 hex characters, no database read) for the browser draft keys
 * (ADR-38), and {@value #NICKNAME} (String) for the page header.
 * <ul>
 *   <li>Controller views: added after the handler (postHandle), never for redirects, so a form post costs no extra
 *       query.</li>
 *   <li>Error pages of GlobalExceptionHandler: added through {@link PageAccountModel}; container error pages
 *       (/error) are controller views.</li>
 *   <li>Pages answered with a 5xx status (busy form pages, busy and server error pages) get only the draft key: no
 *       database read while the database may be the problem. If the nickname read fails the page is still rendered,
 *       without it.</li>
 * </ul>
 */
@Component
public class AccountModelInterceptor implements HandlerInterceptor, PageAccountModel {

    public static final String NICKNAME = "accountNickname";
    /** Model attribute: the per-person part of the browser draft keys (DraftOwnerKey, ADR-38). */
    public static final String DRAFT_OWNER_KEY = "draftOwnerKey";

    private static final Logger log = LoggerFactory.getLogger(AccountModelInterceptor.class);

    private final AccountService accountService;

    public AccountModelInterceptor(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null || isRedirect(modelAndView)) {
            return;
        }
        addTo(modelAndView.getModel(), response.getStatus() < 500);
    }

    @Override
    public void addTo(Map<String, Object> model, boolean allowDatabaseRead) {
        AuthenticatedUser user = loggedIn();
        if (user == null) {
            return;
        }
        model.put(DRAFT_OWNER_KEY, DraftOwnerKey.of(user.userId()));
        if (!allowDatabaseRead) {
            return;
        }
        try {
            model.put(NICKNAME, accountService.nickname());
        } catch (DataAccessException | TransactionException | NotFoundException ex) {
            log.warn("event=account_nickname_unavailable type={}", ex.getClass().getName());
        }
    }

    private static AuthenticatedUser loggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }

    private static boolean isRedirect(ModelAndView modelAndView) {
        String name = modelAndView.getViewName();
        return name != null && name.startsWith("redirect:");
    }
}
