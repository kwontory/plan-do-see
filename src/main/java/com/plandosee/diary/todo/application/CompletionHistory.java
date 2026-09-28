package com.plandosee.diary.todo.application;

import java.util.List;

import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.todo.domain.CompletionHistoryEntry;

/**
 * ADR-16 S03 completion history: one page of completions and reopens merged in time order by the server, with the
 * counts of all of them (completionCount keeps the meaning of the former "completion records" line, T06-C21).
 */
public record CompletionHistory(List<CompletionHistoryEntry> entries, long completionCount, long reopenCount,
                                PageInfo page) {

    public CompletionHistory {
        entries = List.copyOf(entries);
    }
}
