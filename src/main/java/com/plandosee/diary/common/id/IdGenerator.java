package com.plandosee.diary.common.id;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    public UUID newId() {
        return UUID.randomUUID();
    }
}
