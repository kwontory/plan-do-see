package com.plandosee.diary.auth.domain;

import java.util.UUID;

/**
 * The LOCAL login of an active person with its password hash. Read only for checking a password; never put in a
 * model, a log or the export.
 */
public class LocalCredentialRow {

    private UUID userId;
    private UUID identityId;
    private String loginId;
    private String passwordHash;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return "LocalCredentialRow[userId=" + userId + "]";
    }
}
