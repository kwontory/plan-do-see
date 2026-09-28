package com.plandosee.diary.todo.application.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.todo.domain.TagRow;

@Mapper
public interface TagMapper {

    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("name") String name,
                       @Param("now") OffsetDateTime now);

    UUID findActiveIdByName(@Param("userId") UUID userId, @Param("name") String name);

    List<TagRow> listActiveOwned(@Param("userId") UUID userId);

    List<TagRow> listForTodos(@Param("userId") UUID userId, @Param("todoIds") List<UUID> todoIds);

    int deleteTodoLinks(@Param("todoId") UUID todoId);

    int insertTodoLink(@Param("todoId") UUID todoId, @Param("tagId") UUID tagId, @Param("now") OffsetDateTime now);
}
