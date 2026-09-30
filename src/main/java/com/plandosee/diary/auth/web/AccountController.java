package com.plandosee.diary.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.auth.application.AccountProfile;
import com.plandosee.diary.auth.application.AccountService;
import com.plandosee.diary.auth.domain.AuthCodes;
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;

/**
 * Account settings (S07): view {@value #VIEW} with {@code account} (AccountProfile: loginId, nickname),
 * {@code nicknameForm}, {@code passwordForm} and {@code authRules}.
 * <ul>
 *   <li>PUT /account/nickname: success → redirect /account, flash {@link AuthCodes#FLASH_NICKNAME_CHANGED}, or
 *       {@link AuthCodes#FLASH_NICKNAME_UNCHANGED} when the value equals the stored one (nothing written); a rule
 *       violation → the same view (200) with the field error on {@code nickname}.</li>
 *   <li>PUT /account/password (currentPassword, newPassword): success → every session of the person is deleted, this
 *       request is logged out, redirect /login with flash {@link AuthCodes#FLASH_PASSWORD_CHANGED}; a wrong current
 *       password ({@link AuthCodes#CURRENT_PASSWORD_MISMATCH} on {@code currentPassword}) or a new password outside
 *       the rules (on {@code newPassword}) → the same view (200), both password fields cleared.</li>
 *   <li>POST /account/delete (password; an action route like the login and logout posts): success → the account and all its data are deleted (ADR-35), every session
 *       ends, redirect /login with flash {@link AuthCodes#FLASH_ACCOUNT_DELETED}; a wrong password → the same view
 *       (200) with {@code deleteForm} and the field error {@link AuthCodes#DELETE_PASSWORD_MISMATCH} on
 *       {@code password} (cleared), nothing deleted.</li>
 * </ul>
 */
@Controller
public class AccountController {

    static final String VIEW = "account/settings";

    private final AccountService accountService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @InitBinder("passwordForm")
    void rawPasswords(WebDataBinder binder) {
        RawPasswordEditor.register(binder, "currentPassword", "newPassword");
    }

    @InitBinder("deleteForm")
    void rawDeletePassword(WebDataBinder binder) {
        RawPasswordEditor.register(binder, "password");
    }

    @GetMapping("/account")
    public String settings(Model model) {
        AccountProfile profile = accountService.profile();
        model.addAttribute("nicknameForm", new NicknameForm(profile.nickname()));
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return view(model, profile);
    }

    @PutMapping("/account/nickname")
    public String changeNickname(@Valid @ModelAttribute("nicknameForm") NicknameForm form, BindingResult result,
                                 Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                EditOutcome outcome = accountService.changeNickname(form.getNickname());
                FlashMessages.add(redirect, outcome == EditOutcome.UNCHANGED
                        ? AuthCodes.FLASH_NICKNAME_UNCHANGED : AuthCodes.FLASH_NICKNAME_CHANGED);
                return "redirect:/account";
            } catch (DomainRuleException ex) {
                FormErrors.reject(result, ex);
            }
        }
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return view(model, accountService.profile());
    }

    @PutMapping("/account/password")
    public String changePassword(@ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult result,
                                 Model model, HttpServletRequest request, HttpServletResponse response,
                                 Authentication authentication, RedirectAttributes redirect) {
        try {
            accountService.changePassword(form.getCurrentPassword(), form.getNewPassword(), request.getRemoteAddr());
        } catch (DomainRuleException ex) {
            form.clearPasswords();
            FormErrors.reject(result, ex);
            AccountProfile profile = accountService.profile();
            model.addAttribute("nicknameForm", new NicknameForm(profile.nickname()));
            return view(model, profile);
        }
        logoutHandler.logout(request, response, authentication);
        FlashMessages.add(redirect, AuthCodes.FLASH_PASSWORD_CHANGED);
        return "redirect:/login";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(@ModelAttribute("deleteForm") DeleteAccountForm form, BindingResult result, Model model,
                                HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication, RedirectAttributes redirect) {
        try {
            accountService.deleteAccount(form.getPassword(), request.getRemoteAddr());
        } catch (DomainRuleException ex) {
            form.setPassword(null);
            FormErrors.reject(result, ex);
            AccountProfile profile = accountService.profile();
            model.addAttribute("nicknameForm", new NicknameForm(profile.nickname()));
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return view(model, profile);
        }
        logoutHandler.logout(request, response, authentication);
        FlashMessages.add(redirect, AuthCodes.FLASH_ACCOUNT_DELETED);
        return "redirect:/login";
    }

    private static String view(Model model, AccountProfile profile) {
        model.addAttribute("account", profile);
        model.addAttribute("authRules", AuthInputRules.INSTANCE);
        return VIEW;
    }
}
