package engine;

import util.*;

/**A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It uses
 * principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative deepening framework.
 * 
 * @author Viktor
 *
 */
public class Search extends Thread {
	
	public final static int MAX_SEARCH_DEPTH = 64;
	
	private Position pos;
	private Move bestMove;
	private int ply;
	private Move[] pV;
	private HashTable<TTEntry> tT = new HashTable<>();
	private byte tTgen = 0;
	private boolean pondering = false;
	private long searchTime;
	private long deadLine;
	
	public Search(Position pos) {
		this.pos = pos;
		searchTime = 0;
	}
	public Search(Position pos, long searchTimeInMillis) {
		this.pos = pos;
		if (searchTimeInMillis > 0)
			searchTime = searchTimeInMillis;
		else
			searchTime = 0;
	}
	/**Returns the best move from the position if it has already been searched; else it returns null.
	 * 
	 * @return
	 */
	public Move getBestMove() {
		return bestMove;
	}
	/**Returns the best line of play from the position if it has already been searched; else it returns null.
	 * 
	 * @return
	 */
	public Queue<Move> getPv() {
		Queue<Move> pV = new Queue<>();
		int i = 0;
		while (this.pV[i] != null)
			pV.add(this.pV[i++]);
		return pV;
	}
	/**Returns whether pondering mode is active.
	 * 
	 * @return
	 */
	public boolean isPonderingOn() {
		return pondering;
	}
	/**Sets pondering. With pondering on, there is no time limit for the search and the transposition table is never cleared.
	 * 
	 * @param pondering
	 */
	public void setPondering(boolean pondering) {
		this.pondering = pondering;
	}
	/**Returns the allocated search time in milliseconds.
	 * 
	 * @return
	 */
	public long getSearchTime() {
		return searchTime;
	}
	/**Sets the allocated search time.
	 * 
	 * @param searchTimeInMillis
	 */
	public void setSearchTime(long searchTimeInMillis) {
		if (searchTimeInMillis >= 0)
			searchTime = searchTimeInMillis;
	}
	/**Returns a reference to the transposition table for checking its size, load factor, and contents.
	 * 
	 * @return
	 */
	public HashTable<TTEntry> getTranspositionTable() {
		return tT;
	}
	/**Starts searching the current position until the allocated search time has passed, or the thread is interrupted, or the maximum search
	 * depth, 128 has been reached.*/
	public void run() {
		deadLine = (searchTime == 0 || pondering) ? Long.MAX_VALUE : (System.currentTimeMillis() + searchTime - 5);
		pV = new Move[MAX_SEARCH_DEPTH];
		for (int i = 2; i <= MAX_SEARCH_DEPTH; i++) {
			ply = i;
			pVsearch(ply, Evaluator.LOSS, Evaluator.WIN);
			extractPv();
			bestMove = pV[0];
			if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
				break;
		}
		if (!pondering) {
			if (++tTgen >= 3) {
				tT.clear();
				tTgen = 0;
			}
		}
		else
			pondering = false;
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
			score = Evaluator.score(pos);
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, score, (short)0, tTgen));
			return score;
		}
		moveQ = pos.generateMoves();
		if (moveQ.length() == 0) {
			if (pos.getCheck()) {
				tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, Evaluator.LOSS, (short)0, tTgen));
				return Evaluator.LOSS;
			}
			else {
				tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, Evaluator.TIE, (short)0, tTgen));
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
			if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
				break;
			if (alpha >= beta)
				break;
		}
		if (bestMove.value <= origAlpha) 
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_FAIL_LOW, bestMove.value, bestMove.toInt(), tTgen));
		else if (bestMove.value >= beta)
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_FAIL_HIGH, bestMove.value, bestMove.toInt(), tTgen));
		else
			tT.insert(new TTEntry(pos.zobristKey, depth, TTEntry.TYPE_EXACT, bestMove.value, bestMove.toInt(), tTgen));
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
