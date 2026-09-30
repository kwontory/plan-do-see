package com.plandosee.diary.user.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.plandosee.diary.common.error.ConstraintCode;
import com.plandosee.diary.common.error.ConstraintCodeSource;
import com.plandosee.diary.user.domain.UserRules;

/**
 * users constraints (V6) and the code each means. UserService checks the nickname first (UserRules), so this is
 * reached only if the rule and the CHECK drift apart; a blank nickname is caught before the DB.
 */
@Component
public class UserConstraintCodes implements ConstraintCodeSource {

    @Override
    public List<ConstraintCode> constraintCodes() {
        return List.of(new ConstraintCode("ck_users_nickname", "nickname", UserRules.NICKNAME_TOO_LONG,
                String.valueOf(UserRules.NICKNAME_MAX)));
    }
}
