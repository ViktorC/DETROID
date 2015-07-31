package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		long start = System.currentTimeMillis();
		b.perftDivide(3);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
