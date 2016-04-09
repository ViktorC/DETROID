package engine;

import java.util.Observable;
import java.util.Observer;

import util.HashTable;

public class Playground {

	final static String tP1 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
	final static String tP2 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
	final static String tP3 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
	
	public static class PVO implements Observer {

		@Override
		public void update(Observable arg0, Object arg1) {
			System.out.println(arg0);
		}
		
	}
	
	public static void main(String[] args) {
		Position p = new Position("8/8/8/8/2BP4/2nP4/7p/5K1k w - -");
		RelativeHistoryTable hT = new RelativeHistoryTable();
		HashTable<TTEntry> tT = new HashTable<>(64);
		HashTable<ETEntry> eT = new HashTable<>(60);
		HashTable<PTEntry> pT = new HashTable<>(4);
		Search s = new Search(p, 0, 0, 15, 0, null, hT, (byte) 0, tT, eT, pT, 4);
		long start = System.currentTimeMillis();
		s.getResults().addObserver(new PVO());
		s.run();
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
