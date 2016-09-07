package engine;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import engine.Book.SelectionModel;
import engine.Game.Side;
import uci.DebugInfo;
import uci.Engine;
import uci.SearchResults;
import uci.ScoreType;
import uci.SearchInfo;
import uci.Option;
import util.*;

/**
 * 
 * @author Viktor
 *
 */
public class Detroid implements Engine, Observer {
	
	public final static float VERSION_NUMBER = 0.80f;
	public final static String NAME = "DETROID" + " " + VERSION_NUMBER;
	public final static String AUTHOR = "Viktor Csomor";
	
	// An own opening book compiled using SCID 4.62, PGN-Extract 17-21 and Polyglot 1.4w.
	public final static String DEFAULT_BOOK_FILE_PATH;
	static {
		String path;
		try {
			path = new File(Detroid.class.getProtectionDomain().getCodeSource().getLocation().toURI())
					.getAbsoluteFile().getParent() + File.separator + "book.bin";
		} catch (URISyntaxException e) { path = null; }
		DEFAULT_BOOK_FILE_PATH = path;
	}
	// Search, evaluation, and time control parameters.
	public final static String DEFAULT_PARAMETERS_FILE_PATH = "/params.txt";
	
	private Option<?> hashSize;
	private Option<?> clearHash;
	private Option<?> ponder;
	private Option<?> ownBook;
	private Option<?> primaryBookPath;
	private Option<?> secondaryBookPath;
	private Option<?> uciOpponent;
	private HashMap<Option<?>, Object> options;
	
	private Parameters params;
	private Game game;
	private boolean newGame;
	private DebugInformation debugInfo;
	private boolean debug;
	private Book book;
	private boolean outOfBook;
	private Thread search;
	private boolean stop;
	private boolean ponderHit;
	private SearchInformation searchStats;
	private Move searchResult;
	private Short scoreFluctuation;
	private Long timeOfLastSearchResChange;
	private Integer numOfSearchResChanges;
	private RelativeHistoryTable hT;
	private LossyHashTable<TTEntry> tT;		// Transposition table.
	private LossyHashTable<ETEntry> eT;		// Evaluation hash table.
	private LossyHashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen;
	
