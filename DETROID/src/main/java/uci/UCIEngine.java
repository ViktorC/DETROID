package main.java.uci;

import java.util.Collection;
import java.util.Set;

/**
 * The interface needed to be implemented by an engine to ensure it is UCI compliant.
 * 
 * @author Viktor
 *
 */
public interface UCIEngine {
	
	/**
	 * Initialises the engine; e.g. set up the tables, load parameters, etc. The engine is not expected to function properly without calling
	 * this method on the instance first.
	 * 
	 * @throws Exception If the engine cannot be initialized due to some reason.
	 */
	void init()  throws Exception;
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
	 * Returns the options the engine offers.
	 * 
	 * @return The UCI options the engine offers.
	 */
	Collection<Option<?>> getOptions();
	/**
	 * Notifies the engine whether it should keep updating the {@link #uci.DebugInfo DebugInfo} instance exposed by
	 * {@link #uci.Engine.getDebugInfo getDebugInfo} with debug information strings.
	 * 
	 * @param on Whehter the engine should run in debug mode.
	 */
	void setDebugMode(boolean on);
	/**
	 * Sets an option defined by the engine to the specified value.
	 * 
	 * @param setting The UCI option to set.
	 * @param value The value to which the option should be set.
	 * @return Whether the setting was successfully set to the value, e.g. it was an allowed value.
	 */
	<T> boolean setOption(Option<T> setting, T value);
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
	boolean position(String fen);
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
	 * @param depth The depth to which the position should be searched. If it is 0, the engine should return the score determined 
	 * by the quiescence search or if it is not implemented, the static evaluation score.
	 * @param nodes The maximum number of nodes that should be searched.
	 * @param mateDistance Search for a mate in x.
	 * @param searchTime Search exactly this number of ms.
	 * @param infinite Whether the position should be searched infinitely.
	 * @return The best move found and optionally the suggested ponder move in pure algebraic coordinate notation.
	 */
	SearchResults search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth,
			Long nodes, Integer mateDistance, Long searchTime, Boolean infinite);
	/**
	 * Asks the engine to stop searching and return the best move found up until that point.
	 */
	void stop();
	/**
	 * Signals to the engine that the move it was pondering on was actually played and it should keep searching in normal mode.
	 */
	void ponderhit();
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
	
}
