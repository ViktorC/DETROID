package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
		b.printOffsetBoardToConsole();
		long start = System.currentTimeMillis();
		Move.printMovesToConsole(b.generateMoves());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
