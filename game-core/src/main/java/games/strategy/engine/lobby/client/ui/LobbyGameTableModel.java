package games.strategy.engine.lobby.client.ui;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.Messengers;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.util.Tuple;

class LobbyGameTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 6399458368730633993L;

  enum Column {
    Host,
    Name,
    GV,
    Round,
    Players,
    P,
    Status,
    Comments,
    Started,
    UUID
  }

  private final Messengers messengers;
  private final boolean admin;

  // these must only be accessed in the swing event thread
  private final List<Tuple<UUID, GameDescription>> gameList = new ArrayList<>();
  private final ILobbyGameBroadcaster lobbyGameBroadcaster =
      new ILobbyGameBroadcaster() {
        @Override
        public void gameUpdated(final UUID gameId, final GameDescription description) {
          assertSentFromServer();
          updateGame(gameId, description);
        }

        @Override
        public void gameRemoved(final UUID gameId) {
          assertSentFromServer();
          removeGame(gameId);
        }
      };

  LobbyGameTableModel(final boolean admin, final Messengers messengers) {
    this.messengers = messengers;
    this.admin = admin;
    messengers.registerChannelSubscriber(lobbyGameBroadcaster, ILobbyGameBroadcaster.REMOTE_NAME);

    final Map<UUID, GameDescription> games =
        ((ILobbyGameController) messengers.getRemote(ILobbyGameController.REMOTE_NAME)).listGames();
    for (final Map.Entry<UUID, GameDescription> entry : games.entrySet()) {
      updateGame(entry.getKey(), entry.getValue());
    }
  }

  private void removeGame(final UUID gameId) {
    SwingUtilities.invokeLater(
        () -> {
          if (gameId == null) {
            return;
          }

          final Tuple<UUID, GameDescription> gameToRemove = findGame(gameId);
          if (gameToRemove != null) {
            final int index = gameList.indexOf(gameToRemove);
            gameList.remove(gameToRemove);
            fireTableRowsDeleted(index, index);
          }
        });
  }

  private Tuple<UUID, GameDescription> findGame(final UUID gameId) {
    return gameList.stream()
        .filter(game -> game.getFirst().equals(gameId))
        .findFirst()
        .orElse(null);
  }

  ILobbyGameBroadcaster getLobbyGameBroadcaster() {
    return lobbyGameBroadcaster;
  }

  GameDescription get(final int i) {
    return gameList.get(i).getSecond();
  }

  private void assertSentFromServer() {
    if (!MessageContext.getSender().equals(messengers.getServerNode())) {
      throw new IllegalStateException("Invalid sender");
    }
  }

  private void updateGame(final UUID gameId, final GameDescription description) {
    if (gameId == null) {
      return;
    }
    SwingUtilities.invokeLater(
        () -> {
          final Tuple<UUID, GameDescription> toReplace = findGame(gameId);
          if (toReplace == null) {
            gameList.add(Tuple.of(gameId, description));
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
          } else {
            final int replaceIndex = gameList.indexOf(toReplace);
            gameList.set(replaceIndex, Tuple.of(gameId, description));
            fireTableRowsUpdated(replaceIndex, replaceIndex);
          }
        });
  }

  @Override
  public String getColumnName(final int column) {
    return Column.values()[column].toString();
  }

  int getColumnIndex(final Column column) {
    return column.ordinal();
  }

  @Override
  public int getColumnCount() {
    final int adminHiddenColumns = admin ? 0 : -1;
    // -1 so we don't display the UUID
    // -1 again if we are not admin to hide the 'started' column
    return Column.values().length - 1 + adminHiddenColumns;
  }

  @Override
  public int getRowCount() {
    return gameList.size();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final GameDescription description = gameList.get(rowIndex).getSecond();
    switch (column) {
      case Host:
        return description.getHostName();
      case Round:
        final int round = description.getRound();
        return round == 0 ? "-" : String.valueOf(round);
      case Name:
        return description.getGameName();
      case Players:
        return description.getPlayerCount();
      case P:
        return (description.isPassworded() ? "*" : "");
      case GV:
        return description.getGameVersion();
      case Status:
        return description.getStatus();
      case Comments:
        return description.getComment();
      case Started:
        return formatBotStartTime(description.getStartDateTime());
      case UUID:
        return gameList.get(rowIndex).getFirst();
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }

  @VisibleForTesting
  static String formatBotStartTime(final Instant instant) {
    return new DateTimeFormatterBuilder()
        .appendLocalized(null, FormatStyle.SHORT)
        .toFormatter()
        .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
  }
}
