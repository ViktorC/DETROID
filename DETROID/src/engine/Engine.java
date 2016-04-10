package engine;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import engine.Book.SelectionModel;
import engine.Search.Results;
import util.HashTable;
import util.Queue;

public class Engine implements UCI {

	private final static InputStream DEFAULT_INPUT_STREAM = System.in;
	private final static OutputStream DEFAULT_OUTPUT_STREAM = System.out;
	private final static int DEFAULT_HASH_SIZE = 128;
	
	private final static Engine INSTANCE = new Engine();
	
	private Scanner in;
	private PrintStream out;
	
	Observer searchResultObserver;
	
	private Game game;
	private Book book;
	private Search search;
	private RelativeHistoryTable hT;
	private int maxHashMemory;
	private HashTable<TTEntry> tT;		// Transposition table.
	private HashTable<ETEntry> eT;		// Evaluation hash table.
	private HashTable<PTEntry> pT;		// Pawn hash table.
	private byte gen = 0;
	
	private int numOfCores;
	
	boolean verbose;
	
	private Engine() {
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
	private void setInputStream(InputStream in) {
		if (this.in != null)
			this.in.close();
		this.in = new Scanner(in);
	}
	private void setOutputStream(OutputStream out) {
		if (this.out != null)
			this.out.close();
		this.out = new PrintStream(out);
	}
	private void setHashSize(int maxHashSizeMb) {
		maxHashMemory = maxHashSizeMb <= 64 ? 64 : maxHashSizeMb >= 0.5*Runtime.getRuntime().maxMemory()/(1 << 20) ?
				(int)(0.5*Runtime.getRuntime().maxMemory()/(1 << 20)) : maxHashSizeMb;
	}
	private void setObserver(Observer obs) {
		searchResultObserver = obs;
	}
	private Move tryBook() {
		return book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
	}
	private void ponder(Move move) {
		Position copy = game.getPosition().deepCopy();
		if (move != null && copy.isLegalSoft(move))
			copy.makeMove(move);
		search = new Search(copy, 0, 0, 0, 0, null, hT, gen, tT, eT, pT, numOfCores - 1);
		search.run();
	}
	private Move search(long timeLeft, long searchTime, int maxDepth, long maxNodes, Move[] moves) {
		Results res;
		search = new Search(game.getPosition(), timeLeft, searchTime, maxDepth, maxNodes, moves, hT, gen, tT, eT, pT, numOfCores - 1);
		res = search.getResults();
		if (searchResultObserver != null)
			res.addObserver(searchResultObserver);
		search.run();
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
		return res.getPVline().getHead();
	}
	public void listen() {
		String command;
		while (in.hasNextLine()) {
			command = in.nextLine();
		}
	}
}
