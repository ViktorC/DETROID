package engine;


public class Playground {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		Position p = new Position();
		System.out.println(p.perft(6));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
