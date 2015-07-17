package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		b.printOffsetBoardToConsole();
		long start = System.currentTimeMillis();
		Move.printMovesToConsole(b.generateMoves());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
