package games.strategy.net.nio;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.lobby.PlayerNameValidation;
import games.strategy.net.ILoginValidator;
import games.strategy.net.INode;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.PlayerNameAssigner;
import games.strategy.net.ServerMessenger;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.triplea.http.client.ApiKey;

/** Server-side implementation of {@link QuarantineConversation}. */
@Log
public class ServerQuarantineConversation extends QuarantineConversation {
  /**
   * Magic authentication error string to indicate temporary password was used to authenticate and
   * password should be reset.
   */
  public static final String CHANGE_PASSWORD = "change_password";

  /*
   * Communication sequence
   * 1) server reads client name
   * 2) server sends challenge (or null if no challenge is to be made)
   * 3) server reads response (or null if no challenge)
   * 4) server send null then client name and node info on success, or an error message
   * if there is an error
   * 5) if the client reads an error message, the client sends an acknowledgment (we need to
   * make sur the client gets the message before closing the socket).
   */
  private enum Step {
    READ_NAME,
    READ_MAC,
    CHALLENGE,
    ACK_ERROR
  }

  private final ILoginValidator validator;
  private final SocketChannel channel;
  private final NioSocket socket;
  private Step step = Step.READ_NAME;
  private String remoteName;
  private String remoteMac;
  private Map<String, String> challenge;
  private final ServerMessenger serverMessenger;
  private final Function<PlayerName, ApiKey> apiKeyGenerator;

  public ServerQuarantineConversation(
      final ILoginValidator validator,
      final SocketChannel channel,
      final NioSocket socket,
      final ServerMessenger serverMessenger,
      final Function<PlayerName, ApiKey> apiKeyGenerator) {
    this.validator = validator;
    this.socket = socket;
    this.channel = channel;
    this.serverMessenger = serverMessenger;
    this.apiKeyGenerator = apiKeyGenerator;
  }

  public String getRemoteName() {
    return remoteName;
  }

  @Override
  public Action message(final Serializable serializable) {
    try {
      switch (step) {
        case READ_NAME:
          remoteName = ((String) serializable).trim();
          step = Step.READ_MAC;
          return Action.NONE;
        case READ_MAC:
          remoteMac = (String) serializable;
          if (validator != null) {
            challenge = validator.getChallengeProperties(remoteName);
          }
          send((Serializable) challenge);
          step = Step.CHALLENGE;
          return Action.NONE;
        case CHALLENGE:
          @SuppressWarnings("unchecked")
          final Map<String, String> response = (Map<String, String>) serializable;
          String error = null;
          String apiKey = null;

          if (validator != null) {
            error =
                Optional.ofNullable(
                        validator.verifyConnection(
                            challenge,
                            response,
                            remoteName,
                            remoteMac,
                            channel.socket().getRemoteSocketAddress()))
                    .orElseGet(() -> PlayerNameValidation.serverSideValidate(remoteName));
            if (error == null) {
              error =
                  PlayerNameValidation.verifyNameIsNotLoggedInAlready(
                      remoteName,
                      // filter out nodes that are connected from the same computer.
                      // This way we match against nodes from other computers only.
                      serverMessenger.getNodes().stream()
                          .filter(n -> !n.getAddress().equals(channel.socket().getInetAddress()))
                          .map(INode::getName)
                          .collect(Collectors.toSet()));
            }

            if (error != null && !error.equals(CHANGE_PASSWORD)) {
              step = Step.ACK_ERROR;
              send(error);
              return Action.NONE;
            } else {
              send(null);
            }
            apiKey =
                Optional.ofNullable(apiKeyGenerator)
                    .map(keyGenerator -> keyGenerator.apply(PlayerName.of(remoteName)))
                    .map(ApiKey::getValue)
                    .orElse(null);
          } else {
            send(null);
          }

          synchronized (serverMessenger.newNodeLock) {
            final Multimap<String, String> macToName = HashMultimap.create();

            // aggregate all player names by mac address (there can be multiple names per mac
            // address)
            serverMessenger.getNodes().stream()
                .filter(n -> serverMessenger.getPlayerMac(n.getPlayerName()) != null)
                .forEach(
                    n ->
                        macToName.put(
                            serverMessenger.getPlayerMac(n.getPlayerName()), n.getName()));
            remoteName = PlayerNameAssigner.assignName(remoteName, remoteMac, macToName);
          }

          // send the node its assigned name, our name, an error message that could contain a magic
          // string informing client they should reset their password, and last an API key that can
          // be used for further http server interaction.
          send(new String[] {remoteName, serverMessenger.getLocalNode().getName(), error, apiKey});

          // send the node its and our address as we see it
          send(
              new InetSocketAddress[] {
                (InetSocketAddress) channel.socket().getRemoteSocketAddress(),
                serverMessenger.getLocalNode().getSocketAddress()
              });

          // Login succeeded, so notify the ServerMessenger about the login with the name, mac, etc.
          serverMessenger.notifyPlayerLogin(PlayerName.of(remoteName), remoteMac);
          // We are good
          return Action.UNQUARANTINE;
        case ACK_ERROR:
          return Action.TERMINATE;
        default:
          throw new IllegalStateException("Invalid state");
      }
    } catch (final Throwable t) {
      log.log(Level.SEVERE, "Error with connection", t);
      return Action.TERMINATE;
    }
  }

  private void send(final Serializable object) {
    // this messenger is quarantined, so to and from don't matter
    final MessageHeader header = new MessageHeader(Node.NULL_NODE, Node.NULL_NODE, object);
    socket.send(channel, header);
  }

  @Override
  public void close() {}
}
