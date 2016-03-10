package engine;

import java.util.concurrent.atomic.AtomicLong;

import engine.Evaluator.Termination;
import engine.KillerTable.KillerTableEntry;
import engine.Move.MoveType;
import util.*;

/**
 * A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It
 * uses principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative
 * deepening framework.
 * 
 * @author Viktor
 *
 */
public class Search extends Thread {
	
	/**
	 * A simple enum for game tree node types based on their values' relation to alpha and beta.
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
	
	private static int MAX_SEARCH_DEPTH = 8;
	private static int MAX_EXPECTED_TOTAL_SEARCH_DEPTH = 16*MAX_SEARCH_DEPTH;
	
	private int numOfCores;
	
	private Position pos;
	
	private int ply;
	
	private static int NMR = 2;													// Null move pruning reduction.
	private static int LMR = 1;													// Late move reduction.
	private static int DELTA = Material.KNIGHT.score - Material.PAWN.score/2;	// The margin for delta-pruning in the quiescence search.
	
	private GamePhase gamePhase;			// The phase the 'game' at the root position is in.
	private boolean nullMoveObservHolds;	// Whether heuristics based on the null move observation such as stand-pat and NMP are applicable.
	
	private Move[] pV;
	
	private KillerTable kT = new KillerTable(MAX_SEARCH_DEPTH + 1);					// Killer heuristic table.
	private static RelativeHistoryTable hT = new RelativeHistoryTable();		// History heuristic table.
	private static HashTable<TTEntry> tT = new HashTable<>(128, TTEntry.SIZE);	// Transposition table.
	private static HashTable<PTEntry> pT = new HashTable<>(16, PTEntry.SIZE);	// Pawn table.
	private static byte eGen = 0;	// Entry generation.
	
	Evaluator eval = new Evaluator(pT, eGen);
	
	private boolean pondering;
	private long searchTime;
	private long deadLine;
	
	private static AtomicLong nodes;
	
	/**
	 * Creates a new Search thread instance for pondering on the argument position which once started, will not stop until the thread is
	 * interrupted.
	 *
	 * @param pos
	 */
	public Search(Position pos) {
		this.pos = pos.deepCopy();
		gamePhase = Evaluator.evaluateGamePhase(this.pos);
		pondering = true;
		nullMoveObservHolds = gamePhase != GamePhase.END_GAME;
	}
	/**
	 * Creates a new Search thread instance for searching a position for the specified amount of time; if that is <= 0, the engine will ponder
	 * on the position once the search thread is started, and will not stop until it is interrupted.
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
	}
	/**
	 * Returns the best move from the position if it has already been searched; else it returns null. If there is no best move found
	 * (either due to a search bug or because the search thread has not been run yet), it returns a pseudo-random legal move.
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
	/**
	 * Returns the best line of play from the position if it has already been searched; else it returns null.
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
	/**
	 * Returns whether pondering mode is active.
	 * 
	 * @return Whether pondering is on.
	 */
	public boolean isPonderingOn() {
		return pondering;
	}
	/**
	 * Returns the allocated search time in milliseconds.
	 * 
	 * @return The allocated search time in milliseconds.
	 */
	public long getSearchTime() {
		return searchTime;
	}
	/**
	 * Returns a string containing basic statistics about the transposition table.
	 * 
	 * @return A string of the total size and load of the transposition table.
	 */
	public String getTranspositionTableStats() {
		return tT.toString();
	}
	/**
	 * Returns a string containing basic statistics about the pawn table.
	 * 
	 * @return A string of the total size and load of the pawn table.
	 */
	public String getPawnTableStats() {
		return pT.toString();
	}
	/**
	 * Returns the number of nodes searched.
	 * 
	 * @return Searched node count.
	 */
	public long getNodeCount() {
		return nodes.get();
	}
	/**
	 * Starts searching the current position until the allocated search time has passed, or the thread is interrupted, or the maximum search
	 * depth has been reached.
	 */
	public void run() {
		nodes = new AtomicLong(0);
		numOfCores = Runtime.getRuntime().availableProcessors();
		if (numOfCores <= 1 && pondering)
			return;
		if (pondering)
			deadLine = Long.MAX_VALUE;
		else {
			eGen++;
			deadLine = System.currentTimeMillis() + searchTime;
		}
		pV = new Move[MAX_SEARCH_DEPTH];
		for (int i = 2; i <= MAX_SEARCH_DEPTH; i++) {
			ply = i;
			search(ply, Termination.CHECK_MATE.score, -Termination.CHECK_MATE.score, true, 0);
			pV = extractPv();
			if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
				break;
		}
		if (eGen == 127) {
			tT.clear();
			pT.clear();
			eGen = 2;
		}
		else {
			tT.remove(e -> e.generation < eGen);
			pT.remove(e -> e.generation < eGen - 2);
		}
		hT.decrementCurrentValues();
	}
	/**
	 * A principal variation search algorithm utilizing a transposition table. It returns only the score for the searched position, but the
	 * principal variation can be extracted from the transposition table after a search has been run.
	 * 
	 * @param depth Depth of the main search in plies.
	 * @param alpha
	 * @param beta
	 * @param nullMoveAllowed
	 * @param qDepth The depth with which quiescence search should be called.
	 * @return The score of the position searched.
	 */
	private int search(int depth, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
		final int checkMateLim = Termination.CHECK_MATE.score + MAX_EXPECTED_TOTAL_SEARCH_DEPTH;
		final int distFromRoot = ply - depth;
		final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
		final boolean inCheck = pos.getCheck();
		int bestScore, score, searchedMoves, matMoveBreakInd, IIDdepth, extPly, kMove, bestMoveInt;
		Move hashMove, bestMove, killerMove1, killerMove2, move;
		boolean isThereHashMove, isThereKM1, isThereKM2;
		Queue<Move> matMoves, nonMatMoves;
		Move[] matMovesArr, nonMatMovesArr;
		TTEntry e;
		KillerTableEntry kE;
		bestScore = Termination.CHECK_MATE.score;
		bestMove = null;
		searchedMoves = 0;
		hashMove = killerMove1 = killerMove2 = null;
		isThereHashMove = isThereKM1 = isThereKM2 = false;
		matMoves = nonMatMoves = null;
		nodes.incrementAndGet();
		Search: {
			// Check for the repetition rule; return a draw score if it applies.
			if (pos.getRepetitions() >= 3)
				return Termination.DRAW_CLAIMED.score;
			// Mate distance pruning.
			if (-mateScore < beta) {
				if (alpha >= -mateScore)
					return -mateScore;
			}
			if (mateScore > alpha) {
				  if (beta <= mateScore)
					  return mateScore;
			}
			// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
			e = tT.lookUp(pos.key);
			if (e != null) {
				e.generation = eGen;
				/* If the hashed entry's depth is greater than or equal to the current search depth, adjust alpha and beta accordingly or return
				 * the score if the entry stored a PV node. */
				if (e.depth >= depth) {
					// Mate score adjustment to root distance.
					if (e.score <= checkMateLim)
						score = e.score + distFromRoot;
					else if (e.score >= -checkMateLim)
						score = e.score - distFromRoot;
					else
						score = e.score;
					// If it exceeds the current beta, update the history and killer tables and return the score.
					if (score >= beta) {
						if (e.bestMove != 0 && !(hashMove = Move.toMove(e.bestMove)).isMaterial()) {
							kT.add(distFromRoot, hashMove);	// Add to killer moves.
							hT.recordSuccessfulMove(hashMove);	// Record success in the relative history table.
						}
						return score;
					}
					// If the score was exact, or if it belongs to a fail low node and is lower than alpha, return the score.
					if (e.type == NodeType.EXACT.ind || (e.type == NodeType.FAIL_LOW.ind && score <= alpha)) {
						if (e.bestMove != 0 && !(hashMove = Move.toMove(e.bestMove)).isMaterial())
							hT.recordUnsuccessfulMove(hashMove);	// Record failure in the relative history table.
						return score;
					}
				}
				// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
				if (e.bestMove != 0) {
					hashMove = Move.toMove(e.bestMove);
					isThereHashMove = true;
				}
			}
			// Return the score from the quiescence search in case a leaf node has been reached.
			if (depth == 0) {
				score = quiescence(qDepth, alpha, beta);
				if (score > bestScore) {
					bestScore = score;
					if (score > alpha)
						alpha = score;
				}
				break Search;
			}
			// If there is no hash entry in a PV node that is to be searched deep, try IID.
			if (!isThereHashMove) {
				if (depth > 5 && beta > alpha + 1) {
					IIDdepth = (depth > 7) ? depth/2 : depth - 2;
					extPly = ply;
					for (int i = 2; i < IIDdepth; i++) {
						ply = i;
						search(i, alpha, beta, true, qDepth);
					}
					ply = extPly;
					e = tT.lookUp(pos.key);
					if (e != null && e.bestMove != 0) {
						hashMove = Move.toMove(e.bestMove);
						isThereHashMove = true;
					}
				}
			}
			// If there is a hash move, search that first.
			if (isThereHashMove) {
				pos.makeMove(hashMove);
				score = -search(depth - 1, -beta, -alpha, true, qDepth);
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = hashMove;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta) {
							if (!hashMove.isMaterial()) {
								kT.add(distFromRoot, hashMove);	// Add to killer moves.
								hT.recordSuccessfulMove(hashMove);	// Record success in the relative history table.
							}
							break Search;
						}
						else if (!hashMove.isMaterial())
							hT.recordUnsuccessfulMove(hashMove);	// Record failure in the relative history table.
					}
				}
			}
			// Generate material moves.
			matMoves = pos.generateMaterialMoves();
			// If there was no hash move or material moves, perform a mate check.
			if (!isThereHashMove && matMoves.length() == 0) {
				nonMatMoves = pos.generateNonMaterialMoves();
				if (nonMatMoves.length() == 0) {
					score = inCheck ? mateScore : Termination.STALE_MATE.score;
					if (score > bestScore) {
						bestScore = score;
						if (score > alpha)
							alpha = score;
					}
					break Search;
				}
			}
			// Check for the fifty-move rule; return a draw score if it applies.
			if (pos.getFiftyMoveRuleClock() >= 100)
				return Termination.DRAW_CLAIMED.score;
			// If it is not a terminal node, try null move pruning if it is allowed and the side to move is not in check.
			if (nullMoveAllowed && nullMoveObservHolds && depth >= NMR && !inCheck) {
				pos.makeNullMove();
				// Do not allow consecutive null moves.
				if (depth == NMR)
					score = -search(depth - NMR, -beta, -beta + 1, false, qDepth);
				else
					score = -search(depth - NMR - 1, -beta, -beta + 1, false, qDepth);
				pos.unmakeMove();
				if (score >= beta) {
					return score;
				}
			}
			// Order the material moves.
			matMovesArr = orderMaterialMoves(matMoves);
			matMoveBreakInd = 0;
			// Search winning and equal captures.
			for (int i = 0; i < matMovesArr.length; i++) {
				move = matMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				// If the current move's order-value indicates a losing capture, break the search to check the killer moves.
				if (move.value < 0) {
					matMoveBreakInd = i;
					break;
				}
				pos.makeMove(move);
				// PVS.
				if (i == 0)
					score = -search(depth - 1, -beta, -alpha, true, qDepth);
				else {
					score = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
					if (score > alpha && score < beta)
						score = -search(depth - 1, -beta, -score, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta)
							break Search;
					}
				}
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
			// If there are no more winning or equal captures, check and search the killer moves if legal from this position.
			kE = kT.retrieve(distFromRoot);
			if ((kMove = kE.getMove1()) != 0) {	// Killer move no. 1.
				killerMove1 = Move.toMove(kMove);
				if (pos.isLegalSoft(killerMove1) && (!isThereHashMove || !killerMove1.equals(hashMove))) {
					isThereKM1 = true;
					pos.makeMove(killerMove1);
					if (!isThereHashMove && matMoveBreakInd == 0)
						score = -search(depth - 1, -beta, -alpha, true, qDepth);
					else {
						score = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -search(depth - 1, -beta, -score, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = killerMove1;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								hT.recordSuccessfulMove(killerMove1);	// Record success in the relative history table.
								break Search;
							}
							else
								hT.recordUnsuccessfulMove(killerMove1);	// Record failure in the relative history table.
						}
					}
					if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
						break Search;
				}
			}
			if ((kMove = kE.getMove2()) != 0) {	// Killer move no. 2.
				killerMove2 = Move.toMove(kMove);
				if (pos.isLegalSoft(killerMove2) && (!isThereHashMove || !killerMove2.equals(hashMove))) {
					isThereKM2 = true;
					pos.makeMove(killerMove2);
					if (!isThereHashMove && !isThereKM1 && matMoveBreakInd == 0)
						score = -search(depth - 1, -beta, -alpha, true, qDepth);
					else {
						score = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -search(depth - 1, -beta, -score, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = killerMove2;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								hT.recordSuccessfulMove(killerMove2);	// Record success in the relative history table.
								break Search;
							}
							else
								hT.recordUnsuccessfulMove(killerMove2);	// Record failure in the relative history table.
						}
					}
					if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
						break Search;
				}
			}	// Killer move check ending.
			// Search losing captures if there are any.
			for (int i = matMoveBreakInd; i < matMovesArr.length; i++) {
				move = matMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				pos.makeMove(move);
				// PVS.
				if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2)
					score = -search(depth - 1, -beta, -alpha, true, qDepth);
				else {
					score = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
					if (score > alpha && score < beta)
						score = -search(depth - 1, -beta, -score, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta)
							break Search;
					}
				}
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
			// Generate the non-material legal moves if they are not generated yet.
			if (nonMatMoves == null)
				nonMatMoves = pos.generateNonMaterialMoves();
			// Order and search the non-material moves.
			nonMatMovesArr = orderNonMaterialMoves(nonMatMoves);
			for (int i = 0; i < nonMatMovesArr.length; i++) {
				move = nonMatMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				// If this move was the first killer move, skip it.
				if (isThereKM1 && move.equals(killerMove1)) {
					isThereKM1 = false;
					continue;
				}
				// If this move was the second killer move, skip it.
				if (isThereKM2 && move.equals(killerMove2)) {
					isThereKM2 = false;
					continue;
				}
				pos.makeMove(move);
				// Try late move reduction if not within the PV.
				if (depth > 2 && beta == alpha + 1 && !inCheck && pos.getUnmakeRegister().checkers == 0 &&
						alpha < checkMateLim && searchedMoves > 4) {
					score = -search(depth - LMR - 1, -alpha - 1, -alpha, true, qDepth);
					// If it does not fail low, research with "full" window.
					if (score > alpha)
						score = -search(depth - 1, -beta, -score, true, qDepth);
				}
				// Else PVS.
				else if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2 && matMovesArr.length == 0)
					score = -search(depth - 1, -beta, -alpha, true, qDepth);
				else {
					score = -search(depth - 1, -alpha - 1, -alpha, true, qDepth);
					if (score > alpha && score < beta)
						score = -search(depth - 1, -beta, -score, true, qDepth);
				}
				pos.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta) {	// Cutoff from a non-material move.
							kT.add(distFromRoot, move);	// Add to killer moves.
							hT.recordSuccessfulMove(move);	// Record success in the relative history table.
							break Search;
						}
						else
							hT.recordUnsuccessfulMove(move);	// Record failure in the relative history table.
					}
				}
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break Search;
			}
		}
		// If the search has been invoked from quiescence search, do not store entries in the TT.
		if (depth < 0)
			return bestScore;
		// Adjustment of the best score for TT insertion according to the distance from the mate position in case it's a check mate score.
		if (bestScore <= checkMateLim)
			score = bestScore - distFromRoot;
		else if (bestScore >= -checkMateLim)
			score = bestScore + distFromRoot;
		else
			score = bestScore;
		bestMoveInt = bestMove == null ? 0 : bestMove.toInt();
		//	Add new entry to the transposition table.
		if (bestScore <= alpha)
			tT.insert(new TTEntry(pos.key, depth, NodeType.FAIL_LOW.ind, score, bestMoveInt, eGen));
		else if (bestScore >= beta)
			tT.insert(new TTEntry(pos.key, depth, NodeType.FAIL_HIGH.ind, score, bestMoveInt, eGen));
		else
			tT.insert(new TTEntry(pos.key, depth, NodeType.EXACT.ind, score, bestMoveInt, eGen));
		// Return the unadjusted best score.
		return bestScore;
	}
	/**
	 * A search algorithm for diminishing the horizon effect once the main search algorithm has reached a leaf node. It keep searching until
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
		final int mateScore = Termination.CHECK_MATE.score + ply - depth;
		final boolean inCheck = pos.getCheck();
		List<Move> tacticalMoves, allMoves;
		long[] checkSquares;
		Move[] moves;
		Move move;
		int bestScore, searchScore;
		if (depth != 0)
			nodes.incrementAndGet();
		// If the side to move is in check, stand-pat does not hold and the main search will be called later on so no moves need to be generated.
		if (inCheck) {
			tacticalMoves = null;
			bestScore = mateScore;
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
					bestScore = allMoves.length() == 0 ? (inCheck ? mateScore : Termination.STALE_MATE.score) : eval.score(pos);
				}
				// Else no bound.
				else
					bestScore = mateScore;
			}
			// After that, only material moves.
			else {
				tacticalMoves = pos.generateMaterialMoves();
				// Stand-pat, evaluate position.
				if (nullMoveObservHolds) {
					allMoves = pos.generateNonMaterialMoves();
					allMoves.addAll(tacticalMoves);
					bestScore = allMoves.length() == 0 ? (inCheck ? mateScore : Termination.STALE_MATE.score) : eval.score(pos);
				}
				// No bound.
				else
					bestScore = mateScore;
			}
		}
		// Fail soft.
		if (bestScore >= beta)
			return bestScore;
		if (bestScore > alpha)
			alpha = bestScore;
		// If check, call the main search for one ply (while keeping the track of the quiescence search depth to avoid resetting it).
		if (inCheck) {
			searchScore = -search(1, alpha, beta, false, depth - 2);
			if (searchScore > bestScore) {
				bestScore = searchScore;
				if (bestScore > alpha)
					alpha = bestScore;
			}
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
				if (searchScore > bestScore) {
					bestScore = searchScore;
					if (bestScore > alpha) {
						alpha = bestScore;
						if (alpha >= beta)
							break;
					}
				}
				if (currentThread().isInterrupted() || System.currentTimeMillis() >= deadLine)
					break;
			}
		}
		return bestScore;
	}
	/**
	 * Orders material moves and checks, the former of which according to the SEE swap algorithm.
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
	/**
	 * Orders captures and promotions according to the LVA-MVV principle; in case of a promotion, add the standard value of a queen to the score.
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
				move.value = (short)(Material.getByPieceInd(move.capturedPiece).score - Material.getByPieceInd(move.movedPiece).score);
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
	}
	/**
	 * Orders non-material moves according to the relative history heuristic.
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
	/**
	 * Returns an array of Move objects according to the best line of play extracted form the transposition table.
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
