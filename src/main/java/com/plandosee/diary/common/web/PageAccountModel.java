package com.plandosee.diary.common.web;

import java.util.Map;

/**
 * Page-shell attributes of the logged-in person (the auth feature implements it), so that every rendered page,
 * error pages from GlobalExceptionHandler included, shows the shell in the right logged-in state without common code
 * depending on auth. Adds nothing when nobody is logged in.
 */
public interface PageAccountModel {

    /**
     * Puts the attributes into the model. {@code allowDatabaseRead} false for pages answered while the database is
     * busy (5xx): only attributes computed without a query are added.
     */
    void addTo(Map<String, Object> model, boolean allowDatabaseRead);
}
