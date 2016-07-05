package engine;

import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import engine.KillerTable.KillerTableEntry;
import util.*;

/**
 * A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It
 * uses principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative
 * deepening framework.
 * 
 * @author Viktor
 *
 */
public class Search implements Runnable {
	
	/**
	 * An observable class for the principal variation of a search.
	 * 
	 * @author Viktor
	 *
	 */
	public class Results extends Observable {
		
		private Queue<Move> pVline;	// Principal variation.
		private short nominalDepth;	// The depth to which the PV has been searched.
		private short score;		// The result score of the search.
		private long nodes;			// The number of nodes searched.
		private long time;			// Time spent on the search.
		private boolean isFinal;	// Whether it is the final result of the search.
		
		private Results() {
			
		}
		/**
		 * Returns the principal variation of the search as a queue of moves.
		 * 
		 * @return
		 */
		public Queue<Move> getPvLine() {
			return pVline;
		}
		/**
		 * Returns the greatest nominal depth of the search. It does not necessarily mean that the whole ply has been searched.
		 * 
		 * @return
		 */
		public short getNominalDepth() {
			return nominalDepth;
		}
		/**
		 * Returns the result score of the search for the side to move.
		 * 
		 * @return
		 */
		public short getScore() {
			return score;
		}
		/**
		 * Returns the number of nodes searched to reach this result.
		 * 
		 * @return
		 */
		public long getNodes() {
			return nodes;
		}
		/**
		 * Returns the time spent on the search to reach this result in milliseconds.
		 * 
		 * @return
		 */
		public long getTime() {
			return time;
		}
		/**
		 * Returns whether the result is final, i.e. it will not be updated anymore in this run of the search.
		 * 
		 * @return
		 */
		public boolean isFinal() {
			return isFinal;
		}
		private void set(Queue<Move> PVline, short nominalDepth, short score, long nodes, long time, boolean isFinal) {
			this.pVline = PVline;
			this.nominalDepth = nominalDepth;
			this.score = score;
			this.nodes = nodes;
			this.time = time;
			this.isFinal = isFinal;
			setChanged();
			notifyObservers();
		}
		/**
		 * Returns a one-line String representation of the principal variation result.
		 * 
		 * @return
		 */
		public String getPvString() {
			String out = "";
			for (Move m : pVline)
				out += m.toString() + " ";
			out += "\n";
			return out;
		}
		/**
		 * Returns a String of some search statistics such as greatest nominal depth, score, search speed, etc.
		 * 
		 * @return
		 */
		public String getStatString() {
			String out = "";
			out += "Nominal depth: " + nominalDepth + "\n";
			out += "Score: " + score + "\n";
			out += String.format("Time: %.2fs\n", (float)time/1000);
			out += "Nodes: " + nodes + "\n";
			out += "Search speed: " + nodes/Math.max(time, 1) + "kNps\n";
			return out;
		}
		@Override
		public String toString() {
			return getPvString() + getStatString();
		}
	}
	
	/**
	 * A thread-task for parallel search.
	 * 
	 * @author Viktor
	 *
	 */
	private class SearchThread implements Callable<Integer> {
		
		private Position pos;
		private short ply;
		private int depth;
		private int alpha;
		private int beta;
		private boolean nullMoveAllowed;
		private int qDepth;
		private boolean boundsChanged;
		private boolean doStopThread;
		
