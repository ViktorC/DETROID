package engine;

import chess.Move;
import chess.SearchArguments;
import chess.SearchStatistics;
import util.Setting;

public interface Engine {
	
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
	 * Returns the load factor of the hash tables.
	 * 
	 * @return
	 */
	float getHashLoad();
	/**
	 * Returns the settings the engine offers. Each option is defined by an array of KeyValuePairs of varying numbers of elements specifying the
	 * name, type, default value, etc. of the option.
	 * 
	 * @return
	 */
	Iterable<Setting<?>> getOptions();
	/**
	 * Sets an option defined by the engine to the specified value.
	 * 
	 * @param setting
	 * @param value
	 * @throws IllegalArgumentException
	 */
	<T> void setOption(Setting<T> setting, T value);
	/**
	 * Resets the game to a new instance.
	 */
	void newGame();
	/**
	 * Sets the game according to the PGN definition.
	 * 
	 * @param pgn
	 */
	void setGame(String pgn);
	/**
	 * Sends the current position to the engine.
	 * 
	 * @param fen
	 */
	void position(String fen);
	/**
	 * Asks the engine to start searching the current position according to the specified parameters.
	 * 
	 * @param args
	 * @return The best move found.
	 */
	Move search(SearchArguments args);
	/**
	 * Asks the engine to stop searching and return the best move found up until that point.
	 * 
	 * @return The best move found.
	 */
	Move stop();
	/**
	 * Returns an observable object containing information about the results and statistics of the ongoing search.
	 * 
	 * @return
	 */
	SearchStatistics getSearchStats();
}
