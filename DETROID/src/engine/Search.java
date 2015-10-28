package engine;

import engine.Evaluator.MaterialScore;
import engine.KillerTable.KillerTableEntry;
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
		
		EXACT,
		FAIL_HIGH,
		FAIL_LOW;
		
		public final byte ind;
		
		private NodeType() {
			this.ind = (byte)ordinal();
		}
	}
	
	private final static int MAX_USED_MEMORY = (int)(Runtime.getRuntime().maxMemory()*0.9);
	private final static int MAX_SEARCH_DEPTH = 8;
	
	private int numOfCores;
	
	private Position pos;
	private Move bestMove;
	private int ply;
	private Move[] pV;
	private static HashTable<TTEntry> tT = new HashTable<>();
	private KillerTable kT = new KillerTable(MAX_SEARCH_DEPTH);
	private static RelativeHistoryTable hT = new RelativeHistoryTable();
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
			moveList = pos.generateAllMoves().toArray();
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
	 * depth has been reached.*/
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
			hT.decrementCurrentValues();
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
		Move pVmove, bestMove, killerMove1 = null, killerMove2 = null, move;
		KillerTableEntry kE;
		boolean thereIsPvMove = false, checkMemory = false, killersChecked = false,
				thereIsKillerMove1 = false, thereIsKillerMove2 = false;
		Queue<Move> matMoves, nonMatMoves = null;
		Move[] matMovesArr, nonMatMovesArr;
		TTEntry e = tT.lookUp(pos.key);
		if (e != null && e.depth >= depth) {
			if (e.type == NodeType.EXACT.ind)
				return e.score;
			else if (e.type == NodeType.FAIL_HIGH.ind) {
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
			tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, score, 0, tTgen));
			return score;
		}
		Search: {
			bestMove = new Move(Game.State.LOSS.score);
			if (e != null && e.bestMove != 0)
				pVmove = Move.toMove(e.bestMove);
			else
				pVmove = pV[ply - depth];
			if (pVmove != null) {
				thereIsPvMove = true;
				pos.makeMove(pVmove);
				val = -pVsearch(depth - 1, -beta, -alpha);
				pos.unmakeMove();
				if (val > bestMove.value) {
					bestMove = pVmove;
					bestMove.value = val;
					if (val > alpha)
						alpha = val;
				}
				if (alpha >= beta)
					break Search;
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
			matMoves = pos.generateMaterialMoves();
			if (matMoves.length() == 0) {
				nonMatMoves = pos.generateNonMaterialMoves();
				if (nonMatMoves.length() == 0) {
					if (pos.getCheck()) {
						tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, Game.State.LOSS.score, 0, tTgen));
						return Game.State.LOSS.score;
					}
					else {
						tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, Game.State.TIE.score, 0, tTgen));
						return Game.State.TIE.score;
					}
				}
			}
			if (pos.getFiftyMoveRuleClock() >= 100 || pos.getRepetitions() >= 3)
				return Game.State.TIE.score;
			matMovesArr = orderMaterialMoves(matMoves);
			for (int i = 0; i < matMovesArr.length; i++, checkMemory = !checkMemory) {
				if (checkMemory && Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_USED_MEMORY) {
					tT.remove(entry -> entry.depth < 2);
					System.gc();
				}
				move = matMovesArr[i];
				if (thereIsPvMove) {
					if (move.equals(pVmove))
						continue;
				}
				if (!killersChecked && move.value < 0) {
					kE = kT.retrieve(depth);
					if (kE.move1 != 0) {
						killerMove1 = Move.toMove(kE.move1);
						if (pos.isLegal(killerMove1)) {
							thereIsKillerMove1 = true;
							pos.makeMove(killerMove1);
							if (!thereIsPvMove && i == 0)
								val = -pVsearch(depth - 1, -beta, -alpha);
							else {
								val = -pVsearch(depth - 1, -alpha - 1, -alpha);
								if (val > alpha && val < beta)
									val = -pVsearch(depth - 1, -beta, -val);
							}
							pos.unmakeMove();
							if (val > bestMove.value) {
								bestMove = killerMove1;
								bestMove.value = val;
								if (val > alpha)
									alpha = val;
							}
							if (alpha >= beta)
								break Search;
							if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
								break Search;
						}
					}
					if (kE.move2 != 0) {
						killerMove2 = Move.toMove(kE.move2);
						if (pos.isLegal(killerMove2)) {
							thereIsKillerMove2 = true;
							pos.makeMove(killerMove2);
							if (!thereIsPvMove && !thereIsKillerMove1 && i == 0)
								val = -pVsearch(depth - 1, -beta, -alpha);
							else {
								val = -pVsearch(depth - 1, -alpha - 1, -alpha);
								if (val > alpha && val < beta)
									val = -pVsearch(depth - 1, -beta, -val);
							}
							pos.unmakeMove();
							if (val > bestMove.value) {
								bestMove = killerMove2;
								bestMove.value = val;
								if (val > alpha)
									alpha = val;
							}
							if (alpha >= beta)
								break Search;
							if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
								break Search;
						}
					}
					killersChecked = true;
				}
				pos.makeMove(move);
				if (!thereIsPvMove && i == 0)
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
					break Search;
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
			if (nonMatMoves == null)
				nonMatMoves = pos.generateNonMaterialMoves();
			nonMatMovesArr = orderNonMaterialMoves(nonMatMoves);
			for (int i = 0; i < nonMatMovesArr.length; i++, checkMemory = !checkMemory) {
				if (checkMemory && Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_USED_MEMORY) {
					tT.remove(entry -> entry.depth < 2);
					System.gc();
				}
				move = nonMatMovesArr[i];
				if (thereIsPvMove) {
					if (move.equals(pVmove))
						continue;
				}
				if (thereIsKillerMove1) {
					if (move.equals(killerMove1))
						continue;
				}
				if (thereIsKillerMove2) {
					if (move.equals(killerMove2))
						continue;
				}
				pos.makeMove(move);
				if (!thereIsPvMove && matMoves.length() == 0 && i == 0)
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
				if (alpha >= beta) {
					kT.add(depth, move);
					hT.recordSuccessfulMove(move);
					break Search;
				}
				else
					hT.recordUnsuccessfulMove(move);
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
		}
		if (bestMove.value <= origAlpha) 
			tT.insert(new TTEntry(pos.key, depth, NodeType.FAIL_LOW.ind, bestMove.value, bestMove.toInt(), tTgen));
		else if (bestMove.value >= beta)
			tT.insert(new TTEntry(pos.key, depth, NodeType.FAIL_HIGH.ind, bestMove.value, bestMove.toInt(), tTgen));
		else
			tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, bestMove.value, bestMove.toInt(), tTgen));
		return bestMove.value;
	}
	/**Orders captures and promotions according to the LVA-MVV principle; in case of a promotion, add the standard value of a queen to the score.
	 * 
	 * @param moves
	 * @return
	 */
	private Move[] orderMaterialMoves(List<Move> moves) {
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			move.value = MaterialScore.getValueByPieceInd(move.capturedPiece) - MaterialScore.getValueByPieceInd(move.movedPiece);
			if (move.type > 3) {
				move.value += MaterialScore.QUEEN.value;
			}
			arr[i] = move;
			i++;
		}
		return new QuickSort<Move>(arr).getArray();
	}
	/**Orders non-material moves according to the relative history heuristic.
	 * 
	 * @param moves
	 * @return
	 */
	private Move[] orderNonMaterialMoves(List<Move> moves) {
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			move.value = hT.score(move);
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
		while ((e = tT.lookUp(pos.key)) != null && e.bestMove != 0) {
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
