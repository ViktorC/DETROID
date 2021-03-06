package net.viktorc.detroid.framework.engine;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * A base class for reading and selecting moves from a chess opening book. It should allow for using an alternative book once out of the
 * main book.
 *
 * @author Viktor
 */
public abstract class OpeningBook implements Closeable {

  protected Path path;
  protected SeekableByteChannel bookStream;
  protected OpeningBook secondaryBook;

  /**
   * It instantiates a Book object on the opening book file specified by filePath; if the file cannot be accessed, an IOException is
   * thrown.
   *
   * @param filePath The path to the book.
   * @throws Exception If the book cannot be accessed.
   */
  protected OpeningBook(String filePath) throws Exception {
    File file = new File(filePath);
    if (file.exists()) {
      path = file.exists() ? file.toPath() : Paths.get(ClassLoader.getSystemClassLoader().getResource(filePath).toURI());
    } else {
      throw new IllegalArgumentException();
    }
    bookStream = Files.newByteChannel(path, StandardOpenOption.READ);
  }

  /**
   * Returns the path to the main book file this object has been instantiated on.
   *
   * @return The file path to the primary book.
   */
  public String getPrimaryFilePath() {
    return path.toString();
  }

  /**
   * Returns the path to the secondary book file.
   *
   * @return The file path to the secondary book file.
   */
  public String getSecondaryFilePath() {
    return secondaryBook == null ? null : secondaryBook.getPrimaryFilePath();
  }

  @Override
  public void close() throws IOException {
    bookStream.close();
    if (secondaryBook != null) {
      secondaryBook.close();
    }
  }

  /**
   * Picks and returns an opening move for the Position0 from all the relevant entries found in the PolyGlot book based on the specified
   * mathematical model for selection. If there have been no relevant entries found, it returns null.
   *
   * @param pos The position for which an opening move is sought.
   * @param selection The mathematical model to be applied when selecting the move.
   * @return An opening move.
   * @throws Exception If anything goes wrong.
   */
  public abstract Move getMove(Position pos, SelectionModel selection) throws Exception;

  /**
   * An enumeration type for mathematical models used in the process of selecting one from all the available opening moves for a position.
   *
   * @author Viktor
   */
  public enum SelectionModel {

    RANDOM,
    STOCHASTIC,
    DETERMINISTIC;

  }

}