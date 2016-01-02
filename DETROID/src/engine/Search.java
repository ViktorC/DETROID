package engine;

import engine.Evaluator.GamePhase;
import engine.Evaluator.Material;
import engine.Evaluator.Termination;
import engine.KillerTable.KillerTableEntry;
import engine.Move.MoveType;
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
	
	private static int MAX_USABLE_MEMORY = (int)(Runtime.getRuntime().maxMemory()*0.9);
	private static int MAX_SEARCH_DEPTH = 10;
	private static int MAX_EXPECTED_TOTAL_SEARCH_DEPTH = 16*MAX_SEARCH_DEPTH;
	
	private int numOfCores;
	
	private Position pos;
	
	private int ply;
	
	private static int NMR = 2;													// Null move pruning reduction.
	private static int LMR = 1;													// Late move reduction.
	private static int DELTA = Material.KNIGHT.score - Material.PAWN.score/2;	// The margin for delta-pruning in the quiescence search.
	
	private GamePhase gamePhase;			// The phase the 'game' at the root position is in.
	private boolean nullMoveObservHolds;	// Whether heursitcs based on the null move observation such as stand-pat and NMP are applicable.
	
	private Move[] pV;
	
	private KillerTable kT = new KillerTable(MAX_SEARCH_DEPTH);
	private static RelativeHistoryTable hT = new RelativeHistoryTable();
	private static HashTable<TTEntry> tT = new HashTable<>();
	private static byte tTgen = 0;
	
	private boolean pondering;
	private long searchTime;
	private long deadLine;
	
	/**Creates a new Search thread instance for pondering on the argument position which once started, will not stop until the thread is
	 * interrupted.
	 *
	 * @param pos
	 */
	public Search(Position pos) {
		this.pos = pos.deepCopy();
		gamePhase = Evaluator.evaluateGamePhase(this.pos);
		pondering = true;
		nullMoveObservHolds = gamePhase == GamePhase.END_GAME ? false : true;
	}
	/**Creates a new Search thread instance for searching a position for the specified amount of time; if that is <= 0, the engine will ponder on
	 * the position once the search thread is started, and will not stop until it is interrupted.
	 *
	 * @param pos
	 * @param searchTimeInMilliSeconds
	 */
	public Search(Position pos, long searchTimeInMilliSeconds) {
		this(pos);
		if (searchTimeInMilliSeconds > 0) {
			searchTime = searchTimeInMilliSeconds;
			pondering = false;
		}
		else
			pondering = true;
	}
	/**Returns the best move from the position if it has already been searched; else it returns null. If there is no best move found (either due to a
	 * search bug or because the search thread has not been run yet), it returns a pseudo-random legal move.
	 * 
	 * @return The best move according to the results of the search; if such a thing does not exist, a pseudo-random legal move.
	 */
	public Move getBestMove() {
		Move[] moveList;
		TTEntry e = tT.lookUp(pos.key);
		if (e != null && e.bestMove != 0)
			return Move.toMove(e.bestMove);
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
		this.pV = extractPv();
		while (i < this.pV.length && this.pV[i] != null)
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
			search(ply, Termination.CHECK_MATE.score, -Termination.CHECK_MATE.score, true, 0);
			pV = extractPv();
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
	 * @param depth Depth of the main search in plies.
	 * @param alpha
	 * @param beta
	 * @param nullMoveAllowed
	 * @param qDepth The depth with which quiescence search should be called.
	 * @return The score of the position searched.
	 */
	private int search(int depth, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
		int score, origAlpha = alpha, val, searchedMoves = 0, matMoveBreakInd = 0, IIDdepth, extPly,
			checkMateLim = Termination.CHECK_MATE.score + MAX_EXPECTED_TOTAL_SEARCH_DEPTH, distFromRoot = ply - depth;
		Move pVmove = null, bestMove, killerMove1 = null, killerMove2 = null, move;
		boolean thereIsPvMove = false, checkMemory = false, thereIsKillerMove1 = false, thereIsKillerMove2 = false, doIID = false;
		Queue<Move> matMoves = null, nonMatMoves = null;
		Move[] matMovesArr, nonMatMovesArr;
		TTEntry e;
		KillerTableEntry kE;
		bestMove = new Move(Termination.CHECK_MATE.score);
		Search: {
			// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
			e = tT.lookUp(pos.key);
			if (e != null) {
				// Mate score adjustment to root distance.
				if (e.score <= checkMateLim)
					score = e.score + distFromRoot;
				else if (e.score >= -checkMateLim)
					score = e.score - distFromRoot;
				else
					score = e.score;
				/* If the hashed entry's depth is greater than or equal to the current search depth, adjust alpha and beta accordingly or return
				 * the score if the entry stored a PV node. */
				if (e.depth >= depth) {
					if (e.type == NodeType.EXACT.ind)
						return score;
					else if (e.type == NodeType.FAIL_HIGH.ind) {
						if (score > bestMove.value) {
							bestMove.value = score;
							if (score > alpha)
								alpha = score;
						}
					}
					else if (score < beta)
						beta = score;
					if (alpha >= beta)
						return score;
				}
				// Check for the stored move and make it the PV move.
				if (e.bestMove != 0) {
					thereIsPvMove = true;
					pVmove = Move.toMove(e.bestMove);
				}
			}
			// Return the score from the quiescence search in case a leaf node has been reached.
			if (depth == 0) {
				score = quiescence(qDepth, alpha, beta);
				tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, score, 0, tTgen));
				return score;
			}
			// Check for the repetition rule; return a draw score if it applies.
			if (pos.getRepetitions() >= 3)
				return Termination.DRAW_CLAIMED.score;
			// In case there is no hash move...
			if (!thereIsPvMove) {
				// Check the PV form the last iteration.
				pVmove = pV[distFromRoot];
				if (pVmove != null && pos.isLegalSoft(pVmove))
					thereIsPvMove = true;
				// If there is no hash entry at all and the search is within the PV and close enough to the root, try IID.
				else if (e == null && depth > 5 && beta > alpha + 1)
					doIID = true;
			}
			// In case there IS a hash move...
			else {
				// If the hashed move was searched to a smaller depth, try the previous iteration's PV move for this ply.
				if (e.depth < depth - 1) {
					move = pV[ply - depth];
					if (move != null && pos.isLegalSoft(move)) {
						pVmove = move;
						thereIsPvMove = true;
					}
					/* If there was none or it was illegal and it is a PV node, close enough to the root with the hashed move having been
					 * searched to a shallow depth, try IID. */
					else if (depth > 5 && beta > alpha + 1 && e.depth < depth/2 - 1)
							doIID = true;
				}
			}
			// If set, perform internal iterative deepening.
			if (doIID) {
				IIDdepth = (depth > 7) ? depth/2 : depth - 2;
				extPly = ply;
				for (int i = 2; i <= IIDdepth; i++) {
					ply = i;
					search(i, alpha, beta, true, qDepth);
				}
				ply = extPly;
				e = tT.lookUp(pos.key);
				if (e != null && e.bestMove != 0) {
					pVmove = Move.toMove(e.bestMove);
					thereIsPvMove = true;
				}
			}
			// If there is a PV move, search that first.
			if (thereIsPvMove) {
				pos.makeMove(pVmove);
				val = -search(depth - 1, -beta, -alpha, true, qDepth);
				pos.unmakeMove();
				searchedMoves++;
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
			// If there is no hash entry or PV-move for this ply, perform mate check.
			else if (bestMove.value <= Termination.CHECK_MATE.score + MAX_SEARCH_DEPTH && bestMove.value != Termination.STALE_MATE.score){
				matMoves = pos.generateMaterialMoves();
				if (matMoves.length() == 0) {
					nonMatMoves = pos.generateNonMaterialMoves();
					if (nonMatMoves.length() == 0) {
						score = Evaluator.mateScore(pos.getCheck(), distFromRoot);
						tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, score, 0, tTgen));
						return score;
					}
				}
			}
			// Check for the fifty-move rule; return a draw score if it applies.
			if (pos.getFiftyMoveRuleClock() >= 100)
				return Termination.DRAW_CLAIMED.score;
			// If it is not a terminal node, try null move pruning if it is allowed and the side to move is not in check.
			if (nullMoveAllowed && nullMoveObservHolds && depth >= NMR && !pos.getCheck()) {
				pos.makeNullMove();
				// Do not allow consecutive null moves.
				if (depth == NMR)
					val = -search(depth - NMR, -beta, -beta + 1, false, qDepth);
				else
					val = -search(depth - NMR - 1, -beta, -beta + 1, false, qDepth);
				pos.unmakeMove();
				if (val >= beta) {
					bestMove = new Move(val);
					break Search;
				}
			}
			// If the PV-move was searched first or we had a hashed non mate score, the material moves have not been generated yet.
			if (matMoves == null)
				matMoves = pos.generateMaterialMoves();
			// Order the material moves.
			matMovesArr = orderMaterialMoves(matMoves);
			// Search winning and equal captures.
			for (int i = 0; i < matMovesArr.length; i++, checkMemory = !checkMemory) {
				if (checkMemory && Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_USABLE_MEMORY) {
					tT.remove(entry -> entry.depth < 2);
					System.gc();
				}
				move = matMovesArr[i];
				// If this move was the PV-move, skip it.
				if (thereIsPvMove && move.equals(pVmove))
					continue;
				// If the current move's order-value indicates a losing capture, break the search to check the killer moves.
				if (move.value < 0) {
					matMoveBreakInd = i;
					break;
				}
				pos.makeMove(move);
				// PVS.
				if (!thereIsPvMove && i == 0)
					val = -search(depth - 1, -beta, -alpha, true, qDepth);
				else {
					val = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
					if (val > alpha && val < beta)
						val = -search(depth - 1, -beta, -val, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
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
			// If there are no more winning or equal captures, check and search the killer moves if legal from this position.
			kE = kT.retrieve(distFromRoot);
			if (kE.move1 != 0) {	// Killer move no. 1.
				killerMove1 = Move.toMove(kE.move1);
				if (pos.isLegalSoft(killerMove1) && (!thereIsPvMove || !killerMove1.equals(pVmove))) {
					thereIsKillerMove1 = true;
					pos.makeMove(killerMove1);
					if (!thereIsPvMove && matMoveBreakInd == 0)
						val = -search(depth - 1, -beta, -alpha, true, qDepth);
					else {
						val = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
						if (val > alpha && val < beta)
							val = -search(depth - 1, -beta, -val, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
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
			if (kE.move2 != 0) {	// Killer move no. 2.
				killerMove2 = Move.toMove(kE.move2);
				if (pos.isLegalSoft(killerMove2) && (!thereIsPvMove || !killerMove2.equals(pVmove))) {
					thereIsKillerMove2 = true;
					pos.makeMove(killerMove2);
					if (!thereIsPvMove && !thereIsKillerMove1 && matMoveBreakInd == 0)
						val = -search(depth - 1, -beta, -alpha, true, qDepth);
					else {
						val = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
						if (val > alpha && val < beta)
							val = -search(depth - 1, -beta, -val, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
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
			}	// Killer move check ending.
			// Search losing captures if there are any.
			for (int i = matMoveBreakInd; i < matMovesArr.length; i++, checkMemory = !checkMemory) {
				if (checkMemory && Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_USABLE_MEMORY) {
					tT.remove(entry -> entry.depth < 2);
					System.gc();
				}
				move = matMovesArr[i];
				// If this move was the PV-move, skip it.
				if (thereIsPvMove && move.equals(pVmove))
					continue;
				if (move.value < 0) {
					matMoveBreakInd = i;
					break;
				}
				pos.makeMove(move);
				// PVS.
				if (!thereIsPvMove && i == 0)
					val = -search(depth - 1, -beta, -alpha, true, qDepth);
				else {
					val = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
					if (val > alpha && val < beta)
						val = -search(depth - 1, -beta, -val, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
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
			// Generate the non-material legal moves if they are not generated yet.
			if (nonMatMoves == null)
				nonMatMoves = pos.generateNonMaterialMoves();
			// Order and search the non-material moves.
			nonMatMovesArr = orderNonMaterialMoves(nonMatMoves);
			for (int i = 0; i < nonMatMovesArr.length; i++, checkMemory = !checkMemory) {
				if (checkMemory && Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_USABLE_MEMORY) {
					tT.remove(entry -> entry.depth < 2);
					System.gc();
				}
				move = nonMatMovesArr[i];
				// If this move was the PV-move, skip it.
				if (thereIsPvMove && move.equals(pVmove)) {
					thereIsPvMove = false;
					continue;
				}
				// If this move was the first killer move, skip it.
				if (thereIsKillerMove1 && move.equals(killerMove1)) {
					thereIsKillerMove1 = false;
					continue;
				}
				// If this move was the second killer move, skip it.
				if (thereIsKillerMove2 && move.equals(killerMove2)) {
					thereIsKillerMove2 = false;
					continue;
				}
				pos.makeMove(move);
				// Try late move reduction if not within the PV.
				if (depth > 2 && bestMove.value <= origAlpha && !pos.getCheck() && pos.getUnmakeRegister().checkers == 0
					&& searchedMoves > 4 && hT.score(move) <= RelativeHistoryTable.MAX_SCORE/(matMovesArr.length + nonMatMovesArr.length)) {
					val = -search(depth - LMR - 1, -alpha - 1, -alpha, true, qDepth);
					// If it does not fail low, research with full window.
					if (val > origAlpha)
						val = -search(depth - 1, -beta, -alpha, true, qDepth);
				}
				// Else PVS.
				else if (!thereIsPvMove && i == 0 && matMoves.length() == 0)
					val = -search(depth - 1, -beta, -alpha, true, qDepth);
				else {
					val = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
					if (val > alpha && val < beta)
						val = -search(depth - 1, -beta, -val, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (val > bestMove.value) {
					bestMove = move;
					bestMove.value = val;
					if (val > alpha)
						alpha = val;
				}
				if (alpha >= beta) {	// Cutoff from a non-material move.
					kT.add(distFromRoot, move);	// Add to killer moves.
					hT.recordSuccessfulMove(move);	// Record success in the relative history table.
					break Search;
				}
				else
					hT.recordUnsuccessfulMove(move);	// Record failure in the relative history table.
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
		}
		// Adjustment of bestMove's stored score for TT insertion in case it's a check mate score.
		if (bestMove.value <= checkMateLim)
			score = bestMove.value - distFromRoot;
		else if (bestMove.value >= -checkMateLim)
			score = bestMove.value + distFromRoot;
		else
			score = bestMove.value;
		//	Add new entry to the transposition table.
		if (bestMove.value <= origAlpha)
			tT.insert(new TTEntry(pos.key, depth, NodeType.FAIL_LOW.ind, score, bestMove.toInt(), tTgen));
		else if (bestMove.value >= beta)
			tT.insert(new TTEntry(pos.key, depth, NodeType.FAIL_HIGH.ind, score, bestMove.toInt(), tTgen));
		else
			tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, score, bestMove.toInt(), tTgen));
		// Return bestMove's unadjusted score.
		return bestMove.value;
	}
	/**A search algorithm for diminishing the horizon effect once the main search algorithm has reached a leaf node. It keep searching until
	 * the side to move is not in check and does not have any legal winning captures according to SEE.
	 * 
	 * In the first two plies (unless it has been extended due to the side to move being in chess), it also searches moves that give check.
	 * 
	 * @param depth
	 * @param alpha
	 * @param beta
	 * @return
	 */
	public int quiescence(int depth, int alpha, int beta) {
		List<Move> tacticalMoves, allMoves;
		long[] checkSquares;
		Move[] moves;
		Move move;
		int staticScore, searchScore;
		boolean check = pos.getCheck();
		// If the side to move is in check, stand-pat does not hold and the main search will be called later on so no moves need to be generated.
		if (check) {
			tacticalMoves = null;
			staticScore = Termination.CHECK_MATE.score + ply - depth;
		}
		/* Generate tactical and quiet moves separately then combine them in the quiet move list for evaluation of the position for stand-pat
		 * this way the ordering if the interesting moves can be restricted to only the tactical moves. */
		else {
			// For the first two plies, generate non-material moves that give check as well.
			if (depth > -2) {
				checkSquares = pos.squaresToCheckFrom();
				tacticalMoves = pos.generateTacticalMoves(checkSquares);
				// No check and the null move observation holds, thus stand-pat applies, and we can use the position's eval score as our bound.
				if (nullMoveObservHolds) {
					allMoves = pos.generateQuietMoves(checkSquares);
					allMoves.addAll(tacticalMoves); // We need all the lagal moves for the side to move for mate-detection in the evaluation.
					staticScore = Evaluator.score(pos, allMoves, ply - depth);
				}
				// Else no bound.
				else
					staticScore = Termination.CHECK_MATE.score + ply - depth;
			}
			// After that, only material moves.
			else {
				tacticalMoves = pos.generateMaterialMoves();
				// Stand-pat, evaluate position.
				if (nullMoveObservHolds) {
					allMoves = pos.generateNonMaterialMoves();
					allMoves.addAll(tacticalMoves);
					staticScore = Evaluator.score(pos, allMoves, ply - depth);
				}
				// No bound.
				else
					staticScore = Termination.CHECK_MATE.score + ply - depth;
			}
		}
		// Fail hard.
		if (staticScore >= beta)
			return beta;
		if (staticScore > alpha)
			alpha = staticScore;
		// If check, call the main search for one ply (while keeping the track of the quiescence search depth to avoid resetting it).
		if (check) {
			searchScore = -search(1, alpha, beta, false, depth - 2);
			if (searchScore > alpha)
				alpha = searchScore;
		}
		// Quiescence search.
		else {
			moves = orderTacticalMoves(pos, tacticalMoves);
			for (int i = 0; i < moves.length; i++) {
				move = moves[i];
				// If the SEE value is below 0 or the delta pruning limit, break the search because the rest of the moves are even worse.
				if (move.value < 0 || move.value < alpha - DELTA)
					break;
				pos.makeMove(move);
				searchScore = -quiescence(depth - 1, -beta, -alpha);
				pos.unmakeMove();
				if (searchScore > alpha) {
					alpha = searchScore;
					if (alpha >= beta)
						break;
				}
			}
		}
		if (alpha >= beta)
			return beta;
		return alpha;
	}
	/**Orders material moves and checks, the former of which according to the SEE swap algorithm.
	 * 
	 * @param pos
	 * @param moves
	 * @return
	 */
	private Move[] orderTacticalMoves(Position pos, List<Move> moves) {
		Move[] arr = new Move[moves.length()];
		Move move;
		int i = 0;
		while (moves.hasNext()) {
			move = moves.next();
			if (move.capturedPiece == Piece.NULL.ind && move.type < MoveType.PROMOTION_TO_QUEEN.ind)
				move.value = 0;
			else
				move.value = Evaluator.SEE(pos, move);	// Static exchange evaluation.
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
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
			if (move.type > 3) {
				move.value = Material.QUEEN.score;
				if (move.capturedPiece != Piece.NULL.ind)
					move.value += Material.getByPieceInd(move.capturedPiece).score - Material.getByPieceInd(move.movedPiece).score;
			}
			else
				move.value = Material.getByPieceInd(move.capturedPiece).score - Material.getByPieceInd(move.movedPiece).score;
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
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
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
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
		while ((e = tT.lookUp(pos.key)) != null && e.bestMove != 0 && i < MAX_SEARCH_DEPTH) {
			bestMove = Move.toMove(e.bestMove);
			pos.makeMove(bestMove);
			pV[i++] = bestMove;
		}
		for (int j = 0; j < i; j++)
			pos.unmakeMove();
		return pV;
	}
}
