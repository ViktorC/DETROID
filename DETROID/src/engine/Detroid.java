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
		long sizeInBytes = hashSize*1024*1024;
		int totalHashShares = params.TT_SHARE + params.ET_SHARE + params.PT_SHARE;
		tT = new HashTable<>(sizeInBytes*params.TT_SHARE/totalHashShares, TTEntry.SIZE);
		eT = new HashTable<>(sizeInBytes*params.ET_SHARE/totalHashShares, ETEntry.SIZE);
		pT = new HashTable<>(sizeInBytes*params.PT_SHARE/totalHashShares, PTEntry.SIZE);
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
			((whiteTime <= 13000 ? whiteTime : whiteTime - 10000) + Math.max(0, (movesToGo - 1)*whiteIncrement))/movesToGo :
			((blackTime <= 13000 ? blackTime : blackTime - 10000) + Math.max(0, (movesToGo - 1)*blackIncrement))/movesToGo;
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
		game = new Game();
		try {
			book = new Book();
		} catch (IOException e) { e.printStackTrace(); }
		options = new HashMap<>();
		hashSize = new Option.SpinOption("Hash", 32, 1, 512);
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
		searchStats = new SearchStatistics();
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
	public short getHashLoadPermill() {
		long load, capacity;
		capacity = tT.getCapacity() + eT.getCapacity() + pT.getCapacity();
		load = tT.getLoad() + eT.getLoad() + pT.getLoad();
		return (short) (1000*load/capacity);
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
				return true;
			}
		}
		else if (ponder.equals(setting)) {
			options.put(ponder, value);
			return true;
		}
		else if (ownBook.equals(setting)) {
			options.put(ownBook, value);
			return true;
		}
		else if (bookPath.equals(setting)) {
			if (book.setMainBookPath((String)value)) {
				options.put(bookPath, value);
				return true;
			}
		}
		else if (useOwnBookAsSecondary.equals(setting)) {
			if ((Boolean)value == true) {
				if (book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH)) {
					options.put(useOwnBookAsSecondary, value);
					return true;
				}
			}
			else {
				book.setSecondaryBookPath(null);
				options.put(useOwnBookAsSecondary, value);
				return true;
			}
		}
		else if (uciOpponent.equals(setting)) {
			options.put(uciOpponent, value);
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
			Position pos = fen.equals("startpos") ? Position.parse(Position.START_POSITION_FEN) : Position.parse(fen);
			if (newGame)
				game = new Game(pos.toString());
			else if (!game.getStartPos().equals(pos.toString())) {
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
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth, Long nodes, Integer mateDistance,
			Long searchTime, Boolean infinite) {
		Set<Move> moves;
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
			newGame = false;
		}
		if ((Boolean)options.get(ownBook)) {
			search = new Thread(() -> {
				searchResult = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
				searchResult = searchResult == null ? new Move() : searchResult;
			});
			search.start();
			try {
				search.join();
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
		else {
			if (doPonder) {
				if (!(Boolean)options.get(ponder))
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
			search = new Search(game.getPosition(), searchStats, doPonder || (infinite != null && infinite.booleanValue()),
					depth == null ? (mateDistance == null ?  Integer.MAX_VALUE : mateDistance) : depth,
					nodes == null ? Long.MAX_VALUE : nodes, moves, hT, gen, tT, eT, pT, params);
			search.start();
			if (doPonder) {
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
					extraTime = computeSearchTimeExtension(time, whiteTime, blackTime, whiteIncrement, whiteIncrement, movesToGo);
					if (extraTime > 0)
						search.join(extraTime);
				} catch (Exception e) { e.printStackTrace(); }
			}
			if (search.isAlive()) {
				search.interrupt();
				try {
					search.join();
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
		return searchResult.equals(new Move()) ? null : searchResult.toString();
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
	public SearchInfo getSearchInfo() {
		return searchStats;
	}
	@Override
	public void quit() {
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
		SearchStatistics stats = (SearchStatistics)o;
		Move newSearchRes = stats.getPvMoveList() != null ? stats.getPvMoveList().getHead() : new Move();
		scoreFluctuation = (short) (stats.getScore() - searchResult.value);
		searchResult = newSearchRes;
		searchResult.value = stats.getScore();
		timeOfLastSearchResChange = System.currentTimeMillis();
		numOfSearchResChanges++;
	}
}
