package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
		long start = System.currentTimeMillis();
		Search s = Search.getInstance(p);
		s.run();
		Move.printMovesToConsole(s.getPv());
		System.out.println(s.getTranspositionTableStats());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
