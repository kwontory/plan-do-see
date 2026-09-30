package com.plandosee.diary.auth.web;

/**
 * POST /signup. No field constraints on purpose: every sign-up failure is one global code without field errors
 * (ADR-37 amendment 1); AccountService checks everything. The password is bound exactly as typed and is cleared
 * before the form is shown again.
 */
public class SignupForm {

    private String loginId;
    private String password;
    private String nickname;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "SignupForm";
    }
}
