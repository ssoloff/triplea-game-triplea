package org.triplea.server.moderator.toolbox.moderators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.ModeratorsDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.data.UserRole;

@ExtendWith(MockitoExtension.class)
class ModeratorsServiceTest {
  private static final String MODERATOR_NAME = "Adventure is a heavy-hearted sailor.";
  private static final int USER_ID = 1234;
  private static final int MODERATOR_ID = 555;
  private static final String USERNAME = "The reef grows amnesty like a golden lass.";

  @Mock private ModeratorsDao moderatorsDao;
  @Mock private UserJdbiDao userJdbiDao;
  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @InjectMocks private ModeratorsService moderatorsService;

  @Nested
  final class AddModeratorTest {
    @Test
    void throwsIfUserNotFound() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> moderatorsService.addModerator(MODERATOR_ID, USERNAME));
    }

    @Test
    void throwsIfModeratorNotAdded() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.of(USER_ID));
      when(moderatorsDao.setRole(USER_ID, UserRole.MODERATOR)).thenReturn(0);
      assertThrows(
          IllegalStateException.class,
          () -> moderatorsService.addModerator(MODERATOR_ID, USERNAME));
    }

    @Test
    void verifySuccessCase() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.of(USER_ID));
      when(moderatorsDao.setRole(USER_ID, UserRole.MODERATOR)).thenReturn(1);

      moderatorsService.addModerator(MODERATOR_ID, USERNAME);

      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .moderatorUserId(MODERATOR_ID)
                  .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_MODERATOR)
                  .actionTarget(USERNAME)
                  .build());
    }
  }

  @Nested
  final class RemoveModTest {
    @Test
    void throwsIfModNameIsNotFound() {
      when(userJdbiDao.lookupUserIdByName(MODERATOR_NAME)).thenReturn(Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> moderatorsService.removeMod(MODERATOR_ID, MODERATOR_NAME));
    }

    @Test
    void throwsIfModIsNotRemoved() {
      when(userJdbiDao.lookupUserIdByName(MODERATOR_NAME)).thenReturn(Optional.of(USER_ID));
      when(moderatorsDao.setRole(USER_ID, UserRole.PLAYER)).thenReturn(0);

      assertThrows(
          IllegalStateException.class,
          () -> moderatorsService.removeMod(MODERATOR_ID, MODERATOR_NAME));
    }

    @Test
    void verifySuccessfulRemove() {
      when(userJdbiDao.lookupUserIdByName(MODERATOR_NAME)).thenReturn(Optional.of(USER_ID));
      when(moderatorsDao.setRole(USER_ID, UserRole.PLAYER)).thenReturn(1);

      moderatorsService.removeMod(MODERATOR_ID, MODERATOR_NAME);

      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .moderatorUserId(MODERATOR_ID)
                  .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_MODERATOR)
                  .actionTarget(MODERATOR_NAME)
                  .build());
    }
  }

  @Nested
  final class AddSuperModTest {
    @Test
    void throwsIfUserNotFound() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> moderatorsService.addSuperMod(MODERATOR_ID, USERNAME));
    }

    @Test
    void throwsIfSuperModNotAdded() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.of(USER_ID));
      when(moderatorsDao.setRole(USER_ID, UserRole.ADMIN)).thenReturn(0);

      assertThrows(
          IllegalStateException.class, () -> moderatorsService.addSuperMod(MODERATOR_ID, USERNAME));
    }

    @Test
    void verifySuccessfulAdd() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.of(USER_ID));
      when(moderatorsDao.setRole(USER_ID, UserRole.ADMIN)).thenReturn(1);

      moderatorsService.addSuperMod(MODERATOR_ID, USERNAME);

      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .moderatorUserId(MODERATOR_ID)
                  .actionName(ModeratorAuditHistoryDao.AuditAction.ADD_SUPER_MOD)
                  .actionTarget(USERNAME)
                  .build());
    }
  }

  @Nested
  final class UserExistsTest {
    @Test
    void userDoesNotExist() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.empty());
      assertThat(moderatorsService.userExistsByName(USERNAME), is(false));
    }

    @Test
    void userExists() {
      when(userJdbiDao.lookupUserIdByName(USERNAME)).thenReturn(Optional.of(USER_ID));
      assertThat(moderatorsService.userExistsByName(USERNAME), is(true));
    }
  }
}