		SearchThread(Position pos, short ply, int depth, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
			this.pos = pos;
			this.ply = ply;
			this.depth = depth;
			this.alpha = alpha;
			this.beta = beta;
			this.nullMoveAllowed = nullMoveAllowed;
			this.qDepth = qDepth;
			boundsChanged = false;
			doStopThread = false;
		}
		@Override
		public Integer call() {
			return pVsearch(depth, alpha, beta, nullMoveAllowed, qDepth);
		}
		/**
		 * Updates the alpha boundary of the search thread in case an improvement to it has been found in an ancestor node.
		 * 
		 * @param alpha
		 */
		private void updateAlpha(int alpha) {
			this.alpha = alpha;
			boundsChanged = true;
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
		private int pVsearch(int depth, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
			final int distFromRoot = ply - (depth/FULL_PLY + qDepth);
			final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
			final int origAlpha = alpha;
			final boolean isInCheck = pos.isInCheck;
			final boolean isPvNode = beta > origAlpha + 1;
			int bestScore, score, searchedMoves, matMoveBreakInd, kMove, bestMoveInt, evalScore, razRed, extension;
			short extPly;
			Move hashMove, bestMove, killerMove1, killerMove2, move, lastMove;
			boolean moveAllowed, isThereHashMove, isThereKM1, isThereKM2, lastMoveIsMaterial;
			Queue<Move> matMoves, nonMatMoves;
			Move[] matMovesArr, nonMatMovesArr;
			TTEntry e;
			KillerTableEntry kE;
			bestScore = mateScore;
			bestMove = null;
			searchedMoves = 0;
			hashMove = killerMove1 = killerMove2 = null;
			isThereHashMove = isThereKM1 = isThereKM2 = false;
			matMoves = nonMatMoves = null;
			nodes.incrementAndGet();
			if (!pondering && (System.currentTimeMillis() >= deadLine || nodes.get() >= maxNodes))
				doStopSearch.set(true);
			if (Thread.currentThread().isInterrupted())
				doStopThread = true;
			/*if (boundsChanged) {
				if (this.alpha > alpha)
					alpha = this.alpha;
				if (this.beta < beta)
					beta = this.beta;
			}*/
			Search: {
				// Check for the repetition rule; return a draw score if it applies.
				if (pos.repetitions >= 3)
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
					e.generation = hashEntryGen;
					// If the hashed entry's depth is greater than or equal to the current search depth, check if the stored score is usable.
					if (!isPvNode && e.depth >= depth/FULL_PLY) {
						// Mate score adjustment to root distance.
						if (e.score <= L_CHECK_MATE_LIMIT)
							score = e.score + distFromRoot;
						else if (e.score >= W_CHECK_MATE_LIMIT)
							score = e.score - distFromRoot;
						else
							score = e.score;
						/* If the score was exact, or it was the score of an all node and is smaller than or equal to alpha, or it is that of a cut
						 * node and is greater than or equal to beta, return the score. */
						if (e.type == NodeType.EXACT.ind ||
								/* To make sure that a score that might not have been the exact score for the subtree below the node regardless of the
								 * alpha-beta boundaries is not treated as an exact score in the current context, we can not allow it to fall between
								 * the current alpha and beta. If it was a fail high node, the score is a lower boundary of the exact score of the
								 * node due to there possibly being siblings to the right of the child node that raised alpha higher than beta and
								 * caused a cut-off that could raise alpha even higher. If it was a fail low node, the score is a higher boundary for
								 * the exact score of the node, because all children of a fail low node are fail high nodes (-score <= alpha ->
								 * score >= -alpha [-alpha = beta in the child node]). To keep the interval of values the exact score could take on
								 * out of (alpha, beta), the score has to be lower than or equal to alpha if it is a higher boundary, i.e. fail low
								 * score, and it has to be greater than or equal to beta if it is a lower boundary i.e. fail high score.
								 */
								(e.type == NodeType.FAIL_HIGH.ind && score >= beta) || (e.type == NodeType.FAIL_LOW.ind && score <= alpha)) {
							if (score >= beta && e.bestMove != 0 && !(bestMove = Move.toMove(e.bestMove)).isMaterial()) {
								// Only if not in the quiescence search's check extension.
								if (qDepth == 0)
									kT.add(distFromRoot, bestMove);
							}
							return score;
						}
						
					}
					// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
					if (e.bestMove != 0) {
						hashMove = Move.toMove(e.bestMove);
						isThereHashMove = true;
					}
				}
				// Check extension (less than a whole ply because the quiescence search handles checks).
				depth = isInCheck && qDepth == 0 ? depth + FULL_PLY/4 : depth;
				// Return the score from the quiescence search in case a leaf node has been reached.
				if (depth/FULL_PLY <= 0) {
					score = quiescence(qDepth, alpha, beta);
					if (score > bestScore) {
						bestMove = null;
						bestScore = score;
						if (score > alpha)
							alpha = score;
					}
					break Search;
				}
				// If there is no hash entry in a PV node that is to be searched deep, try IID.
				if (isPvNode && !isThereHashMove && depth/FULL_PLY >= 5) {
					extPly = ply;
					for (short i = 1; i < depth/FULL_PLY*3/5; i++) {
						ply = i;
						pVsearch(ply*FULL_PLY, alpha, beta, true, qDepth);
					}
					ply = extPly;
					e = tT.lookUp(pos.key);
					if (e != null && e.bestMove != 0) {
						hashMove = Move.toMove(e.bestMove);
						isThereHashMove = true;
					}
				}
				lastMove = pos.getLastMove();
				lastMoveIsMaterial = lastMove != null && lastMove.isMaterial();
				// If there is a hash move, search that first.
				if (isThereHashMove) {
					moveAllowed = true;
					// Check if move is allowed.
					if (areMovesRestricted && distFromRoot == 0 && qDepth == 0) {
						moveAllowed = false;
						while (allowedRootMoves.hasNext()) {
							if (hashMove.equals(allowedRootMoves.next())) {
								moveAllowed = true;
								allowedRootMoves.reset();
								break;
							}
						}
					}
					if (moveAllowed) {
						// Recapture extension (includes capturing newly promoted pieces).
						extension = lastMoveIsMaterial && hashMove.capturedPiece != Piece.NULL.ind && hashMove.to == lastMove.to ? FULL_PLY/2 : 0;
						pos.makeMove(hashMove);
						score = -pVsearch(depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
						pos.unmakeMove();
						searchedMoves++;
						if (score > bestScore) {
							bestMove = hashMove;
							bestScore = score;
							if (score > alpha) {
								alpha = score;
								if (alpha >= beta) {
									if (!hashMove.isMaterial()) {
										if (qDepth == 0)
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
				}
				// Generate material moves.
				matMoves = pos.getTacticalMoves();
				// If there was no hash move or material moves, perform a mate check.
				if (!isThereHashMove && matMoves.length() == 0) {
					nonMatMoves = pos.getQuietMoves();
					if (nonMatMoves.length() == 0) {
						score = isInCheck ? mateScore : Termination.STALE_MATE.score;
						if (score > bestScore) {
							bestMove = null;
							bestScore = score;
							if (score > alpha)
								alpha = score;
						}
						break Search;
					}
				}
				// Check for the fifty-move rule; return a draw score if it applies.
				if (pos.fiftyMoveRuleClock >= 100)
					return Termination.DRAW_CLAIMED.score;
				// If it is not a terminal or PV node, try null move pruning if it is allowed and the side to move is not in check.
				if (nullMoveAllowed && nullMoveObservHolds && !isInCheck && !isPvNode && depth/FULL_PLY >= params.NMR) {
					pos.makeNullMove();
					// Do not allow consecutive null moves.
					if (depth/FULL_PLY == params.NMR) {
						score = -pVsearch(depth - params.NMR*FULL_PLY, -beta, -beta + 1, false, qDepth);
						// Mate threat extension.
						if (score <= L_CHECK_MATE_LIMIT)
							depth += FULL_PLY;
					}
					else
						score = -pVsearch(depth - (params.NMR + 1)*FULL_PLY, -beta, -beta + 1, false, qDepth);
					pos.unmakeMove();
					if (score >= beta) {
						return score;
					}
				}
				// Sort the material moves.
				matMovesArr = orderMaterialMovesMVVLVA(matMoves);
				matMoveBreakInd = 0;
				// Search winning and equal captures.
				for (int i = 0; i < matMovesArr.length; i++) {
					move = matMovesArr[i];
					// If this move was the hash move, skip it.
					if (isThereHashMove && move.equals(hashMove)) {
						isThereHashMove = false;
						continue;
					}
					// Check if move is allowed.
					if (areMovesRestricted && distFromRoot == 0 && qDepth == 0) {
						moveAllowed = false;
						while (allowedRootMoves.hasNext()) {
							if (move.equals(allowedRootMoves.next())) {
								moveAllowed = true;
								allowedRootMoves.reset();
								break;
							}
						}
						if (!moveAllowed)
							continue;
					}
					// If the current move's order-value indicates a losing capture, break the search to check the killer moves.
					if (move.value < 0) {
						matMoveBreakInd = i;
						break;
					}
					// Recapture extension (includes capturing newly promoted pieces).
					extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? FULL_PLY/2 : 0;
					pos.makeMove(move);
					// PVS.
					if (i == 0)
						score = -pVsearch(depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(depth + extension - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					/*if (boundsChanged) {
						if (this.alpha > alpha)
							alpha = this.alpha;
						if (this.beta < beta)
							beta = this.beta;
						if (depth == 0)
							boundsChanged = false;
					}*/
					if (score > bestScore) {
						bestMove = move;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta)
								break Search;
						}
					}
					if (doStopSearch.get() || doStopThread)
						break Search;
				}
				// If there are no more winning or equal captures, check and search the killer moves if legal from this position.
				kE = kT.retrieve(distFromRoot);
				if ((kMove = kE.getMove1()) != 0) {	// Killer move no. 1.
					killerMove1 = Move.toMove(kMove);
					moveAllowed = true;
					// Check if move is allowed.
					if (areMovesRestricted && distFromRoot == 0 && qDepth == 0) {
						moveAllowed = false;
						while (allowedRootMoves.hasNext()) {
							if (killerMove1.equals(allowedRootMoves.next())) {
								moveAllowed = true;
								allowedRootMoves.reset();
								break;
							}
						}
					}
					if (moveAllowed && pos.isLegalSoft(killerMove1) && (!isThereHashMove || !killerMove1.equals(hashMove))) {
						isThereKM1 = true;
						pos.makeMove(killerMove1);
						if (!isThereHashMove && matMoveBreakInd == 0)
							score = -pVsearch(depth - FULL_PLY, -beta, -alpha, true, qDepth);
						else {
							score = -pVsearch(depth - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
							if (score > alpha && score < beta)
								score = -pVsearch(depth - FULL_PLY, -beta, -alpha, true, qDepth);
						}
						pos.unmakeMove();
						searchedMoves++;
						/*if (boundsChanged) {
							if (this.alpha > alpha)
								alpha = this.alpha;
							if (this.beta < beta)
								beta = this.beta;
							if (depth == 0)
								boundsChanged = false;
						}*/
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
						if (doStopSearch.get() || doStopThread)
							break Search;
					}
				}
				if ((kMove = kE.getMove2()) != 0) {	// Killer move no. 2.
					killerMove2 = Move.toMove(kMove);
					moveAllowed = true;
					// Check if move is allowed.
					if (areMovesRestricted && distFromRoot == 0 && qDepth == 0) {
						moveAllowed = false;
						while (allowedRootMoves.hasNext()) {
							if (killerMove2.equals(allowedRootMoves.next())) {
								moveAllowed = true;
								allowedRootMoves.reset();
								break;
							}
						}
					}
					if (moveAllowed && pos.isLegalSoft(killerMove2) && (!isThereHashMove || !killerMove2.equals(hashMove))) {
						isThereKM2 = true;
						pos.makeMove(killerMove2);
						if (!isThereHashMove && !isThereKM1 && matMoveBreakInd == 0)
							score = -pVsearch(depth - FULL_PLY, -beta, -alpha, true, qDepth);
						else {
							score = -pVsearch(depth - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
							if (score > alpha && score < beta)
								score = -pVsearch(depth - FULL_PLY, -beta, -alpha, true, qDepth);
						}
						pos.unmakeMove();
						searchedMoves++;
						/*if (boundsChanged) {
							if (this.alpha > alpha)
								alpha = this.alpha;
							if (this.beta < beta)
								beta = this.beta;
							if (depth == 0)
								boundsChanged = false;
						}*/
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
						if (doStopSearch.get() || doStopThread)
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
					// Check if move is allowed.
					if (areMovesRestricted && distFromRoot == 0 && qDepth == 0) {
						moveAllowed = false;
						while (allowedRootMoves.hasNext()) {
							if (move.equals(allowedRootMoves.next())) {
								moveAllowed = true;
								allowedRootMoves.reset();
								break;
							}
						}
						if (!moveAllowed)
							continue;
					}
					// Recapture extension.
					extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? FULL_PLY/2 : 0;
					pos.makeMove(move);
					// PVS.
					if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2)
						score = -pVsearch(depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(depth + extension - FULL_PLY, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(depth + extension - FULL_PLY, -beta, -alpha, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					/*if (boundsChanged) {
						if (this.alpha > alpha)
							alpha = this.alpha;
						if (this.beta < beta)
							beta = this.beta;
						if (depth == 0)
							boundsChanged = false;
					}*/
					if (score > bestScore) {
						bestMove = move;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta)
								break Search;
						}
					}
					if (doStopSearch.get() || doStopThread)
						break Search;
				}
				// Generate the non-material legal moves if they are not generated yet.
				if (nonMatMoves == null)
					nonMatMoves = pos.getQuietMoves();
				// One reply extension.
				if (matMoves.length() == 0 && nonMatMoves.length() == 1)
					depth += FULL_PLY/2;
				evalScore = Integer.MIN_VALUE;
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
					// Check if move is allowed.
					if (areMovesRestricted && distFromRoot == 0 && qDepth == 0) {
						moveAllowed = false;
						while (allowedRootMoves.hasNext()) {
							if (move.equals(allowedRootMoves.next())) {
								moveAllowed = true;
								allowedRootMoves.reset();
								break;
							}
						}
						if (!moveAllowed)
							continue;
					}
					razRed = 0;
					// Futility pruning, extended futility pruning, and razoring.
					if (!isPvNode && depth/FULL_PLY <= 3 && !isInCheck && !pos.givesCheck(move)) {
						if (evalScore == Integer.MIN_VALUE)
							evalScore = eval.score(pos, alpha, beta);
						if (depth/FULL_PLY == 1) {
							if (evalScore + params.FMAR1 <= alpha)
								continue;
						}
						else if (depth/FULL_PLY == 2) {
							if (evalScore + params.FMAR2 <= alpha)
								continue;
						}
						else {
							if (evalScore + params.FMAR3 <= alpha)
								razRed = 1;
						}
					}
					pos.makeMove(move);
					// Try late move reduction if not within the PV.
					if (razRed == 0 && !isPvNode && !isInCheck && depth/FULL_PLY > 2 && searchedMoves > params.LMRMSM &&
							pos.getUnmakeRegister().checkers == 0) {
						score = -pVsearch(depth - (params.LMR + 1)*FULL_PLY, -alpha - 1, -alpha, true, qDepth);
						// If it does not fail low, research with full window.
						if (score > alpha)
							score = -pVsearch(depth - FULL_PLY, -beta, -alpha, true, qDepth);
					}
					// Else PVS.
					else if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2 && matMovesArr.length == 0)
						score = -pVsearch(depth - (razRed + 1)*FULL_PLY, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(depth - (razRed + 1)*FULL_PLY, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(depth - (razRed + 1)*FULL_PLY, -beta, -alpha, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
					/*if (boundsChanged) {
						if (this.alpha > alpha)
							alpha = this.alpha;
						if (this.beta < beta)
							beta = this.beta;
						if (depth == 0)
							boundsChanged = false;
					}*/
					if (score > bestScore) {
						bestMove = move;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {	// Cutoff from a non-material move.
								if (qDepth == 0)
									kT.add(distFromRoot, move);	// Add to killer moves.
								hT.recordSuccessfulMove(move);	// Record success in the relative history table.
								break Search;
							}
							else
								hT.recordUnsuccessfulMove(move);	// Record failure in the relative history table.
						}
					}
					if (doStopSearch.get() || doStopThread)
						break Search;
				}
			}
			// If the search has been invoked from quiescence search, do not store entries in the TT.
			if (qDepth < 0)
				return bestScore;
			// Adjustment of the best score for TT insertion according to the distance from the mate position in case it's a check mate score.
			if (bestScore <= L_CHECK_MATE_LIMIT)
				score = bestScore - distFromRoot;
			else if (bestScore >= W_CHECK_MATE_LIMIT)
				score = bestScore + distFromRoot;
			else
				score = bestScore;
			bestMoveInt = bestMove == null ? 0 : bestMove.toInt();
			//	Add new entry to the transposition table.
			if (bestScore <= origAlpha)
				tT.insert(new TTEntry(pos.key, (short)(depth/FULL_PLY), NodeType.FAIL_LOW.ind, (short)score, bestMoveInt, hashEntryGen));
			else if (bestScore >= beta)
				tT.insert(new TTEntry(pos.key, (short)(depth/FULL_PLY), NodeType.FAIL_HIGH.ind, (short)score, bestMoveInt, hashEntryGen));
			else
				tT.insert(new TTEntry(pos.key, (short)(depth/FULL_PLY), NodeType.EXACT.ind, (short)score, bestMoveInt, hashEntryGen));
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
			final int distFromRoot = ply - depth;
			final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
			final boolean isInCheck = pos.isInCheck;
			List<Move> materialMoves, quietMoves;
			Move[] moves;
			Move move;
			int bestScore, searchScore;
			if (depth != 0)
				nodes.incrementAndGet();
			if (!pondering && (System.currentTimeMillis() >= deadLine || nodes.get() >= maxNodes))
				doStopSearch.set(true);
			if (Thread.currentThread().isInterrupted())
				doStopThread = true;
			/*if (boundsChanged) {
				if (this.alpha > alpha)
					alpha = this.alpha;
				if (this.beta < beta)
					beta = this.beta;
			}*/
			// Check for the repetition rule; return a draw score if it applies.
			if (pos.repetitions >= 3)
				return Termination.DRAW_CLAIMED.score;
			// Just for my peace of mind.
			if (distFromRoot >= MAX_EXPECTED_TOTAL_SEARCH_DEPTH)
				return eval.score(pos, alpha, beta);
			// If check, call the main search for one ply (while keeping the track of the quiescence search depth to avoid resetting it).
			if (isInCheck) {
				// If the side to move is in check, stand-pat does not hold and it's not enough to just search the material moves.
				nodes.decrementAndGet();
				return pVsearch(FULL_PLY, alpha, beta, false, depth - 1);
			}
			// Quiescence search.
			else {
				materialMoves = pos.getTacticalMoves();
				if (materialMoves.length() == 0) {
					quietMoves = pos.getQuietMoves();
					if (quietMoves.length() == 0)
						return Termination.STALE_MATE.score;
				}
				// Check for the fifty-move rule; return a draw score if it applies.
				if (pos.fiftyMoveRuleClock >= 100)
					return Termination.DRAW_CLAIMED.score;
				if (materialMoves.length() == 0)
					return eval.score(pos, alpha, beta);
				else {
					// Stand pat if the null move observation holds; otherwise the mate score is the lower bound.
					bestScore = nullMoveObservHolds ? eval.score(pos, alpha, beta) : mateScore;
				}
				// Fail soft.
				if (bestScore > alpha) {
					alpha = bestScore;
					if (bestScore >= beta)
						return bestScore;
				}
				moves = orderMaterialMovesSEE(pos, materialMoves);
				for (int i = 0; i < moves.length; i++) {
					move = moves[i];
					// If the SEE value is below 0 or the delta pruning limit, break the search because the rest of the moves are even worse.
					if (nullMoveObservHolds && (move.value < 0 || move.value < alpha - params.Q_DELTA))
						break;
					pos.makeMove(move);
					searchScore = -quiescence(depth - 1, -beta, -alpha);
					pos.unmakeMove();
					/*if (boundsChanged) {
						if (this.alpha > alpha)
							alpha = this.alpha;
						if (this.beta < beta)
							beta = this.beta;
					}*/
					if (searchScore > bestScore) {
						bestScore = searchScore;
						if (bestScore > alpha) {
							alpha = bestScore;
							if (alpha >= beta)
								break;
						}
					}
					if (doStopSearch.get() || doStopThread)
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
		private Move[] orderMaterialMovesSEE(Position pos, List<Move> moves) {
			Move[] arr = new Move[moves.length()];
			Move move;
			int i = 0;
			while (moves.hasNext()) {
				move = moves.next();
				move.value = eval.SEE(pos, move);	// Static exchange evaluation.
				arr[i++] = move;
			}
			return QuickSort.sort(arr);
		}
		/**
		 * Orders captures and promotions according to the MVV-LVA principle; in case of a promotion, add the standard value of a queen to the score.
		 * 
		 * @param moves
		 * @return
		 */
		private Move[] orderMaterialMovesMVVLVA(List<Move> moves) {
			Move[] arr = new Move[moves.length()];
			Move move;
			int i = 0;
			while (moves.hasNext()) {
				move = moves.next();
				if (move.type >= MoveType.PROMOTION_TO_QUEEN.ind) {
					move.value = (short)(params.QUEEN_VALUE - params.PAWN_VALUE);
					if (move.capturedPiece != Piece.NULL.ind)
						move.value += eval.materialValueByPieceInd(move.capturedPiece) - eval.materialValueByPieceInd(move.movedPiece);
				}
				else
					move.value = (short)(eval.materialValueByPieceInd(move.capturedPiece) - eval.materialValueByPieceInd(move.movedPiece));
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
	}
	
	public final static int MAX_NOMINAL_SEARCH_DEPTH = 64;
	private final static int MAX_EXPECTED_TOTAL_SEARCH_DEPTH = 8*3*MAX_NOMINAL_SEARCH_DEPTH;	// Including extensions and quiescence search.
	private final static int L_CHECK_MATE_LIMIT = Termination.CHECK_MATE.score + MAX_EXPECTED_TOTAL_SEARCH_DEPTH;
	private final static int W_CHECK_MATE_LIMIT = -L_CHECK_MATE_LIMIT;
	
	private final static int FULL_PLY = 4;	// For fractional ply extensions.
	
	private Parameters params;
	
	private ThreadPoolExecutor threadPool;
	private ExecutorCompletionService<Integer> compService;
	private int numOfThreads;
	
	private Position position;
	private boolean nullMoveObservHolds;	// Whether heuristics based on the null move observation such as stand-pat and NMP are applicable.
	private Results results;
	
	private KillerTable kT;			// Killer heuristic table.
	private RelativeHistoryTable hT;// History heuristic table.
	private HashTable<TTEntry> tT;	// Transposition table.
	private byte hashEntryGen;		// Entry generation.
	
	private Evaluator eval;
	
	private boolean pondering;
	private long searchTime;
	private long deadLine;
	private int maxDepth;
	private long maxNodes;
	private AtomicLong nodes;	// Number of searched positions.
	private List<Move> allowedRootMoves;	// The only moves to search at the root.
	private boolean areMovesRestricted;
	
	private AtomicBoolean doStopSearch;
	
	/**
	 * Creates a new Search thread instance for searching a position for the specified amount of time, maximum number of searched nodes, and
	 * moves to search; if maxNodes is <= 0, the engine will ponder on the position once the search thread is started, and will not stop until
	 * it is interrupted.
	 * 
	 * @param pos The position to search.
	 * @param timeLeft Time left until the end of the time control.
	 * @param maxNodes Max number of searched nodes.
	 * @param moves Moves to search.
	 * @param gamePhase The game phase in which the position is.
	 * @param historyTable History table.
	 * @param hashEntryGen Hash entry generation.
	 * @param transposTable Transposition table.
	 * @param evalTable Evaluation hash table.
	 * @param pawnTable Pawn hash table.
	 */
	public Search(Position position, long timeLeft, long oppTimeLeft, long searchTime, int maxDepth, long maxNodes, List<Move> moves,
			RelativeHistoryTable historyTable, byte hashEntryGen, HashTable<TTEntry> transposTable, HashTable<ETEntry> evalTable,
			HashTable<PTEntry> pawnTable, Parameters params, int numOfSearchThreads) {
		this.params = params;
		eval = new Evaluator(params, evalTable, pawnTable, hashEntryGen);
		numOfThreads = numOfSearchThreads;
		this.position = position.deepCopy();
		int phaseScore = eval.phaseScore(position);
		nullMoveObservHolds = eval.phaseScore(position) < params.GAME_PHASE_END_GAME_LOWER;
		if (timeLeft <= 0 && searchTime <= 0 && maxDepth <= 0 && maxNodes <= 0) {
			pondering = true;
			this.maxDepth = MAX_NOMINAL_SEARCH_DEPTH;
		}
		else {
			pondering = false;
			this.searchTime = searchTime > 0 ? searchTime : allocateSearchTime(this.position, phaseScore, timeLeft, oppTimeLeft);
			this.maxDepth = maxDepth > 0 ? maxDepth : MAX_NOMINAL_SEARCH_DEPTH;
			this.maxNodes = maxNodes > 0 ? maxNodes : Long.MAX_VALUE;
		}
		allowedRootMoves = moves;
		areMovesRestricted = allowedRootMoves != null;
		doStopSearch = new AtomicBoolean(false);
		kT = new KillerTable(3*this.maxDepth);	// In case all the extensions are activated during the search.
		this.hT = historyTable;
		this.tT = transposTable;
		this.hashEntryGen = hashEntryGen;
		results = new Results();
	}
	/**
	 * Returns an observable container class for the results of the search.
	 * 
	 * @return
	 */
	public Results getResults() {
		return results;
	}
	/**
	 * Returns a queue of Move objects according to the best line of play extracted form the transposition table.
	 * 
	 * @param ply
	 * @return
	 */
	private Queue<Move> extractPv(int ply) {
		Move[] pVarr = new Move[ply];
		Queue<Move> pV = new Queue<>();
		TTEntry e;
		Move bestMove;
		int i, j;
		i = j = 0;
		while ((e = tT.lookUp(position.key)) != null && e.bestMove != 0 && e.type == NodeType.EXACT.ind && i < ply) {
			bestMove = Move.toMove(e.bestMove);
			position.makeMove(bestMove);
			pVarr[i++] = bestMove;
		}
		for (int k = 0; k < i; k++)
			position.unmakeMove();
		while (j < pVarr.length && pVarr[j] != null)
			pV.add(pVarr[j++]);
		return pV;
	}
	private long allocateSearchTime(Position pos, int phaseScore, long timeLeft, long oppTimeLeft) {
		// !FIXME
		return 3600*1000;
	}
	/**
	 * Starts searching the current position until the allocated search time has passed, or the thread is interrupted, or the maximum search
	 * depth has been reached.
	 */
	private void searchRoot() {
		int alpha, beta, score, failHigh, failLow;
		Stack<Future<Integer>> futures;
		compService = new ExecutorCompletionService<Integer>(threadPool);
		nodes = new AtomicLong(0);
		alpha = Termination.CHECK_MATE.score;
		beta = -alpha;
		failHigh = failLow = 0; // The number of consecutive fail highs/fail lows.
		// Iterative deepening.
		for (short i = 1; i <= maxDepth; i++) {
			futures = new Stack<>();
			for (int j = 0; j < numOfThreads; j++)
				futures.add(compService.submit(new SearchThread(position.deepCopy(), i, i*FULL_PLY, alpha, beta, true, 0)));
			try {
				score = compService.take().get();
				if (i == maxDepth) {
					for (Future<Integer> f : futures)
						f.cancel(true);
				}
			} catch (InterruptedException | ExecutionException e) {
				score = alpha;
				doStopSearch.set(true);
				e.printStackTrace();
			}
			if (doStopSearch.get() || Thread.currentThread().isInterrupted() || (!pondering && System.currentTimeMillis() >= deadLine)) {
				results.set(extractPv(i), i, (short)score, nodes.get(), System.currentTimeMillis() - (deadLine - searchTime), true);
				break;
			}
			// Aspiration windows with gradual widening.
			if (score <= alpha) {
				if (score <= L_CHECK_MATE_LIMIT) {
					alpha = Termination.CHECK_MATE.score;
					beta = -Termination.CHECK_MATE.score;
					failLow = 2;
					failHigh = 2;
				}
				else {
					alpha = failLow == 0 ? Math.max(score - 2*params.A_DELTA, Termination.CHECK_MATE.score) :
						failLow == 1 ? Math.max(score - 4*params.A_DELTA, Termination.CHECK_MATE.score) : Termination.CHECK_MATE.score;
					failLow++;
				}
				i--;
				continue;
			}
			if (score >= beta) {
				if (score >= W_CHECK_MATE_LIMIT) {
					alpha = Termination.CHECK_MATE.score;
					beta = -Termination.CHECK_MATE.score;
					failLow = 2;
					failHigh = 2;
				}
				else {
					beta = failHigh == 0 ? Math.min(score + 2*params.A_DELTA, -Termination.CHECK_MATE.score) :
						failHigh == 1 ? Math.min(score + 4*params.A_DELTA, -Termination.CHECK_MATE.score) : -Termination.CHECK_MATE.score;
					failHigh++;
				}
				i--;
				continue;
			}
			results.set(extractPv(i), i, (short)score, nodes.get(), System.currentTimeMillis() - (deadLine - searchTime), i == maxDepth);
			failHigh = failLow = 0;
			alpha = score >= W_CHECK_MATE_LIMIT ? alpha : Math.max(score - params.A_DELTA, Termination.CHECK_MATE.score);
			beta = score <= L_CHECK_MATE_LIMIT ? beta : Math.min(score + params.A_DELTA, -Termination.CHECK_MATE.score);
		}
	}
	@Override
	public void run() {
		threadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(numOfThreads);
		deadLine = System.currentTimeMillis() + searchTime;
		searchRoot();
		threadPool.shutdown();
	}
}
