package com.plandosee.diary.auth.domain;

/** Outcome of a recorded attempt. Login entries are failures only; a successful login clears them. */
public enum LoginAttemptResult {
    SUCCESS,
    FAILURE
}
