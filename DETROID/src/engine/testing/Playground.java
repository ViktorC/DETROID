package engine.testing;

import engine.Board;
import engine.Move;
import engine.Search;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		long start = System.currentTimeMillis();
		System.out.println(Move.pseudoAlgebraicNotation((new Search(b, 5)).getBestMove()));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
