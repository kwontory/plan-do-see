package com.plandosee.diary.common.config;

import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoCurrentUserProvider implements CurrentUserProvider {

    private final UUID demoUserId;

    public DemoCurrentUserProvider(@Value("${app.demo-user-id}") UUID demoUserId) {
        this.demoUserId = Objects.requireNonNull(demoUserId, "app.demo-user-id");
    }

    @Override
    public UUID currentUserId() {
        return demoUserId;
    }
}
