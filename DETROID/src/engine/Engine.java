package engine;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Observer;
import java.util.Scanner;

import engine.Book.SelectionModel;
import engine.Search.Results;
import util.HashTable;
import util.Queue;

public class Engine implements UCI {

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
	
	public Engine(InputStream in, OutputStream out, Observer searchResultObserver, int maxHashSizeMb) {
		this.in = new Scanner(in);
		this.out = new PrintStream(out);
		this.searchResultObserver = searchResultObserver;
		maxHashMemory = maxHashSizeMb <= 64 ? 64 : maxHashSizeMb >= 0.75*Runtime.getRuntime().maxMemory()/(1 << 20) ?
				(int)(0.75*Runtime.getRuntime().maxMemory()/(1 << 20)) : maxHashSizeMb;
		hT = new RelativeHistoryTable();
		tT = new HashTable<>(maxHashMemory/2, TTEntry.SIZE);
		eT = new HashTable<>(maxHashMemory*15/32, ETEntry.SIZE);
		pT = new HashTable<>(maxHashMemory/32);
		book = Book.getInstance();
		numOfCores = Runtime.getRuntime().availableProcessors();
	}
	private Move tryBook() {
		return book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
	}
	private void ponder(Move move) {
		Position copy = game.getPosition().deepCopy();
		if (move != null && copy.isLegalSoft(move))
			copy.makeMove(move);
		search = new Search(copy, 0, 0, null, hT, gen, tT, eT, pT, numOfCores - 1);
		search.run();
	}
	private Move search(long timeLeft, long maxNodes, Move[] moves) {
		Results res;
		search = new Search(game.getPosition(), timeLeft, maxNodes, moves, hT, gen, tT, eT, pT, numOfCores - 1);
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
