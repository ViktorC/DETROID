package engine;


public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10; i++)
			p.perft(6);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
