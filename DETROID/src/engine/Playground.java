package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position();
		long start = System.currentTimeMillis();
		Search s = new Search(p, 6);
		long end = System.currentTimeMillis();
		Move.printMovesToConsole(s.pV);
		System.out.println(Search.tT.size() + "\n" + Search.tT.load());
		System.out.println(end - start);
	}
}
