package games.strategy.engine.framework.persistence.serializable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

import games.strategy.engine.data.GameData;
import games.strategy.internal.persistence.serializable.GameDataPersistenceDelegate;
import games.strategy.internal.persistence.serializable.VersionPersistenceDelegate;
import games.strategy.persistence.serializable.DefaultPersistenceDelegateRegistry;
import games.strategy.persistence.serializable.ObjectInputStream;
import games.strategy.persistence.serializable.ObjectOutputStream;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.Version;

/**
 * A factory for creating input and output object streams for importing and exporting game data.
 *
 * <p>
 * The methods of this class are thread safe.
 * </p>
 */
final class ObjectStreams {
  private ObjectStreams() {}

  /**
   * Creates a new instance of the {@link ObjectInputStream} class using a persistence delegate registry capable of
   * persisting game data.
   *
   * @param is The input stream from which to read; must not be {@code null}.
   *
   * @return A new instance of the {@link ObjectInputStream} class; never {@code null}.
   *
   * @throws IOException If an I/O error occurs while reading the stream header.
   * @throws StreamCorruptedException If the stream header is incorrect.
   */
  static ObjectInputStream newObjectInputStream(final InputStream is) throws IOException {
    assert is != null;

    return new ObjectInputStream(is, newPersistenceDelegateRegistry());
  }

  private static PersistenceDelegateRegistry newPersistenceDelegateRegistry() {
    final PersistenceDelegateRegistry persistenceDelegateRegistry = new DefaultPersistenceDelegateRegistry();
    persistenceDelegateRegistry.registerPersistenceDelegate(GameData.class, new GameDataPersistenceDelegate());
    persistenceDelegateRegistry.registerPersistenceDelegate(Version.class, new VersionPersistenceDelegate());
    return persistenceDelegateRegistry;
  }

  /**
   * Creates a new instance of the {@link ObjectInputStream} class using a persistence delegate registry capable of
   * persisting game data and wrapping the underlying output stream with a close shield.
   *
   * @param is The input stream from which to read; must not be {@code null}. This stream will not be closed when the
   *        returned {@link ObjectInputStream} is closed.
   *
   * @return A new instance of the {@link ObjectInputStream} class; never {@code null}.
   *
   * @throws IOException If an I/O error occurs while reading the stream header.
   * @throws StreamCorruptedException If the stream header is incorrect.
   */
  static ObjectInputStream newObjectInputStreamWithCloseShield(final InputStream is) throws IOException {
    assert is != null;

    return newObjectInputStream(new CloseShieldInputStream(is));
  }

  /**
   * Creates a new instance of the {@link ObjectOutputStream} class using a persistence delegate registry capable of
   * persisting game data.
   *
   * @param os The output stream on which to write; must not be {@code null}.
   *
   * @return A new instance of the {@link ObjectOutputStream} class; never {@code null}.
   *
   * @throws IOException If an I/O error occurs while writing the stream header.
   */
  static ObjectOutputStream newObjectOutputStream(final OutputStream os) throws IOException {
    assert os != null;

    return new ObjectOutputStream(os, newPersistenceDelegateRegistry());
  }

  /**
   * Creates a new instance of the {@link ObjectOutputStream} class using a persistence delegate registry capable of
   * persisting game data and wrapping the underlying output stream with a close shield.
   *
   * @param os The output stream on which to write; must not be {@code null}. This stream will not be closed when the
   *        returned {@link ObjectOutputStream} is closed.
   *
   * @return A new instance of the {@link ObjectOutputStream} class; never {@code null}.
   *
   * @throws IOException If an I/O error occurs while writing the stream header.
   */
  static ObjectOutputStream newObjectOutputStreamWithCloseShield(final OutputStream os) throws IOException {
    assert os != null;

    return newObjectOutputStream(new CloseShieldOutputStream(os));
  }
}
