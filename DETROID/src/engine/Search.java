package engine;

import util.*;

/**The heart of the chess engine, the search engine. It is responsible for tactical playing strength and foresight.
 * 
 * @author Viktor
 *
 */
public class Search {
	
	Board board;
	static Evaluator eval = new Evaluator();
	
	long bestMove;
	
	public Search(Board board, int depth) {
		this.board = board;
		this.bestMove = this.alphaBetaNegamax(depth, -666666L, 666666L);
	}
	public long getBestMove() {
		return this.bestMove;
	}
	private long alphaBetaNegamax(int depth, long alpha, long beta) {
		LongQueue moves;
		long move, value, bestMove = -666667L << Move.VALUE.shift;
		if (board.getRepetitions() >= 3 || board.getFiftyMoveRuleClock() >= 100)
			return 0;
		if ((moves = board.generateMoves()).length() == 0) {
			if (board.getCheck())
				return (-666666L << Move.VALUE.shift);
			else
				return 0;
		}
		if (depth == 0)
			return (eval.score(board) << Move.VALUE.shift);
		while (moves.hasNext()) {
			move = moves.next();
			this.board.makeMove(move);
			value = -(this.alphaBetaNegamax(depth - 1, -beta, -alpha) >>> Move.VALUE.shift);
			this.board.unMakeMove();
			if (value > (bestMove >>> Move.VALUE.shift)) {
				bestMove = move | (value << Move.VALUE.shift);
				if (value > alpha)
					alpha = value;
			}
			if (alpha >= beta)
				break;
		}
		System.out.println(Move.pseudoAlgebraicNotation(bestMove));
		return bestMove;
	}
}
