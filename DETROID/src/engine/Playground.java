package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		long start = System.currentTimeMillis();
		Search s = Search.getInstance(p);
		s.run();
		Move.printMovesToConsole(s.getPv());
		System.out.println(s.getTranspositionTableStats());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
