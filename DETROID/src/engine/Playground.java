package engine;


public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		long start = System.currentTimeMillis();
		System.out.println(p.perft(6));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
