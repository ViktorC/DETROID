package net.viktorc.detroid.framework.uci;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import net.viktorc.detroid.framework.uci.Option.CheckOption;
import net.viktorc.detroid.framework.uci.Option.SpinOption;

/**
 * The interface needed to be implemented by an engine to ensure it is UCI compliant.
 *
 * @author Viktor
 */
public interface UCIEngine extends Closeable {

  /**
   * A special UCI position command for the start position.
   */
  String START_POSITION = "startpos";
  /**
   * A standard check type UCI option that determines whether the engine should use its own book. It should be false by default.
   */
  String OWN_BOOK_OPTION_NAME = "OwnBook";
  /**
   * A standard spin type UCI option that determines the number of search threads to use.
   */
  String THREADS_OPTION_NAME = "Threads";
  /**
   * A standard spin type UCI option that determines the size of the hash tables used by the chess engine. Its unit is MB.
   */
  String HASH_OPTION_NAME = "Hash";

  /**
   * Initialises the engine; e.g. set up the tables, load parameters, etc. The engine is not expected to function properly without calling
   * this method on the instance first.
   *
   * @throws Exception If the engine cannot be initialized due to some reason.
   */
  void init() throws Exception;

  /**
   * Returns whether the method {@link #init() init} has already been called on the instance.
   *
   * @return Whether the engine has been initialized.
   */
  boolean isInit();

  /**
   * Returns the name of the engine.
   *
   * @return The name of the engine.
   */
  String getName();

  /**
   * Returns the name of the author of the engine.
   *
   * @return The name of the author of the engine.
   */
  String getAuthor();

  /**
   * Returns the options the engine offers and their values. Changes to the map are not propagated to the engine.
   *
   * @return The UCI options the engine offers and their values.
   */
  Map<Option<?>, Object> getOptions();

  /**
   * Sets an option defined by the engine to the specified value.
   *
   * @param setting The UCI option to set.
   * @param value The value to which the option should be set.
   * @param <T> The type of the value the option holds.
   * @return Whether the setting was successfully set to the value, i.e. the option exists and it is an allowed value.
   */
  <T> boolean setOption(Option<T> setting, T value);

  /**
   * Notifies the engine whether it should keep updating the {@link net.viktorc.detroid.framework.uci.DebugInformation} instance exposed by
   * the {@link #getDebugInfo() getDebugInfo} method with debug information strings.
   *
   * @param on Whether the engine should run in debug mode.
   */
  void setDebugMode(boolean on);

  /**
   * Resets the game. It might be a good idea for the engine to wipe the hash tables at this point.
   */
  void newGame();

  /**
   * Sends the current position to the engine. The string "startpos" denotes the starting position and should be handled by the engine.
   *
   * @param fen The current position in FEN.
   * @return Whether the position could be successfully set up.
   */
  boolean setPosition(String fen);

  /**
   * Prompts the engine to make the move defined in Pure Algebraic Coordinate Notation.
   *
   * @param pacn The move to play in PACN.
   * @return Whether the move was successfully made.
   */
  boolean play(String pacn);

  /**
   * Prompts the engine to start searching the current position according to the specified parameters.
   *
   * @param searchMoves A set of the moves to search at the root node in pure algebraic coordinate notation.
   * @param ponder Whether the engine should search in pondering mode.
   * @param whiteTime The time left on the clock for white in ms.
   * @param blackTime The time left on the clock for black in ms.
   * @param whiteIncrement Increment per move in ms.
   * @param blackIncrement Increment per move in ms.
   * @param movesToGo The number of moves until the next time control.
   * @param depth The depth to which the position should be searched. If it is 0, the engine should return the score determined by the
   * quiescence search or if it is not implemented, the static evaluation score.
   * @param nodes The maximum number of nodes that should be searched.
   * @param mateDistance Search for a mate in x.
   * @param searchTime Search exactly this number of ms.
   * @param infinite Whether the position should be searched infinitely.
   * @return The best move found and optionally the suggested ponder move in pure algebraic coordinate notation and optionally the search
   * score and search score type.
   */
  SearchResults search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
      Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth,
      Long nodes, Integer mateDistance, Long searchTime, Boolean infinite);

  /**
   * Prompts the engine to stop searching and return the best move found up until that point. It should cause the {@link #search(Set,
   * Boolean, Long, Long, Long, Long, Integer, Integer, Long, Integer, Long, Boolean) search} method to terminate within a few milliseconds
   * at most. If the engine is not searching at the time of the call, it should be ignored.
   */
  void stop();

  /**
   * Signals to the engine that the move it was pondering on was actually played and it should keep searching in normal mode.
   */
  void ponderHit();

  /**
   * Returns an observable object containing information about the results and statistics of the ongoing/last search.
   *
   * @return An observable object containing information about the results and statistics of the ongoing/last search.
   */
  SearchInformation getSearchInfo();

  /**
   * Returns the load factor of the hash tables in permills.
   *
   * @return The load factor of the hash tables in permills.
   */
  short getHashLoadPermill();

  /**
   * Returns an observable object containing information that is not related to the game but can help detecting bugs in debug mode.
   *
   * @return An observable object containing information that is not related to the game but can help detecting bugs in debug mode.
   */
  DebugInformation getDebugInfo();

  /**
   * Signals the engine that it should clean up and free the resources it has been using.
   */
  void quit();

  /**
   * Sets the current position of the engine to the starting position.
   *
   * @return Whether the position could be successfully set up.
   */
  default boolean setPosition() {
    return setPosition(START_POSITION);
  }

  /**
   * It looks for an option of the specified type and name among the options provided by the engine, and if it is found, it attempts to set
   * it to the specified value.
   *
   * @param <T> The value type of the option.
   * @param <U> The option type.
   * @param optionType The type of the option to look for.
   * @param optionName The name of the option.
   * @param value The value to which it should be set.
   * @return Whether the option was found and successfully set.
   */
  @SuppressWarnings("unchecked")
  default <T, U extends Option<T>> boolean setOption(Class<U> optionType, String optionName, T value) {
    U option = null;
    Map<Option<?>, Object> options = getOptions();
    for (Option<?> o : options.keySet()) {
      if (optionName.equals(o.getName()) && o.getClass().equals(optionType)) {
        option = (U) o;
      }
    }
    return option != null && setOption(option, value);
  }

  /**
   * It attempts to set whether the engine should use its own opening book if the option is provided by the engine.
   *
   * @param value Whether the engine should use its own opening book.
   * @return Whether the option was successfully set.
   */
  default boolean setOwnBookOption(boolean value) {
    return setOption(CheckOption.class, OWN_BOOK_OPTION_NAME, value);
  }

  /**
   * It attempts to set the hash size the engine should use if the option is provided by the engine.
   *
   * @param value The hash size to use (in bytes).
   * @return Whether the option was successfully set.
   */
  default boolean setHashSizeOption(int value) {
    return setOption(SpinOption.class, HASH_OPTION_NAME, value);
  }

  /**
   * It attempts to set the number of threads the engine should use if the option is provided by the engine.
   *
   * @param value The number of search threads the engine should use.
   * @return Whether the option was successfully set.
   */
  default boolean setThreadsOption(int value) {
    return setOption(SpinOption.class, THREADS_OPTION_NAME, value);
  }

  @Override
  default void close() {
    quit();
  }

}