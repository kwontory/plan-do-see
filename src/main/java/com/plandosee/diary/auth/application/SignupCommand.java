package com.plandosee.diary.auth.application;

import com.plandosee.diary.auth.domain.AuthCodes;
import com.plandosee.diary.auth.domain.AuthRules;
import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.user.domain.UserRules;

/**
 * Sign-up input, self-validating (ADR-22, ADR-30): the login id is stored in lower case (AuthRules), the password is
 * kept exactly as typed, the nickname is normalized (UserRules.NICKNAME). An invalid command cannot be created; the
 * service turns every violation into the one sign-up code (ADR-37), so the field codes here never reach a screen.
 */
public record SignupCommand(String loginId, String password, String nickname) {

    public SignupCommand {
        InputCheck check = new InputCheck();
        String stored = AuthRules.loginId(loginId);
        check.rule(stored != null, "loginId", AuthCodes.SIGNUP_REJECTED);
        loginId = stored;
        String passwordCode = AuthRules.passwordCode(password);
        check.rule(passwordCode == null, "password", AuthCodes.SIGNUP_REJECTED);
        nickname = check.text("nickname", UserRules.NICKNAME, nickname);
        check.done();
    }

    @Override
    public String toString() {
        return "SignupCommand[loginId=" + loginId + ", nickname=" + nickname + "]";
    }
}
