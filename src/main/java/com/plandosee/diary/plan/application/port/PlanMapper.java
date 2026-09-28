package com.plandosee.diary.plan.application.port;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.plan.domain.PlanRevisionRow;
import com.plandosee.diary.plan.domain.PlanRow;

@Mapper
public interface PlanMapper {

    int insert(PlanRow plan);

    PlanRow findActiveOwned(@Param("userId") UUID userId, @Param("planId") UUID planId);

    PlanRow lockActiveOwned(@Param("userId") UUID userId, @Param("planId") UUID planId);

    List<PlanRow> listActiveOwned(@Param("userId") UUID userId);

    long countActiveOwned(@Param("userId") UUID userId);

    List<PlanRow> listActiveOwnedPage(@Param("userId") UUID userId, @Param("limit") int limit,
                                      @Param("offset") long offset);

    int updateOwned(PlanRow plan);

    int nextRevisionNo(@Param("planId") UUID planId);

    int insertRevision(PlanRevisionRow revision);

    List<PlanRevisionRow> listRevisionsOwned(@Param("userId") UUID userId, @Param("planId") UUID planId);
}
