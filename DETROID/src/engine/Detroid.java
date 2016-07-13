package engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Observer;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import chess.Book;
import chess.ETEntry;
import chess.Game;
import chess.Move;
import chess.PTEntry;
import chess.Parameters;
import chess.RelativeHistoryTable;
import chess.SearchArguments;
import chess.SearchStatistics;
import chess.TTEntry;
import util.*;

public class Detroid implements Engine {

	private final static Detroid INSTANCE = new Detroid();
	
	private final static String NAME = "DETROID v1.00";
	private final static String AUTHOR = "Viktor Csomor";
	
	private boolean debug;
	
	private ExecutorService background;
	private List<Future<?>> backgroundTasks;
	
	private Scanner in;
	
	private List<Observer> observers;
	
	private Setting<?> hashSize;
	private Setting<?> useBook;
	private Setting<?> bookPath;
	private Setting<?> useOwnBookAsSecondary;
	private HashMap<Setting<?>, Object> settings;
	
	private Parameters params;
	private Game game;
	private Book book;
	private RelativeHistoryTable hT;
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen = 0;
	
	private Detroid() {
		background = Executors.newCachedThreadPool();
		backgroundTasks = new Queue<>();
		params = new Parameters();
		book = Book.getInstance();
		observers = new Queue<>();
		settings = new HashMap<>();
		in = new Scanner(System.in);
		SettingFactory factory = new SettingFactory();
		hashSize = factory.buildNumberSetting("Hash", 64, 8, 512);
		useBook = factory.buildBoolSetting("UseOwnBook", false);
		bookPath = factory.buildStringSetting("OwnBookPath", Book.DEFAULT_BOOK_FILE_PATH);
		useOwnBookAsSecondary = factory.buildBoolSetting("UseOwnBookAsSecondary", false);
		settings.put(hashSize, hashSize.getDefaultValue());
		settings.put(useBook, useBook.getDefaultValue());
		settings.put(bookPath, bookPath.getDefaultValue());
		settings.put(useOwnBookAsSecondary, useOwnBookAsSecondary.getDefaultValue());
		setHashSize((int)settings.get(hashSize));
		hT = new RelativeHistoryTable(params);
	}
	public Detroid getInstance() {
		return INSTANCE;
	}
	private void setHashSize(int hashSize) {
		int totalHashShares = params.TT_SHARE + params.ET_SHARE + params.PT_SHARE;
		tT = new HashTable<>(hashSize*params.TT_SHARE/totalHashShares, TTEntry.SIZE);
		eT = new HashTable<>(hashSize*params.ET_SHARE/totalHashShares, ETEntry.SIZE);
		pT = new HashTable<>(hashSize*params.PT_SHARE/totalHashShares, PTEntry.SIZE);
	}
	private boolean setBookPath(String path) {
		if ((boolean)settings.get(useOwnBookAsSecondary))
			book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH);
		return book.setMainBookPath(path);
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
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Collection<Setting<?>> getOptions() {
		return settings.keySet();
	}
	@Override
	public <T> boolean setOption(Setting<T> setting, T value) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void newGame() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setGame(String pgn) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void position(String fen) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Move search(SearchArguments args) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Move stop() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public SearchStatistics getSearchStats() {
		// TODO Auto-generated method stub
		return null;
	}
}
