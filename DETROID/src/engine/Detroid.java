package engine;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import engine.Book.SelectionModel;
import engine.Game.Side;
import uci.Engine;
import uci.ScoreType;
import uci.SearchInfo;
import uci.Setting;
import util.*;

public class Detroid implements Engine, Observer {
	
	private final static float VERSION_NUMBER = 1.00f;
	private final static String NAME = "DETROID" + " " + VERSION_NUMBER;
	private final static String AUTHOR = "Viktor Csomor";
	
	private Setting<?> hashSize;
	private Setting<?> ponder;
	private Setting<?> ownBook;
	private Setting<?> bookPath;
	private Setting<?> useOwnBookAsSecondary;
	private Setting<?> uciOpponent;
	private HashMap<Setting<?>, Object> settings;
	
	private Parameters params;
	private Game game;
	private boolean newGame;
	private Book book;
	private Thread search;
	private boolean ponderHit;
	private SearchStatistics searchStats;
	private Move searchResult;
	private Short scoreFluctuation;
	private Long timeOfLastSearchResChange;
	private Integer numOfSearchResChanges;
	private RelativeHistoryTable hT;
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen;
	
	public Detroid() {
		
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
		final int AVG_MOVES_PER_GAME = 40;
		whiteIncrement = whiteIncrement == null ? 0 : whiteIncrement;
		blackIncrement = blackIncrement == null ? 0 : blackIncrement;
		if (movesToGo == null) {
			phaseScore = new Evaluator(params, eT, pT, gen).phaseScore(game.getPosition());
			movesToGo = AVG_MOVES_PER_GAME - AVG_MOVES_PER_GAME*(phaseScore/params.GAME_PHASE_END_GAME_UPPER);
		}
		return game.getSideToMove() == Side.WHITE ?
			((whiteTime <= 12000 ? whiteTime : whiteTime - 10000) + movesToGo*whiteIncrement)/movesToGo :
			((blackTime <= 12000 ? blackTime : blackTime - 10000) + movesToGo*blackIncrement)/movesToGo;
	}
	private long computeSearchTimeExtension(long origSearchTime, Long whiteTime, Long blackTime, Long whiteIncrement,
			Long blackIncrement, Integer movesToGo) {
		if (searchStats.getScoreType() == ScoreType.LOWER_BOUND || searchStats.getScoreType() == ScoreType.UPPER_BOUND ||
				scoreFluctuation >= params.SCORE_FLUCTUATION_LIMIT || timeOfLastSearchResChange >= System.currentTimeMillis() -
				origSearchTime/params.FRACTION_OF_ORIG_SEARCH_TIME_SINCE_LAST_RESULT_CHANGE_LIMIT ||
				numOfSearchResChanges/(origSearchTime/1000) >= params.RESULT_CHANGES_PER_SECOND_LIMIT) {
			return computeSearchTime(game.getSideToMove() == Side.WHITE ? whiteTime - origSearchTime : whiteTime,
				game.getSideToMove() == Side.WHITE ? blackTime : blackTime - origSearchTime, whiteIncrement, blackIncrement, movesToGo);
		}
		return 0;
	}
	@Override
	public synchronized void init() {
		params = new Parameters();
		try {
			game = new Game(Position.START_POSITION_FEN);
		} catch (ChessParseException e) { }
		try {
			book = new Book();
		} catch (IOException e) { e.printStackTrace(); }
		settings = new HashMap<>();
		Setting.Builder factory = new Setting.Builder();
		hashSize = factory.buildIntegerSetting("Hash", 16, 1, 512);
		ponder = factory.buildBooleanSetting("Ponder", true);
		ownBook = factory.buildBooleanSetting("OwnBook", false);
		bookPath = factory.buildStringSetting("BookPath", Book.DEFAULT_BOOK_FILE_PATH);
		useOwnBookAsSecondary = factory.buildBooleanSetting("UseOwnBookAsSecondary", false);
		uciOpponent = factory.buildStringSetting("UCI_Opponent", null);
		settings.put(hashSize, hashSize.getDefaultValue());
		settings.put(ponder, ponder.getDefaultValue());
		settings.put(ownBook, ownBook.getDefaultValue());
		settings.put(bookPath, bookPath.getDefaultValue());
		settings.put(useOwnBookAsSecondary, useOwnBookAsSecondary.getDefaultValue());
		settings.put(uciOpponent, uciOpponent.getDefaultValue());
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
	public Set<Setting<?>> getOptions() {
		Set<Setting<?>> set = new HashSet<>();
		for (Setting<?> e : settings.keySet())
			set.add(e);
		return set;
	}
	@Override
	public synchronized <T> boolean setOption(Setting<T> setting, T value) {
		if (value == null) return false;
		if (hashSize.equals(setting)) {
			if (hashSize.getMin().intValue() <= ((Integer)value).intValue() &&
					hashSize.getMax().intValue() >= ((Integer)value).intValue()) {
				settings.put(hashSize, value);
				setHashSize(((Integer)value).intValue());
				return true;
			}
		}
		else if (ponder.equals(setting)) {
			settings.put(ponder, value);
			return true;
		}
		else if (ownBook.equals(setting)) {
			settings.put(ownBook, value);
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
		else if (uciOpponent.equals(setting)) {
			settings.put(uciOpponent, value);
			return true;
		}
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
		System.gc();
	}
	@Override
	public synchronized boolean position(String fen) {
		try {
			Position pos = Position.parse(fen);
			if (!game.getStartPos().equals(pos.toString())) {
				newGame();
				game = new Game(pos.toString());
			}
			else {
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
			e.printStackTrace();
			return false;
		}
	}
	@Override
	public boolean play(String pacn) {
		return game.play(pacn);
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
		if (newGame) {
			if (game.getSideToMove() == Side.WHITE) {
				game.setWhitePlayerName(NAME);
				game.setBlackPlayerName((String)settings.get(uciOpponent));
			}
			else {
				game.setWhitePlayerName((String)settings.get(uciOpponent));
				game.setBlackPlayerName(NAME);
			}
			newGame = false;
		}
		if ((Boolean)settings.get(ownBook)) {
			search = new Thread(() -> {
				searchResult = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
			});
			search.start();
			try {
				search.join();
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
		else {
			if (ponder) {
				if (!(Boolean)settings.get(ponder))
					return null;
			}
			if (searchMoves != null && !searchMoves.isEmpty()) {
				moves = new HashSet<>();
				try {
					for (String s : searchMoves)
						moves.add(game.getPosition().parsePACN(s));
				} catch (ChessParseException | NullPointerException e) {
					e.printStackTrace();
					return null;
				}
			}
			else
				moves = null;
			search = new Search(game.getPosition(), searchStats, ponder || infinite, depth == null ? (mateDistance == null ?  0 : mateDistance) : depth,
					nodes == null ? 0 : nodes, moves, hT, gen, tT, eT, pT, params);
			search.start();
			if (ponder) {
				while (search.isAlive() && !ponderHit) {
					try {
						wait(200);
					} catch (InterruptedException e) { e.printStackTrace(); }
				}
				if (ponderHit)
					ponderHit = false;
			}
			time = searchTime == null || searchTime == 0 ?
					computeSearchTime(whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo) : searchTime;
			try {
				search.join(time);
			} catch (InterruptedException e) { e.printStackTrace(); }
			if (searchTime == null || searchTime == 0) {
				try {
					search.join(computeSearchTimeExtension(time, whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo));
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
	public void stop() {
		if (search != null && search.isAlive())
			search.interrupt();
	}
	@Override
	public void ponderhit() {
		ponderHit = true;
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
