package com.plandosee.diary.auth.web;

/** POST /account/delete: the password, bound exactly as typed and never shown again. */
public class DeleteAccountForm {

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "DeleteAccountForm";
    }
}
