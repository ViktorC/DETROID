package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
		long start = System.currentTimeMillis();
		Search s = new Search(p, 8);
		long end = System.currentTimeMillis();
		Move.printMovesToConsole(s.pV);
		System.out.println(Search.tT.size() + "\n" + Search.tT.load());
		System.out.println(end - start);
	}
}
