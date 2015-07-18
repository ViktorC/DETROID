package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -");
		b.printOffsetBoardToConsole();
		long start = System.currentTimeMillis();
		LongQueue moves = b.generateMoves();
		System.out.println(moves.length() + "\n");
		Move.printMovesToConsole(moves);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
