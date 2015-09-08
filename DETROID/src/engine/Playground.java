package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		long start = System.currentTimeMillis();
		Search s = new Search(p);
		s.run();
		long end = System.currentTimeMillis();
		Move.printMovesToConsole(s.getPv());
		System.out.println(s.tT.size() + "\n" + s.tT.load());
		System.out.println(end - start);
	}
}
