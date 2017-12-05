package net.viktorc.detroid.framework.engine;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.viktorc.detroid.framework.util.BitOperations;

/**
 * A Gaviota tablebase probing implementation using JNI.
 * 
 * @author Viktor
 *
 */
class GaviotaTableBaseJNI extends EndGameTableBase {
	
	// Probing results.
	static final int DRAW = 0;
	static final int WMATE = 1;
	static final int BMATE = 2;
	static final int FORBID = 3;
	static final int UNKNOWN = 7;
	
	// Integer statistics indices.
	static final int WDL_EASY_HITS = 0;
	static final int WDL_HARD_PROB = 1;
	static final int WDL_SOFT_PROB = 2;
	static final int WDL_CACHE_SIZE = 3;
	static final int DTM_EASY_HITS = 4;
	static final int DTM_HARD_PROB = 5;
	static final int DTM_SOFT_PROB = 6;
	static final int DTM_CACHE_SIZE = 7;
	static final int TOTAL_HITS = 8;
	static final int MEMORY_HITS = 9;
	static final int DRIVE_HITS = 10;
	static final int DRIVE_MISSES = 11;
	static final int BYTES_READ = 12;
	static final int FILES_OPENED = 13;
	
	// Floating point statistics indices.
	static final int WDL_OCCUPANCY = 0;
	static final int DTM_OCCUPANCY = 1;
	static final int MEMORY_EFFICIENCY = 2;
	
	// Side to move.
	static final int WHITE_TO_MOVE = 0;
	static final int BLACK_TO_MOVE = 1;
	
	// Pieces.
	static final char NO_PIECE = 0;
	static final char PAWN = 1;
	static final char KNIGHT = 2;
	static final char BISHOP = 3;
	static final char ROOK = 4;
	static final char QUEEN = 5;
	static final char KING = 6;
	
	// Square.
	static final int NO_SQUARE = 64;
	
	// Castling masks.
	static final int NO_CASTLE = 0;
	static final int WSHORT = 8;
	static final int WLONG = 4;
	static final int BSHORT = 2;
	static final int BLONG = 1;
	
	/* The fraction (of 128) of the cache that should be devoted to storing WDL information; 
	 * The rest is to store DTM information. */
	private static final int WDL_FRACTION = 64;
	
	private static final GaviotaTableBaseJNI INSTANCE = new GaviotaTableBaseJNI();
	
	private final Lock probeLock;
	private boolean hasBeenInit;
	
