package engine;

import util.*;

public class Evaluator {
	
	
	public int score(Position pos) {
		int score = 0;
		score += BitOperations.getCardinality(pos.whiteQueens)*900;
		score += BitOperations.getCardinality(pos.whiteRooks)*500;
		score += BitOperations.getCardinality(pos.whiteBishops)*300;
		score += BitOperations.getCardinality(pos.whiteKnights)*300;
		score += BitOperations.getCardinality(pos.whitePawns)*100;
		score -= BitOperations.getCardinality(pos.blackQueens)*900;
		score -= BitOperations.getCardinality(pos.blackRooks)*500;
		score -= BitOperations.getCardinality(pos.blackBishops)*300;
		score -= BitOperations.getCardinality(pos.blackKnights)*300;
		score -= BitOperations.getCardinality(pos.blackPawns)*100;
		if (!pos.whitesTurn)
			score *= -1;
		return score; 
	}
}
