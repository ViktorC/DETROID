package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
		long start = System.currentTimeMillis();
		System.out.println(b.perft(3));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
