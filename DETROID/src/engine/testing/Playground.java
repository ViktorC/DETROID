package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		long start = System.currentTimeMillis();
		System.out.println(b.perft(5));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
