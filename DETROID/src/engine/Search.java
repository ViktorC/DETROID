package engine;

import util.*;

public class Search {
	
	Position pos;
	Evaluator eval;
	int bestMove;
	
	public Search(Position pos, int depth) {
		this.pos = pos;
		eval = new Evaluator();
		bestMove = negaMax(depth, -100000, 100000);
	}
	private int negaMax(int depth, int alpha, int beta) {
		int move, score, bestMove = -100000;
		IntQueue moves = pos.generateMoves();
		if (moves.length() == 0)
			return -100000;
		if (pos.getFiftyMoveRuleClock() >= 100 || pos.getRepetitions() >= 3)
			return 0;
		if (depth == 0)
			return eval.score(pos);
		while (moves.hasNext()) {
			move = moves.next();
			pos.makeMove(move);
			score = -negaMax(depth - 1, -beta, -alpha);
			pos.unmakeMove();
			if (score > (bestMove & Move.VALUE.mask)) {
				bestMove = score | move;
				if (score > alpha)
					alpha = score;
			}
			if (alpha >= beta)
				break;
		}
		return bestMove;
	}
}
