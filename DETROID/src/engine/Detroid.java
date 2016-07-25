package engine;

import java.io.IOException;
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

public class Detroid implements Engine, Observer {
	
	private final static float VERSION_NUMBER = 0.80f;
	private final static String NAME = "DETROID" + " " + VERSION_NUMBER;
	private final static String AUTHOR = "Viktor Csomor";
	
	private Option<?> hashSize;
	private Option<?> ponder;
	private Option<?> ownBook;
	private Option<?> bookPath;
	private Option<?> useOwnBookAsSecondary;
	private Option<?> uciOpponent;
	private HashMap<Option<?>, Object> options;
	
	private Parameters params;
	private Game game;
	private boolean newGame;
	private DebugInformation debugInfo;
	private boolean debug;
	private Book book;
	private Thread search;
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
	private void setHashSize(int hashSize) {
		long sizeInBytes = hashSize*1024*1024;
		int totalHashShares = params.TT_SHARE + params.ET_SHARE + params.PT_SHARE;
		tT = new LossyHashTable<>(sizeInBytes*params.TT_SHARE/totalHashShares, TTEntry.SIZE);
		eT = new LossyHashTable<>(sizeInBytes*params.ET_SHARE/totalHashShares, ETEntry.SIZE);
		pT = new LossyHashTable<>(sizeInBytes*params.PT_SHARE/totalHashShares, PTEntry.SIZE);
		if (debug) debugInfo.set("Hash capacity data\n" +
				"Transposition table capacity - " + tT.getCapacity() + "\n" +
				"Evaluation table capacity - " + eT.getCapacity() + "\n" +
				"Pawn table capacity - " + pT.getCapacity());
		System.gc();
	}
	private long computeSearchTime(Long whiteTime, Long blackTime, Long whiteIncrement, Long blackIncrement, Integer movesToGo) {
		int phaseScore;
		whiteIncrement = whiteIncrement == null ? 0 : whiteIncrement;
		blackIncrement = blackIncrement == null ? 0 : blackIncrement;
		if (movesToGo == null) {
			phaseScore = new Evaluator(params, eT, pT, gen).phaseScore(game.getPosition());
			movesToGo = Game.AVG_MOVES_PER_GAME - Game.AVG_MOVES_PER_GAME*phaseScore/params.GAME_PHASE_END_GAME_UPPER +
					params.MOVES_TO_GO_SAFETY_MARGIN;
			if (debug) debugInfo.set("Search time data\n" +
					"Phase score - " + phaseScore + "/256\n" +
					"Expected number of moves left until end - " + movesToGo);
		}
		return game.getSideToMove() == Side.WHITE ?
				(long) (Math.max(1, (whiteTime*0.95d)) + Math.max(0, (movesToGo - 1)*whiteIncrement))/(movesToGo + 1):
				(long) (Math.max(1, (blackTime*0.95d)) + Math.max(0, (movesToGo - 1)*blackIncrement))/(movesToGo + 1);
	}
	private long computeSearchTimeExtension(long origSearchTime, Long whiteTime, Long blackTime, Long whiteIncrement,
			Long blackIncrement, Integer movesToGo) {
		if (searchResult.equals(new Move()) || (game.getSideToMove() == Side.WHITE ?
				whiteTime - origSearchTime > 1000*params.MOVES_TO_GO_SAFETY_MARGIN :
				blackTime - origSearchTime > 1000*params.MOVES_TO_GO_SAFETY_MARGIN) &&
				(searchStats.getScoreType() == ScoreType.LOWER_BOUND || searchStats.getScoreType() == ScoreType.UPPER_BOUND ||
				Math.abs(scoreFluctuation) >= params.SCORE_FLUCTUATION_LIMIT || timeOfLastSearchResChange >= System.currentTimeMillis() -
				origSearchTime/params.FRACTION_OF_ORIG_SEARCH_TIME_SINCE_LAST_RESULT_CHANGE_LIMIT ||
				(numOfSearchResChanges/Math.max(1, origSearchTime))*1000 >= params.RESULT_CHANGES_PER_SECOND_LIMIT)) {
			if (debug)
				debugInfo.set("Search time extension data\n" +
						"PV root move invalid - " + searchResult.equals(new Move()) + "\n" +
						"Score fluctuation - " + scoreFluctuation + "\n" +
						"Score type - " + searchStats.getScoreType() + "\n" +
						"Last PV root change " + (System.currentTimeMillis() - timeOfLastSearchResChange) + "ms ago\n" +
						"Number of PV root changes per second - " + (numOfSearchResChanges/Math.max(1, origSearchTime))*1000);
			return computeSearchTime(game.getSideToMove() == Side.WHITE ? whiteTime - origSearchTime : whiteTime,
				game.getSideToMove() == Side.WHITE ? blackTime : blackTime - origSearchTime, whiteIncrement, blackIncrement, movesToGo);
		}
		return 0;
	}
	private String getRandomMove() {
		List<Move> moves = game.getPosition().getMoves();
		String[] arr = new String[moves.size()];
		return arr[(int) (Math.random()*arr.length)];
	}
	@Override
	public synchronized void init() {
		params = new Parameters();
		if (debug) debugInfo.set("Parameters successfully initialized");
		game = new Game();
		debug = false;
		debugInfo = new DebugInformation();
		try {
			book = new Book();
			if (debug) debugInfo.set("Book successfully initialized");
		} catch (IOException e) { if (debug) debugInfo.set(e.getMessage()); }
		options = new HashMap<>();
		hashSize = new Option.SpinOption("Hash", 32, 1, (int) Math.min(512, Runtime.getRuntime().maxMemory()/(1L << 20)/2));
		ponder = new Option.CheckOption("Ponder", true);
		ownBook = new Option.CheckOption("OwnBook", false);
		bookPath = new Option.StringOption("BookPath", Book.DEFAULT_BOOK_FILE_PATH);
		useOwnBookAsSecondary = new Option.CheckOption("UseOwnBookAsSecondary", false);
		uciOpponent = new Option.StringOption("UCI_Opponent", null);
		options.put(hashSize, hashSize.getDefaultValue());
		options.put(ponder, ponder.getDefaultValue());
		options.put(ownBook, ownBook.getDefaultValue());
		options.put(bookPath, bookPath.getDefaultValue());
		options.put(useOwnBookAsSecondary, useOwnBookAsSecondary.getDefaultValue());
		options.put(uciOpponent, uciOpponent.getDefaultValue());
		if (debug) debugInfo.set("Options successfully set up");
		searchStats = new SearchInformation();
		searchStats.addObserver(this);
		setHashSize((int)options.get(hashSize));
		hT = new RelativeHistoryTable(params);
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
	public Set<Option<?>> getOptions() {
		Set<Option<?>> set = new HashSet<>();
		options.forEach((s, o) -> set.add(s));
		return set;
	}
	@Override
	public synchronized <T> boolean setOption(Option<T> setting, T value) {
		if (value == null) return false;
		if (hashSize.equals(setting)) {
			if (hashSize.getMin().intValue() <= ((Integer)value).intValue() &&
					hashSize.getMax().intValue() >= ((Integer)value).intValue()) {
				options.put(hashSize, value);
				setHashSize(((Integer)value).intValue());
				if (debug) debugInfo.set("Hash size successfully set to " + value);
				return true;
			}
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
		}
		else if (bookPath.equals(setting)) {
			if (book.setMainBookPath((String)value)) {
				options.put(bookPath, value);
				if (debug) debugInfo.set("Book path successfully set to " + value);
				return true;
			}
		}
		else if (useOwnBookAsSecondary.equals(setting)) {
			if ((Boolean)value == true) {
				if (book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH)) {
					options.put(useOwnBookAsSecondary, value);
					if (debug) debugInfo.set("Use of own book as secondary book successfully set to " + value);
					return true;
				}
			}
			else {
				book.setSecondaryBookPath(null);
				options.put(useOwnBookAsSecondary, value);
				if (debug) debugInfo.set("Secondary book path successfully set to " + value);
				return true;
			}
		}
		else if (uciOpponent.equals(setting)) {
			options.put(uciOpponent, value);
			if (debug) debugInfo.set("Opponent name successfully set to " + value);
			return true;
		}
		if (debug) debugInfo.set("The setting was denied");
		return false;
	}
	@Override
	public synchronized void newGame() {
		newGame = true;
		tT.clear();
		eT.clear();
		pT.clear();
		hT.reset();
		gen = 0;
		if (debug) debugInfo.set("Hash tables cleared");
		System.gc();
	}
	@Override
	public synchronized boolean position(String fen) {
		try {
			Position pos = fen.equals("startpos") ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			if (newGame) {
				if (debug) debugInfo.set("New game set");
				game = new Game(pos.toString());
			}
			else if (!game.getStartPos().equals(pos.toString())) {
				newGame();
				if (debug) debugInfo.set("New game set due to new start position");
				game = new Game(pos.toString());
			}
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
					tT.remove(e -> e.generation < gen - 1);
					eT.remove(e -> e.generation < gen - 1);
					pT.remove(e -> e.generation < gen - 3);
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
		long time, extraTime;
		boolean doPonder = ponder != null && ponder.booleanValue();
		searchResult = new Move();
		scoreFluctuation = 0;
		timeOfLastSearchResChange = System.currentTimeMillis();
		numOfSearchResChanges = 0;
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
		if ((Boolean)options.get(ownBook)) {
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
		}
		else {
			if (doPonder) {
				if (!(Boolean)options.get(ponder)) {
					if (debug) debugInfo.set("Ponder mode started with ponder option disallowed - Abort");
					return null;
				}
			}
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
			search = new Search(game.getPosition(), searchStats, doPonder || (infinite != null && infinite.booleanValue()),
					depth == null ? (mateDistance == null ?  Integer.MAX_VALUE : mateDistance) : depth,
					nodes == null ? Long.MAX_VALUE : nodes, moves, hT, gen, tT, eT, pT, params);
			search.start();
			if (debug) debugInfo.set("Search started");
			if (doPonder) {
				if (debug) debugInfo.set("In ponder mode");
				while (search.isAlive() && !ponderHit) {
					try {
						wait(200);
					} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
				}
				if (ponderHit) {
					if (debug) debugInfo.set("Ponderhit acknowledged");
					ponderHit = false;
				}
				if (debug) debugInfo.set("Ponder stopped");
			}
			time = searchTime == null || searchTime == 0 ?
					computeSearchTime(whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo) : searchTime;
			if (debug) debugInfo.set("Base search time - " + time);
			try {
				search.join(time);
			} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
			if (debug) debugInfo.set("Base search time up");
			if (searchTime == null || searchTime == 0) {
				try {
					extraTime = computeSearchTimeExtension(time, whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo);
					if (debug) debugInfo.set("Extra search time - " + extraTime);
					if (extraTime > 0)
						search.join(extraTime);
				} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
			}
			if (debug) debugInfo.set("Extra time up");
			if (search.isAlive()) {
				search.interrupt();
				try {
					search.join();
				} catch (InterruptedException e) { if (debug) debugInfo.set(e.getMessage()); }
			}
			if (debug) debugInfo.set("Search stopped");
		}
		pV = searchStats.getPv();
		ponderMove = pV.length > 1 ? pV[1] : null;
		if (searchResult.equals(new Move())) {
			bestMove = getRandomMove();
			if (debug) debugInfo.set("No valid PV root move found\n" +
					"Random move selected");
		}
		else
			bestMove = searchResult.toString();
		return new SearchResults(bestMove, ponderMove);
	}
	@Override
	public void stop() {
		if (search != null && search.isAlive()) {
			if (debug) debugInfo.set("Stopping search...");
			search.interrupt();
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
		if (debug) debugInfo.set("Total hash size in MB - " + (tT.sizeInBytes() + eT.sizeInBytes() + pT.sizeInBytes())/(2L << 20));
		return (short) (1000*load/capacity);
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
		Move newSearchRes = stats.getPvMoveList() != null ? stats.getPvMoveList().getHead() : null;
		scoreFluctuation = (short) (stats.getScore() - searchResult.value);
		searchResult = newSearchRes != null ? newSearchRes : searchResult;
		searchResult.value = stats.getScore();
		timeOfLastSearchResChange = System.currentTimeMillis();
		numOfSearchResChanges++;
	}
}
