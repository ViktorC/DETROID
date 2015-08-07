package engine.testing;

import engine.Board;
import engine.Move;
import engine.Search;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		long start = System.currentTimeMillis();
		System.out.println(((-666667L << Move.VALUE.getShift()) >>> Move.VALUE.getShift()) & Move.VALUE.getMask());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
