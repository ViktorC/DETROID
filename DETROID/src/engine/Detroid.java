package engine;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observer;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	private Game game;
	private Book book;
	private Search search;
	private SearchStatistics searchStats;
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
	private boolean setBookPath(String path) {
		if ((boolean)settings.get(useOwnBookAsSecondary))
			book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH);
		return book.setMainBookPath(path);
	}
	@Override
	public void init() {
		params = new Parameters();
		book = Book.getInstance();
		settings = new HashMap<>();
		SettingFactory factory = new SettingFactory();
		hashSize = factory.buildNumberSetting("Hash", 64, 8, 512);
		useBook = factory.buildBoolSetting("UseBook", false);
		bookPath = factory.buildStringSetting("BookPath", Book.DEFAULT_BOOK_FILE_PATH);
		useOwnBookAsSecondary = factory.buildBoolSetting("UseOwnBookAsSecondary", false);
		settings.put(hashSize, hashSize.getDefaultValue());
		settings.put(useBook, useBook.getDefaultValue());
		settings.put(bookPath, bookPath.getDefaultValue());
		settings.put(useOwnBookAsSecondary, useOwnBookAsSecondary.getDefaultValue());
		searchStats = new SearchStatistics();
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
	public <T> boolean setOption(Setting<T> setting, T value) {
		if (value == null) return false;
		if (hashSize.equals(setting)) {
			if (hashSize.getMin().intValue() <= ((Number)value).intValue() &&
					hashSize.getMax().intValue() >= ((Number)value).intValue()) {
				settings.put(hashSize, value);
				setHashSize(((Number)value).intValue());
			}
		}
		else if (useBook.equals(setting)) {
			
		}
		else if (bookPath.equals(setting)) {
			
		}
		else if (useOwnBookAsSecondary.equals(setting)) {
			
		}
		return false;
	}
	@Override
	public void newGame() {
		tT.clear();
		eT.clear();
		pT.clear();
		hT.reset();
		gen = 0;
		System.gc();
	}
	@Override
	public boolean position(String fen) {
		try {
			if (!game.getPosition().toString().equals(fen))
				game.setPosition(Position.parse(fen));
			return true;
		} catch (ChessParseException e) {
			return false;
		}
	}
	@Override
	public String search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
			Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth,
			Long nodes, Short mateDistance, Long searchTime, Boolean infinite) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String stop() {
		if (search != null && search.isAlive()) {
			search.interrupt();
			try {
				search.join();
			} catch (InterruptedException e) { }
			return searchStats.getPvLine() != null ? searchStats.getPvLine().getHead().toString() : null;
		}
		return null;
	}
	@Override
	public SearchInfo getInfo() {
		return searchStats;
	}
}