	public Detroid() {
		
	}
	/**
	 * Sets the hash tables according to the hash size limit defined in megabytes.
	 * 
	 * @param hashSize
	 */
	private void setHashSize(int hashSize) {
		long sizeInBytes = hashSize*1024*1024;
		int totalHashShares = params.TT_SHARE + params.ET_SHARE + params.PT_SHARE;
		SizeEstimator estimator = SizeEstimator.getInstance();
		tT = new LossyHashTable<>(sizeInBytes*params.TT_SHARE/totalHashShares/estimator.sizeOf(TTEntry.class));
		eT = new LossyHashTable<>(sizeInBytes*params.ET_SHARE/totalHashShares/estimator.sizeOf(ETEntry.class));
		pT = new LossyHashTable<>(sizeInBytes*params.PT_SHARE/totalHashShares/estimator.sizeOf(PTEntry.class));
		if (debug) debugInfo.set("Hash capacity data\n" +
				"Transposition table capacity - " + tT.getCapacity() + "\n" +
				"Evaluation table capacity - " + eT.getCapacity() + "\n" +
				"Pawn table capacity - " + pT.getCapacity());
		System.gc();
	}
	/**
	 * Clears the transposition, pawn, evaluation, and history tables.
	 */
	private void clearHash() {
		tT.clear();
		eT.clear();
		pT.clear();
		hT.reset();
		System.gc();
		gen = 0;
		if (debug) debugInfo.set("Hash tables cleared");
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
			phaseScore = new Evaluator(params, eT, pT, gen).phaseScore(game.getPosition());
			movesToGo = Game.AVG_MOVES_PER_GAME - Game.AVG_MOVES_PER_GAME*phaseScore/params.GAME_PHASE_ENDGAME_UPPER +
					params.MOVES_TO_GO_SAFETY_MARGIN;
			if (debug) debugInfo.set("Search time data\n" +
					"Phase score - " + phaseScore + "/256\n" +
					"Expected number of moves left until end - " + movesToGo);
		}
		return game.getSideToMove() == Side.WHITE ?
				(long) (Math.max(1, (whiteTime + Math.max(0, (movesToGo - 1)*whiteIncrement))*
						params.FRACTION_OF_TOTAL_TIME_TO_USE)/(movesToGo + 1)) :
				(long) (Math.max(1, (blackTime + Math.max(0, (movesToGo - 1)*blackIncrement))*
						params.FRACTION_OF_TOTAL_TIME_TO_USE)/(movesToGo + 1));
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
		if (debug)
			debugInfo.set("Search time extension data\n" +
					"PV root move invalid - " + searchResult.equals(new Move()) + "\n" +
					"Score fluctuation - " + Math.abs(scoreFluctuation) + "\n" +
					"Score type - " + searchStats.getScoreType() + "\n" +
					"Last PV root change " + (System.currentTimeMillis() - timeOfLastSearchResChange) + "ms ago\n" +
					"Number of PV root changes per depth - " + numOfSearchResChanges/searchStats.getDepth());
		if (searchResult.equals(new Move()) || (game.getSideToMove() == Side.WHITE ?
				whiteTime - origSearchTime > 1000*params.MOVES_TO_GO_SAFETY_MARGIN :
				blackTime - origSearchTime > 1000*params.MOVES_TO_GO_SAFETY_MARGIN) &&
				(searchStats.getScoreType() == ScoreType.LOWER_BOUND || searchStats.getScoreType() == ScoreType.UPPER_BOUND ||
				Math.abs(scoreFluctuation) >= params.SCORE_FLUCTUATION_LIMIT || timeOfLastSearchResChange >= System.currentTimeMillis() -
				origSearchTime*params.FRACTION_OF_ORIG_SEARCH_TIME_SINCE_LAST_RESULT_CHANGE_LIMIT ||
				numOfSearchResChanges/searchStats.getDepth() >= params.RESULT_CHANGES_PER_DEPTH_LIMIT)) {
			return computeSearchTime(game.getSideToMove() == Side.WHITE ?  new Long(whiteTime - origSearchTime) : whiteTime,
					game.getSideToMove() == Side.WHITE ? blackTime : new Long(blackTime - origSearchTime),
					whiteIncrement, blackIncrement, movesToGo);
		}
		return 0;
	}
	/**
	 * Picks a move from the list of legal moves for the current position and returns a String representation of it in pure algebraic coordinate
	 * notation.
	 * 
	 * @return
	 */
	private String getRandomMove() {
		int i = 0;
		ArrayList<Move> moves = game.getPosition().getMoves();
		String[] arr = new String[moves.size()];
		for (Move m : moves)
			arr[i++] = m.toString();
		return arr[(int) (Math.random()*arr.length)];
	}
	@Override
	public synchronized void init() {
		try {
			params = new Parameters(DEFAULT_PARAMETERS_FILE_PATH);
		} catch (IOException e) { }
		try {
			book = new PolyglotBook(DEFAULT_BOOK_FILE_PATH);
		} catch (IOException e) { }
		debugInfo = new DebugInformation();
		debug = false;
		ponderHit = false;
		stop = false;
		outOfBook = false;
		game = new Game();
		options = new HashMap<>();
		hashSize = new Option.SpinOption("Hash", 128, 1, (int) Math.min(1024, Runtime.getRuntime().maxMemory()/(1L << 20)/2));
		clearHash = new Option.ButtonOption("ClearHash");
		ponder = new Option.CheckOption("Ponder", true);
		ownBook = new Option.CheckOption("OwnBook", false);
		primaryBookPath = new Option.StringOption("PrimaryBookPath", book == null ? null : book.getPrimaryFilePath());
		secondaryBookPath = new Option.StringOption("SecondaryBookPath", null);
		uciOpponent = new Option.StringOption("UCI_Opponent", null);
		options.put(hashSize, hashSize.getDefaultValue());
		options.put(clearHash, clearHash.getDefaultValue());
		options.put(ponder, ponder.getDefaultValue());
		options.put(ownBook, ownBook.getDefaultValue());
		options.put(primaryBookPath, primaryBookPath.getDefaultValue());
		options.put(secondaryBookPath, secondaryBookPath.getDefaultValue());
		options.put(uciOpponent, uciOpponent.getDefaultValue());
		searchStats = new SearchInformation();
		searchStats.addObserver(this);
		setHashSize((int)options.get(hashSize));
		hT = new RelativeHistoryTable(params);
		System.gc();
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
	public void debug(boolean on) {
		debug = on;
	}
	@Override
	public Collection<Option<?>> getOptions() {
		Collection<Option<?>> list = new ArrayList<>();
		list.add(hashSize);
		list.add(clearHash);
		list.add(ponder);
		list.add(ownBook);
		list.add(primaryBookPath);
		list.add(secondaryBookPath);
		list.add(uciOpponent);
		return list;
	}
	@Override
	public synchronized <T> boolean setOption(Option<T> setting, T value) {
		if (value == null) return false;
		if (hashSize.equals(setting)) {
			if (hashSize.getMin().intValue() <= ((Integer)value).intValue() &&
					hashSize.getMax().intValue() >= ((Integer)value).intValue()) {
				if (((Integer)value).intValue() != ((Integer)options.get(hashSize)).intValue()) {
					options.put(hashSize, value);
					setHashSize(((Integer)value).intValue());
				}
				if (debug) debugInfo.set("Hash size successfully set to " + value);
				return true;
			}
		}
		else if (clearHash.equals(setting)) {
			clearHash();
			return true;
		}
		else if (ponder.equals(setting)) {
			options.put(ponder, value);
			if (debug) debugInfo.set("Ponder successfully set to " + value);
			return true;
		}
		else if (ownBook.equals(setting)) {
			if (book != null) {
				options.put(ownBook, value);
				if (debug) debugInfo.set("Use of own book successfully set to " + value);
				return true;
			}
			if (debug) debugInfo.set("No book file found at " + options.get(primaryBookPath));
		}
		else if (primaryBookPath.equals(setting)) {
			try {
				PolyglotBook newBook = new PolyglotBook((String) value, book.getSecondaryFilePath());
				book.close();
				book = newBook;
				options.put(primaryBookPath, book.getPrimaryFilePath());
				if (debug) debugInfo.set("Primary book file path successfully set to " + value);
				return true;
			} catch (IOException e) { if (debug) debugInfo.set(e.getMessage()); }
		}
		else if (secondaryBookPath.equals(setting)) {
			try {
				PolyglotBook newBook = new PolyglotBook(book.getPrimaryFilePath(), (String) value);
				book.close();
				book = newBook;
				options.put(secondaryBookPath, book.getSecondaryFilePath());
				if (debug) debugInfo.set("Secondary book file path successfully set to " + value);
				return true;
			} catch (IOException e) { if (debug) debugInfo.set(e.getMessage()); }
		}
		else if (uciOpponent.equals(setting)) {
			options.put(uciOpponent, value);
			if (debug) debugInfo.set("Opponent name successfully set to " + value);
			return true;
		}
		if (debug) debugInfo.set("The setting was not accepted");
		return false;
	}
	@Override
	public synchronized void newGame() {
		newGame = true;
		outOfBook = false;
		clearHash();
	}
	@Override
	public synchronized boolean position(String fen) {
		try {
			Position pos = fen.equals("startpos") ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			// If the start position of the game is different or the engine got the new game signal, reset the game and the hash tables.
			if (newGame) {
				if (debug) debugInfo.set("New game set");
				game = new Game(pos.toString());
			}
			else if (!game.getStartPos().equals(pos.toString())) {
				newGame();
				if (debug) debugInfo.set("New game set due to new start position");
				game = new Game(pos.toString());
			}
			// Otherwise just clear the 'ancient' entries from the hash tables and decrement the history values.
			else {
				if (debug) debugInfo.set("Position set within the same game");
				gen++;
				if (gen == 127) {
					tT.clear();
					eT.clear();
					pT.clear();
					gen = 0;
				}
				else {
					tT.remove(e -> e.generation < gen - params.TT_ENTRY_LIFECYCLE);
					eT.remove(e -> e.generation < gen - params.ET_ENTRY_LIFECYCLE);
					pT.remove(e -> e.generation < gen - params.PT_ENTRY_LIFECYCLE);
				}
				hT.decrementCurrentValues();
				System.gc();
				game = new Game(game.getStartPos(), game.getEvent(), game.getSite(), game.getWhitePlayerName(), game.getBlackPlayerName());
			}
			return true;
		} catch (ChessParseException | NullPointerException e) {
			if (debug) debugInfo.set(e.getMessage());
			return false;
		}
	}
	@Override
	public boolean play(String pacn) {
		if (game.play(pacn)) {
			if (debug) debugInfo.set("Move \"" + pacn + "\" played successfully");
			return true;
		}
		if (debug) debugInfo.set("Move \"" + pacn + "\" could not be played");
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
		doPonder = false;
		doInfinite = infinite != null && infinite;
		// Reset search stats.
		searchResult = new Move();
		scoreFluctuation = 0;
		timeOfLastSearchResChange = System.currentTimeMillis();
		numOfSearchResChanges = 0;
		// Set the names of the players once it is known which colour we are playing.
		if (newGame) {
			if (game.getSideToMove() == Side.WHITE) {
				game.setWhitePlayerName(NAME);
				game.setBlackPlayerName((String)options.get(uciOpponent));
			}
			else {
				game.setWhitePlayerName((String)options.get(uciOpponent));
				game.setBlackPlayerName(NAME);
			}
			if (debug) debugInfo.set("Players' names set\n" +
					"White - " + game.getWhitePlayerName() + "\n" +
					"Black - " + game.getBlackPlayerName());
			newGame = false;
		}
		// Search the book if possible.
		if ((Boolean)options.get(ownBook) && !outOfBook && searchMoves == null && (ponder == null || !ponder) && depth == null &&
				nodes == null && mateDistance == null && searchTime == null && (infinite == null || !infinite)) {
			bookSearchStart = System.currentTimeMillis();
			search = new Thread(() -> {
				searchResult = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
				searchResult = searchResult == null ? new Move() : searchResult;
			});
			search.start();
			if (debug) debugInfo.set("Book search started");
			try {
				search.join();
			} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
			if (debug) debugInfo.set("Book search done");
			ponderMove = null;
			// If the book search has not been externally stopped, use the remaining time for a normal search.
			if (!stop) {
				if (searchResult.equals(new Move())) {
					if (debug) debugInfo.set("No book move found. Out of book.");
					outOfBook = true;
					search(searchMoves, ponder, game.getSideToMove() == Side.WHITE ? whiteTime - (System.currentTimeMillis() - bookSearchStart) :
						whiteTime, game.getSideToMove() == Side.WHITE ? blackTime : blackTime - (System.currentTimeMillis() - bookSearchStart),
						whiteIncrement, blackIncrement, movesToGo, depth, nodes, mateDistance, searchTime, infinite);
				}
			}
		}
		// Run a game tree search.
		else {
			// Check if pondering is possible.
			if (ponder != null && ponder) {
				if (!(Boolean)options.get(this.ponder)) {
					if (debug) debugInfo.set("Ponder mode started with ponder option disallowed - Abort");
					return null;
				}
				else
					doPonder = true;
			}
			// Set root move restrictions if there are any.
			if (searchMoves != null && !searchMoves.isEmpty()) {
				moves = new HashSet<>();
				try {
					for (String s : searchMoves)
						moves.add(game.getPosition().parsePACN(s));
				} catch (ChessParseException | NullPointerException e) {
					if (debug) debugInfo.set("Search moves could not be parsed\n" + e.getMessage());
					return null;
				}
			}
			else
				moves = null;
			// Start the search.
			Search: {
				search = new Thread(new Search(game.getPosition(), searchStats, doPonder || doInfinite,
						depth == null ? (mateDistance == null ?  Integer.MAX_VALUE : mateDistance) : depth,
						nodes == null ? Long.MAX_VALUE : nodes, moves, hT, gen, tT, eT, pT, params));
				search.start();
				if (debug) debugInfo.set("Search started");
				// If in ponder mode, run the search until the ponderhit signal or until it is externally stopped. 
				if (doPonder) {
					if (debug) debugInfo.set("In ponder mode");
					while (search.isAlive() && !ponderHit) {
						try {
							wait(200);
						} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
					}
					if (debug) debugInfo.set("Ponder stopped");
					// If the search terminated due to a ponderhit, keep searching...
					if (ponderHit) {
						if (debug) debugInfo.set("Ponderhit acknowledged");
						ponderHit = false;
					}
					// If it did not, return the best move found.
					else
						return new SearchResults(searchResult == null || searchResult.equals(new Move()) ? getRandomMove().toString() :
							searchResult.toString(), null);
				}
				// If in infinite mode, let the search run until it is externally stopped.
				else if (doInfinite) {
					if (debug) debugInfo.set("In infinite mode");
					try {
						search.join();
					} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
					break Search;
				}
				// Compute search time and wait for the search to terminate for the computed amount of time.
				time = searchTime == null || searchTime == 0 ?
						computeSearchTime(whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo) : searchTime;
				if (debug) debugInfo.set("Base search time - " + time);
				try {
					search.join(time);
					if (debug) debugInfo.set("Base search time up");
				} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
				// If time was up, check if the search should be extended.
				if (!stop && (searchTime == null) && (depth == null) && (nodes == null) && (mateDistance == null)) {
					try {
						extraTime = computeSearchTimeExtension(time, whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo);
						if (debug) debugInfo.set("Extra search time - " + extraTime);
						if (extraTime > 0)
							search.join(extraTime);
						if (debug) debugInfo.set("Extra time up");
					} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
				}
				// Time is up, stop the search.
				if (search.isAlive()) {
					search.interrupt();
					try {
						search.join();
					} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
				}
			}
			if (debug) debugInfo.set("Search stopped");
			// Set ponder move based on the PV.
			pV = searchStats.getPv();
			ponderMove = pV != null && pV.length > 1 ? pV[1] : null;
		}
		// Set final results.
		if (searchResult.equals(new Move())) {
			bestMove = getRandomMove();
			if (debug) debugInfo.set("No valid PV root move found\n" +
					"Random move selected");
		}
		else
			bestMove = searchResult.toString();
		stop = false;
		return new SearchResults(bestMove, ponderMove);
	}
	@Override
	public void stop() {
		if (search != null && search.isAlive()) {
			if (debug) debugInfo.set("Stopping search...");
			search.interrupt();
			stop = true;
		}
	}
	@Override
	public void ponderhit() {
		if (debug) debugInfo.set("Signaling ponderhit...");
		ponderHit = true;
	}
	@Override
	public SearchInfo getSearchInfo() {
		return searchStats;
	}
	@Override
	public short getHashLoadPermill() {
		long load, capacity;
		capacity = tT.getCapacity() + eT.getCapacity() + pT.getCapacity();
		load = tT.getLoad() + eT.getLoad() + pT.getLoad();
		if (debug) debugInfo.set("Total hash size in MB - " + String.format("%.2f", (float)((double)(SizeEstimator.getInstance().sizeOf(tT) +
				SizeEstimator.getInstance().sizeOf(eT) + SizeEstimator.getInstance().sizeOf(pT)))/(1L << 20)));
		return (short) (1000*Math.max(1, load)/capacity);
	}
	@Override
	public DebugInfo getDebugInfo() {
		return debugInfo;
	}
	@Override
	public void quit() {
		if (debug) debugInfo.set("Shutting down...");
		try {
			book.close();
		} catch (IOException e) { e.printStackTrace(); }
		if (search.isAlive())
			search.interrupt();
		searchStats.deleteObservers();
		hT = null;
		tT = null;
		eT = null;
		pT = null;
		System.gc();
	}
	@Override
	public void update(Observable o, Object arg) {
		SearchInformation stats = (SearchInformation)o;
		Move newSearchRes = stats.getPvMoveList() != null ? stats.getPvMoveList().get(0) : new Move();
		newSearchRes = newSearchRes == null ? new Move() : newSearchRes;
		if (!searchResult.equals(newSearchRes)) {
			timeOfLastSearchResChange = System.currentTimeMillis();
			numOfSearchResChanges++;
		}
		scoreFluctuation = (short) (stats.getScore() - searchResult.value);
		searchResult = !newSearchRes.equals(new Move()) ? newSearchRes : searchResult;
		searchResult.value = stats.getScore();
	}
}
