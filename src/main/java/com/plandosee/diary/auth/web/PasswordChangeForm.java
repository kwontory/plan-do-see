package com.plandosee.diary.auth.web;

/**
 * PUT /account/password. Both passwords are bound exactly as typed (never trimmed) and are cleared before the form is
 * shown again; AccountService checks them (AuthRules).
 */
public class PasswordChangeForm {

    private String currentPassword;
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    void clearPasswords() {
        currentPassword = null;
        newPassword = null;
    }

    @Override
    public String toString() {
        return "PasswordChangeForm";
    }
}
