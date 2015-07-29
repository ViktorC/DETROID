package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -");
		long start = System.currentTimeMillis();
		System.out.println(b.perftWithMoveConsoleOutput(2));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
