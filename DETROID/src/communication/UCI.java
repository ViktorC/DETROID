package communication;

import util.KeyValuePair;

/**
 * An interface supporting the UCI protocol as specified by Stefan-Meyer Kahlen.
 * 
 * @author Viktor
 *
 */
public interface UCI {

	public final static String[] SEARCH_ARGS = new String[] { "searchmoves", "ponder", "wtime", "bTime", "winc", "binc", "movestogo", "depth",
		"nodes", "mate", "movetime", "infinite" };
	
	/**
	 * Tells the engine to switch to UCI mode.
	 * 
	 * @return Whether the engine successfully switched to UCI mode.
	 */
	boolean uci();
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
	 * Sets whether the engine should run in debug mode. In debug mode, the engine can (and is expected to) send detailed info strings
	 * such as search statistics.
	 * 
	 * @param on
	 */
	void debug(boolean on);
	/**
	 * Asks the engine whether it is done doing what the GUI commanded it to do and waits for the response.
	 * 
	 * @return
	 */
	boolean isReady();
	/**
	 * 
	 * 
	 * @return
	 */
	KeyValuePair<String, String>[] options();
	/**
	 * Sets an option defined by the engine to the specified value.
	 * 
	 * @param option
	 */
	void setOption(KeyValuePair<String, String> option);
	/**
	 * Resets the game to a new instance.
	 */
	void uciNewGame();
	/**
	 * Sends the current position to the engine.
	 * 
	 * @param fen
	 */
	void position(String fen);
	/**
	 * Asks the engine to start searching the current position according to the specified parameters.
	 * 
	 * @param params
	 * @return The best move found in long algebraic notation.
	 */
	String go(KeyValuePair<String, Object>[] params);
	/**
	 * Asks the engine to stop searching and return the best move found up until that point.
	 * 
	 * @return The best move found in long algebraic notation.
	 */
	String stop();
	/**
	 * Tells the engine that ponder move fed to the search was actually made, so the engine should search further but not in ponder mode
	 * anymore.
	 */
	void ponderHit();
	/**
	 * Asks the engine to shut down.
	 */
	void quit();
}
