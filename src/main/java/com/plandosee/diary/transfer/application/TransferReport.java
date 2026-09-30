package com.plandosee.diary.transfer.application;

import com.plandosee.diary.transfer.domain.OwnedCounts;

/** Outcome and the per-table counts of both owners before and after (null where not read). */
public record TransferReport(TransferOutcome outcome, OwnedCounts demoBefore, OwnedCounts targetBefore,
                             OwnedCounts demoAfter, OwnedCounts targetAfter) {

    static TransferReport refused(TransferOutcome outcome) {
        return new TransferReport(outcome, null, null, null, null);
    }
}
