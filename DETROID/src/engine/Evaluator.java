package engine;

/**A class used for evaluating a chess position and scoring it in terms of centipawns.
 * 
 * @author Viktor
 *
 */
public class Evaluator {

	private int whiteKingValue = 400;
	private int whiteQueenValue = 900;
	private int whiteRookValue = 500;
	private int whiteBishopValue = 300;
	private int whiteKnightValue = 300;
	private int whitePawnValue = 100;
	
	private int blackKingValue = 400;
	private int blackQueenValue = 900;
	private int blackRookValue = 500;
	private int blackBishopValue = 300;
	private int blackKnightValue = 300;
	private int blackPawnValue = 100;
	
	public long score(Board board) {
		long score = 0;
		score += this.whiteKingValue;
		score += BitOperations.getCardinality(board.getWhiteQueens())*this.whiteQueenValue;
		score += BitOperations.getCardinality(board.getWhiteRooks())*this.whiteRookValue;
		score += BitOperations.getCardinality(board.getWhiteBishops())*this.whiteBishopValue;
		score += BitOperations.getCardinality(board.getWhiteKnights())*this.whiteKnightValue;
		score += BitOperations.getCardinality(board.getWhitePawns())*this.whitePawnValue;
		score -= this.blackKingValue;
		score -= BitOperations.getCardinality(board.getBlackQueens())*this.blackQueenValue;
		score -= BitOperations.getCardinality(board.getBlackRooks())*this.blackRookValue;
		score -= BitOperations.getCardinality(board.getBlackBishops())*this.blackBishopValue;
		score -= BitOperations.getCardinality(board.getBlackKnights())*this.blackKnightValue;
		score -= BitOperations.getCardinality(board.getBlackPawns())*this.blackPawnValue;
		if (board.getTurn())
			return score;
		else
			return -score;
	}
}
