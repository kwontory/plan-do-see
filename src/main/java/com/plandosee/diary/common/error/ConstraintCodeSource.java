package com.plandosee.diary.common.error;

import java.util.List;

/**
 * ADR-22: each feature lists the DB constraints of its own tables and the error code each one means, so the common
 * translator never depends on feature packages. Implementations are Spring beans.
 */
public interface ConstraintCodeSource {

    List<ConstraintCode> constraintCodes();
}
