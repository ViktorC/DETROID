package engine;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import engine.Book.SelectionModel;
import engine.Engine.SearchInfo.ScoreType;
import util.*;

public class Detroid implements Engine {
	
	private final static float VERSION_NUMBER = 1.00f;
	private final static String NAME = "DETROID" + " " + VERSION_NUMBER;
	private final static String AUTHOR = "Viktor Csomor";

	private final static Detroid INSTANCE = new Detroid();
	
	private Setting<?> hashSize;
	private Setting<?> useBook;
	private Setting<?> bookPath;
	private Setting<?> useOwnBookAsSecondary;
	private HashMap<Setting<?>, Object> settings;
	
	private Parameters params;
	private Position position;
	private Book book;
	private Thread search;
	private SearchStatistics searchStats;
	private Move searchResult;
	private Short scoreFluctuation;
	private Long timeOfLastSearchResChange;
	private Integer numOfSearchResChanges;
	private RelativeHistoryTable hT;
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen = 0;
	
	private Detroid() {
		
	}
	public Detroid getInstance() {
		return INSTANCE;
	}
	private void setHashSize(int hashSize) {
		int totalHashShares = params.TT_SHARE + params.ET_SHARE + params.PT_SHARE;
		tT = new HashTable<>(hashSize*params.TT_SHARE/totalHashShares, TTEntry.SIZE);
		eT = new HashTable<>(hashSize*params.ET_SHARE/totalHashShares, ETEntry.SIZE);
		pT = new HashTable<>(hashSize*params.PT_SHARE/totalHashShares, PTEntry.SIZE);
		System.gc();
	}
	private long computeSearchTime(Long whiteTime, Long blackTime, Long whiteIncrement, Long blackIncrement, Integer movesToGo) {
		int phaseScore;
		final int avgNumOfMovesPerGame = 40;
		whiteIncrement = whiteIncrement == null ? 0 : whiteIncrement;
		blackIncrement = blackIncrement == null ? 0 : blackIncrement;
		if (movesToGo == null) {
			phaseScore = new Evaluator(params, eT, pT, gen).phaseScore(position);
			movesToGo = avgNumOfMovesPerGame - avgNumOfMovesPerGame*(phaseScore/params.GAME_PHASE_END_GAME_UPPER);
		}
		return position.isWhitesTurn ? ((whiteTime <= 12000 ? whiteTime : whiteTime - 10000) + movesToGo*whiteIncrement)/movesToGo :
			((blackTime <= 12000 ? blackTime : blackTime - 10000) + movesToGo*blackIncrement)/movesToGo;
	}
	private long computeSearchTimeExtension(long origSearchTime, Long whiteTime, Long blackTime, Long whiteIncrement,
			Long blackIncrement, Integer movesToGo) {
		// @!TODO
		if (searchStats.getScoreType() != ScoreType.LOWER_BOUND || searchStats.getScoreType() != ScoreType.UPPER_BOUND) {
			
		}
		if (scoreFluctuation >= params.SCORE_FLUCTUATION_LIMIT) {
			
		}
		if (timeOfLastSearchResChange >= System.currentTimeMillis() -
				origSearchTime/params.FRACTION_OF_ORIG_SEARCH_TIME_SINCE_LAST_RESULT_CHANGE_LIMIT) {
			
		}
		if (numOfSearchResChanges/(origSearchTime/1000) >= params.RESULT_CHANGES_PER_SECOND_LIMIT) {
			
		}
		return 0;
	}
	@Override
	public synchronized void init() {
		params = new Parameters();
		try {
			position = Position.parse(Position.START_POSITION_FEN);
		} catch (ChessParseException e) { }
		book = Book.getInstance();
		settings = new HashMap<>();
		Setting.Builder factory = new Setting.Builder();
		hashSize = factory.buildNumberSetting("Hash", 64, 8, 512);
		useBook = factory.buildBoolSetting("UseBook", false);
		bookPath = factory.buildStringSetting("BookPath", Book.DEFAULT_BOOK_FILE_PATH);
		useOwnBookAsSecondary = factory.buildBoolSetting("UseOwnBookAsSecondary", false);
		settings.put(hashSize, hashSize.getDefaultValue());
		settings.put(useBook, useBook.getDefaultValue());
		settings.put(bookPath, bookPath.getDefaultValue());
		settings.put(useOwnBookAsSecondary, useOwnBookAsSecondary.getDefaultValue());
		searchStats = new SearchStatistics();
		searchStats.addObserver(this);
		setHashSize((int)settings.get(hashSize));
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
	public float getHashLoad() {
		long load, capacity;
		capacity = tT.getCapacity() + eT.getCapacity() + pT.getCapacity();
		load = tT.getLoad() + eT.getLoad() + pT.getLoad();
		return load/capacity;
	}
	@Override
	public Set<Map.Entry<Setting<?>, Object>> getOptions() {
		Set<Map.Entry<Setting<?>, Object>> set = new HashSet<>();
		for (Map.Entry<Setting<?>, Object> e : settings.entrySet())
			set.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
		return set;
	}
	@Override
	public synchronized <T> boolean setOption(Setting<T> setting, T value) {
		if (value == null) return false;
		if (hashSize.equals(setting)) {
			if (hashSize.getMin().intValue() <= ((Number)value).intValue() &&
					hashSize.getMax().intValue() >= ((Number)value).intValue()) {
				settings.put(hashSize, value);
				setHashSize(((Number)value).intValue());
				return true;
			}
		}
		else if (useBook.equals(setting)) {
			settings.put(useBook, value);
			return true;
		}
		else if (bookPath.equals(setting)) {
			if (book.setMainBookPath((String)value)) {
				settings.put(bookPath, value);
				return true;
			}
		}
		else if (useOwnBookAsSecondary.equals(setting)) {
			if ((Boolean)value == true) {
				if (book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH)) {
					settings.put(useOwnBookAsSecondary, value);
					return true;
				}
			}
			else {
				book.setSecondaryBookPath(null);
				settings.put(useOwnBookAsSecondary, value);
				return true;
			}
		}
		return false;
	}
	@Override
	public synchronized void newGame() {
		tT.clear();
		eT.clear();
		pT.clear();
		hT.reset();
		gen = 0;
		System.gc();
	}
	@Override
	public synchronized boolean position(String fen) {
		try {
			position = Position.parse(fen);
			return true;
		} catch (ChessParseException e) {
			e.printStackTrace();
			return false;
		}
	}
	@Override
	public synchronized String search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth, Long nodes, Short mateDistance,
			Long searchTime, Boolean infinite) {
		Set<Move> moves;
		long time;
		searchResult = null;
		scoreFluctuation = null;
		timeOfLastSearchResChange = null;
		numOfSearchResChanges = 0;
		if ((Boolean)settings.get(useBook)) {
			search = new Thread(() -> {
				searchResult = book.getMove(position, SelectionModel.STOCHASTIC);
			});
			search.start();
			try {
				search.join();
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
		else {
			if (searchMoves != null && !searchMoves.isEmpty()) {
				moves = new HashSet<>();
				try {
					for (String s : searchMoves)
						moves.add(position.parsePACN(s));
				} catch (ChessParseException | NullPointerException e) {
					e.printStackTrace();
					return null;
				}
			}
			else
				moves = null;
			search = new Search(position, searchStats, ponder || infinite, depth == null ? 0 : depth, nodes == null ? 0 : nodes,
					moves, hT, gen, tT, eT, pT, params);
			search.start();
			time = searchTime == null || searchTime == 0 ?
					computeSearchTime(whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo) : searchTime;
			try {
				wait(time);
			} catch (InterruptedException e) { e.printStackTrace(); }
			if (searchTime == null || searchTime == 0) {
				try {
					wait(computeSearchTimeExtension(time, whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo));
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
			if (search.isAlive()) {
				search.interrupt();
				try {
					search.join();
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
		return searchResult == null ? null : searchResult.toString();
	}
	@Override
	public String stop() {
		if (search != null && search.isAlive()) {
			search.interrupt();
			try {
				search.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return searchResult == null ? null : searchResult.toString();
		}
		return null;
	}
	@Override
	public SearchInfo getInfo() {
		return searchStats;
	}
	@Override
	public void update(Observable o, Object arg) {
		SearchStatistics stats = (SearchStatistics)o;
		Move newSearchRes = stats.getPvLine() != null ? stats.getPvLine().getHead() : null;
		if ((searchResult != null && newSearchRes == null) || (searchResult == null && newSearchRes != null) ||
				(searchResult != null && newSearchRes != null && !searchResult.equals(newSearchRes))) {
			if (searchResult != null)
				scoreFluctuation = (short) (stats.getScore() - searchResult.value);
			searchResult = newSearchRes;
			searchResult.value = stats.getScore();
			timeOfLastSearchResChange = System.currentTimeMillis();
			numOfSearchResChanges++;
		}
	}
}
