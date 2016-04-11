package engine;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Observer;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import engine.Book.SelectionModel;
import engine.Search.Results;
import util.HashTable;

public class Engine implements UCI {

	private final static InputStream DEFAULT_INPUT_STREAM = System.in;
	private final static OutputStream DEFAULT_OUTPUT_STREAM = System.out;
	private final static int DEFAULT_HASH_SIZE = 128;
	
	private final static Engine INSTANCE = new Engine();
	
	private ThreadPoolExecutor background;
	private Future<?> backgroundTask;
	
	private Scanner in;
	private PrintStream out;
	
	private Observer searchResultObserver;
	
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
		background = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);
		setInputStream(DEFAULT_INPUT_STREAM);
		setOutputStream(DEFAULT_OUTPUT_STREAM);
		setHashSize(DEFAULT_HASH_SIZE);
		hT = new RelativeHistoryTable();
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
	public void setHashSize(int maxHashSizeMb) {
		maxHashMemory = maxHashSizeMb <= 64 ? 64 : maxHashSizeMb >= 0.5*Runtime.getRuntime().maxMemory()/(1 << 20) ?
				(int)(0.5*Runtime.getRuntime().maxMemory()/(1 << 20)) : maxHashSizeMb;
	}
	public void setObserver(Observer obs) {
		searchResultObserver = obs;
	}
	private Future<?> submitTask(Runnable task) {
		return background.submit(task);
	}
	private Move tryBook() {
		return book.getMove(game.position, SelectionModel.STOCHASTIC);
	}
	private void ponder(Move move) {
		Search search;
		Position copy = game.position.deepCopy();
		if (move != null && copy.isLegalSoft(move))
			copy.makeMove(move);
		search = new Search(copy, 0, 0, -1, 0, null, hT, gen, tT, eT, pT, Math.max(numOfCores - 1, 1));
		search.run();
	}
	private Move search(long timeLeft, long searchTime, int maxDepth, long maxNodes, Move[] moves) {
		Search search;
		Results res;
		search = new Search(game.position, timeLeft, searchTime, maxDepth, maxNodes, moves, hT, gen, tT, eT, pT, Math.max(numOfCores - 1, 1));
		res = search.getResults();
		if (searchResultObserver != null)
			res.addObserver(searchResultObserver);
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
		}
		return res.getPVline() == null ? null : res.getPVline().getHead();
	}
	public void listen() {
		String command;
		while (in.hasNextLine()) {
			command = in.nextLine();
		}
	}
}
