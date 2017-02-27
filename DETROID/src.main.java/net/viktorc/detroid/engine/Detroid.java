package net.viktorc.detroid.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.viktorc.detroid.engine.Book.SelectionModel;
import net.viktorc.detroid.engine.Game.Side;
import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.uci.DebugInformation;
import net.viktorc.detroid.framework.uci.Option;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchInformation;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.GameState;
import net.viktorc.detroid.util.LossyHashTable;
import net.viktorc.detroid.util.SizeEstimator;

/**
 * A UCI compatible, tunable chess engine that utilizes magic bit boards and most search heuristics and supports Polyglot 
 * opening books.
 * 
 * @author Viktor
 *
 */
public class Detroid implements ControllerEngine, TunableEngine, Observer {
	
	public final static float VERSION_NUMBER = 1.0f;
	public final static String NAME = "DETROID " + VERSION_NUMBER;
	public final static String AUTHOR = "Viktor Csomor";
	// Search, evaluation, and time control parameters.
	public final static String DEFAULT_PARAMETERS_FILE_PATH = "params.xml";
	// An own opening book compiled using SCID 4.62, PGN-Extract 17-21 and Polyglot 1.4w.
	public final static String DEFAULT_BOOK_FILE_PATH = "book.bin";
	// The minimum allowed hash size in MB.
	private final static int MIN_HASH_SIZE = 1;
	// The maximum allowed hash size in MB.
	private final static int MAX_HASH_SIZE = (int) (Runtime.getRuntime().maxMemory()/(2L << 20));
	
	private Option<?> hashSize;
	private Option<?> clearHash;
	private Option<?> ponder;
	private Option<?> ownBook;
	private Option<?> primaryBookPath;
	private Option<?> secondaryBookPath;
	private Option<?> parametersPath;
	private Option<?> uciOpponent;
	private Map<Option<?>, Object> options;
	
	private Params params;
	private Game game;
	private DebugInfo debugInfo;
	private Book book;
	private Evaluator eval;
	private RelativeHistoryTable hT;
	// Transposition table.
	private LossyHashTable<TTEntry> tT;
	// Evaluation hash table.
	private LossyHashTable<ETEntry> eT;
	private SearchInfo searchStats;
	private Move searchResult;
	private Short scoreFluctuation;
	private Long timeOfLastSearchResChange;
	private Integer numOfSearchResChanges;
	private ExecutorService executor;
	private Future<?> search;
	private ReadWriteLock searchResLock;
	private volatile boolean isInit;
	private volatile boolean debugMode;
	private volatile boolean controllerMode;
	private volatile boolean deterministicZeroDepthMode;
	private volatile boolean stop;
	private volatile boolean cancelledResultsReady;
	private volatile boolean ponderHit;
	private volatile boolean newGame;
	private volatile boolean outOfBook;
	private volatile byte gen;
	
