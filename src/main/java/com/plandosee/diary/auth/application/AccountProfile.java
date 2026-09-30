package com.plandosee.diary.auth.application;

/** What the account page shows: the login id (never changes) and the nickname. */
public record AccountProfile(String loginId, String nickname) {
}
