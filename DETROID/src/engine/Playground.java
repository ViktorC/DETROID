package engine;

import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import engine.Book.SelectionModel;
import engine.Search.Results;
import util.HashTable;
import util.List;
import util.Stack;

public class Playground {

	final static String tP1 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
	final static String tP2 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
	final static String tP3 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
	
	public static class PVO implements Observer {

		@Override
		public void update(Observable arg0, Object arg1) {
			Results res = (Results)arg0;
			System.out.print(res.getPvString() + (res.isFinal() ? res.getStatString() : ""));
		}
		
	}
	
	static byte gen = 0;
	
	public static void main(String[] args) throws ChessParseException {
		Results r;
		Move playerMove, bookMove;
		List<Move> moveRestrictions = null;
		Position p = Position.parse(Position.START_POSITION_FEN);
		RelativeHistoryTable hT = new RelativeHistoryTable();
		HashTable<TTEntry> tT = new HashTable<>(64);
		HashTable<ETEntry> eT = new HashTable<>(60);
		HashTable<PTEntry> pT = new HashTable<>(4);
		Book book = Book.getInstance();
		Scanner in = new Scanner(System.in);
		boolean outOfBook = false;
		while (p.getMoves().length() != 0) {
			if (p.isWhitesTurn) {
				if (!outOfBook) {
					bookMove = book.getMove(p, SelectionModel.STOCHASTIC);
					if (bookMove != null) {
						System.out.println("Book move found: " + bookMove);
						moveRestrictions = new Stack<>(bookMove);
					}
					else {
						moveRestrictions = null;
						outOfBook = true;
					}
				}
				Search s = new Search(p, 0, 0, 10000, 0, 0, moveRestrictions, hT, gen, tT, eT, pT, 1);
				r = s.getResults();
				r.addObserver(new PVO());
				s.run();
				p.makeMove(r.getPvLine().getHead());
				if (gen == 127) {
					tT.clear();
					eT.clear();
					pT.clear();
					gen = 0;
				}
				//!FIXME If the tables are not cleared after each search, the PVs are cut short.
				else {
					tT.remove(e -> e.generation <= gen);
					eT.remove(e -> e.generation <= gen);
					pT.remove(e -> e.generation <= gen);
				}
				hT.decrementCurrentValues();
				gen++;
			}
			else {
				System.out.print("Make your move: ");
				while (true) {
					try {
						playerMove = p.parsePACN(in.nextLine());
						if (p.isLegal(playerMove)) {
							p.makeMove(playerMove);
							break;
						}
						else
							System.out.print("Illegal move. Try again: ");
					}
					catch (ChessParseException e) {
						System.out.print("Wrong format. Try again: ");
					}
				}
			}
		}
		in.close();
	}
}
