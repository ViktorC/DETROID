package net.viktorc.detroid.framework.engine;

import java.io.Closeable;
import java.nio.file.Paths;

/**
 * An abstract class for chess endgame tablebases. As there is no Java EGTB, it assumes 
 * the probing library to be a native shared library.
 * 
 * @author Viktor
 *
 */
abstract class EndGameTableBase implements Closeable {

	/**
	 * The maximum number of pieces on the board for which there are endgame tablebases.
	 */
	static final int MAX_NUMBER_OF_PIECES = 6;
	
	private boolean probingLibLoaded;
	
	/**
	 * Loads the probing library at the given path.
	 * 
	 * @param path The path to the shared library containing the probing code.
	 */
	void loadProbingLibrary(String path) {
		try {
			System.load(Paths.get(path).toAbsolutePath().toString());
			probingLibLoaded = true;
		} catch (Throwable e) {
			probingLibLoaded = false;
		}
	}
	/**
	 * Whether the last call to {@link #loadProbingLibrary(String)} resulted in the 
	 * successful loading of the library.
	 * 
	 * @return Whether the last library loading attempt has been successful.
	 */
	boolean isProbingLibLoaded() {
		return probingLibLoaded;
	}
	/**
	 * Initializes the probing code, potentially including the setting up of the 
	 * cache.
	 * 
	 * @param path The path to the folders containing the endgame tablebase files.
	 * @param cacheSize The size of the probing cache in bytes.
	 * @param args Optional arguments.
	 */
	abstract void init(String path, long cacheSize, Object... args);
	/**
	 * Returns whether the probing code has been initialized.
	 * 
	 * @return Whether the probing code has been initialized.
	 */
	abstract boolean isInit();
	/**
	 * Flushes the probing cache.
	 */
	abstract void clearCache();
	/**
	 * It returns whether there are at least some tablebase files loaded 
	 * for positions with the given number of pieces on the board.
	 * 
	 * @param piecesOnBoard The total number of pieces on the board.
	 * @return If there are tablebases loaded for the specified number 
	 * of men on the board.
	 */
	abstract boolean areTableBasesAvailable(int piecesOnBoard);
	/**
	 * It probes for the given position and if found, it returns whether it is 
	 * a winning position, a losing position, or a draw; else it returns null.
	 * 
	 * @param pos The chess position to look for.
	 * @param soft Whether only the cache should be probed.
	 * @return  Whether it is a winning position, a losing position, or a draw.
	 */
	abstract WDL probeWDL(Position pos, boolean soft);
	/**
	 * It probes for the given position and if found, it returns whether it is 
	 * a win, loss, or draw; and in case it is a win or loss, it also returns 
	 * the distance to mate. If the position is not found, it returns null.
	 * 
	 * @param pos The chess position to look for.
	 * @param soft Whether only the cache should be probed.
	 * @return  Whether it is a win, loss, or draw; and potentially the distance 
	 * to mate.
	 */
	abstract DTM probeDTM(Position pos, boolean soft);
	
	/**
	 * A simple enum for possible outcomes for positions in endgame tablebases.
	 * 
	 * @author Viktor
	 *
	 */
	enum WDL {
		
		WIN, DRAW, LOSS;
		
	}
	
	/**
	 * A class containing the outcome of a position in an endgame tablebase and potentially 
	 * the distance to mate from the position.
	 * 
	 * @author Viktor
	 *
	 */
	static class DTM {
		
		private final WDL wdl;
		private final int distance;
		
		/**
		 * Constructs an instance with the specified parameters.
		 * 
		 * @param wdl The outcome of the position.
		 * @param distance The distance to mate.
		 */
		DTM(WDL wdl, int distance) {
			super();
			this.wdl = wdl;
			this.distance = distance;
		}
		/**
		 * Returns the outcome of the position. I.e. win, draw, or loss.
		 * 
		 * @return The outcome.
		 */
		WDL getWdl() {
			return wdl;
		}
		/**
		 * Returns the distance to mate from the position.
		 * 
		 * @return The distance to mate.
		 */
		int getDistance() {
			return distance;
		}
		@Override
		public String toString() {
			return String.format("WDL: %s; DTM: %d", wdl == null ? "?" : wdl.name(), distance);
		}
		
	}
	
}
