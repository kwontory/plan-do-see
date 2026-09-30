package com.plandosee.diary.export.domain;

import java.util.UUID;

/**
 * The export's owner card: id, login id (null for the demo owner, which has no login) and nickname. Password hashes,
 * sessions, login attempts and email columns are never read.
 */
public class ExportOwnerRow {

    private UUID id;
    private String loginId;
    private String nickname;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
