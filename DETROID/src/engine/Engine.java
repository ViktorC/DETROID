package engine;

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
	private final static int DEFAULT_HASH_SIZE = 64;
	
	private boolean debug;
	
	private ExecutorService background;
	private List<Future<?>> backgroundTasks;
	
	private Scanner in;
	
	private List<Observer> observers;
	
	private Parameters params;
	private Game game;
	private Book book;
	private RelativeHistoryTable hT;
	private int maxHashMemory;
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen = 0;
	
	private int numOfCores;
	
	boolean verbose;
	
	private Engine() {
		background = Executors.newCachedThreadPool();
		backgroundTasks = new Queue<>();
		observers = new Queue<>();
		in = new Scanner(System.in);
		params = new Parameters();
		hT = new RelativeHistoryTable(params);
		tT = new HashTable<>(maxHashMemory/2, TTEntry.SIZE);
		eT = new HashTable<>(maxHashMemory*15/32, ETEntry.SIZE);
		pT = new HashTable<>(maxHashMemory/32);
		book = Book.getInstance();
		numOfCores = Runtime.getRuntime().availableProcessors();
	}
	public Engine getInstance() {
		return INSTANCE;
	}
	private void setHashSize(int maxHashSizeMb) {
		maxHashMemory = maxHashSizeMb <= 64 ? 64 : maxHashSizeMb >= 0.5*Runtime.getRuntime().maxMemory()/(1 << 20) ?
				(int)(0.5*Runtime.getRuntime().maxMemory()/(1 << 20)) : maxHashSizeMb;
	}
	private Move tryBook() {
		return book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
	}
	@Override
	public boolean uci() {
		// The engine supports UCI mode by default.
		return true;
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
	public void subscribe(Observer observer) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void debug(boolean on) {
		debug = on;
	}
	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Iterable<Iterable<KeyValuePair<OptionAttributes, ?>>> options() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setOption(OptionAttributes option, Object value) {
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
