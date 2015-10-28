package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		Search s = Search.getInstance(p);
		long start = System.currentTimeMillis();
		s.start();
		Move.printMovesToConsole(s.getPv());
		System.out.println(s.getTranspositionTableStats());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
