package com.plandosee.diary.auth.web;

import com.plandosee.diary.auth.domain.AuthRules;
import com.plandosee.diary.user.domain.UserRules;

/**
 * Model attribute {@code authRules}: the input limits for the help text and the browser attributes of the sign-up and
 * account forms (the server checks the same constants).
 */
public record AuthInputRules(int loginIdMin, int loginIdMax, int passwordMin, int passwordMax, int passwordMaxBytes,
                             int nicknameMax) {

    static final AuthInputRules INSTANCE = new AuthInputRules(AuthRules.LOGIN_ID_MIN, AuthRules.LOGIN_ID_MAX,
            AuthRules.PASSWORD_MIN, AuthRules.PASSWORD_MAX, AuthRules.PASSWORD_MAX_UTF8_BYTES, UserRules.NICKNAME_MAX);
}
