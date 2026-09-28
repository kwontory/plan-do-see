package com.plandosee.diary.review.application.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.review.domain.EvidenceQuery;
import com.plandosee.diary.review.domain.EvidenceTodo;
import com.plandosee.diary.review.domain.ReviewCounts;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewScope;

@Mapper
public interface ReviewMapper {

    int insert(ReviewRow review);

    ReviewRow findActiveOwned(@Param("userId") UUID userId, @Param("reviewId") UUID reviewId);

    ReviewRow lockActiveOwned(@Param("userId") UUID userId, @Param("reviewId") UUID reviewId);

    List<ReviewRow> listForPlanOwned(@Param("userId") UUID userId, @Param("planId") UUID planId);

    ReviewRow findByNextPlanOwned(@Param("userId") UUID userId, @Param("nextPlanId") UUID nextPlanId);

    int updateImprovementOwned(@Param("userId") UUID userId, @Param("reviewId") UUID reviewId,
                               @Param("improvement") String improvement, @Param("now") OffsetDateTime now);

    int markTransferredOwned(@Param("userId") UUID userId, @Param("reviewId") UUID reviewId,
                             @Param("nextPlanId") UUID nextPlanId, @Param("now") OffsetDateTime now);

    ReviewCounts summary(ReviewScope scope);

    List<EvidenceTodo> evidenceTodos(EvidenceQuery query);

    List<ExecutionLogRow> evidenceLogs(EvidenceQuery query);
}
