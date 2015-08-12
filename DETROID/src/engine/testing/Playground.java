package engine.testing;

import engine.Position;

public class Playground {

	public static void main(String[] args) {
		Position b = new Position();
		long start = System.currentTimeMillis();
		System.out.println(b.perft(6));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
