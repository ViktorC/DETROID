package engine;

import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import util.Setting;

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
	 * Returns the load factor of the hash tables.
	 * 
	 * @return
	 */
	float getHashLoad();
	/**
	 * Returns the setting option the engine offers. Each option is defined by a key-value pair with the key being of type Setting and the value
	 * being an Object (the type parameter of the Setting instance).
	 * 
	 * @return
	 */
	Set<Map.Entry<Setting<?>, Object>> getOptions();
	/**
	 * Sets an option defined by the engine to the specified value.
	 * 
	 * @param setting
	 * @param value
	 * @return
	 */
	<T> boolean setOption(Setting<T> setting, T value);
	/**
	 * Sets the game according to the PGN definition.
	 * 
	 * @param pgn
	 * @return
	 */
	boolean setGame(String pgn);
	/**
	 * Sends the current position to the engine.
	 * 
	 * @param fen
	 * @return
	 */
	boolean position(String fen);
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
	 * @return The best move found in pure algebraic coordinate notation.
	 */
	String search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth,
			Long nodes, Short mateDistance, Long searchTime, Boolean infinite);
	/**
	 * Asks the engine to stop searching and return the best move found up until that point.
	 * 
	 * @return The best move found in pure algebraic coordinate notation.
	 */
	String stop();
	/**
	 * Returns an observable object containing information about the results and statistics of the ongoing search.
	 * 
	 * @return
	 */
	SearchInfo getInfo();
	
	public abstract class SearchInfo extends Observable {

		/**
		 * An enum for different score types that a search can return.
		 * 
		 * @author Viktor
		 *
		 */
		public enum ScoreType {

			EXACT,
			LOWER_BOUND,
			UPPER_BOUND,
			MATE
			
		}
		
		/**
		 * Returns a collection of the prinicipal variation with the moves in pure algebraic coordinate notation.
		 * 
		 * @return
		 */
		public abstract Collection<String> getPv();
		/**
		 * Returns the greatest nominal depth of the search.
		 * 
		 * @return
		 */
		public abstract short getDepth();
		/**
		 * Returns the result score of the search for the side to move.
		 * 
		 * @return
		 */
		public abstract short getScore();
		/**
		 * Returns whether it is an exact score, a lower bound, an upper bound, or a mate score, in which case the score denotes the mate
		 * distance in half moves. If the side to move in the root position is going to get mated, the negative distance is returned.
		 * 
		 * @return
		 */
		public abstract ScoreType getScoreType();
		/**
		 * Returns the number of nodes searched to reach this result.
		 * 
		 * @return
		 */
		public abstract long getNodes();
		/**
		 * Returns the time spent on the search to reach this result in milliseconds.
		 * 
		 * @return
		 */
		public abstract long getTime();

	}
}
