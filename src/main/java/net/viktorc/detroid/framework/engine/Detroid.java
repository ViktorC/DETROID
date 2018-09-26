package net.viktorc.detroid.framework.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import net.viktorc.detroid.framework.engine.OpeningBook.SelectionModel;
import net.viktorc.detroid.framework.engine.Game.Side;
import net.viktorc.detroid.framework.engine.GaviotaTableBaseJNI.CompressionScheme;
import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.uci.DebugInformation;
import net.viktorc.detroid.framework.uci.Option;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchInformation;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.util.Cache;
import net.viktorc.detroid.framework.util.SizeEstimator;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A UCI compatible, tunable chess engine that utilizes magic bit boards and most search heuristics and supports Polyglot 
 * opening books.
 * 
 * @author Viktor
 *
 */
public class Detroid implements ControllerEngine, TunableEngine {
	
	static final float VERSION_NUMBER = 1.0f;
	static final String NAME = "DETROID " + VERSION_NUMBER;
	static final String AUTHOR = "Viktor Csomor";
	// Search, evaluation, and time control parameters.
	static final String DEFAULT_PARAMETERS_FILE_PATH = "params.xml";
	// The default path to the Polyglot opening book (compiled using SCID 4.62, PGN-Extract 17-21 and Polyglot 1.4w).
	static final String DEFAULT_BOOK_FILE_PATH = "book.bin";
	// The default path to the Gaviota probing library.
	static final String DEFAULT_EGTB_LIB_PATH;

	// Determine the default path to the Gaviota EGTB probing library based on the OS.
	static {
		boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
		boolean arch32 = "32".equals(System.getProperty("sun.arch.data.model"));
		String lib = windows ? arch32 ? "gtb32.dll" : "gtb.dll" :
				arch32 ? "libgtb32.so" : "libgtb.so";
		DEFAULT_EGTB_LIB_PATH = "gtb/" + lib;
	}
	
	// The default path to the 3 and 4 men Gaviota endgame tablebases.
	static final String DEFAULT_EGTB_FOLDERS_PATH = "gtb/3;gtb/4";
	// The default compression scheme of the Gaviotat endgame tablebase files.
	static final CompressionScheme DEFAULT_EGTB_COMP_SCHEME = CompressionScheme.CP4;
	// The minimum allowed number of search threads to use.
	static final int MIN_SEARCH_THREADS = 1;
	// The maximum allowed number of search threads to use.
	static final int MAX_SEARCH_THREADS = Runtime.getRuntime().availableProcessors();
	// The default number of search threads to use.
	static final int DEFAULT_SEARCH_THREADS = Math.max(MIN_SEARCH_THREADS, MAX_SEARCH_THREADS/2);
	// The minimum allowed hash size in MB.
	static final int MIN_HASH_SIZE = 1;
	// The maximum allowed hash size in MB.
	static final int MAX_HASH_SIZE = (int) (Runtime.getRuntime().maxMemory()/(2L << 20));
	// The default hash size in MB.
	static final int DEFAULT_HASH_SIZE = Math.min(DEFAULT_SEARCH_THREADS*32, MAX_HASH_SIZE);
	// The minimum allowed endgame tablebase cache size in MB.
	static final int MIN_EGTB_CACHE_SIZE = 0;
	// The maximum allowed endgame tablebase cache size in MB.
	static final int MAX_EGTB_CACHE_SIZE = Math.min(256, MAX_HASH_SIZE);
	// The default endgame tablebase cache size in MB.
	static final int DEFAULT_EGTB_CACHE_SIZE = Math.min(DEFAULT_SEARCH_THREADS*16, MAX_EGTB_CACHE_SIZE);
	
	private final Object lock;
	
	private Option<?> hashSize;
	private Option<?> clearHash;
	private Option<?> ponder;
	private Option<?> ownBook;
	private Option<?> primaryBookPath;
	private Option<?> secondaryBookPath;
	private Option<?> egtbLibPath;
	private Option<?> egtbFilesPath;
	private Option<?> egtbCompScheme;
	private Option<?> egtbCacheSize;
	private Option<?> egtbClearCache;
	private Option<?> numOfSearchThreads;
	private Option<?> parametersPath;
	private Option<?> uciOpponent;
	private Option<?> uciAnalysis;
	private Map<Option<?>, Object> options;
	
	private DetroidParameters params;
	private DetroidSearchInformation searchInfo;
	private DetroidDebugInfo debugInfo;
	private OpeningBook book;
	private EndGameTableBase egtb;
	private Game game;
	private Evaluator eval;
	private Cache<TTEntry> tT;
	private Cache<ETEntry> eT;
	private ExecutorService executor;
	private Future<SearchResults> search;
	private volatile int movesOutOfBook;
	private volatile boolean bookMove;
	private volatile boolean outOfBook;
	private volatile boolean init;
	private volatile boolean debugMode;
	private volatile boolean controllerMode;
	private volatile boolean deterministicZeroDepthMode;
	private volatile boolean stop;
	private volatile boolean ponderHit;
	private volatile boolean newGame;
	private volatile byte gen;
	
