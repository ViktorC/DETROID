package communication;

import java.util.Observer;

import util.KeyValuePair;

/**
 * An interface supporting the UCI protocol as specified by Stefan-Meyer Kahlen.
 * 
 * @author Viktor
 *
 */
public interface UCI {

	/**
	 * The possible parameters in a 'go' command.
	 * 
	 * @author Viktor
	 *
	 */
	public enum SearchAttributes {
		
		SEARCHMOVES,
		PONDER,
		WTIME,
		BTIME,
		WINC,
		BINC,
		MOVESTOGO,
		DEPTH,
		NODES,
		MATE,
		MOVETIME,
		INFINITE
		
	}
	/**
	 * The possible parameters sent to the GUI in info strings when in debug mode.
	 * 
	 * @author Viktor
	 *
	 */
	public enum InfoAttributes {
		
		DEPTH,
		SELDEPTH,
		TIME,
		NODES,
		PV,
		MULTIPV,
		SCORE,
		CURRMOVE,
		CURRMOVENUMBER,
		HASHFULL,
		NPS,
		TBHITS,
		CPULOAD,
		STRING,
		REFUTATION,
		CURRLINE
		
	}
	/**
	 * The possible types of the score attribute from the info attributes.
	 * 
	 * @author Viktor
	 *
	 */
	public enum InfoScoreTypes {
		
		CP,
		MATE,
		LOWERBOUND,
		UPPERBOUND
		
	}
	/**
	 * The attributes that can and have to be specified (depending on the type of the option) in the 'option' command.
	 * 
	 * @author Viktor
	 *
	 */
	public enum OptionAttributes {
		
		NAME,
		TYPE,
		DEFAULT_,
		MIN,
		MAX,
		VAR
		
	}
	/**
	 * The different values the 'type' attribute from the option attributes can take on.
	 * 
	 * @author Viktor
	 *
	 */
	public enum OptionTypes {
		
		CHECK,
		SPIN,
		COMBO,
		BUTTON,
		STRING
		
	}
	
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
	 * Necessary for the GUI to be provided with up to date info strings in debug mode.
	 * 
	 * @param observer
	 */
	void subscribe(Observer observer);
	/**
	 * Sets whether the engine should run in debug mode. In debug mode, the engine can (and is expected to) send detailed info strings
	 * such as search statistics to the GUI.
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
	 * Returns the options the engine offers. Each option is defined by an array of KeyValuePairs of varying numbers of elements specifying the
	 * name, type, default value, etc. of the option.
	 * 
	 * @return
	 */
	Iterable<Iterable<KeyValuePair<OptionAttributes, ?>>> options();
	/**
	 * Sets an option defined by the engine to the specified value.
	 * 
	 * @param optionName
	 * @param value
	 */
	void setOption(OptionAttributes option, Object value);
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
	String go(Iterable<KeyValuePair<SearchAttributes, ?>> params);
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
