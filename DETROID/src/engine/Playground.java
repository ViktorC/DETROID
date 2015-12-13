package engine;

public class Playground {

	final static String tP1 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
	final static String tP2 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
	final static String tP3 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
	
	public static void main(String[] args) {
		Position p = new Position("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4");
		long start = System.currentTimeMillis();
		System.out.println(Long.toHexString(Zobrist.getPolyglotHashKey(p)));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
