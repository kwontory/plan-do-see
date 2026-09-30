package com.plandosee.diary.auth.domain;

/**
 * Result and error codes of sign-up, login, logout and account settings (ADR-13: codes only, the copy is written in
 * messages.properties by Frontend). Login and sign-up failures never say why (ADR-37): one code each.
 */
public final class AuthCodes {

    /** Login failed for any reason (unknown id, wrong password, blocked, deleted account). Flash on /login. */
    public static final String LOGIN_FAILED = "auth.login.failed";
    /** Sign-up failed for any reason (id taken, blocked, any input rule). Global error of the sign-up form. */
    public static final String SIGNUP_REJECTED = "auth.signup.rejected";
    /** Account created and logged in. Flash on / (the first page). */
    public static final String FLASH_SIGNED_UP = "flash.auth.signedUp";
    /** Logged out. Flash on /login. */
    public static final String FLASH_LOGGED_OUT = "flash.auth.loggedOut";
    /** Nickname saved. Flash on /account. */
    public static final String FLASH_NICKNAME_CHANGED = "flash.account.nicknameChanged";
    /** Password changed; every session of the account ended, log in again. Flash on /login. */
    public static final String FLASH_PASSWORD_CHANGED = "flash.account.passwordChanged";
    /** The nickname saved equals the stored one; nothing changed. Flash on /account. */
    public static final String FLASH_NICKNAME_UNCHANGED = "flash.account.nicknameUnchanged";
    /** Password change: the current password is wrong (or tries are blocked). Field currentPassword. */
    public static final String CURRENT_PASSWORD_MISMATCH = "account.password.currentMismatch";
    /** Account deletion: the password is wrong (or tries are blocked); nothing was deleted. Field password. */
    public static final String DELETE_PASSWORD_MISMATCH = "account.delete.passwordMismatch";
    /** Account and all its data deleted, every session ended. Flash on /login. */
    public static final String FLASH_ACCOUNT_DELETED = "flash.auth.accountDeleted";
    /** Password shorter or longer than allowed. Arguments {0} min, {1} max. */
    public static final String PASSWORD_LENGTH = "validation.password.length";
    /** Password over the UTF-8 byte limit (bcrypt). Argument {0} the byte limit. */
    public static final String PASSWORD_BYTES = "validation.password.bytes";

    private AuthCodes() {
    }
}
