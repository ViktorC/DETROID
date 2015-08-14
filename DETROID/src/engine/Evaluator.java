package engine;

public class Evaluator {
	
	public int score(Position pos) {
		int score = 0;
		score += BitOperations.getCardinality(pos.getWhiteQueens())*900;
		score += BitOperations.getCardinality(pos.getWhiteRooks())*500;
		score += BitOperations.getCardinality(pos.getWhiteBishops())*300;
		score += BitOperations.getCardinality(pos.getWhiteKnights())*300;
		score += BitOperations.getCardinality(pos.getWhitePawns())*100;
		score -= BitOperations.getCardinality(pos.getBlackQueens())*900;
		score -= BitOperations.getCardinality(pos.getBlackRooks())*500;
		score -= BitOperations.getCardinality(pos.getBlackBishops())*300;
		score -= BitOperations.getCardinality(pos.getBlackKnights())*300;
		score -= BitOperations.getCardinality(pos.getBlackPawns())*100;
		if (!pos.getTurn())
			score *= -1;
		return score; 
	}
}
