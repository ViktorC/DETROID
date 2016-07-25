package uci;

import java.util.Set;

public interface Engine {
	
	/**
	 * Initialize the engine; e.g. set up the tables, load parameters, etc.
	 */
	void init();
	/**
	 * The name of the engine.
	 * 
	 * @return
	 */
	String getName();
	/**
	 * The name of the author of the engine.
	 * 
	 * @return
	 */
	String getAuthor();
	/**
	 * Returns the load factor of the hash tables in permills.
	 * 
	 * @return
	 */
	short getHashLoadPermill();
	/**
	 * Returns the options the engine offers.
	 * 
	 * @return
	 */
	Set<Option<?>> getOptions();
	/**
	 * Sets an option defined by the engine to the specified value.
	 * 
	 * @param setting
	 * @param value
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
	 * @param fen
	 * @return
	 */
	boolean position(String fen);
	/**
	 * Asks the engine to make the move defined in pure algebraic chess notation.
	 * 
	 * @param pacn
	 * @return Whether the move was successfully made.
	 */
	boolean play(String pacn);
	/**
	 * Asks the engine to start searching the current position according to the specified parameters.
	 * 
	 * @param searchMoves A set of the moves to search at the root node in pure algebraic coordinate notation.
	 * @param ponder Whether the engine should search in pondering mode.
	 * @param whiteTime The time left on the clock for white in ms.
	 * @param blackTime The time left on the clock for black in ms.
	 * @param whiteIncrement Increment per move in ms.
	 * @param blackIncrement Increment per move in ms.
	 * @param movesToGo The number of moves until the next time control.
	 * @param depth The depth to which the position should be searched.
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
	 * Tells the engine that the move it was pondering on was actually played and it should keep searching in normal mode.
	 */
	void ponderhit();
	/**
	 * Returns an observable object containing information about the results and statistics of the ongoing or if none, last search.
	 * 
	 * @return
	 */
	SearchInfo getSearchInfo();
	/**
	 * Signals the engine that it should clean up and free the resources it has been using.
	 */
	void quit();
}
