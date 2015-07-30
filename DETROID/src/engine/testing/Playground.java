package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
		long start = System.currentTimeMillis();
		System.out.println(b.perftWithMoveConsoleOutput(2));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
