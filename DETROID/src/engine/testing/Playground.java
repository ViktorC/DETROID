package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board();
		long start = System.currentTimeMillis();
		b.printStateToConsole();
		System.out.println(b.makeMove("e   2 e 4"));
		b.printStateToConsole();
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
