package org.triplea.lobby.common.login;

/** The property keys that may be present in a lobby authentication protocol challenge. */
public final class LobbyLoginChallengeKeys {
  public static final String RSA_PUBLIC_KEY = "RSAPUBLICKEY";
  public static final String SALT = "SALT";

  private LobbyLoginChallengeKeys() {}
}
