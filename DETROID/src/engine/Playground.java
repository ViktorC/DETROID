package engine;


public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		long start = System.currentTimeMillis();
		Search s = new Search(p, 8);
		System.out.println(s.bestMove);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
