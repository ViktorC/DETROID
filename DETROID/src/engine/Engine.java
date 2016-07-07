package engine;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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

	private final static InputStream DEFAULT_INPUT_STREAM = System.in;
	private final static OutputStream DEFAULT_OUTPUT_STREAM = System.out;
	private final static int DEFAULT_HASH_SIZE = 128;
	
	private final static Engine INSTANCE = new Engine();
	
	private ExecutorService background;
	private List<Future<?>> backgroundTasks;
	
	private Scanner in;
	private PrintStream out;
	
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
		setInputStream(DEFAULT_INPUT_STREAM);
		setOutputStream(DEFAULT_OUTPUT_STREAM);
		setHashSize(DEFAULT_HASH_SIZE);
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
	public void setInputStream(InputStream in) {
		if (this.in != null)
			this.in.close();
		this.in = new Scanner(in);
	}
	public void setOutputStream(OutputStream out) {
		if (this.out != null)
			this.out.close();
		this.out = new PrintStream(out);
	}
	public void subscribe(Observer obs) {
		observers.add(obs);
	}
	private void setHashSize(int maxHashSizeMb) {
		maxHashMemory = maxHashSizeMb <= 64 ? 64 : maxHashSizeMb >= 0.5*Runtime.getRuntime().maxMemory()/(1 << 20) ?
				(int)(0.5*Runtime.getRuntime().maxMemory()/(1 << 20)) : maxHashSizeMb;
	}
	private Move tryBook() {
		return book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
	}
	private void ponder(Move move) {
		Search search;
		Position copy = game.getPosition().deepCopy();
		if (move != null && copy.isLegalSoft(move))
			copy.makeMove(move);
		search = new Search(copy, 0, 0, 0, -1, 0, null, hT, gen, tT, eT, pT, params, Math.max(numOfCores - 1, 1));
		search.run();
	}
	private Move search(long wTimeLeft, long bTimeLeft, long searchTime, int maxDepth, long maxNodes, List<Move> moves) {
		Search search;
		Results res;
		search = game.getPosition().isWhitesTurn ?
			new Search(game.getPosition(), wTimeLeft, bTimeLeft, searchTime, maxDepth, maxNodes, moves,
					hT, gen, tT, eT, pT, params, Math.max(numOfCores - 1, 1)) :
			new Search(game.getPosition(), bTimeLeft, wTimeLeft, searchTime, maxDepth, maxNodes, moves,
					hT, gen, tT, eT, pT, params, Math.max(numOfCores - 1, 1));
		res = search.getResults();
		search.run();
		if (!Thread.currentThread().isInterrupted()) {
			if (gen == 127) {
				tT.clear();
				eT.clear();
				pT.clear();
				gen = 0;
			}
			else {
				tT.remove(e -> e.generation < gen);
				eT.remove(e -> e.generation < gen);
				pT.remove(e -> e.generation < gen - 1);
			}
			hT.decrementCurrentValues();
			gen++;
		}
		return res.getPvLine() == null ? null : res.getPvLine().getHead();
	}
	@Override
	public String uci() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String id() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void debug(boolean on) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String isReady() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public KeyValuePair<String, String>[] options() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setOption(KeyValuePair<String, String> option) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String register() {
		// TODO Auto-generated method stub
		return null;
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
	public String go(KeyValuePair<String, String>[] params) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
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