	/**
	 * Instantiates the chess engine object.
	 */
	public Detroid() {
		lock = new Object();
	}
	/**
	 * Sets the hash tables according to the hash size limit defined in megabytes.
	 * 
	 * @param hashSize
	 */
	private void setHashSize(int hashSize) {
		long sizeInBytes = hashSize*1024L*1024L;
		int totalHashShares = params.tTshare + params.eTshare;
		SizeEstimator estimator = SizeEstimator.getInstance();
		tT = new Cache<>(TTEntry::new,
				(int) (sizeInBytes*params.tTshare/totalHashShares/estimator.sizeOf(TTEntry.class)));
		eT = new Cache<>(ETEntry::new,
				(int) (sizeInBytes*params.eTshare/totalHashShares/estimator.sizeOf(ETEntry.class)));
		// Prompt for garbage collection.
		System.gc();
		if (debugMode) debugInfo.set("Hash capacity data\n" +
				"Transposition table capacity - " + tT.capacity() + "\n" +
				"Evaluation table capacity - " + eT.capacity());
	}
	/**
	 * Clears the transposition, pawn, evaluation, and history tables.
	 */
	private void clearHash() {
		tT.clear();
		eT.clear();
		gen = 0;
		if (debugMode) debugInfo.set("Hash tables cleared");
	}
	/**
	 * A logistic function for determining the expected number of moves left based on the mate score calculated 
	 * from the material on the board.
	 * 
	 * @param phaseScore The phase score calculated from the material on the board.
	 * @return The expected number of moves.
	 */
	private int movesLeftBasedOnPhaseScore(int phaseScore) {
		double L = params.maxMovesToGo - params.minMovesToGo;
		double exp = Math.pow(Math.E, 3.5e-2d*((double) (phaseScore - (params.gamePhaseOpeningLower +
				params.gamePhaseEndgameUpper)))/2);
		double res = L/(1d + exp);
		return (int) Math.round(res + params.minMovesToGo);
	}
	/**
	 * Computes and returns the number of milliseconds the engine should spend on searching the current position.
	 * 
	 * @param whiteTime
	 * @param blackTime
	 * @param whiteIncrement
	 * @param blackIncrement
	 * @param movesToGo
	 * @param phaseScore
	 * @return
	 */
	private long computeSearchTime(Long whiteTime, Long blackTime, Long whiteIncrement,
			Long blackIncrement, Integer movesToGo, int phaseScore) {
		whiteIncrement = whiteIncrement == null ? 0 : whiteIncrement;
		blackIncrement = blackIncrement == null ? 0 : blackIncrement;
		if ((game.getSideToMove() == Side.WHITE && (whiteTime == null || whiteTime <= 0) ||
				(game.getSideToMove() == Side.BLACK && (blackTime == null || blackTime <= 0))))
			return 0;
		if (movesToGo == null) {
			movesToGo = movesLeftBasedOnPhaseScore(phaseScore);
			if (debugMode) debugInfo.set("Search time data\n" +
					"Phase score - " + phaseScore + "/256\n" +
					"Expected number of moves left until end - " + movesToGo);
		}
		long target = game.getSideToMove() == Side.WHITE ?
				Math.max(1, (whiteTime + Math.max(0, movesToGo)*whiteIncrement)/Math.max(1, movesToGo)) :
				Math.max(1, (blackTime + Math.max(0, movesToGo)*blackIncrement)/Math.max(1, movesToGo));
		// Extended thinking time for the first moves out of the book based on Hyatt's Using Time Wisely.
		return Math.round(target*(2d - ((double) Math.min(movesOutOfBook, 10))/10));
	}
	/**
	 * Determines whether the search time should be extended.
	 * 
	 * @return
	 */
	private boolean doExtendSearch() {
		return searchInfo.getScoreType() == ScoreType.LOWER_BOUND || searchInfo.getScoreType() == ScoreType.UPPER_BOUND;
	}
	/**
	 * Determines whether the search should be cancelled without waiting for the allotted time to pass.
	 * 
	 * @param searchTime
	 * @param timeLeft
	 * @return
	 */
	private boolean doTerminateSearchPrematurely(long searchTime, long timeLeft) {
		return timeLeft < searchTime*params.minTimePortionNeededForExtraDepth64th/64 &&
				!doExtendSearch();
	}
	/**
	 * Manges the amount of time spent on searching the position unless it is fixed.
	 * 
	 * @param searchTime
	 * @param whiteTime
	 * @param blackTime
	 * @param whiteIncrement
	 * @param blackIncrement
	 * @param movesToGo
	 * @throws Exception
	 */
	private void manageSearchTime(Long searchTime, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo) throws Exception {
		try {
			boolean fixed = searchTime != null && searchTime > 0;
			if (debugMode) debugInfo.set("Search time fixed: " + fixed);
			if (fixed)
				search.get(searchTime, TimeUnit.MILLISECONDS);
			else {
				// Calculate phase score.
				int phaseScore = eval.phaseScore(game.getPosition());
				long time = computeSearchTime(whiteTime, blackTime, whiteIncrement, blackIncrement, movesToGo, phaseScore);
				long timeLeft = time;
				boolean doTerminate = false;
				if (debugMode) debugInfo.set("Base search time - " + time);
				synchronized (search) {
					while (!search.isDone() && timeLeft > 0 && !doTerminate) {
						long start = System.currentTimeMillis();
						search.wait(timeLeft);
						timeLeft -= (System.currentTimeMillis() - start);
						doTerminate = doTerminate || (!search.isDone() && timeLeft > 0 && doTerminateSearchPrematurely(time, timeLeft));
					}
				}
				if (debugMode) {
					String reason = search.isDone() ? "search terminated" : doTerminate ?
							"search cancelled to save time" : "time up";
					debugInfo.set(String.format("Base search over. Reason: %s", reason));
				}
				// If time was up, check if the search should be extended.
				if (!doTerminate && !search.isDone() && doExtendSearch()) {
					try {
						if (debugMode) debugInfo.set("Search extended.");
						search.get(computeSearchTime(game.getSideToMove() == Side.WHITE ? whiteTime - time :
								whiteTime, game.getSideToMove() == Side.WHITE ? blackTime : blackTime - time,
								whiteIncrement, blackIncrement, movesToGo, phaseScore), TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) {
						if (debugMode) debugInfo.set("Extra time up");
					} catch (CancellationException e) { }
				}
			}
		} finally {
			// Time is up, cancel the search.
			if (!search.isDone())
				search.cancel(true);
		}
	}
	/**
	 * Picks a move from the list of legal moves for the current position and returns a String representation of it in pure algebraic coordinate
	 * notation.
	 * 
	 * @return
	 */
	private String getRandomMove() {
		int i = 0;
		List<Move> moves = game.getPosition().getMoves();
		if (moves.size() == 0)
			return null;
		String[] arr = new String[moves.size()];
		for (Move m : moves)
			arr[i++] = m.toString();
		return arr[(int) (Math.random()*arr.length)];
	}
	@Override
	public void init() throws Exception {
		synchronized (lock) {
			params = new DetroidParameters();
			params.loadFrom(DEFAULT_PARAMETERS_FILE_PATH);
			try {
				book = new PolyglotBook(DEFAULT_BOOK_FILE_PATH);
			} catch (Exception e) { }
			egtb = GaviotaTableBaseJNI.getInstance();
			egtb.loadProbingLibrary(DEFAULT_EGTB_LIB_PATH);
			if (egtb.isProbingLibLoaded())
				egtb.init(DEFAULT_EGTB_FOLDERS_PATH, DEFAULT_EGTB_CACHE_SIZE*1024L*1024L,
						DEFAULT_EGTB_COMP_SCHEME);
			debugInfo = new DetroidDebugInfo();
			debugMode = false;
			controllerMode = false;
			deterministicZeroDepthMode = false;
			ponderHit = false;
			stop = false;
			game = new Game();
			options = new LinkedHashMap<>();
			parametersPath = new Option.StringOption("ParametersPath", DEFAULT_PARAMETERS_FILE_PATH);
			numOfSearchThreads = new Option.SpinOption(THREADS_OPTION_NAME, DEFAULT_SEARCH_THREADS, MIN_SEARCH_THREADS, MAX_SEARCH_THREADS);
			hashSize = new Option.SpinOption(HASH_OPTION_NAME, DEFAULT_HASH_SIZE, MIN_HASH_SIZE, MAX_HASH_SIZE);
			clearHash = new Option.ButtonOption("ClearHash");
			ownBook = new Option.CheckOption(OWN_BOOK_OPTION_NAME, true);
			primaryBookPath = new Option.StringOption("PolyglotBookPrimaryPath", DEFAULT_BOOK_FILE_PATH);
			secondaryBookPath = new Option.StringOption("PolyglotBookSecondaryPath", "");
			egtbLibPath = new Option.StringOption("GaviotaTbLibPath", DEFAULT_EGTB_LIB_PATH);
			egtbFilesPath = new Option.StringOption("GaviotaTbPath", DEFAULT_EGTB_FOLDERS_PATH);
			egtbCompScheme = new Option.ComboOption("GaviotaTbCompScheme", DEFAULT_EGTB_COMP_SCHEME.toString(),
					new TreeSet<>(Arrays.stream(CompressionScheme.values()).map(c -> c.toString()).collect(Collectors.toList())));
			egtbCacheSize = new Option.SpinOption("GaviotaTbCache", DEFAULT_EGTB_CACHE_SIZE, MIN_EGTB_CACHE_SIZE, MAX_EGTB_CACHE_SIZE);
			egtbClearCache = new Option.ButtonOption("GaviotaTbClearCache");
			ponder = new Option.CheckOption("Ponder", true);
			uciOpponent = new Option.StringOption("UCI_Opponent", "?");
			uciAnalysis = new Option.CheckOption("UCI_AnalyseMode", false);
			options.put(parametersPath, parametersPath.getDefaultValue().get());
			options.put(numOfSearchThreads, numOfSearchThreads.getDefaultValue().get());
			options.put(hashSize, hashSize.getDefaultValue().get());
			options.put(clearHash, null);
			options.put(ownBook, ownBook.getDefaultValue().get());
			options.put(primaryBookPath, primaryBookPath.getDefaultValue().get());
			options.put(secondaryBookPath, secondaryBookPath.getDefaultValue().get());
			options.put(egtbLibPath, egtbLibPath.getDefaultValue().get());
			options.put(egtbFilesPath, egtbFilesPath.getDefaultValue().get());
			options.put(egtbCompScheme, DEFAULT_EGTB_COMP_SCHEME);
			options.put(egtbCacheSize, egtbCacheSize.getDefaultValue().get());
			options.put(egtbClearCache, null);
			options.put(ponder, ponder.getDefaultValue().get());
			options.put(uciOpponent, uciOpponent.getDefaultValue().get());
			options.put(uciAnalysis, uciAnalysis.getDefaultValue().get());
			searchInfo = new DetroidSearchInformation();
			setHashSize(controllerMode || deterministicZeroDepthMode ? MIN_HASH_SIZE : DEFAULT_HASH_SIZE);
			eval = new Evaluator(params, controllerMode || deterministicZeroDepthMode ? null : eT);
			executor = Executors.newSingleThreadExecutor();
			init = true;
			
		}
	}
	@Override
	public boolean isInit() {
		return init;
	}
	@Override
	public String getName() {
		return NAME;
	}
	@Override
	public String getAuthor() {
		return AUTHOR;
	}
	@Override
	public Map<Option<?>,Object> getOptions() {
		return new LinkedHashMap<>(options);
	}
	@Override
	public <T> boolean setOption(Option<T> setting, T value) {
		synchronized (lock) {
			try {
				if (hashSize.equals(setting)) {
					int val = (Integer) value;
					if (MIN_HASH_SIZE <= val && MAX_HASH_SIZE >= val) {
						if (val != ((Integer) options.get(hashSize)).intValue()) {
							setHashSize(val);
							eval = new Evaluator(params, controllerMode || deterministicZeroDepthMode ? null : eT);
							options.put(hashSize, value);
						}
						if (debugMode) debugInfo.set("Hash size successfully set to " + value);
						return true;
					}
				} else if (clearHash.equals(setting)) {
					clearHash();
					return true;
				} else if (ponder.equals(setting)) {
					options.put(ponder, value);
					if (debugMode) debugInfo.set("Ponder successfully set to " + value);
					return true;
				} else if (ownBook.equals(setting)) {
					if (book != null) {
						options.put(ownBook, value);
						outOfBook = false;
						if (debugMode) debugInfo.set("Use of own book successfully set to " + value);
						return true;
					}
					if (debugMode) debugInfo.set("No book file found at " + options.get(primaryBookPath));
				} else if (primaryBookPath.equals(setting)) {
					try {
						String secondaryFilePath = book == null ? null : book.getSecondaryFilePath();
						PolyglotBook newBook = new PolyglotBook((String) value, secondaryFilePath);
						if (book != null) {
							try {
								book.close();
							} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
						}
						book = newBook;
						options.put(primaryBookPath, book.getPrimaryFilePath());
						outOfBook = false;
						if (debugMode) debugInfo.set("Primary book file path successfully set to " + value);
						return true;
					} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
				} else if (secondaryBookPath.equals(setting)) {
					if (book != null) {
						try {
							PolyglotBook newBook = new PolyglotBook(book.getPrimaryFilePath(), (String) value);
							try {
								book.close();
							} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
							book = newBook;
							options.put(secondaryBookPath, book.getSecondaryFilePath());
							outOfBook = false;
							if (debugMode) debugInfo.set("Secondary book file path successfully set to " + value);
							return true;
						} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
					}
				} else if (egtbLibPath.equals(setting)) {
					try {
						egtb.loadProbingLibrary((String) value);
						if (egtb.isProbingLibLoaded()) {
							options.put(egtbLibPath, value);
							if (debugMode) debugInfo.set("EGTB library path successfully set to " + value);
							return true;
						}
					} catch (Exception e) { if (debugMode) debugInfo.set(e.getMessage()); }
				} else if (egtbFilesPath.equals(setting)) {
					if (egtb.isProbingLibLoaded()) {
						egtb.init((String) value, ((Integer) options.get(egtbCacheSize))*1024L*1024L,
								((CompressionScheme) options.get(egtbCompScheme)));
						if (egtb.isInit()) {
							options.put(egtbFilesPath, value);
							if (debugMode) debugInfo.set("EGTB files path successfully set to " + value + "; available tablebases: " +
									egtb.areTableBasesAvailable(1) + ", " + egtb.areTableBasesAvailable(2) + ", " +
									egtb.areTableBasesAvailable(3) + ", " + egtb.areTableBasesAvailable(4) + ", " +
									egtb.areTableBasesAvailable(5));
							return true;
						} else
							egtb.init((String) options.get(egtbFilesPath), ((Integer) options
									.get(egtbCacheSize))*1024L*1024L, ((CompressionScheme) options
									.get(egtbCompScheme)));
					}
				} else if (egtbCompScheme.equals(setting)) {
					if (egtb.isProbingLibLoaded()) {
						String comp = (String) value;
						CompressionScheme scheme = Arrays.stream(CompressionScheme.values())
								.filter(c -> comp.equals(c.name()))
								.collect(Collectors.toList()).get(0);
						egtb.init((String) options.get(egtbFilesPath), ((Integer) options
								.get(egtbCacheSize))*1024L*1024L, scheme);
						if (egtb.isInit()) {
							options.put(egtbCompScheme, scheme);
							if (debugMode) debugInfo.set("EGTB compression scheme successfully set to " + value);
							return true;
						} else
							egtb.init((String) options.get(egtbFilesPath), ((Integer) options
									.get(egtbCacheSize))*1024L*1024L, ((CompressionScheme) options
									.get(egtbCompScheme)));
					}
				} else if (egtbCacheSize.equals(setting)) {
					if (egtb.isProbingLibLoaded()) {
						int val = (Integer) value;
						if (MIN_EGTB_CACHE_SIZE <= val && MAX_EGTB_CACHE_SIZE >= val) {
							if (val != ((Integer) options.get(egtbCacheSize)).intValue()) {
								egtb.init((String) options.get(egtbFilesPath), val*1024L*1024L,
										((CompressionScheme) options.get(egtbCompScheme)));
								options.put(egtbCacheSize, value);
							}
							if (egtb.isInit()) {
								if (debugMode) debugInfo.set("EGTB cache size successfully set to " + value);
								return true;
							}
						}
					}
				} else if (egtbClearCache.equals(setting)) {
					if (egtb.isProbingLibLoaded()) {
						egtb.clearCache();
						return true;
					}
				} else if (numOfSearchThreads.equals(setting)) {
					if (value != null && MIN_SEARCH_THREADS <= (Integer) value &&
							MAX_SEARCH_THREADS >= (Integer) value) {
						options.put(numOfSearchThreads, value);
						if (debugMode) debugInfo.set("Number of search threads successfully set to " + value);
						return true;
					}
				} else if (parametersPath.equals(setting)) {
					try {
						String filePath = (String) value;
						params.loadFrom(filePath);
						options.put(parametersPath, filePath);
						notifyParametersChanged();
						if (debugMode) debugInfo.set("Parameters file path successfully set to " + value);
						return true;
					} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
				} else if (uciOpponent.equals(setting)) {
					options.put(uciOpponent, value);
					if (debugMode) debugInfo.set("Opponent name successfully set to " + value);
					return true;
				} else if (uciAnalysis.equals(setting)) {
					options.put(uciAnalysis, value);
					if (debugMode) debugInfo.set("Analysis mode successfully set to " + value);
					return true;
				}
				if (debugMode) debugInfo.set("The setting was not accepted");
				return false;
			} catch (Exception e) {
				if (debugMode) debugInfo.set("The setting was not accepted\n" + e.getMessage());
				return false;
			}
		}
	}
	@Override
	public void setDebugMode(boolean on) {
		debugMode = on;
	}
	@Override
	public void newGame() {
		synchronized (lock) {
			newGame = true;
			outOfBook = false;
			movesOutOfBook = 0;
			if (!controllerMode && !deterministicZeroDepthMode)
				clearHash();
			if (egtb.isProbingLibLoaded())
				egtb.clearCache();
		}
	}
	@Override
	public boolean setPosition(String fen) {
		synchronized (lock) {
			try {
				Position pos = fen.equals(START_POSITION) ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
				// If the start position of the game is different or the engine got the new game signal, reset the game and the hash tables.
				if (newGame) {
					if (debugMode) debugInfo.set("New game set");
					game = new Game(pos);
				} else if (!game.getStartPos().toString().equals(pos.toString())) {
					newGame();
					if (debugMode) debugInfo.set("New game set due to new start position");
					game = new Game(pos);
				}
				// Otherwise just clear the obsolete entries from the hash tables.
				else {
					if (debugMode) debugInfo.set("Position set within the same game");
					gen++;
					if (!bookMove)
						movesOutOfBook++;
					if (!controllerMode && !deterministicZeroDepthMode) {
						if (gen == 127) {
							tT.clear();
							eT.clear();
							gen = 0;
						} else {
							tT.remove(e -> e.generation < gen - params.tTentryLifeCycle);
							eT.remove(e -> e.generation < gen - params.eTentryLifeCycle);
						}
					}
					game = new Game(game.getStartPos(), game.getEvent(), game.getSite(), game.getWhitePlayerName(), game.getBlackPlayerName());
				}
				return true;
			} catch (ChessParseException | NullPointerException e) {
				if (debugMode) debugInfo.set(e.getMessage());
				return false;
			}
		}
	}
	@Override
	public boolean play(String pacn) {
		synchronized (lock) {
			if (game.play(pacn)) {
				if (debugMode) debugInfo.set("Move \"" + pacn + "\" played successfully");
				return true;
			}
			if (debugMode) debugInfo.set("Move \"" + pacn + "\" could not be played");
			return false;
		}
	}
	@Override
	public SearchResults search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth, Long nodes, Integer mateDistance,
			Long searchTime, Boolean infinite) {
		synchronized (lock) {
			Set<Move> allowedMoves;
			int numOfSearchThreads;
			long bookSearchStart;
			boolean analysisMode;
			boolean doPonder, doInfinite;
			SearchResults results;
			doPonder = false;
			doInfinite = (infinite != null && infinite) || ((ponder == null || !ponder) && whiteTime == null && blackTime == null &&
					depth == null && nodes == null && mateDistance == null && searchTime == null);
			bookMove = false;
			stop = ponderHit = false;
			// Set the names of the players once it is known which colour we are playing.
			if (newGame) {
				if (game.getSideToMove() == Side.WHITE) {
					game.setWhitePlayerName(NAME);
					game.setBlackPlayerName((String) options.get(uciOpponent));
				} else {
					game.setWhitePlayerName((String) options.get(uciOpponent));
					game.setBlackPlayerName(NAME);
				}
				if (debugMode) debugInfo.set("Players' names set\n" +
						"White - " + game.getWhitePlayerName() + "\n" +
						"Black - " + game.getBlackPlayerName());
				newGame = false;
			}
			results = null;
			analysisMode = (Boolean) options.get(uciAnalysis);
			// Search the book if possible.
			if (book != null && !deterministicZeroDepthMode && !analysisMode && !outOfBook &&
					searchMoves == null && (ponder == null || !ponder) && depth == null && nodes == null &&
					mateDistance == null && searchTime == null && (infinite == null || !infinite) &&
					(Boolean) options.get(ownBook)) {
				bookSearchStart = System.currentTimeMillis();
				search = executor.submit(() -> {
					Move bookMove;
					try {
						bookMove = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
					} catch (Exception e) {
						bookMove = null;
					}
					return new SearchResults(bookMove == null ? null : bookMove.toString(), null, null, null);
				});
				if (debugMode) debugInfo.set("Book search started");
				try {
					results = search.get();
					bookMove = results != null && results.getBestMove() != null;
				} catch (InterruptedException | ExecutionException e1) {
					if (debugMode) debugInfo.set(e1.getMessage());
					Thread.currentThread().interrupt();
					return null;
				} catch (CancellationException e2) { }
				if (debugMode) debugInfo.set("Book search done");
				// If the book search has not been externally stopped, use the remaining time for a normal search if no move was found.
				if (!stop && !bookMove) {
					if (debugMode) debugInfo.set("No book move found. Out of book.");
					outOfBook = true;
					search(searchMoves, ponder, game.getSideToMove() == Side.WHITE ? (whiteTime != null ?
							whiteTime - (System.currentTimeMillis() - bookSearchStart) : null) : whiteTime,
							game.getSideToMove() == Side.WHITE ? blackTime : (blackTime != null ?
							blackTime - (System.currentTimeMillis() - bookSearchStart) : null), whiteIncrement,
							blackIncrement, movesToGo, depth, nodes, mateDistance, searchTime, infinite);
				}
			}
			// Run a game tree search.
			else {
				// Check if pondering is possible.
				if (ponder != null && ponder) {
					if (!(Boolean) options.get(this.ponder)) {
						if (debugMode) debugInfo.set("Ponder mode started with ponder option disallowed - Abort");
						return null;
					} else
						doPonder = true;
				}
				// Set root move restrictions if there are any.
				if (searchMoves != null && !searchMoves.isEmpty()) {
					allowedMoves = new HashSet<>();
					try {
						for (String s : searchMoves)
							allowedMoves.add(game.getPosition().parsePACN(s));
					} catch (ChessParseException | NullPointerException e) {
						if (debugMode) debugInfo.set("Search moves could not be parsed\n" + e.getMessage());
						return null;
					}
				} else
					allowedMoves = null;
				// Start the search.
				Search: {
					if (egtb.isProbingLibLoaded() && egtb.isInit())
						egtb.resetStats();
					numOfSearchThreads = deterministicZeroDepthMode ? 1 : (int) options.get(this.numOfSearchThreads);
					Search gameTreeSearch = new Search(game.getPosition(), params, eval, egtb, searchInfo,
							numOfSearchThreads, tT, gen, analysisMode, doPonder || doInfinite, depth == null ?
							(mateDistance == null ? Integer.MAX_VALUE : mateDistance) : depth, nodes == null ?
							Long.MAX_VALUE : nodes, allowedMoves);
					search = gameTreeSearch;
					executor.submit(gameTreeSearch);
					if (debugMode) debugInfo.set("Search started");
					// If in ponder mode, run the search until the ponder hit signal or until it is externally stopped. 
					if (doPonder) {
						if (debugMode) debugInfo.set("In ponder mode");
						while (!stop && !ponderHit) {
							try {
								lock.wait();
							} catch (InterruptedException e) {
								if (debugMode) debugInfo.set(e.getMessage());
								Thread.currentThread().interrupt();
								return null;
							}
						}
						if (debugMode) debugInfo.set("Ponder stopped");
						// If the search terminated due to a ponder hit, keep searching...
						if (ponderHit) {
							if (debugMode) debugInfo.set("Ponderhit acknowledged");
							ponderHit = false;
						}
						// If it did not, return the best move found.
						else {
							try {
								search.get();
							} catch (InterruptedException e) {
								if (debugMode) debugInfo.set(e.getMessage());
								Thread.currentThread().interrupt();
								return null;
							} catch (ExecutionException e) {
								if (debugMode) debugInfo.set(e.getMessage());
								return null;
							}
							break Search;
						}
					}
					// If in infinite mode, let the search run until it is externally stopped.
					else if (doInfinite) {
						if (debugMode) debugInfo.set("In infinite mode");
						try {
							search.get();
						} catch (InterruptedException e) {
							if (debugMode) debugInfo.set(e.getMessage());
							Thread.currentThread().interrupt();
							return null;
						} catch (ExecutionException e) {
							if (debugMode) debugInfo.set(e.getMessage());
							return null;
						}
						break Search;
					}
					// Fixed node or depth search
					else if (whiteTime == null && blackTime == null && searchTime == null) {
						if (debugMode) debugInfo.set("In fixed mode");
						try {
							search.get();
						} catch (InterruptedException e) {
							if (debugMode) debugInfo.set(e.getMessage());
							Thread.currentThread().interrupt();
							return null;
						} catch (ExecutionException e) {
							if (debugMode) debugInfo.set(e.getMessage());
							return null;
						}
						break Search;
					}
					try {
						// Time based search.
						manageSearchTime(searchTime, whiteTime, blackTime, whiteIncrement, blackIncrement, movesToGo);
					} catch (Exception e) {
						if (debugMode) debugInfo.set(e.getMessage());
						return null;
					}
				}
				if (debugMode) debugInfo.set("Search stopped");
			}
			// Return the final results.
			if (!bookMove) {
				try {
					results = search.get();
				} catch (InterruptedException e) {
					if (debugMode) debugInfo.set(e.getMessage());
					Thread.currentThread().interrupt();
					return null;
				} catch (ExecutionException e) {
					if (debugMode) debugInfo.set(e.getMessage());
					return null;
				}
				if (!deterministicZeroDepthMode && (results == null || results.getBestMove() == null))
					results = new SearchResults(getRandomMove(), null, null, null);
			}
			return results;
		}
	}
	@Override
	public void stop() {
		if (search != null) {
			if (debugMode) debugInfo.set("Stopping search...");
			stop = true;
			search.cancel(true);
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	@Override
	public void ponderHit() {
		if (debugMode) debugInfo.set("Signaling ponderhit...");
		ponderHit = true;
		synchronized (lock) {
			lock.notifyAll();
		}
	}
	@Override
	public SearchInformation getSearchInfo() {
		return searchInfo;
	}
	@Override
	public short getHashLoadPermill() {
		long transLoad, transCapacity;
		long evalLoad, evalCapacity;
		long totalLoad, totalCapacity;
		transLoad = tT.size();
		evalLoad = eT.size();
		totalLoad = transLoad + evalLoad;
		transCapacity = tT.capacity();
		evalCapacity = eT.capacity();
		totalCapacity = transCapacity + evalCapacity ;
		if (debugMode) {
			debugInfo.set(String.format("TT load factor - %.2f%nET load factor - %.2f",
					((float) transLoad)/transCapacity, ((float) evalLoad)/evalCapacity));
			debugInfo.set(String.format("Total hash size in MB - %.2f", (float) ((double) tT.memorySize() +
					eT.memorySize())/(1L << 20)));
		}
		/* Due to the non-thread-safe nature of the hash tables, incorrect size values may be returned; ensure 
		 * the load does not exceed 1000. */
		return (short) Math.min(1000, 1000*totalLoad/totalCapacity);
	}
	@Override
	public DebugInformation getDebugInfo() {
		return debugInfo;
	}
	@Override
	public void quit() {
		if (init) {
			if (debugMode) debugInfo.set("Shutting down...");
		} else {
			if (debugMode) debugInfo.set("The engine has not been initialized yet; cannot shut down.");
			return;
		}
		if (book != null) {
			try {
				book.close();
			} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
		}
		if (egtb.isProbingLibLoaded()) {
			try {
				egtb.close();
			} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
		}
		executor.shutdown();
		searchInfo.deleteObservers();
		tT = null;
		eT = null;
		init = false;
	}
	@Override
	public boolean isWhitesTurn() {
		return game.getSideToMove() == Side.WHITE;
	}
	@Override
	public String getStartPosition() {
		return game.getStartPos().toString();
	}
	@Override
	public GameState getGameState() {
		synchronized (lock) {
			return game.getState();
		}
	}
	@Override
	public List<String> getMoveHistory() {
		List<String> moves = new ArrayList<>();
		for (Move m : game.getPosition().moveList)
			moves.add(m.toString());
		Collections.reverse(moves);
		return moves;
	}
	@Override
	public List<String> getLegalMoves() {
		Position pos = game.getPosition();
		List<Move> moves = pos.getMoves();
		List<String> moveStrings = new ArrayList<>(moves.size());
		for (Move m : moves)
			moveStrings.add(m.toString());
		return moveStrings;
	}
	@Override
	public boolean setGame(String pgn) {
		synchronized (lock) {
			try {
				newGame();
				game = Game.parse(pgn);
				if (debugMode) debugInfo.set("Game successfully set to:\n" + pgn);
				return true;
			} catch (ChessParseException e) {
				if (debugMode) debugInfo.set("Game could not be set. Invalid PGN:\n" + pgn);
				return false;
			}
		}
	}
	@Override
	public void setPlayers(String whitePlayer, String blackPlayer) {
		game.setWhitePlayerName(whitePlayer);
		game.setBlackPlayerName(blackPlayer);
	}
	@Override
	public void setEvent(String event) {
		game.setEvent(event);
	}
	@Override
	public void setSite(String site) {
		game.setSite(site);
	}
	@Override
	public void setControllerMode(boolean on) {
		synchronized (lock) {
			controllerMode = on;
			if (init)
				setHashSize(on ? MIN_HASH_SIZE : DEFAULT_HASH_SIZE);
		}
	}
	@Override
	public void drawByAgreement() {
		game.setState(GameState.DRAW_BY_AGREEMENT);
	}
	@Override
	public void whiteForfeit() {
		game.setState(GameState.UNSPECIFIED_BLACK_WIN);
	}
	@Override
	public void blackForfeit() {
		game.setState(GameState.UNSPECIFIED_WHITE_WIN);
	}
	@Override
	public String unplayLastMove() {
		synchronized (lock) {
			return game.unplay();
		}
	}
	@Override
	public String convertPACNToSAN(String move) {
		try {
			Position pos = game.getPosition();
			return pos.toSAN(pos.parsePACN(move));
		} catch (ChessParseException | NullPointerException e) {
			if (debugMode) debugInfo.set("Error while converting PACN to SAN: " + e.getMessage());
			throw new IllegalArgumentException("The parameter move cannot be converted to SAN.", e);
		}
	}
	@Override
	public String convertSANToPACN(String move) {
		try {
			Position pos = game.getPosition();
			return pos.parseSAN(move).toString();
		} catch (ChessParseException | NullPointerException e) {
			if (debugMode) debugInfo.set("Error while converting SAN to PACN: " + e.getMessage());
			throw new IllegalArgumentException("The parameter move cannot be converted to PACN.", e);
		}
	}
	@Override
	public String toPGN() {
		synchronized (lock) {
			return game.toString();
		}
	}
	@Override
	public String toFEN() {
		synchronized (lock) {
			return game.getPosition().toString();
		}
	}
	@Override
	public long perft(int depth) {
		return game.getPosition().perft(depth);
	}
	@Override
	public EngineParameters getParameters() {
		return params;
	}
	@Override
	public void notifyParametersChanged() {
		synchronized (lock) {
			if (init)
				eval = new Evaluator(params, controllerMode || deterministicZeroDepthMode ? null : eT);
		}
	}
	@Override
	public void setDeterministicZeroDepthMode(boolean on) {
		synchronized (lock) {
			deterministicZeroDepthMode = on;
			if (init)
				setHashSize(on ? MIN_HASH_SIZE : DEFAULT_HASH_SIZE);
		}
	}
	
}
