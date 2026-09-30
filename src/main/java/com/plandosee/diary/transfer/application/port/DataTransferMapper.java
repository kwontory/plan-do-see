package com.plandosee.diary.transfer.application.port;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.transfer.domain.OwnedCounts;

/**
 * The one-time T06 data transfer (ADR-35). Like export, this port reads across features on purpose (counts of every
 * owned table) and changes only the three owner columns (plans, tags, reviews), all in one transaction of an
 * operator command that never runs in a web request.
 */
@Mapper
public interface DataTransferMapper {

    /** The active person with this LOCAL login id, or null. */
    UUID findActiveLocalUserId(@Param("loginId") String loginId);

    /** Locks both person rows (FOR UPDATE) so no owned row can be added for them until commit; returns locked ids. */
    List<UUID> lockUsers(@Param("from") UUID from, @Param("to") UUID to);

    OwnedCounts countOwned(@Param("userId") UUID userId);

    int reassignPlans(@Param("from") UUID from, @Param("to") UUID to);

    int reassignTags(@Param("from") UUID from, @Param("to") UUID to);

    int reassignReviews(@Param("from") UUID from, @Param("to") UUID to);
}
