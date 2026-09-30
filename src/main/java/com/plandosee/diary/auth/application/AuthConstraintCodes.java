package com.plandosee.diary.auth.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.plandosee.diary.auth.domain.AuthCodes;
import com.plandosee.diary.common.error.ConstraintCode;
import com.plandosee.diary.common.error.ConstraintCodeSource;

/**
 * auth tables (V6). A taken login id (uq_auth_identities_provider_user, also a racing duplicate sign-up) and every
 * other sign-up constraint mean the one sign-up code (ADR-37). The service checks the rules first, so apart from the
 * duplicate these are reached only if a rule and its CHECK drift apart.
 */
@Component
public class AuthConstraintCodes implements ConstraintCodeSource {

    @Override
    public List<ConstraintCode> constraintCodes() {
        return List.of(
                new ConstraintCode("uq_auth_identities_provider_user", null, AuthCodes.SIGNUP_REJECTED),
                new ConstraintCode("ux_auth_identities_user_local", null, AuthCodes.SIGNUP_REJECTED),
                new ConstraintCode("ck_auth_identities_local_login_id", null, AuthCodes.SIGNUP_REJECTED),
                new ConstraintCode("ck_auth_identities_provider", null, AuthCodes.SIGNUP_REJECTED),
                new ConstraintCode("ck_auth_identities_local_no_email", null, AuthCodes.SIGNUP_REJECTED),
                new ConstraintCode("ck_password_credentials_bcrypt", null, AuthCodes.SIGNUP_REJECTED));
    }
}
