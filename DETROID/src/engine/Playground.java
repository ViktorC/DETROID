package engine;

public class Playground {

	public static void main(String[] args) {
		Position p = new Position("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
		long start = System.currentTimeMillis();
		System.out.println(p.stagedQuickPerft(6));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
