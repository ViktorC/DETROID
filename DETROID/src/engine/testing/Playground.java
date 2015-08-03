package engine.testing;

import engine.Board;

public class Playground {

	public static void main(String[] args) {
		Board b = new Board("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
		long start = System.currentTimeMillis();
		b.perftDivide(3);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
