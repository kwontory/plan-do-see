package com.plandosee.diary.user.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.user.application.port.UserMapper;

/**
 * The configured demo user must exist, otherwise every owned query would silently return nothing.
 */
@Component
public class DemoUserVerifier {

    private final CurrentUserProvider currentUserProvider;
    private final UserMapper userMapper;

    public DemoUserVerifier(CurrentUserProvider currentUserProvider, UserMapper userMapper) {
        this.currentUserProvider = currentUserProvider;
        this.userMapper = userMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        if (userMapper.findActiveById(currentUserProvider.currentUserId()) == null) {
            throw new IllegalStateException("Configured app.demo-user-id does not match an active users row");
        }
    }
}
