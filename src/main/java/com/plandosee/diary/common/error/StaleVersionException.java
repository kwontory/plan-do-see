package com.plandosee.diary.common.error;

import java.util.List;

/**
 * An edit form was saved from an out-of-date version, and the content it sends differs from what another
 * save stored meanwhile. Nothing was written (no row change, no history row). Carries the error code
 * ({@code <feature>.edit.staleVersion}), the latest stored version, and the names of the form fields whose submitted
 * value differs from the latest stored value (computed by the server). Feature subclasses add the latest
 * snapshot itself, read under the row lock in the same transaction.
 */
public abstract class StaleVersionException extends RuntimeException {

    private final String code;
    private final int latestVersion;
    private final List<String> changedFields;

    protected StaleVersionException(String code, int latestVersion, List<String> changedFields) {
        super(code, null, false, false);
        this.code = code;
        this.latestVersion = latestVersion;
        this.changedFields = List.copyOf(changedFields);
    }

    public String code() {
        return code;
    }

    public int latestVersion() {
        return latestVersion;
    }

    public List<String> changedFields() {
        return changedFields;
    }
}
