package com.plandosee.diary.execution.application.port;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.execution.domain.ExecutionLogRow;

@Mapper
public interface ExecutionLogMapper {

    int insert(ExecutionLogRow log);

    List<ExecutionLogRow> listForTodoOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);
}
