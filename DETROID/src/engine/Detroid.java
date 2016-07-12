package engine;

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
	
	private final static String NAME = "DETROID";
	private final static String AUTHOR = "Viktor Csomor";
	
	private boolean debug;
	
	private ExecutorService background;
	private List<Future<?>> backgroundTasks;
	
	private Scanner in;
	
	private List<Observer> observers;
	
	private HashMap<Setting<?>, Object> settings;
	
	private Parameters params;
	private Game game;
	private boolean useBook;
	private Book book;
	private RelativeHistoryTable hT;
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen = 0;
	
	private int numOfCores;
	
	boolean verbose;
	
	private Detroid() {
		background = Executors.newCachedThreadPool();
		backgroundTasks = new Queue<>();
		params = new Parameters();
		book = Book.getInstance();
		observers = new Queue<>();
		settings = new HashMap<>();
		in = new Scanner(System.in);
//		SettingFactory factory = new SettingFactory();
//		settings.put(factory.buildNumberSetting("Hash", 64, 8, 512), 8);
//		settings.put(factory.buildBoolSetting("UseOwnBook", false), false);
//		settings.put(factory.buildStringSetting("OwnBookPath", Book.DEFAULT_BOOK_FILE_PATH), Book.DEFAULT_BOOK_FILE_PATH);
//		settings.put(factory.buildBoolSetting("UseOwnBookAsSecondary", false), false);
//		hT = new RelativeHistoryTable(params);
//		setHashSize((int)settings..getValue());
		numOfCores = Runtime.getRuntime().availableProcessors();
	}
	public Detroid getInstance() {
		return INSTANCE;
	}
	private void setHashSize(int hashSize) {
		int totalHashShares = params.getTT_SHARE() + params.getET_SHARE() + params.getPT_SHARE();
		tT = new HashTable<>(hashSize*params.getTT_SHARE()/totalHashShares, TTEntry.SIZE);
		eT = new HashTable<>(hashSize*params.getET_SHARE()/totalHashShares, ETEntry.SIZE);
		pT = new HashTable<>(hashSize*params.getPT_SHARE()/totalHashShares, PTEntry.SIZE);
	}
//	private boolean setBookPath(String path) {
//		if ((boolean)settings.get("UseOwnBookAsSecondary").getValue())
//			book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH);
//		return book.setMainBookPath(path);
//	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getAuthor() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public float getHashLoad() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Iterable<Setting<?>> getOptions() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public <T> void setOption(Setting<T> setting, T value) {
		// TODO Auto-generated method stub
		
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
