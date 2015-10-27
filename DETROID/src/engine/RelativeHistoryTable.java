package engine;

public class RelativeHistoryTable {

	private final static int MAX_VALUE = 1000;
	
	private int[][] historyT;	//an [origin square][destination square] table for the history heuristic
	private int[][] butterflyT;	//an [origin square][destination square] table for the butterfly heuristic
	
	public RelativeHistoryTable() {
		historyT = new int[64][64];
		butterflyT = new int[64][64];
	}
	public void recordSuccessfulMove(Move m) {
		historyT[m.from][m.to] += MAX_VALUE;
		butterflyT[m.from][m.to]++;
	}
	public void recordUnsuccessfulMove(Move m) {
		butterflyT[m.from][m.to]++;
	}
}
