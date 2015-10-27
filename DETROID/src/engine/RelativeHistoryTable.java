package engine;

public class RelativeHistoryTable {

	private int[][] historyT;	//a [piece][destination square] table for the history heuristic
	private int[][] butterflyT;	//a [piece][destination square] table for the butterfly heuristic
	
	public RelativeHistoryTable() {
		historyT = new int[Piece.values().length][64];
		butterflyT = new int[Piece.values().length][64];
	}
}
