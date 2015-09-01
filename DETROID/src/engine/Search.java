package engine;

import util.*;

/**A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It uses
 * principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative deepening framework.
 * 
 * @author Viktor
 *
 */
public class Search {
	
	Position pos;
	Evaluator eval;
	Move[] pV;
	short ply;
	static HashTable<TTEntry> tT = new HashTable<>();
	static byte tTage = 0;
	
	public Search(Position pos, int depth) {
		this.pos = pos;
		eval = new Evaluator(pos);
		pV = new Move[depth];
		for (short i = 1; i <= depth; i++) {
			ply = i;
			pVsearch(i, Evaluator.LOSS, Evaluator.WIN);
			extractPv();
		}
		tTage++;
		if (tTage == 4)
			tT.clear();
	}
	/**A principal variation search algorithm utilizing a transposition table. It returns only the score for the searched position, but the principal
	 * variation can be extracted from the transposition table after a search has been run.
	 * 
	 * @param depth
	 * @param alpha
	 * @param beta
	 * @return
	 */
	private int pVsearch(int depth, int alpha, int beta) {
		int score, origAlpha = alpha, val;
		Move bestMove, move;
		Queue<Move> moveQ;
		Move[] moveArr;
		TTEntry e = tT.lookUp(pos.zobristKey);
		if (e != null && e.depth >= depth) {
			if (e.type == TTEntry.TYPE_EXACT)
				return e.score;
			else if (e.type == TTEntry.TYPE_FAIL_HIGH) {
				if (e.score > alpha)
					alpha = e.score;
			}
			else
				if (e.score < beta)
					beta = e.score;
			if (beta <= alpha)
				return e.score;
		}
		if (depth == 0) {
			score = eval.score();
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, score, (short)0, tTage));
			return score;
		}
		moveQ = pos.generateMoves();
		if (moveQ.length() == 0) {
			if (pos.getCheck()) {
				tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, Evaluator.LOSS, (short)0, tTage));
				return Evaluator.LOSS;
			}
			else {
				tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, Evaluator.TIE, (short)0, tTage));
				return Evaluator.TIE;
			}
		}
		if (pos.getFiftyMoveRuleClock() >= 100 || pos.getRepetitions() >= 3)
			return Evaluator.TIE;
		moveArr = orderMoves(moveQ, depth);
		bestMove = new Move(Evaluator.LOSS);
		for (int i = 0; i < moveArr.length; i++) {
			move = moveArr[i];
			pos.makeMove(move);
			if (i == 0)
				val = -pVsearch(depth - 1, -beta, -alpha);
			else {
				val = -pVsearch(depth - 1, -alpha - 1, -alpha);
				if (val > alpha && val < beta)
					val = -pVsearch(depth - 1, -beta, -val);
			}
			pos.unmakeMove();
			if (val > bestMove.value) {
				bestMove = move;
				bestMove.value = val;
				if (val > alpha)
					alpha = val;
			}
			if (alpha >= beta)
				break;
		}
		if (bestMove.value <= origAlpha) 
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_FAIL_LOW, bestMove.value, bestMove.toInt(), tTage));
		else if (bestMove.value >= beta)
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_FAIL_HIGH, bestMove.value, bestMove.toInt(), tTage));
		else
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, bestMove.value, bestMove.toInt(), tTage));
		return bestMove.value;
	}
	/**Orders a list of moves according to the PV node of the given depth, history heuristics, and the MVV-LVA principle; and returns it as an array.
	 * 
	 * @param moves
	 * @param depth
	 * @return
	 */
	private Move[] orderMoves(List<Move> moves, int depth) {
		boolean thereIsPvMove = false, thereIsRefutMove = false;
		Move pVmove, refutMove = null;
		if ((pVmove = pV[ply - depth]) != null)
			thereIsPvMove = true;
		TTEntry e = tT.lookUp(pos.zobristKey);
		if (e != null && e.bestMove != 0) {
			refutMove = Move.toMove(e.bestMove);
			thereIsRefutMove = true;
		}
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			move.value = (move.capturedPiece == 0) ? 0 : Piece.getByNumericNotation(move.capturedPiece).standardValue - Piece.getByNumericNotation(move.movedPiece).standardValue;
			if (thereIsPvMove && move.equals(pVmove)) {
				move.value += (ply - 1 - depth)*250;
				thereIsPvMove = false;
			}
			if (thereIsRefutMove && move.equals(refutMove)) {
				move.value += e.depth*250;
				thereIsRefutMove = false;
			}
			arr[i] = move;
			i++;
		}
		return new QuickSort<Move>(arr).getArray();
		
	}
	/**Sets the instance field pV according to the line of best play extracted form the transposition table.*/
	private void extractPv() {
		TTEntry e;
		Move bestMove;
		int i = 0;
		while ((e = tT.lookUp(pos.zobristKey)) != null && e.bestMove != 0) {
			bestMove = Move.toMove(e.bestMove);
			pos.makeMove(bestMove);
			pV[i] = bestMove;
			i++;
		}
		for (int j = 0; j < i; j++)
			pos.unmakeMove();
	}
}