	public Detroid() {
		
	}
	/**
	 * Sets the hash tables according to the hash size limit defined in megabytes.
	 * 
	 * @param hashSize
	 */
	private void setHashSize(int hashSize) {
		long sizeInBytes = hashSize*1024*1024;
		int totalHashShares = params.tTshare + params.eTshare;
		SizeEstimator estimator = SizeEstimator.getInstance();
		tT = new LossyHashTable<>((int) (sizeInBytes*params.tTshare/totalHashShares/estimator.sizeOf(TTEntry.class)));
		eT = new LossyHashTable<>((int) (sizeInBytes*params.eTshare/totalHashShares/estimator.sizeOf(ETEntry.class)));
		if (debugMode) debugInfo.set("Hash capacity data\n" +
				"Transposition table capacity - " + tT.getCapacity() + "\n" +
				"Evaluation table capacity - " + eT.getCapacity());
	}
	/**
	 * Clears the transposition, pawn, evaluation, and history tables.
	 */
	private void clearHash() {
		tT.clear();
		eT.clear();
		hT.reset();
		gen = 0;
		if (debugMode) debugInfo.set("Hash tables cleared");
	}
	/**
	 * Computes and returns the number of milliseconds the engine should initially spend on searching the current position.
	 * 
	 * @param whiteTime
	 * @param blackTime
	 * @param whiteIncrement
	 * @param blackIncrement
	 * @param movesToGo
	 * @return
	 */
	private long computeSearchTime(Long whiteTime, Long blackTime, Long whiteIncrement, Long blackIncrement, Integer movesToGo) {
		int phaseScore;
		whiteIncrement = whiteIncrement == null ? 0 : whiteIncrement;
		blackIncrement = blackIncrement == null ? 0 : blackIncrement;
		if ((game.getSideToMove() == Side.WHITE && (whiteTime == null || whiteTime <= 0) ||
				(game.getSideToMove() == Side.BLACK && (blackTime == null || blackTime <= 0))))
			return 0;
		if (movesToGo == null) {
			phaseScore = eval.phaseScore(game.getPosition());
			movesToGo = params.avgMovesPerGame - params.avgMovesPerGame*phaseScore/params.gamePhaseEndgameUpper +
					params.movesToGoSafetyMargin;
			if (debugMode) debugInfo.set("Search time data\n" +
					"Phase score - " + phaseScore + "/256\n" +
					"Expected number of moves left until end - " + movesToGo);
		}
		return game.getSideToMove() == Side.WHITE ?
				Math.max(1, (whiteTime + Math.max(0, movesToGo - 1)*whiteIncrement)*
				params.fractionOfTotalTimeToUseHth/100/(movesToGo + 1)) :
				Math.max(1, (blackTime + Math.max(0, movesToGo - 1)*blackIncrement)*
				params.fractionOfTotalTimeToUseHth/100/(movesToGo + 1));
	}
	/**
	 * Computes and returns the search time extension in milliseconds if the search should be extended at all.
	 * 
	 * @param origSearchTime
	 * @param whiteTime
	 * @param blackTime
	 * @param whiteIncrement
	 * @param blackIncrement
	 * @param movesToGo
	 * @return
	 */
	private long computeSearchTimeExtension(long origSearchTime, Long whiteTime, Long blackTime, Long whiteIncrement,
			Long blackIncrement, Integer movesToGo) {
		searchResLock.readLock().lock();
		try {
			if (debugMode)
				debugInfo.set("Search time extension data\n" +
						"PV root move invalid - " + searchResult.equals(new Move()) + "\n" +
						"Score fluctuation - " + Math.abs(scoreFluctuation) + "\n" +
						"Score type - " + searchStats.getScoreType() + "\n" +
						"Last PV root change " + (System.currentTimeMillis() - timeOfLastSearchResChange) + "ms ago\n" +
						"Number of PV root changes per depth - " + numOfSearchResChanges/searchStats.getDepth());
			if (searchResult.equals(new Move()) || (game.getSideToMove() == Side.WHITE ?
					whiteTime - origSearchTime > 1000*params.movesToGoSafetyMargin :
					blackTime - origSearchTime > 1000*params.movesToGoSafetyMargin) &&
					(searchStats.getScoreType() == ScoreType.LOWER_BOUND || searchStats.getScoreType() == ScoreType.UPPER_BOUND ||
					Math.abs(scoreFluctuation) >= params.scoreFluctuationLimit || timeOfLastSearchResChange >= System.currentTimeMillis() -
					origSearchTime*params.fractionOfOrigSearchTimeSinceLastResultChangeLimitHth/100 ||
					numOfSearchResChanges/searchStats.getDepth() >= params.resultChangesPerDepthLimit)) {
				return computeSearchTime(game.getSideToMove() == Side.WHITE ?  new Long(whiteTime - origSearchTime) : whiteTime,
						game.getSideToMove() == Side.WHITE ? blackTime : new Long(blackTime - origSearchTime),
						whiteIncrement, blackIncrement, movesToGo);
			}
			return 0;
		} finally {
			searchResLock.readLock().unlock();
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
	public synchronized void init() throws Exception {
		params = new Params();
		params.loadFrom(DEFAULT_PARAMETERS_FILE_PATH);
		try {
			book = new PolyglotBook(DEFAULT_BOOK_FILE_PATH);
		} catch (IOException e) { }
		debugInfo = new DebugInfo();
		debugMode = false;
		controllerMode = false;
		deterministicZeroDepthMode = false;
		ponderHit = false;
		stop = false;
		outOfBook = false;
		game = new Game();
		options = new LinkedHashMap<>();
		hashSize = new Option.SpinOption(HASH_SIZE_OPTION_NAME, Math.min(params.defaultHashSize, MAX_HASH_SIZE),
				MIN_HASH_SIZE, MAX_HASH_SIZE);
		clearHash = new Option.ButtonOption("ClearHash");
		ponder = new Option.CheckOption("Ponder", true);
		ownBook = new Option.CheckOption(USE_OWN_BOOK_OPTION_NAME, false);
		primaryBookPath = new Option.StringOption("PrimaryBookPath", DEFAULT_BOOK_FILE_PATH);
		secondaryBookPath = new Option.StringOption("SecondaryBookPath", "");
		parametersPath = new Option.StringOption("ParametersPath", DEFAULT_PARAMETERS_FILE_PATH);
		uciOpponent = new Option.StringOption("UCI_Opponent", "?");
		options.put(hashSize, hashSize.getDefaultValue().get());
		options.put(clearHash, null);
		options.put(ponder, ponder.getDefaultValue().get());
		options.put(ownBook, ownBook.getDefaultValue().get());
		options.put(primaryBookPath, primaryBookPath.getDefaultValue().get());
		options.put(secondaryBookPath, secondaryBookPath.getDefaultValue().get());
		options.put(parametersPath, parametersPath.getDefaultValue().get());
		options.put(uciOpponent, uciOpponent.getDefaultValue().get());
		searchStats = new SearchInfo();
		searchStats.addObserver(this);
		setHashSize(controllerMode || deterministicZeroDepthMode ? MIN_HASH_SIZE : params.defaultHashSize);
		hT = new RelativeHistoryTable(params);
		eval = new Evaluator(params, controllerMode || deterministicZeroDepthMode ? null : eT);
		executor = Executors.newSingleThreadExecutor();
		searchResLock = new ReentrantReadWriteLock();
		isInit = true;
	}
	@Override
	public boolean isInit() {
		return isInit;
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
	public synchronized <T> boolean setOption(Option<T> setting, T value) {
		try {
			if (hashSize.equals(setting)) {
				if (hashSize.getMin().get().intValue() <= ((Integer) value).intValue() &&
						hashSize.getMax().get().intValue() >= ((Integer) value).intValue()) {
					if (((Integer) value).intValue() != ((Integer) options.get(hashSize)).intValue()) {
						options.put(hashSize, value);
						setHashSize(((Integer) value).intValue());
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
					if (debugMode) debugInfo.set("Use of own book successfully set to " + value);
					return true;
				}
				if (debugMode) debugInfo.set("No book file found at " + options.get(primaryBookPath));
			} else if (primaryBookPath.equals(setting)) {
				try {
					PolyglotBook newBook = new PolyglotBook((String) value, book.getSecondaryFilePath());
					book.close();
					book = newBook;
					options.put(primaryBookPath, book.getPrimaryFilePath());
					if (debugMode) debugInfo.set("Primary book file path successfully set to " + value);
					return true;
				} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
			} else if (secondaryBookPath.equals(setting)) {
				try {
					PolyglotBook newBook = new PolyglotBook(book.getPrimaryFilePath(), (String) value);
					book.close();
					book = newBook;
					options.put(secondaryBookPath, book.getSecondaryFilePath());
					if (debugMode) debugInfo.set("Secondary book file path successfully set to " + value);
					return true;
				} catch (IOException e) { if (debugMode) debugInfo.set(e.getMessage()); }
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
			}
			if (debugMode) debugInfo.set("The setting was not accepted");
			return false;
		} catch (Exception e) {
			if (debugMode) debugInfo.set("The setting was not accepted\n" + e.getMessage());
			return false;
		}
	}
	@Override
	public void setDebugMode(boolean on) {
		debugMode = on;
	}
	@Override
	public synchronized void newGame() {
		newGame = true;
		outOfBook = false;
		if (!controllerMode && !deterministicZeroDepthMode)
			clearHash();
	}
	@Override
	public synchronized boolean setPosition(String fen) {
		try {
			Position pos = fen.equals("startpos") ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			// If the start position of the game is different or the engine got the new game signal, reset the game and the hash tables.
			if (newGame) {
				if (debugMode) debugInfo.set("New game set");
				game = new Game(pos);
			} else if (!game.getStartPos().toString().equals(pos.toString())) {
				newGame();
				if (debugMode) debugInfo.set("New game set due to new start position");
				game = new Game(pos);
			}
			// Otherwise just clear the 'ancient' entries from the hash tables and decrement the history values.
			else {
				if (debugMode) debugInfo.set("Position set within the same game");
				gen++;
				if (!controllerMode && !deterministicZeroDepthMode) {
					if (gen == 127) {
						tT.clear();
						eT.clear();
						gen = 0;
					} else {
						tT.remove(e -> e.generation < gen - params.tTentryLifeCycle);
						eT.remove(e -> e.generation < gen - params.eTentryLifeCycle);
					}
					hT.decreaseCurrentValues();
				}
				game = new Game(game.getStartPos(), game.getEvent(), game.getSite(), game.getWhitePlayerName(), game.getBlackPlayerName());
			}
			return true;
		} catch (ChessParseException | NullPointerException e) {
			if (debugMode) debugInfo.set(e.getMessage());
			return false;
		}
	}
	@Override
	public synchronized boolean play(String pacn) {
		if (game.play(pacn)) {
			if (debugMode) debugInfo.set("Move \"" + pacn + "\" played successfully");
			return true;
		}
		if (debugMode) debugInfo.set("Move \"" + pacn + "\" could not be played");
		return false;
	}
	@Override
	public synchronized SearchResults search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth, Long nodes, Integer mateDistance,
			Long searchTime, Boolean infinite) {
		Set<Move> moves;
		String[] pV;
		String bestMove, ponderMove;
		long time, extraTime, bookSearchStart;
		boolean doPonder, doInfinite;
		boolean isBookMove;
		doPonder = false;
		doInfinite = infinite != null && infinite;
		isBookMove = false;
		stop = ponderHit = false;
		// Reset search stats.
		searchResLock.writeLock().lock();
		try {
			searchResult = new Move();
			scoreFluctuation = 0;
			timeOfLastSearchResChange = System.currentTimeMillis();
			numOfSearchResChanges = 0;
		} finally {
			searchResLock.writeLock().unlock();
		}
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
		// Search the book if possible.
		if ((Boolean) options.get(ownBook) && !deterministicZeroDepthMode && !outOfBook && searchMoves == null &&
				(ponder == null || !ponder) && depth == null && nodes == null && mateDistance == null && searchTime == null &&
				(infinite == null || !infinite)) {
			bookSearchStart = System.currentTimeMillis();
			search = executor.submit(() -> {
				Move searchResult = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
				searchResult = searchResult == null ? Move.NULL_MOVE : searchResult;
				return searchResult;
			});
			if (debugMode) debugInfo.set("Book search started");
			try {
				searchResult = (Move) search.get();
				isBookMove = !searchResult.equals(Move.NULL_MOVE);
			} catch (InterruptedException | ExecutionException e1) {
				if (debugMode) debugInfo.set(e1.getMessage());
			} catch (CancellationException e2) { }
			if (debugMode) debugInfo.set("Book search done");
			ponderMove = null;
			// If the book search has not been externally stopped, use the remaining time for a normal search if no move was found.
			if (!stop && searchResult.equals(Move.NULL_MOVE)) {
				if (searchResult.equals(new Move())) {
					if (debugMode) debugInfo.set("No book move found. Out of book.");
					outOfBook = true;
					search(searchMoves, ponder, game.getSideToMove() == Side.WHITE ? (whiteTime != null ?
							whiteTime - (System.currentTimeMillis() - bookSearchStart) : null) : whiteTime,
							game.getSideToMove() == Side.WHITE ? blackTime : (blackTime != null ?
							blackTime - (System.currentTimeMillis() - bookSearchStart) : null), whiteIncrement,
							blackIncrement, movesToGo, depth, nodes, mateDistance, searchTime, infinite);
				}
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
				moves = new HashSet<>();
				try {
					for (String s : searchMoves)
						moves.add(game.getPosition().parsePACN(s));
				} catch (ChessParseException | NullPointerException e) {
					if (debugMode) debugInfo.set("Search moves could not be parsed\n" + e.getMessage());
					return null;
				}
			} else
				moves = null;
			// Start the search.
			Search: {
				search = executor.submit(new Search(game.getPosition(), searchStats, doPonder || doInfinite,
						depth == null ? (mateDistance == null ?  Integer.MAX_VALUE : mateDistance) : depth,
						nodes == null ? Long.MAX_VALUE : nodes, moves, eval, hT, gen, tT, params));
				if (debugMode) debugInfo.set("Search started");
				// If in ponder mode, run the search until the ponderhit signal or until it is externally stopped. 
				if (doPonder) {
					if (debugMode) debugInfo.set("In ponder mode");
					while (!stop && !ponderHit) {
						try {
							wait();
						} catch (InterruptedException e) { if (debugMode) debugInfo.set(e.getMessage()); }
					}
					if (debugMode) debugInfo.set("Ponder stopped");
					// If the search terminated due to a ponderhit, keep searching...
					if (ponderHit) {
						if (debugMode) debugInfo.set("Ponderhit acknowledged");
						ponderHit = false;
					}
					// If it did not, return the best move found.
					else {
						searchResLock.readLock().lock();
						if (debugMode) debugInfo.set("Returning best move after ponderring was terminated.");
						try {
							return searchResult.equals(Move.NULL_MOVE) ? new SearchResults(getRandomMove().toString(),
									null, null, null) : new SearchResults(searchResult.toString(), null, searchStats.getScore(),
									searchStats.getScoreType());
						} finally {
							searchResLock.readLock().unlock();
						}
					}
				}
				// If in infinite mode, let the search run until it is externally stopped.
				else if (doInfinite) {
					if (debugMode) debugInfo.set("In infinite mode");
					while (!stop) {
						try {
							wait();
						} catch (InterruptedException e) { if (debugMode) debugInfo.set(e.getMessage()); }
					}
					break Search;
				}
				// Fixed node or depth search
				else if (whiteTime == null && blackTime == null && searchTime == null) {
					if (debugMode) debugInfo.set("In fixed mode");
					try {
						search.get();
					} catch (InterruptedException | ExecutionException e1) {
						if (debugMode) debugInfo.set(e1.getMessage());
					} catch (CancellationException e2) { }
					break Search;
				}
				// Compute search time and wait for the search to terminate for the computed amount of time.
				time = searchTime == null || searchTime == 0 ?
						computeSearchTime(whiteTime, blackTime, whiteIncrement, blackIncrement, movesToGo) : searchTime;
				if (debugMode) debugInfo.set("Base search time - " + time);
				try {
					search.get(time, TimeUnit.MILLISECONDS);
				} catch (InterruptedException | ExecutionException e1) {
					if (debugMode) debugInfo.set(e1.getMessage());
				} catch (TimeoutException e2) {
					if (debugMode) debugInfo.set("Base search time up");
				} catch (CancellationException e3) { }
				// If time was up, check if the search should be extended.
				if (!search.isDone() && searchTime == null) {
					try {
						extraTime = computeSearchTimeExtension(time, whiteTime, blackTime, whiteIncrement, blackIncrement, movesToGo);
						if (debugMode) debugInfo.set("Extra search time - " + extraTime);
						if (extraTime > 0)
							search.get(extraTime, TimeUnit.MILLISECONDS);
					} catch (InterruptedException | ExecutionException e1) {
						if (debugMode) debugInfo.set(e1.getMessage());
					} catch (TimeoutException e2) {
						if (debugMode) debugInfo.set("Extra time up");
					} catch (CancellationException e3) { }
				}
				// Time is up, stop the                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  search.
				if (!search.isDone()) {
					search.cancel(true);
					/* Wait up to x nanoseconds after cancellation for the search result to be up to date with a 
					 * higher probability. */
					int wait = params.nanoSecondsToWaitForResult;
					long deadLine = System.nanoTime() + wait;
					while (!cancelledResultsReady && System.nanoTime() < deadLine) {
						try {
							wait(0, wait);
						} catch (InterruptedException e) {
							if (debugMode) debugInfo.set(e.getMessage());
							wait = (int) (deadLine - System.nanoTime());
						}
					}
				}
			}
			if (debugMode) debugInfo.set("Search stopped");
			if (deterministicZeroDepthMode) {
				searchResLock.readLock().lock();
				try {
					return new SearchResults(null, null, searchStats.getScore(), searchStats.getScoreType());
				} finally {
					searchResLock.readLock().unlock();
				}
			}
			// Set ponder move based on the PV.
			pV = searchStats.getPv();
			ponderMove = pV != null && pV.length > 1 ? pV[1] : null;
		}
		Short score;
		ScoreType scoreType;
		// Set final results.
		searchResLock.readLock().lock();
		try {
			if (isBookMove) {
				bestMove = searchResult.toString();
				score = null;
				scoreType = null;
			} else if (searchResult.equals(Move.NULL_MOVE)) {
				bestMove = getRandomMove();
				score = null;
				scoreType = null;
				if (debugMode) debugInfo.set("No valid PV root move found\n" +
						"Random move selected");
			} else {
				bestMove = searchResult.toString();
				score = searchStats.getScore();
				scoreType = searchStats.getScoreType();
			}
			return new SearchResults(bestMove, ponderMove, score, scoreType);
		} finally {
			searchResLock.readLock().unlock();
		}
	}
	@Override
	public void stop() {
		if (search != null) {
			if (debugMode) debugInfo.set("Stopping search...");
			stop = true;
			search.cancel(true);
			synchronized (this) {
				notify();
			}
		}
	}
	@Override
	public void ponderHit() {
		if (debugMode) debugInfo.set("Signaling ponderhit...");
		ponderHit = true;
		synchronized (this) {
			notify();
		}
	}
	@Override
	public SearchInformation getSearchInfo() {
		return searchStats;
	}
	@Override
	public short getHashLoadPermill() {
		long load, capacity;
		capacity = tT.getCapacity() + eT.getCapacity();
		load = tT.getLoad() + eT.getLoad();
		if (debugMode) debugInfo.set("Total hash size in MB - " + String.format("%.2f",
				(float) ((double) (SizeEstimator.getInstance().sizeOf(tT) + SizeEstimator.getInstance().sizeOf(eT)))/(1L << 20)));
		return (short) (1000*load/capacity);
	}
	@Override
	public DebugInformation getDebugInfo() {
		return debugInfo;
	}
	@Override
	public void quit() {
		if (debugMode) debugInfo.set("Shutting down...");
		try {
			book.close();
		} catch (IOException e) { e.printStackTrace(); }
		executor.shutdown();
		searchStats.deleteObservers();
		hT = null;
		tT = null;
		eT = null;
		isInit = false;
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
	public synchronized GameState getGameState() {
		return game.getState();
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
	public synchronized boolean setGame(String pgn) {
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
		controllerMode = on;
		if (isInit)
			notifyParametersChanged();
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
	public synchronized String unplayLastMove() {
		return game.unplay();
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
	public synchronized String toPGN() {
		return game.toString();
	}
	@Override
	public synchronized String toFEN() {
		return game.getPosition().toString();
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
	public synchronized void notifyParametersChanged() {
		if (isInit()) {
			setHashSize(controllerMode || deterministicZeroDepthMode ? MIN_HASH_SIZE :
					options.get(hashSize) == hashSize.getDefaultValue() ? params.defaultHashSize :
					(short) ((Integer) options.get(hashSize)).shortValue());
			eval = new Evaluator(params, controllerMode || deterministicZeroDepthMode ? null : eT);
		}
	}
	@Override
	public synchronized void setDeterministicZeroDepthMode(boolean on) {
		deterministicZeroDepthMode = on;
		if (isInit)
			notifyParametersChanged();
	}
	@Override
	public void update(Observable o, Object arg) {
		cancelledResultsReady = false;
		short score;
		SearchInfo stats = (SearchInfo) o;
		if (stats.getDepth() == 0)
			return;
		Move newSearchRes = stats.getPvMoveList() != null && stats.getPvMoveList().size() > 0 ?
				stats.getPvMoveList().get(0) : Move.NULL_MOVE;
		newSearchRes = newSearchRes == null ? Move.NULL_MOVE : newSearchRes;
		searchResLock.writeLock().lock();
		try {
			if (!searchResult.equals(newSearchRes)) {
				timeOfLastSearchResChange = System.currentTimeMillis();
				numOfSearchResChanges++;
			}
			if (stats.isCancelled() && !Move.NULL_MOVE.equals(searchResult) &&
					(stats.getScoreType() == ScoreType.LOWER_BOUND || stats.getScoreType() == ScoreType.UPPER_BOUND))
				score = searchResult.value;
			else
				score = stats.getScore();
			scoreFluctuation = (short) (score - searchResult.value);
			searchResult = !newSearchRes.equals(Move.NULL_MOVE) ? newSearchRes : searchResult;
			searchResult.value = score;
		} finally {
			searchResLock.writeLock().unlock();
		}
		if (stats.isCancelled()) {
			cancelledResultsReady = true;
			synchronized (this) {
				notify();
			}
		}
	}
	
}
