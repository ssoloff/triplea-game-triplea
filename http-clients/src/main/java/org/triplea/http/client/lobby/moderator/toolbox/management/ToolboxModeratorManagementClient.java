package org.triplea.http.client.lobby.moderator.toolbox.management;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client for moderator management. Other than the fetch to display the list of moderators,
 * most actions will be available to a "super-mod" only (super-mod = admin of mods).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxModeratorManagementClient {
  public static final String FETCH_MODERATORS_PATH = "/moderator-toolbox/fetch-moderators";
  public static final String IS_SUPER_MOD_PATH = "/moderator-toolbox/is-super-mod";
  public static final String REMOVE_MOD_PATH = "/moderator-toolbox/super-mod/remove-mod";
  public static final String ADD_SUPER_MOD_PATH = "/moderator-toolbox/super-mod/add-super-mod";
  public static final String CHECK_USER_EXISTS_PATH = "/moderator-toolbox/does-user-exist";
  public static final String ADD_MODERATOR_PATH = "/moderator-toolbox/super-mod/add-moderator";

  private final AuthenticationHeaders httpHeaders;
  private final ToolboxModeratorManagementFeignClient client;

  public static ToolboxModeratorManagementClient newClient(
      final URI serverUri, final ApiKey apiKey) {
    return new ToolboxModeratorManagementClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(ToolboxModeratorManagementFeignClient.class, serverUri).get());
  }

  public List<ModeratorInfo> fetchModeratorList() {
    return client.fetchModerators(httpHeaders.createHeaders());
  }

  public boolean isCurrentUserSuperMod() {
    return client.isSuperMod(httpHeaders.createHeaders());
  }

  public void removeMod(final String moderatorName) {
    checkArgument(moderatorName != null);
    client.removeMod(httpHeaders.createHeaders(), moderatorName);
  }

  public void addSuperMod(final String moderatorName) {
    checkArgument(moderatorName != null);
    client.addSuperMod(httpHeaders.createHeaders(), moderatorName);
  }

  public boolean checkUserExists(final String usernameRequested) {
    checkArgument(usernameRequested.length() > 3);
    return client.checkUserExists(httpHeaders.createHeaders(), usernameRequested);
  }

  public void addModerator(final String username) {
    checkArgument(username != null);
    client.addModerator(httpHeaders.createHeaders(), username);
  }
}
