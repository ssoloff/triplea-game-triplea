package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class ChatParticipant implements Serializable {
  private static final long serialVersionUID = 7103177780407531008L;
  @NonNull private final PlayerName playerName;
  private final boolean isModerator;
}
