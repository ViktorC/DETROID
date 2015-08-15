package engine;

import util.*;

public class Search {
	
	Position pos;
	Evaluator eval;
	Move bestMove;
	
	public Search(Position pos, int depth) {
		this.pos = pos;
		eval = new Evaluator();
		bestMove = negaMax(depth, Game.LOSS, Game.WIN);
	}
	private Move negaMax(int depth, int alpha, int beta) {
		int score;
		Move move, temp, bestMove = new Move();
		Queue<Move> moves = pos.generateMoves();
		if (moves.length() == 0)
			return new Move();
		if (pos.getFiftyMoveRuleClock() >= 100 || pos.getRepetitions() >= 3)
			return new Move(Game.TIE);
		if (depth == 0)
			return new Move(eval.score(pos));
		while (moves.hasNext()) {
			move = moves.next();
			pos.makeMove(move);
			temp = negaMax(depth - 1, -beta, -alpha);
			score = -temp.value;
			pos.unmakeMove();
			if (score > bestMove.value) {
				bestMove.value = score;
				bestMove.from = move.from;
				bestMove.to = move.to;
				bestMove.type = move.type;
				if (score > alpha)
					alpha = score;
			}
			if (alpha >= beta)
				break;
		}
		return bestMove;
	}
}