package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -");
		long start = System.currentTimeMillis();
		LongQueue moves = b.generateMoves();
		System.out.println(moves.length());
		Move.printMovesToConsole(moves);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
