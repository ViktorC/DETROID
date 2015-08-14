package engine;


public class Playground {

	public static void main(String[] args) {
		Position b = new Position();
		long start = System.currentTimeMillis();
		Search srch = new Search(b, 3);
		System.out.println(Move.pseudoAlgebraicNotation(srch.bestMove));
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
}
