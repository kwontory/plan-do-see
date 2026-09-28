package com.plandosee.diary.review.application;

import java.util.UUID;

/**
 * created is false when the review had already been transferred and the existing next plan was returned.
 */
public record TransferResult(UUID nextPlanId, boolean created) {
}
