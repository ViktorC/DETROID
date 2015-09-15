package engine;

import util.*;

/**A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It uses
 * principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative deepening framework.
 * 
 * @author Viktor
 *
 */
public class Search extends Thread {
	
	/**A simple enum for game tree node types based on their values' relation to alpha and beta.
	 * 
	 * @author Viktor
	 *
	 */
	public enum NodeType {
		
		EXACT		((byte)0),
		FAIL_HIGH	((byte)1),
		FAIL_LOW	((byte)2);
		
		public final byte num;
		
		private NodeType(byte num) {
			this.num = num;
		}
	}
	
	private final static int MAX_USED_MEMORY = (int)(Runtime.getRuntime().maxMemory()*0.9);
	private final static int MAX_SEARCH_DEPTH = 64;
	
	private int numOfCores;
	
	private Position pos;
	private Move bestMove;
	private int ply;
	private Move[] pV;
	private static HashTable<TTEntry> tT = new HashTable<>();
	private static byte tTgen = 0;
	private boolean pondering = false;
	private long searchTime;
	private long deadLine;
	
	private Search(Position pos, long searchTimeInMilliSeconds) {
		this.pos = pos;
		if (searchTimeInMilliSeconds > 0)
			searchTime = searchTimeInMilliSeconds;
		else
			pondering = true;
	}
	/**Returns a new Search thread instance instead for pondering on the argument position which once started, will not stop until the thread is
	 * interrupted.
	 *
	 * @param pos
	 * @return A new Search instance for pondering.
	 */
	public static Search getInstance(Position pos) {
		return new Search(pos.copy(), 0);
	}
	/**Returns a new Search thread instance for searching a position for the specified amount of time; if that is <= 0, the engine will ponder on the
	 * position once the search thread is started, and will not stop until it is interrupted.
	 *
	 * @param pos
	 * @param searchTimeInMilliSeconds
	 * @return A new Search instance.
	 */
	public static Search getInstance(Position pos, long searchTimeInMilliSeconds) {
		return new Search(pos.copy(), searchTimeInMilliSeconds);
	}
	/**Returns the best move from the position if it has already been searched; else it returns null. If there is no best move found (either due to a
	 * search bug or because the search thread has not been run yet), it returns a pseudo-random legal move.
	 * 
	 * @return The best move according to the results of the search; if such a thing does not exist, a pseudo-random legal move.
	 */
	public Move getBestMove() {
		Move[] moveList;
		if (bestMove != null)
			return bestMove;
		else {
			moveList = pos.generateMoves().toArray();
			return moveList[(int)Math.random()*moveList.length];
		}
	}
	/**Returns the best line of play from the position if it has already been searched; else it returns null.
	 * 
	 * @return The principal variation according to the results of the search.
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
	 * @return Whether pondering is on.
	 */
	public boolean isPonderingOn() {
		return pondering;
	}
	/**Returns the allocated search time in milliseconds.
	 * 
	 * @return The allocated search time in milliseconds.
	 */
	public long getSearchTime() {
		return searchTime;
	}
	/**Returns a string containing basic statistics about the transposition table.
	 * 
	 * @return A string of the total size and load of the transposition table.
	 */
	public String getTranspositionTableStats() {
		return tT.size() + "\n" + tT.load();
	}
	/**Starts searching the current position until the allocated search time has passed, or the thread is interrupted, or the maximum search
	 * depth, 128 has been reached.*/
	public void run() {
		numOfCores = Runtime.getRuntime().availableProcessors();
		if (numOfCores <= 1 && pondering)
			return;
		deadLine = pondering ? Long.MAX_VALUE : (System.currentTimeMillis() + searchTime);
		pV = new Move[MAX_SEARCH_DEPTH];
		for (int i = 2; i <= MAX_SEARCH_DEPTH; i++) {
			ply = i;
			pVsearch(ply, Game.State.LOSS.score, Game.State.WIN.score);
			pV = extractPv();
			bestMove = pV[0];
			if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
				break;
		}
		if (!pondering) {
			if (tTgen == 127) {
				tT.remove(e -> (e.generation -= 125) < 0);
				tTgen = 2;
			}
			else {
				tT.remove(e -> e.generation < tTgen - 2);
			}
			tTgen++;
		}
	}
	/**A principal variation search algorithm utilizing a transposition table. It returns only the score for the searched position, but the principal
	 * variation can be extracted from the transposition table after a search has been run.
	 * 
	 * @param depth
	 * @param alpha
	 * @param beta
	 * @return The score of the position searched.
	 */
	private int pVsearch(int depth, int alpha, int beta) {
		int score, origAlpha = alpha, val;
		Move bestMove, move;
		Queue<Move> moveQ;
		Move[] moveArr;
		TTEntry e = tT.lookUp(pos.zobristKey);
		if (e != null && e.depth >= depth) {
			if (e.type == NodeType.EXACT.num)
				return e.score;
			else if (e.type == NodeType.FAIL_HIGH.num) {
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
			tT.insert(new TTEntry(pos.zobristKey, depth, NodeType.EXACT.num, score, (short)0, tTgen));
			return score;
		}
		moveQ = pos.generateMoves();
		if (moveQ.length() == 0) {
			if (pos.getCheck()) {
				tT.insert(new TTEntry(pos.zobristKey, depth, NodeType.EXACT.num, Game.State.LOSS.score, (short)0, tTgen));
				return Game.State.LOSS.score;
			}
			else {
				tT.insert(new TTEntry(pos.zobristKey, depth, NodeType.EXACT.num, Game.State.TIE.score, (short)0, tTgen));
				return Game.State.TIE.score;
			}
		}
		if (pos.getFiftyMoveRuleClock() >= 100 || pos.getRepetitions() >= 3)
			return Game.State.TIE.score;
		moveArr = orderMoves(moveQ, depth);
		bestMove = new Move(Game.State.LOSS.score);
		for (int i = 0; i < moveArr.length; i++) {
			if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_USED_MEMORY) {
				tT.remove(entry -> entry.depth < 2);
				System.gc();
			}
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
			if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
				break;
		}
		if (bestMove.value <= origAlpha) 
			tT.insert(new TTEntry(pos.zobristKey, depth, NodeType.FAIL_LOW.num, bestMove.value, bestMove.toInt(), tTgen));
		else if (bestMove.value >= beta)
			tT.insert(new TTEntry(pos.zobristKey, depth, NodeType.FAIL_HIGH.num, bestMove.value, bestMove.toInt(), tTgen));
		else
			tT.insert(new TTEntry(pos.zobristKey, depth, NodeType.EXACT.num, bestMove.value, bestMove.toInt(), tTgen));
		return bestMove.value;
	}
	/**Orders a list of moves according to the PV node of the given depth, history heuristics, and the MVV-LVA principle; and returns it as an array.
	 * 
	 * @param moves
	 * @param depth
	 * @return The ordered move list.
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
	/**Returns an array of Move objects according to the best line of play extracted form the transposition table.
	 * 
	 * @return An array of Move objects according to the best line of play.
	 */
	private Move[] extractPv() {
		Move[] pV = new Move[MAX_SEARCH_DEPTH];
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
		return pV;
	}
}
