package engine;

import java.util.HashMap;
import java.util.Observer;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import communication.UCI;
import engine.Book.SelectionModel;
import engine.Search.Results;
import util.*;

public class Engine implements UCI {

	private final static Engine INSTANCE = new Engine();
	
	private final static String NAME = "DETROID";
	private final static String AUTHOR = "Viktor Csomor";
	
	private boolean debug;
	
	private ExecutorService background;
	private List<Future<?>> backgroundTasks;
	
	private Scanner in;
	
	private List<Observer> observers;
	
	private HashMap<String, Setting<?>> settings;
	
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
	
	private Engine() {
		background = Executors.newCachedThreadPool();
		backgroundTasks = new Queue<>();
		params = new Parameters();
		book = Book.getInstance();
		observers = new Queue<>();
		settings = new HashMap<>();
		in = new Scanner(System.in);
		SettingFactory factory = new SettingFactory();
		settings.put("Hash", factory.buildNumberSetting("Hash", 64, 8, 512));
		settings.put("UseOwnBook", factory.buildBoolSetting("OwnBook", false));
		settings.put("OwnBookPath", factory.buildStringSetting("OwnBookPath", Book.DEFAULT_BOOK_FILE_PATH));
		settings.put("UseOwnBookAsSecondary", factory.buildBoolSetting("UseOwnBookAsSecondary", false));
		hT = new RelativeHistoryTable(params);
		setHashSize((int)settings.get("Hash").getValue());
		numOfCores = Runtime.getRuntime().availableProcessors();
	}
	public Engine getInstance() {
		return INSTANCE;
	}
	private void setHashSize(int hashSize) {
		int totalHashShares = params.TT_SHARE + params.ET_SHARE + params.PT_SHARE;
		tT = new HashTable<>(hashSize*params.TT_SHARE/totalHashShares, TTEntry.SIZE);
		eT = new HashTable<>(hashSize*params.ET_SHARE/totalHashShares, ETEntry.SIZE);
		pT = new HashTable<>(hashSize*params.PT_SHARE/totalHashShares, PTEntry.SIZE);
	}
	private boolean setBookPath(String path) {
		if ((boolean)settings.get("UseOwnBookAsSecondary").getValue())
			book.setSecondaryBookPath(Book.DEFAULT_BOOK_FILE_PATH);
		return book.setMainBookPath(path);
	}
	@Override
	public boolean uci() {
		// TODO Auto-generated method stub
		return false;
	}
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
	public void subscribe(Observer observer) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void debug(boolean on) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Iterable<Setting<?>> options() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setOption(String name, Object value) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void uciNewGame() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void position(String fen) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String go(Iterable<KeyValuePair<SearchAttributes, ?>> params) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String stop() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void ponderHit() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void quit() {
		// TODO Auto-generated method stub
		
	}
}
