package games.strategy.engine.lobby;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.common.LobbyConstants;

class PlayerNameValidationTest {
  @Test
  void usernameValidationWithInvalidNames() {
    Arrays.asList(
            null,
            "",
            "a",
            "ab", // still too short
            Strings.repeat("a", PlayerNameValidation.MAX_LENGTH + 1),
            "ab*", // no special characters other than '-' and '_'
            "ab$",
            ".ab",
            "a,b",
            "ab?",
            "   ", // no spaces
            "---", // must start with a character
            "___",
            "_ab",
            "01a",
            "123",
            "-ab",
            "a b")
        .forEach(
            invalidName -> {
              assertThat(
                  "Expected name to be marked as invalid: " + invalidName,
                  PlayerNameValidation.isValid(invalidName),
                  is(false));
              assertThat(
                  "Expected name to have validation error messages: " + invalidName,
                  PlayerNameValidation.validate(invalidName),
                  not(emptyString()));
            });

    Arrays.asList(LobbyConstants.LOBBY_WATCHER_NAME, LobbyConstants.ADMIN_USERNAME)
        .forEach(
            invalidNamePart -> {
              assertThat(
                  "user names cannot contain anything from the forbidden name list",
                  PlayerNameValidation.isValid(invalidNamePart),
                  is(false));
              assertThat(
                  "verify we are doing a contains match to make sure "
                      + "user name does not contain anything forbidden.",
                  PlayerNameValidation.isValid("xyz" + invalidNamePart + "abc"),
                  is(false));

              assertThat(
                  "case insensitive on our matches.",
                  PlayerNameValidation.isValid(invalidNamePart.toUpperCase()),
                  is(false));
              assertThat(
                  "case insensitive on our matches.",
                  PlayerNameValidation.isValid(invalidNamePart.toLowerCase()),
                  is(false));
            });
  }

  @Test
  void usernameValidationWithValidNames() {
    Arrays.asList("abc", Strings.repeat("a", PlayerNameValidation.MAX_LENGTH), "a12", "a--")
        .forEach(
            validName -> {
              assertThat(
                  "Expected name to be marked as valid: " + validName,
                  PlayerNameValidation.isValid(validName),
                  is(true));

              assertThat(
                  String.format(
                      "Expected name: %s, to have no validation error messages, but had %s",
                      validName, PlayerNameValidation.validate(validName)),
                  PlayerNameValidation.validate(validName),
                  nullValue());
            });
  }

  @Test
  void verifyNameIsNotLoggedInAlready() {
    final String name = "name";

    final Collection<Collection<String>> validInputs =
        Arrays.asList(
            Arrays.asList("okay", "other"),
            Arrays.asList("name1", "nam", "name_"),
            Collections.emptySet());

    validInputs.forEach(
        valid ->
            assertThat(
                PlayerNameValidation.verifyNameIsNotLoggedInAlready(name, valid), nullValue()));
  }

  @Test
  void verifyNameIsNotLoggedInAlreadyNegativeCases() {
    final String name = "name";

    final Collection<Collection<String>> validInputs =
        Arrays.asList(singleton("NAME"), singleton("name"), Arrays.asList("abc", "Name"));

    validInputs.forEach(
        valid ->
            assertThat(
                PlayerNameValidation.verifyNameIsNotLoggedInAlready(name, valid), notNullValue()));
  }
}
