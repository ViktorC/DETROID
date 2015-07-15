package engine.testing;

import engine.Board;
import engine.Move;
import engine.Board.*;
import util.*;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		long start = System.currentTimeMillis();
		System.out.println(b.toString());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
