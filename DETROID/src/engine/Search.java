package engine;

import util.*;

public class Search {
	
	Position pos;
	Evaluator eval;
	Stack<Move> pV;
	short ply;
	static HashTable<TTEntry> tT = new HashTable<>(1 << 16);
	static byte tTage = 0;
	
	public Search(Position pos, int depth) {
		this.pos = pos;
		eval = new Evaluator();
		for (short i = 1; i < depth; i++) {
			ply = i;
			pVsearch(i, Game.LOSS, Game.WIN);
		}
		tTage++;
	}
	private int pVsearch(int depth, int alpha, int beta) {
		int score, origAlpha = alpha, val;
		Move bestMove, move;
		Move[] moves;
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
		moves = pos.generateMoves().toArray();
		if (moves.length == 0) {
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, Game.LOSS, (short)0, tTage));
			return Game.LOSS;
		}
		if (depth == 0) {
			score = eval.score(pos);
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, score, (short)0, tTage));
			return score;
		}
		bestMove = new Move(Game.LOSS);
		for (int i = 0; i < moves.length; i++) {
			move = moves[i];
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
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_FAIL_LOW, bestMove.value, bestMove.toShort(), tTage));
		else if (bestMove.value >= beta)
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_FAIL_HIGH, bestMove.value, bestMove.toShort(), tTage));
		else
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, bestMove.value, bestMove.toShort(), tTage));
		return bestMove.value;
	}
	private Move[] orderMoves(List<Move> moves) {
		TTEntry e = tT.lookUp(pos.zobristKey);
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			move.value = (move.capturedPiece == 0) ? 0 : Piece.getByNumericNotation(move.capturedPiece).standardValue - Piece.getByNumericNotation(move.movedPiece).standardValue;
		}
	}
	private Stack<Move> extractPv() {
		
	}
}
