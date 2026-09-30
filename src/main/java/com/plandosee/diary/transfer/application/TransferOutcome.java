package com.plandosee.diary.transfer.application;

/** Result of the one-time data transfer command. Only TRANSFERRED and NOTHING_TO_TRANSFER are successes. */
public enum TransferOutcome {
    TRANSFERRED,
    NOTHING_TO_TRANSFER,
    TARGET_NOT_FOUND,
    TARGET_HAS_DATA,
    TARGET_IS_DEMO_USER;

    public boolean success() {
        return this == TRANSFERRED || this == NOTHING_TO_TRANSFER;
    }
}
