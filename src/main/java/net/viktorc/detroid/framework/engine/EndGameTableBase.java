package net.viktorc.detroid.framework.engine;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
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
	synchronized void loadProbingLibrary(String path) {
		try {
			File file = new File(path);
			Path libPath = file.exists() ? file.toPath() :
					Paths.get(ClassLoader.getSystemClassLoader().getResource(path).toURI());
			System.load(libPath.toAbsolutePath().toString());
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
	synchronized boolean isProbingLibLoaded() {
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
	 * Returns some basic usage stats about the endgame tablebase.
	 * 
	 * @return Endgame tablebase stats.
	 */
	abstract EGTBStats getStats();
	/**
	 * Resets the endgame tablebase stats.
	 */
	abstract void resetStats();
	/**
	 * It probes for the given position and if found, it returns whether it is 
	 * a winning position, a losing position, or a draw; else it returns null.
	 * 
	 * @param pos The chess position to look for.
	 * @param soft Whether only the cache should be probed.
	 * @return  Whether it is a winning position, a losing position, or a draw.
	 */
	abstract WDL probeWDL(Position0 pos, boolean soft);
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
	abstract DTM probeDTM(Position0 pos, boolean soft);
	
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
	
	/**
	 * A simple container class for basic endgame tablebase probing stats.
	 * 
	 * @author Viktor
	 *
	 */
	static class EGTBStats {

		private final long totalHardProbes;
		private final long totalSoftProbes;
		private final long totalDriveHits;
		private final long totalCacheHits;
		
		EGTBStats(long totalHardProbes, long totalSoftProbes, long totalDriveHits, long totalCacheHits) {
			this.totalHardProbes = totalHardProbes;
			this.totalSoftProbes = totalSoftProbes;
			this.totalDriveHits = totalDriveHits;
			this.totalCacheHits = totalCacheHits;
		}
		/**
		 * Returns the total number of hard probes.
		 * 
		 * @return The total number of probes that may go to the drive.
		 */
		long getTotalHardProbes() {
			return totalHardProbes;
		}
		/**
		 * Returns the total number of soft probes.
		 * 
		 * @return The total number of probes only into the cache.
		 */
		long getTotalSoftProbes() {
			return totalSoftProbes;
		}
		/**
		 * Returns the total number of drive hits.
		 * 
		 * @return The number of drive hits.
		 */
		long getTotalDriveHits() {
			return totalDriveHits;
		}
		/**
		 * Return the total number of cache hits.
		 * 
		 * @return The number of memory hits.
		 */
		long getTotalCacheHits() {
			return totalCacheHits;
		}
		
	}
	
}
