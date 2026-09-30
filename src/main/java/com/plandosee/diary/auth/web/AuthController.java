package com.plandosee.diary.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.auth.application.AccountService;
import com.plandosee.diary.auth.application.SignupResult;
import com.plandosee.diary.auth.domain.AuthCodes;
import com.plandosee.diary.auth.domain.AuthenticatedUser;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;

/**
 * Login and sign-up pages (S06). POST /login and POST /logout are handled by Spring Security (SecurityConfig).
 * <ul>
 *   <li>GET /login: view {@value #LOGIN_VIEW} with {@code authRules} (maxlength of the boxes). Flash {@code flashKey} (login failed, logged out, password changed)
 *       and {@code loginId} (typed id to show again after a failed login) arrive as model attributes.</li>
 *   <li>GET /signup: view {@value #SIGNUP_VIEW} with {@code signupForm} and {@code authRules}.</li>
 *   <li>POST /signup: success → the new person is logged in (new session id, SessionLogin) and redirected to /,
 *       flash {@link AuthCodes#FLASH_SIGNED_UP}; failure → the same view (200) with only the global error
 *       {@link AuthCodes#SIGNUP_REJECTED}, login id and nickname kept, password cleared.</li>
 *   <li>A logged-in person opening /login or /signup is sent to /.</li>
 * </ul>
 */
@Controller
public class AuthController {

    static final String LOGIN_VIEW = "auth/login";
    static final String SIGNUP_VIEW = "auth/signup";

    private final AccountService accountService;
    private final SessionLogin sessionLogin;

    public AuthController(AccountService accountService, SessionLogin sessionLogin) {
        this.accountService = accountService;
        this.sessionLogin = sessionLogin;
    }

    @InitBinder("signupForm")
    void rawPassword(WebDataBinder binder) {
        RawPasswordEditor.register(binder, "password");
    }

    @GetMapping("/login")
    public String login(Authentication authentication, CsrfToken csrfToken, Model model) {
        if (loggedIn(authentication)) {
            return "redirect:/";
        }
        prepareCsrfToken(csrfToken);
        model.addAttribute("authRules", AuthInputRules.INSTANCE);
        return LOGIN_VIEW;
    }

    @GetMapping("/signup")
    public String signupForm(Authentication authentication, CsrfToken csrfToken, Model model) {
        if (loggedIn(authentication)) {
            return "redirect:/";
        }
        prepareCsrfToken(csrfToken);
        model.addAttribute("signupForm", new SignupForm());
        model.addAttribute("authRules", AuthInputRules.INSTANCE);
        return SIGNUP_VIEW;
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("signupForm") SignupForm form, BindingResult result, Model model,
                         HttpServletRequest request, HttpServletResponse response, CsrfToken csrfToken,
                         RedirectAttributes redirect) {
        SignupResult created;
        try {
            created = accountService.signup(form.getLoginId(), form.getPassword(), form.getNickname(),
                    request.getRemoteAddr());
        } catch (DomainRuleException ex) {
            form.setPassword(null);
            FormErrors.reject(result, ex);
            prepareCsrfToken(csrfToken);
            model.addAttribute("authRules", AuthInputRules.INSTANCE);
            return SIGNUP_VIEW;
        }
        sessionLogin.logIn(new AuthenticatedUser(created.userId()), request, response);
        FlashMessages.add(redirect, AuthCodes.FLASH_SIGNED_UP);
        return "redirect:/";
    }

    /**
     * Pages seen before login carry a form, so their CSRF token (kept in a new session) is created before rendering
     * starts: a session cannot be created once the page has begun to be sent.
     */
    private static void prepareCsrfToken(CsrfToken csrfToken) {
        if (csrfToken != null) {
            csrfToken.getToken();
        }
    }

    static boolean loggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
