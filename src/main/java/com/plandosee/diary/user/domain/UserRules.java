package com.plandosee.diary.user.domain;

import com.plandosee.diary.common.domain.TextInput.Lines;
import com.plandosee.diary.common.domain.TextRule;

/**
 * The one place for the user field rules. The nickname is the name shown on screen: one line, 1 to
 * {@value #NICKNAME_MAX} characters after trim, duplicates allowed. The sign-up command, the nickname form and the DB
 * CHECK ck_users_nickname (V6) use the same value.
 */
public final class UserRules {

    public static final int NICKNAME_MAX = 20;
    public static final String NICKNAME_REQUIRED = "validation.nickname.required";
    /** Argument {0}: the limit as a plain string. */
    public static final String NICKNAME_TOO_LONG = "validation.nickname.max";

    public static final TextRule NICKNAME = new TextRule(Lines.SINGLE, NICKNAME_MAX, NICKNAME_REQUIRED,
            NICKNAME_TOO_LONG, String.valueOf(NICKNAME_MAX));

    private UserRules() {
    }
}
