package com.plandosee.diary.common.error;

/**
 * Thrown when an owned record does not exist, is soft-deleted, or belongs to another user.
 * The three cases are intentionally indistinguishable to the client.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource) {
        super(resource + " not found");
    }
}
