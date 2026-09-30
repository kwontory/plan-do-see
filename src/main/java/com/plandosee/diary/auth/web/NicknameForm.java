package com.plandosee.diary.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.plandosee.diary.common.domain.TextInput.Lines;
import com.plandosee.diary.common.web.PlainText;
import com.plandosee.diary.user.domain.UserRules;

/** PUT /account/nickname. Same rule as UserRules.NICKNAME (UserService checks it again). */
public class NicknameForm {

    @NotBlank(message = "{" + UserRules.NICKNAME_REQUIRED + "}")
    @Size(max = UserRules.NICKNAME_MAX, message = "{" + UserRules.NICKNAME_TOO_LONG + "}")
    @PlainText(Lines.SINGLE)
    private String nickname;

    public NicknameForm() {
    }

    public NicknameForm(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
