package com.plandosee.diary.user.domain;

import java.util.UUID;

/**
 * Only the fields needed for display and export. Email columns are never read in T06.
 */
public class UserRow {

    private UUID id;
    private String nickname;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