	/**
	 * Singleton...
	 */
	private GaviotaTableBaseJNI() {
		probeLock = new ReentrantLock(true);
	}
	/**
	 * Returns the only instance of the class.
	 * 
	 * @return The only <code>GaviotaTableBaseJNI</code> instance.
	 */
	static GaviotaTableBaseJNI getInstance() {
		return INSTANCE;
	}
	/**
	 * Resolves the <code>WDL</code> status based on the specified parameters.
	 * 
	 * @param res The probing result integer.
	 * @param whitesTurn Whether it's white's turn to move.
	 * @return The <code>WDL</code> instance.
	 */
	private WDL resIntToWDL(int res, boolean whitesTurn) {
		switch (res) {
			case DRAW:
				return WDL.DRAW;
			case WMATE:
				return whitesTurn ? WDL.WIN : WDL.LOSS;
			case BMATE:
				return whitesTurn ? WDL.LOSS : WDL.WIN;
			default:
				return null;
		}
	}
	/**
	 * Probes the loaded tablebase files or cache for the specified position.
	 * 
	 * @param pos The position to look for.
	 * @param dtm Whether the result should include DTM information as well; which is more 
	 * expensive.
	 * @param soft Whether only the cache should be probed.
	 * @return If the position is found, a {@link net.viktorc.detroid.framework.engine.EndGameTableBase.DTM} 
	 * or {@link net.viktorc.detroid.framework.engine.EndGameTableBase.WDL} instance depending on 
	 * the <code>dtm</code> argument; else null.
	 */
	private Object probe(Position pos, boolean dtm, boolean soft) {
		boolean lockAcquired = false;
		int sideToMove = pos.whitesTurn ? 0 : 1;
		int enPassant = pos.enPassantRights == EnPassantRights.NONE.ind ? NO_SQUARE :
				pos.enPassantRights + (pos.whitesTurn ? EnPassantRights.TO_W_DEST_SQR_IND :
				EnPassantRights.TO_B_DEST_SQR_IND);
		int castling = (pos.blackCastlingRights%CastlingRights.ALL.ind == 0 ?
				pos.blackCastlingRights : pos.blackCastlingRights^CastlingRights.ALL.ind) |
				((pos.whiteCastlingRights%CastlingRights.ALL.ind == 0 ? pos.whiteCastlingRights :
				pos.whiteCastlingRights^CastlingRights.ALL.ind) << 2);
		byte[] wAllOccuppied = BitOperations.serialize(pos.allWhiteOccupied);
		int[] wSquares = new int[wAllOccuppied.length + 1];
		char[] wPieces = new char[wAllOccuppied.length + 1];
		int i = 0;
		for (; i < wAllOccuppied.length; i++) {
			int square = wAllOccuppied[i];
			wSquares[i] = square;
			byte piece = pos.offsetBoard[square];
			wPieces[i] = (char) (Piece.W_PAWN.ind - ((piece - 1)%Piece.W_PAWN.ind));
		}
		wSquares[i] = NO_SQUARE;
		wPieces[i] = NO_PIECE;
		byte[] bAllOccupied = BitOperations.serialize(pos.allBlackOccupied);
		int[] bSquares = new int[bAllOccupied.length + 1];
		char[] bPieces = new char[bAllOccupied.length + 1];
		i = 0;
		for (; i < bAllOccupied.length; i++) {
			int square = bAllOccupied[i];
			bSquares[i] = square;
			byte piece = pos.offsetBoard[square];
			bPieces[i] = (char) (Piece.W_PAWN.ind - ((piece - 1)%Piece.W_PAWN.ind));
		}
		bSquares[i] = NO_SQUARE;
		bPieces[i] = NO_PIECE;
		if (soft)
			lockAcquired = probeLock.tryLock();
		else {
			probeLock.lock();
			lockAcquired = true;
		}
		if (lockAcquired) {
			try {
				if (dtm) {
					int[] res = soft ? probeSoft(sideToMove, enPassant, castling, wSquares, bSquares,
							wPieces, bPieces) : probe(sideToMove, enPassant, castling, wSquares,
							bSquares, wPieces, bPieces);
					if (res == null)
						return null;
					return new DTM(resIntToWDL(res[0], pos.whitesTurn), res[1]);
				} else
					return resIntToWDL(soft ? probeSoftWDL(sideToMove, enPassant, castling, wSquares,
							bSquares, wPieces, bPieces) : probeWDL(sideToMove, enPassant, castling,
							wSquares, bSquares, wPieces, bPieces), pos.whitesTurn);
			} finally {
				probeLock.unlock();
			}
		} else
			return null;
	}
	/**
	 * Initializes the Gaviota probing library using the specified parameters.
	 * 
	 * @param verbose Whether information regarding the state of the library after 
	 * initialization should be returned.
	 * @param compScheme The compression scheme to use.
	 * @param paths The paths to the folders containing the tablebase files.
	 * @return Optionally, information regarding the state of the library.
	 */
	native String init(boolean verbose, int compScheme, String paths);
	/**
	 * Restarts the Gaviota probing library using the specified parameters.
	 * 
	 * @param verbose Whether information regarding the state of the library after 
	 * initialization should be returned.
	 * @param compScheme The compression scheme to use.
	 * @param paths The paths to the folders containing the tablebase files.
	 * @return Optionally, information regarding the state of the library.
	 */
	native String restart(boolean verbose, int compScheme, String paths);
	/**
	 * Returns an integer denoting for which numbers of pieces the library is 
	 * initialized for.
	 * 
	 * @return An integer denoting the availability of tablebase files.
	 */
	native int availability();
	/**
	 * Returns the amount of memory allocated for indices in bytes.
	 * 
	 * @return The number of bytes allocated for indices.
	 */
	native long indexMemory();
	/**
	 * Initializes the cache using the specified parameters.
	 * 
	 * @param size The cache size in bytes.
	 * @param wdlFraction The fraction of the cache (of a total of 128) that should be 
	 * reserved for WDL information.
	 * @return Whether the cache was successfully initialized.
	 */
	native boolean initCache(long size, int wdlFraction);
	/**
	 * Restarts the cache using the specified parameters.
	 * 
	 * @param size The cache size in bytes.
	 * @param wdlFraction The fraction of the cache (of a total of 128) that should be 
	 * reserved for WDL information.
	 * @return Whether the cache was successfully resterted.
	 */
	native boolean restartCache(long size, int wdlFraction);
	/**
	 * Returns whether the cache is on.
	 * 
	 * @return Whether the cache is on.
	 */
	native boolean isCacheOn();
	/**
	 * Closes the cache.
	 */
	native void closeCache();
	/**
	 * Sets the values of the two array parameters according to the statistics of 
	 * the probing library.
	 * 
	 * @param intStats A long array of length 14. The elements of the array after 
	 * the execution of the method contain the following statistics:</br>
	 * <code>[0] - WDL_EASY_HITS</code></br>
	 * <code>[1] - WDL_HARD_PROB</code></br>
	 * <code>[2] - WDL_SOFT_PROB</code></br>
	 * <code>[3] - WDL_CACHE_SIZE</code></br>
	 * <code>[4] - DTM_EASY_HITS</code></br>
	 * <code>[5] - DTM_HARD_PROB</code></br>
	 * <code>[6] - DTM_SOFT_PROB</code></br>
	 * <code>[7] - DTM_CACHE_SIZE</code></br>
	 * <code>[8] - TOTAL_HITS</code></br>
	 * <code>[9] - MEMORY_HITS</code></br>
	 * <code>[10] - DRIVE_HITS</code></br>
	 * <code>[11] - DRIVE_MISSES</code></br>
	 * <code>[12] - BYTES_READ</code></br>
	 * <code>[13] - FILES_OPENED</code>
	 * @param fpStats A double array of length 3. The elements of the array after 
	 * the execution of the method contain the following statistics:</br>
	 * <code>[0] - WDL_OCCUPANCY</code></br>
	 * <code>[1] - DTM_OCCUPANCY</code></br>
	 * <code>[2] - MEMORY_EFFICIENCY</code>
	 */
	native void getStats(long[] intStats, double[] fpStats);
	/**
	 * Resets the stored statistics of the probing library.
	 */
	native void resetStats();
	/**
	 * Probes the cache for both WDL and DTM information for the given position. If the 
	 * cache lookup fails, it proceeds to search the files.
	 * 
	 * @param sideToMove An integer denoting the side to move. <code>0</code> stands for 
	 * white, <code>1</code> stands for black.
	 * @param enPassantSqr The index of the destination square of a potential en passant 
	 * or {@link #NO_SQUARE} if none.
	 * @param catlingRights An integer representing the castling rights in the given 
	 * position. It is the union of the appropriate castles represented by {@link #NO_CASTLE}, 
	 * {@link #WSHORT}, {@link #WLONG}, {@link #BSHORT}, and {@link #BLONG}.
	 * @param wSqrs An array of the square indices of all the white pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param bSqrs An array of the square indices of all the black pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param wPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>wSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @param bPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>bSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @return An two-element integer array or <code>null</code> if the position was not 
	 * found. The first element of the array contains the WDL information while the second 
	 * one contains the distance to mate.
	 */
	native int[] probe(int sideToMove, int enPassantSqr, int catlingRights,
			int[] wSqrs, int[] bSqrs, char[] wPcs, char[] bPcs);
	/**
	 * Probes the cache for both WDL and DTM information for the given position. If the 
	 * cache lookup fails, it returns <code>null</code>.
	 * 
	 * @param sideToMove An integer denoting the side to move. <code>0</code> stands for 
	 * white, <code>1</code> stands for black.
	 * @param enPassantSqr The index of the destination square of a potential en passant 
	 * or {@link #NO_SQUARE} if none.
	 * @param catlingRights An integer representing the castling rights in the given 
	 * position. It is the union of the appropriate castles represented by {@link #NO_CASTLE}, 
	 * {@link #WSHORT}, {@link #WLONG}, {@link #BSHORT}, and {@link #BLONG}.
	 * @param wSqrs An array of the square indices of all the white pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param bSqrs An array of the square indices of all the black pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param wPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>wSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @param bPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>bSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @return An two-element integer array or null if the position was not found. The 
	 * first element of the array contains the WDL information while the second one 
	 * contains the distance to mate.
	 */
	native int[] probeSoft(int sideToMove, int enPassantSqr, int catlingRights,
			int[] wSqrs, int[] bSqrs, char[] wPcs, char[] bPcs);
	/**
	 * Probes the cache for WDL information for the given position. If the cache lookup 
	 * fails, it proceeds to search the files.
	 * 
	 * @param sideToMove An integer denoting the side to move. <code>0</code> stands for 
	 * white, <code>1</code> stands for black.
	 * @param enPassantSqr The index of the destination square of a potential en passant 
	 * or {@link #NO_SQUARE} if none.
	 * @param catlingRights An integer representing the castling rights in the given 
	 * position. It is the union of the appropriate castles represented by {@link #NO_CASTLE}, 
	 * {@link #WSHORT}, {@link #WLONG}, {@link #BSHORT}, and {@link #BLONG}.
	 * @param wSqrs An array of the square indices of all the white pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param bSqrs An array of the square indices of all the black pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param wPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>wSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @param bPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>bSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @return An integer array containing WDL information as defined by the constants 
	 * {@link #WMATE}, {@link #BMATE}, {@link #DRAW}, {@link #FORBID}, and {@link #UNKNOWN}.
	 */
	native int probeWDL(int sideToMove, int enPassantSqr, int catlingRights,
			int[] wSqrs, int[] bSqrs, char[] wPcs, char[] bPcs);
	/**
	 * Probes the cache for WDL information for the given position. If the cache lookup 
	 * fails, it returns {@link #UNKNOWN}.
	 * 
	 * @param sideToMove An integer denoting the side to move. <code>0</code> stands for 
	 * white, <code>1</code> stands for black.
	 * @param enPassantSqr The index of the destination square of a potential en passant 
	 * or {@link #NO_SQUARE} if none.
	 * @param catlingRights An integer representing the castling rights in the given 
	 * position. It is the union of the appropriate castles represented by {@link #NO_CASTLE}, 
	 * {@link #WSHORT}, {@link #WLONG}, {@link #BSHORT}, and {@link #BLONG}.
	 * @param wSqrs An array of the square indices of all the white pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param bSqrs An array of the square indices of all the black pieces and a final entry 
	 * containing {@link #NO_SQUARE}.
	 * @param wPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>wSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @param bPcs An array of the pieces as defined by the constants {@link #PAWN}, 
	 * {@link #KNIGHT}, {@link #BISHOP}, {@link #ROOK}, {@link #QUEEN}, and {@link #KING}. 
	 * Each element of the array denotes the piece on the square the element at the same 
	 * index in the <code>bSqrs</code> array represents. The array also contains a final 
	 * element denoting the end of the array with the value {@link #NO_PIECE}.
	 * @return An integer array containing WDL information as defined by the constants 
	 * {@link #WMATE}, {@link #BMATE}, {@link #DRAW}, {@link #FORBID}, and {@link #UNKNOWN}.
	 */
	native int probeSoftWDL(int sideToMove, int enPassantSqr, int catlingRights,
			int[] wSqrs, int[] bSqrs, char[] wPcs, char[] bPcs);
	@Override
	native boolean isInit();
	@Override
	native void clearCache();
	@Override
	public native void close();
	@Override
	boolean areTableBasesAvailable(int piecesOnBoard) {
		if (piecesOnBoard >= 3 && piecesOnBoard <= MAX_NUMBER_OF_PIECES) {
			long mask = 1L << (2*(piecesOnBoard - 3));
			return (availability() & mask) != 0;
		}
		return false;
	}
	@Override
	EGTBStats getStats() {
		long[] intStats = new long[14];
		double[] fpStats = new double[3];
		getStats(intStats, fpStats);
		return new EGTBStats(intStats[WDL_HARD_PROB] + intStats[DTM_HARD_PROB],
				intStats[WDL_SOFT_PROB] + intStats[DTM_SOFT_PROB],
				intStats[DRIVE_HITS], intStats[MEMORY_HITS]);
	}
	@Override
	void init(String path, long cacheSize, Object... args) {
		CompressionScheme compScheme = (CompressionScheme) args[0];
		String absPath = Arrays.stream(path.split(";")).collect(StringBuilder::new,
				(s, p) -> s.append(";").append(p), (s1, s2) -> s1.append(s2)).toString();
		if (!hasBeenInit) {
			init(false, compScheme.ordinal(), absPath);
			if (isInit()) {
				initCache(cacheSize, WDL_FRACTION);
				hasBeenInit = true;
			}
		} else {
			restart(false, compScheme.ordinal(), absPath);
			if (isInit())
				restartCache(cacheSize, WDL_FRACTION);
		}
	}
	@Override
	WDL probeWDL(Position pos, boolean soft) {
		return (WDL) probe(pos, false, soft);
	}
	@Override
	DTM probeDTM(Position pos, boolean soft) {
		return (DTM) probe(pos, true, soft);
	}
	
	/**
	 * Gaviota endgame tablebase file compression schemes.
	 * 
	 * @author Viktor
	 *
	 */
	enum CompressionScheme {
		
		UNCOMPRESSED,
		CP1,
		CP2,
		CP3,
		CP4;
		
	}
	
}
