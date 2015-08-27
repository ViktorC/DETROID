package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -");
		long start = System.currentTimeMillis();
		Search s = new Search(p, 2);
		Move.printMovesToConsole(s.pV);
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
