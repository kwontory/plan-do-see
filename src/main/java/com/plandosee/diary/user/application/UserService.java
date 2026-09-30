package com.plandosee.diary.user.application;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.user.application.port.UserMapper;
import com.plandosee.diary.user.domain.UserRow;
import com.plandosee.diary.user.domain.UserRules;

/**
 * Person rows (users) for other features. Sign-up and account settings (auth) call these methods and never the
 * user mapper. Every method joins the caller's transaction, so a sign-up creates the person, the login and the
 * password in one transaction. The nickname is checked again here (UserRules.NICKNAME) whatever the caller did.
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final IdGenerator idGenerator;

    public UserService(UserMapper userMapper, IdGenerator idGenerator) {
        this.userMapper = userMapper;
        this.idGenerator = idGenerator;
    }

    /** New person with the given nickname; returns the new id. */
    @Transactional
    public UUID create(String nickname, OffsetDateTime now) {
        UserRow user = new UserRow();
        user.setId(idGenerator.newId());
        user.setNickname(UserRules.NICKNAME.apply("nickname", nickname));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user.getId();
    }

    /** The active person, or NotFoundException. */
    @Transactional(readOnly = true)
    public UserRow requireActive(UUID userId) {
        UserRow user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new NotFoundException("user");
        }
        return user;
    }

    /**
     * Renames the active person; the nickname is normalized and checked. UNCHANGED (nothing written) when the
     * normalized value equals the stored one. NotFoundException when not active.
     */
    @Transactional
    public EditOutcome rename(UUID userId, String nickname, OffsetDateTime now) {
        String value = UserRules.NICKNAME.apply("nickname", nickname);
        UserRow current = requireActive(userId);
        if (value.equals(current.getNickname())) {
            return EditOutcome.UNCHANGED;
        }
        if (userMapper.updateNickname(userId, value, now) != 1) {
            throw new NotFoundException("user");
        }
        return EditOutcome.UPDATED;
    }

    /**
     * Account deletion only (ADR-35): physically deletes the person row; every owned row and login must be gone
     * (foreign keys). Joins the caller's transaction.
     */
    @Transactional
    public void deleteForAccountRemoval(UUID userId) {
        if (userMapper.delete(userId) != 1) {
            throw new NotFoundException("user");
        }
    }
}
