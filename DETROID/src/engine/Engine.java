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
		maxHashMemory = maxHashSizeMb <= 64 || maxHashSizeMb >= Runtime.getRuntime().maxMemory()/(1 << 20) ?
				(int)(Runtime.getRuntime().maxMemory()/(1 << 20))/2 : maxHashSizeMb;
		hT = new RelativeHistoryTable();
		tT = new HashTable<>(maxHashMemory/2, TTEntry.SIZE);
		eT = new HashTable<>(maxHashMemory*15/32, ETEntry.SIZE);
		pT = new HashTable<>(maxHashMemory/32);
		book = Book.getInstance();
		numOfCores = Runtime.getRuntime().availableProcessors();
	}
	private void ponder(Move move) {
		Position copy = game.getPosition().deepCopy();
		if (move != null && copy.isLegalSoft(move))
			copy.makeMove(move);
		GamePhase phase = Evaluator.evaluateGamePhase(copy);
		search = new Search(copy, 0, 0, null, phase, hT, gen, tT, eT, pT);
		search.run();
	}
	private Queue<Move> search(long timeLeft, long maxNodes, Move[] moves) {
		long start, end;
		Move bookMove;
		Results res;
		start = System.currentTimeMillis();
		GamePhase phase = Evaluator.evaluateGamePhase(game.getPosition());
		if (phase == GamePhase.OPENING) {
			bookMove = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
			if (bookMove != null)
				return new Queue<>(bookMove);
		}
		end = System.currentTimeMillis();
		gen++;
		search = new Search(game.getPosition(), timeLeft - (end - start), maxNodes, moves, phase, hT, gen, tT, eT, pT);
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
		return res.getPVline();
	}
	public void listen() {
		String command;
		while (in.hasNextLine()) {
			command = in.nextLine();
		}
	}
}
