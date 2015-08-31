package engine;

import util.*;

public class Search {
	
	Position pos;
	Evaluator eval;
	Move[] pV;
	short ply;
	HashTable<TTEntry> tT = new HashTable<>();
	static byte tTage = 0;
	
	public Search(Position pos, int depth) {
		this.pos = pos;
		eval = new Evaluator();
		pV = new Move[depth];
		for (short i = 1; i <= depth; i++) {
			ply = i;
			pVsearch(i, Game.LOSS, Game.WIN);
			extractPv();
		}
		tTage++;
	}
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
		if (pos.getFiftyMoveRuleClock() >= 100 || pos.getRepetitions() >= 3)
			return Game.TIE;
		moveQ = pos.generateMoves();
		if (moveQ.length() == 0) {
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, Game.LOSS, (short)0, tTage));
			return Game.LOSS;
		}
		if (depth == 0) {
			score = eval.score(pos);
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, score, (short)0, tTage));
			return score;
		}
		moveArr = orderMoves(moveQ, depth);
		bestMove = new Move(Game.LOSS);
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
