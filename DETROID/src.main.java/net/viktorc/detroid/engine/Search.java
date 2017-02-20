package net.viktorc.detroid.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.viktorc.detroid.engine.KillerTable.KillerTableEntry;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.util.LossyHashTable;
import net.viktorc.detroid.util.QuickSort;

/**
 * A chess game tree search based on the PVS algorithm supported by a transposition table and an evaluation hash table within an iterative deepening 
 * framework with aspiration windows utilizing heuristics such as null move pruning; late move reductions; futility pruning; extended futility 
 * pruning; razoring; IID; quiescence search; and fractional depth extensions such as check extension, recapture extension, and one reply extension. 
 * For move ordering, it relies on a table for killer moves, a table for the relative history score of moves, and the MVVLVA and SEE heuristics.
 * 
 * @author Viktor
 *
 */
class Search implements Runnable {
	
	private final Params params;
	private final Position position;
	private final Evaluator eval;
	private final SearchInfo stats;
	// Killer heuristic table.
	private final KillerTable kT;
	// History heuristic table.
	private final RelativeHistoryTable hT;
	// Transposition table.
	private final LossyHashTable<TTEntry> tT;
	private final byte hashEntryGen;
	private final boolean useTt;
	// Whether heuristics such as forward pruning or those based on the null move observation such as stand-pat and NMP are applicable.
	private final boolean isEndgame;
	// Including extensions and quiescence search.
	private final int maxExpectedSearchDepth;
	private final int lCheckMateLimit;
	private final int wCheckMateLimit;
	private final int maxDepth;
	private final long maxNodes;
	private final boolean ponder;
	private final ArrayList<Move> allowedRootMoves;
	private final boolean areMovesRestricted;
	
	private Long startTime;
	private long nodes;
	private boolean doStopSearch;
	
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
	Search(Position position, SearchInfo stats, boolean ponder, int maxDepth, long maxNodes, Set<Move> moves,
			Evaluator eval, RelativeHistoryTable historyTable, final byte hashEntryGen,
			LossyHashTable<TTEntry> transposTable, Params params) {
		this.params = params;
		this.stats = stats;
		this.eval = eval;
		this.position = position;
		isEndgame = eval.phaseScore(position) >= this.params.gamePhaseEndgameLower;
		this.ponder = ponder;
		if (!ponder) {
			maxDepth = Math.max(0, maxDepth);
			this.maxDepth = Math.min(this.params.maxNominalSearchDepth, maxDepth);
			this.maxNodes = maxNodes;
		} else {
			this.maxDepth = this.params.maxNominalSearchDepth;
			this.maxNodes = Long.MAX_VALUE;
		}
		allowedRootMoves = moves != null ? new ArrayList<>(moves) : null;
		areMovesRestricted = moves != null && moves.size() > 0;
		doStopSearch = false;
 		/* In case all the extensions are activated during the search and the quiscence search probes 2 fold beyond
 		 * the main search depth. */
		maxExpectedSearchDepth = 2*2*this.maxDepth;
		lCheckMateLimit = Termination.CHECK_MATE.score + maxExpectedSearchDepth;
		wCheckMateLimit = -lCheckMateLimit;
		if (this.maxDepth > 0)
			kT = new KillerTable(this.maxDepth*2);
		else
			kT = null;
		this.hT = historyTable;
		this.tT = transposTable;
		this.hashEntryGen = hashEntryGen;
		useTt = tT != null;
	}
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		iterativeDeepening();
		startTime = null;
	}
	/**
	 * An iterative deepening framework with gradually widening aspiration windows.
	 */
	private void iterativeDeepening() {
		int alpha, beta, score, failHigh, failLow;
		nodes = 0;
		alpha = Termination.CHECK_MATE.score;
		beta = -alpha;
		// The number of consecutive fail-highs/fail-lows.
		failHigh = failLow = 0;
		// Iterative deepening.
		for (short i = (short) (maxDepth == 0 ? 0 : 1); i <= maxDepth; i++) {
			score = search(i, alpha, beta);
			if (doStopSearch || Thread.currentThread().isInterrupted())
				break;
			// Aspiration windows with gradual widening.
			// Disallowed in end games because it delays mate detection.
			if (!isEndgame) {
				if (score <= alpha) {
					if (score <= lCheckMateLimit) {
						alpha = Termination.CHECK_MATE.score;
						beta = -Termination.CHECK_MATE.score;
						failLow = 2;
						failHigh = 2;
					} else {
						alpha = failLow == 0 ? Math.max(score - 2*params.aspirationDelta, Termination.CHECK_MATE.score) :
								failLow == 1 ? Math.max(score - 4*params.aspirationDelta, Termination.CHECK_MATE.score) :
								Termination.CHECK_MATE.score;
						failLow++;
					}
					i--;
				} else if (score >= beta) {
					if (score >= wCheckMateLimit) {
						alpha = Termination.CHECK_MATE.score;
						beta = -Termination.CHECK_MATE.score;
						failLow = 2;
						failHigh = 2;
					} else {
						beta = failHigh == 0 ? Math.min(score + 2*params.aspirationDelta, -Termination.CHECK_MATE.score) :
								failHigh == 1 ? Math.min(score + 4*params.aspirationDelta, -Termination.CHECK_MATE.score) :
								-Termination.CHECK_MATE.score;
						failHigh++;
					}
					i--;
				} else {
					failHigh = failLow = 0;
					alpha = score >= wCheckMateLimit ? alpha : Math.max(score - params.aspirationDelta, Termination.CHECK_MATE.score);
					beta = score <= lCheckMateLimit ? beta : Math.min(score + params.aspirationDelta, -Termination.CHECK_MATE.score);
				}
			}
		}
	}
	/**
	 * Searches a root position and returns its score.
	 * 
	 * @param ply
	 * @param alpha
	 * @param beta
	 * @return
	 */
	private int search(short ply, int alpha, int beta) {
		int depth = ply*params.fullPly;
		final int depthLimit = depth + params.fullPly;
		final int origAlpha = alpha;
		int score, bestScore, extension, numOfMoves;
		Move hashMove, bestMove, move, lastMove;
		boolean lastMoveIsMaterial, statsUpdated;
		List<Move> tacticalMoves, quietMoves, allMoves;
		Move[] tacticalMovesArr, quietMovesArr, allMovesArr;
		TTEntry e;
		statsUpdated = false;
		bestScore = Integer.MIN_VALUE;
		bestMove = hashMove = null;
		// If ply equals 0, perform only quiescence search.
		if (ply == 0) {
			bestScore = quiescence(0, alpha, beta);
			updateInfo(null, 0, ply, alpha, beta, bestScore, true);
			return bestScore;
		}
		tacticalMoves = quietMoves = null;
		// Check for the 3-fold repetition rule.
		if (position.getNumberOfRepetitions(0) >= 2)
			return Termination.DRAW_CLAIMED.score;
		// Generate moves.
		tacticalMoves = position.getTacticalMoves();
		quietMoves = position.getQuietMoves();
		numOfMoves = tacticalMoves.size() + quietMoves.size();
		// Mate check.
		if (numOfMoves == 0)
			return position.isInCheck ? Termination.CHECK_MATE.score : Termination.STALE_MATE.score;
		// One reply extension.
		else if (numOfMoves == 1)
			depth = Math.min(depthLimit, depth + params.singleReplyExt);
		// Check for the 50 move rule.
		if (position.fiftyMoveRuleClock >= 100)
			return Termination.DRAW_CLAIMED.score;
		// Check extension.
		depth = position.isInCheck ? Math.min(depthLimit, depth + params.checkExt) : depth;
		// Hash look-up.
		e = useTt ? tT.get(position.key) : null;
		if (e != null) {
			e.generation = hashEntryGen;
			if (e.bestMove != 0)
				hashMove = Move.toMove(e.bestMove);
		}
		// Sort moves.
		tacticalMovesArr = orderMaterialMovesSEE(position, tacticalMoves);
		quietMovesArr = orderNonMaterialMoves(quietMoves);
		allMoves = new ArrayList<>();
		if (hashMove != null && (quietMoves.contains(hashMove) || tacticalMoves.contains(hashMove)))
			allMoves.add(hashMove);
		else
			hashMove = null;
		for (int i = 0; i < tacticalMovesArr.length; i++) {
			if (!allMoves.contains(tacticalMovesArr[i]))
				allMoves.add(tacticalMovesArr[i]);
		}
		for (int i = 0; i < quietMovesArr.length; i++) {
			if (!allMoves.contains(quietMovesArr[i]))
				allMoves.add(quietMovesArr[i]);
		}
		if (areMovesRestricted)
			allMoves.retainAll(allowedRootMoves);
		allMovesArr = new Move[allMoves.size()];
		allMovesArr = allMoves.toArray(allMovesArr);
		lastMove = position.getLastMove();
		lastMoveIsMaterial = lastMove != null && lastMove.isMaterial();
		// Iterate over moves.
		for (int i = 0; i < allMovesArr.length; i++) {
			move = allMovesArr[i];
			// Recapture extension.
			extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
				params.recapExt : 0;
			position.makeMove(move);
			// Full window search for the first move...
			if (i == 0)
				score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, 1, -beta, -alpha, true);
			// PVS for the rest.
			else {
				score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, 1, -alpha - 1, -alpha, true);
				if (score > alpha && score < beta)
					score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, 1, -beta, -alpha, true);
			}
			position.unmakeMove();
			// Score check.
			if (score > bestScore) {
				bestMove = move;
				bestScore = score;
				if (score > alpha) {
					alpha = score;
					if (score >= beta)
						break;
					// Insert into TT and update stats if applicable.
					if (!useTt || insertNodeIntoTt(position.key, origAlpha, beta, move, score, (short) 0, (short) (depth/params.fullPly))) {
						statsUpdated = true;
						updateInfo(move, i + 1, ply, origAlpha, beta, score, doStopSearch || Thread.currentThread().isInterrupted());
					}
				}
			}
			if (doStopSearch || Thread.currentThread().isInterrupted())
				break;
		}
		// If the search stats have not been updated yet, probably due to failing low or high, do it now.
		if (!statsUpdated) {
			if (useTt)
				insertNodeIntoTt(position.key, origAlpha, beta, bestMove, bestScore, (short) 0, (short) (depth/params.fullPly));
			updateInfo(null, 0, ply, origAlpha, beta, hashMove == null ? bestScore : e.score, doStopSearch ||
					Thread.currentThread().isInterrupted());
		}
		return bestScore;
	}
	/**
	 * A principal variation search algorithm utilizing a transposition table. It returns only the score for the searched position, but the
	 * principal variation can be extracted from the transposition table after a search has been run.
	 * 
	 * @param depth
	 * @param distFromRoot
	 * @param alpha
	 * @param beta
	 * @param nullMoveAllowed
	 * @return
	 */
	private int pVsearch(int depth, int distFromRoot, int alpha, int beta, boolean nullMoveAllowed) {
		final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
		final int origAlpha = alpha;
		final int depthLimit = distFromRoot >= maxDepth ? depth : depth + params.fullPly;
		final boolean isInCheck = position.isInCheck;
		final boolean isPvNode = beta > origAlpha + 1;
		int bestScore, score, evalScore;
		int extension;
		int matMoveBreakInd;
		int searchedMoves;
		int kMove;
		int razRed;
		Move hashMove, bestMove, killerMove1, killerMove2, move, lastMove;
		boolean isThereHashMove, isThereKM1, isThereKM2, lastMoveIsTactical, isDangerous, isReducible;
		List<Move> tacticalMoves, quietMoves;
		Move[] tacticalMovesArr, quietMovesArr;
		TTEntry e;
		KillerTableEntry kE;
		bestScore = mateScore;
		bestMove = null;
		searchedMoves = 0;
		hashMove = killerMove1 = killerMove2 = null;
		isThereHashMove = isThereKM1 = isThereKM2 = false;
		tacticalMoves = quietMoves = null;
		nodes++;
		if ((!ponder && nodes >= maxNodes) || Thread.currentThread().isInterrupted())
			doStopSearch = true;
		Search: {
			// Check for the repetition rule; return a draw score if it applies.
			if (position.getNumberOfRepetitions(distFromRoot) >= 2)
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
			// Check extension.
			depth = isInCheck ? Math.min(depthLimit, depth + params.checkExt) : depth;
			// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
			e = useTt ? tT.get(position.key) : null;
			if (e != null) {
				e.generation = hashEntryGen;
				// If the hashed entry's depth is greater than or equal to the current search depth, check if the stored score is usable.
				if (e.depth >= depth/params.fullPly) {
					// Mate score adjustment to root distance.
					if (e.score <= lCheckMateLimit)
						score = e.score + distFromRoot;
					else if (e.score >= wCheckMateLimit)
						score = e.score - distFromRoot;
					else
						score = e.score;
					/* If the score was exact, or it was the score of an all node and is smaller than or equal to alpha, or it is that of a cut
					 * node and is greater than or equal to beta, return the score. */
					if (!isPvNode && (e.type == NodeType.EXACT.ind ||
							/* To make sure that a score that might not have been the exact score for the subtree below the node regardless of the
							 * alpha-beta boundaries is not treated as an exact score in the current context, we can not allow it to fall between
							 * the current alpha and beta. If it was a fail high node, the score is a lower boundary of the exact score of the
							 * node due to there possibly being siblings to the right of the child node [that raised alpha higher than beta and
							 * caused a cut-off] that could raise alpha even higher. If it was a fail low node, the score is a higher boundary for
							 * the exact score of the node, because all children of a fail low node are fail high nodes (-score <= alpha ->
							 * score >= -alpha [-alpha = beta in the child node]). To keep the interval of values the exact score could take on
							 * out of (alpha, beta), the score has to be lower than or equal to alpha if it is a higher boundary, i.e. fail low
							 * score, and it has to be greater than or equal to beta if it is a lower boundary i.e. fail high score.
							 */
							(e.type == NodeType.FAIL_HIGH.ind && score >= beta) || (e.type == NodeType.FAIL_LOW.ind && score <= alpha)))
						return score;
				}
				// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
				if (e.bestMove != 0) {
					hashMove = Move.toMove(e.bestMove);
					isThereHashMove = position.isLegalSoft(hashMove);
				}
			}
			// Return the score from the quiescence search in case a leaf node has been reached.
			if (depth/params.fullPly <= 0) {
				nodes--;
				score = quiescence(distFromRoot, alpha, beta);
				if (score > bestScore) {
					bestMove = null;
					bestScore = score;
					if (score > alpha)
						alpha = score;
				}
				break Search;
			}
//			// If there is no hash entry in a PV node that is to be searched deep, try IID.
//			if (isPvNode && !isThereHashMove && depth/params.fullPly >= params.iidMinActivationDepth) {
//				pVsearch(depth*params.iidRelDepthHth/100, distFromRoot, alpha, beta, true);
//				e = tT.get(position.key);
//				if (e != null && e.bestMove != 0) {
//					hashMove = Move.toMove(e.bestMove);
//					isThereHashMove = position.isLegalSoft(hashMove);
//				}
//			}
			lastMove = position.getLastMove();
			lastMoveIsTactical = lastMove != null && lastMove.isMaterial();
			// If there is a hash move, search that first.
			if (isThereHashMove) {
				// Recapture extension (includes capturing newly promoted pieces).
				extension = lastMoveIsTactical && hashMove.capturedPiece != Piece.NULL.ind && hashMove.to == lastMove.to ? params.recapExt : 0;
				position.makeMove(hashMove);
				score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				position.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = hashMove;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta) {
							if (!hashMove.isMaterial()) {
								// Add to killer moves.
								kT.add(distFromRoot, hashMove);
								// Record success in the relative history table.
								hT.recordSuccessfulMove(hashMove);
							}
							break Search;
						}
					}
				}
				if (!hashMove.isMaterial())
					// Record failure in the relative history table.
					hT.recordUnsuccessfulMove(hashMove);
			}
			// Generate material moves.
			tacticalMoves = position.getTacticalMoves();
			// If there was no hash move or material moves, perform a mate check.
			if (!isThereHashMove && tacticalMoves.size() == 0) {
				quietMoves = position.getQuietMoves();
				if (quietMoves.size() == 0) {
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
			if (position.fiftyMoveRuleClock >= 100)
				return Termination.DRAW_CLAIMED.score;
			isDangerous = isPvNode || isInCheck || isEndgame || Math.abs(beta) >= wCheckMateLimit ||
					!position.areTherePiecesOtherThanKingsAndPawns();
			// If it is not a terminal or PV node, try null move pruning if it is allowed and the side to move is not in check.
			if (nullMoveAllowed && !isDangerous && depth/params.fullPly > params.nullMoveReduction) {
				position.makeNullMove();
				// Do not allow consecutive null moves.
				score = -pVsearch(depth - (1 + params.nullMoveReduction)*params.fullPly, distFromRoot + 1, -beta, -beta + 1, false);
				position.unmakeMove();
				if (score >= beta)
					return score;
			}
			// Sort the material moves.
			tacticalMovesArr = orderMaterialMovesMVVLVA(tacticalMoves);
			matMoveBreakInd = 0;
			// Search winning and equal captures.
			for (int i = 0; i < tacticalMovesArr.length; i++) {
				move = tacticalMovesArr[i];
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
				// Recapture extension (includes capturing newly promoted pieces).
				extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
						params.recapExt : 0;
				position.makeMove(move);
				// PVS.
				if (i == 0)
					score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				else {
					score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
					if (score > alpha && score < beta)
						score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				}
				position.unmakeMove();
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
				if (doStopSearch)
					break Search;
			}
			// If there are no more winning or equal captures, check and search the killer moves if legal from this position.
			kE = kT.retrieve(distFromRoot);
			if ((kMove = kE.getMove1()) != 0) {	// Killer move no. 1.
				killerMove1 = Move.toMove(kMove);
				if (position.isLegalSoft(killerMove1) && (!isThereHashMove || !killerMove1.equals(hashMove))) {
					isThereKM1 = true;
					position.makeMove(killerMove1);
					if (!isThereHashMove && matMoveBreakInd == 0)
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					else {
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
						if (score > alpha && score < beta)
							score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					}
					position.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = killerMove1;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								// Record success in the relative history table.
								hT.recordSuccessfulMove(killerMove1);
								break Search;
							}
						}
					}
					// Record failure in the relative history table.
					hT.recordUnsuccessfulMove(killerMove1);
					if (doStopSearch)
						break Search;
				}
			}
			if ((kMove = kE.getMove2()) != 0) {
				killerMove2 = Move.toMove(kMove);
				if (position.isLegalSoft(killerMove2) && (!isThereHashMove || !killerMove2.equals(hashMove))) {
					isThereKM2 = true;
					position.makeMove(killerMove2);
					if (!isThereHashMove && !isThereKM1 && matMoveBreakInd == 0)
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					else {
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
						if (score > alpha && score < beta)
							score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					}
					position.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = killerMove2;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								// Make it killer move no. 1.
								kT.add(distFromRoot, killerMove2);
								// Record success in the relative history table.
								hT.recordSuccessfulMove(killerMove2);
								break Search;
							}
						}
					}
					// Record failure in the relative history table.
					hT.recordUnsuccessfulMove(killerMove2);
					if (doStopSearch)
						break Search;
				}
			}	// Killer move check ending.
			// Search losing captures if there are any.
			for (int i = matMoveBreakInd; i < tacticalMovesArr.length; i++) {
				move = tacticalMovesArr[i];
				// If this move was the hash move, skip it.
				if (isThereHashMove && move.equals(hashMove)) {
					isThereHashMove = false;
					continue;
				}
				// Recapture extension.
				extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? params.recapExt : 0;
				position.makeMove(move);
				// PVS.
				if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2)
					score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				else {
					score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
					if (score > alpha && score < beta)
						score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				}
				position.unmakeMove();
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
				if (doStopSearch)
					break Search;
			}
			// Generate the non-material legal moves if they are not generated yet.
			if (quietMoves == null)
				quietMoves = position.getQuietMoves();
			// One reply extension.
			if (tacticalMoves.size() == 0 && quietMoves.size() == 1)
				depth = Math.min(depthLimit, depth + params.singleReplyExt);
			evalScore = Integer.MIN_VALUE;
			// Order and search the non-material moves.
			quietMovesArr = orderNonMaterialMoves(quietMoves);
			for (int i = 0; i < quietMovesArr.length; i++) {
				move = quietMovesArr[i];
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
				isReducible = !isDangerous && Math.abs(alpha) < wCheckMateLimit && !position.givesCheck(move);
				razRed = 0;
				// Futility pruning, extended futility pruning, and razoring.
				if (isReducible && depth/params.fullPly <= 3) {
					if (evalScore == Integer.MIN_VALUE)
						evalScore = eval.score(position, hashEntryGen, alpha, beta);
					switch (depth/params.fullPly) {
						case 1: {
							// Frontier futility pruning.
							if (evalScore + params.futilityMargin1 <= alpha) {
								// Record failure in the relative history table.
								hT.recordUnsuccessfulMove(move);
								continue;
							}
						} break;
						case 2: {
							// Extended futility pruning.
							if (evalScore + params.futilityMargin2 <= alpha) {
								// Record failure in the relative history table.
								hT.recordUnsuccessfulMove(move);
								continue;
							}
							/* Razoring (in most cases down to the quiescence search) if alpha doesn't exceed the static evaluation score  
							 * by a margin great enough to completely prune the branch. */
							if (evalScore + params.razoringMargin1 <= alpha)
								razRed = 1;
						} break;
						case 3: {
							// Deep futility pruning.
							if (evalScore + params.futilityMargin3 <= alpha) {
								// Record failure in the relative history table.
								hT.recordUnsuccessfulMove(move);
								continue;
							}
							// Deep razoring.
							if (evalScore + params.razoringMargin2 <= alpha)
								razRed = 1;
						}
					}
				}
				position.makeMove(move);
				// Try late move reduction if not within the PV.
				if (isReducible && depth/params.fullPly >= params.lateMoveReductionMinActivationDepth &&
						searchedMoves > params.minMovesSearchedForLmr) {
					score = -pVsearch(depth - params.lateMoveReduction*params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
					// If it does not fail low, research with full window.
					if (score > alpha)
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				}
				// Else simple PVS with razoring.
				else if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2 && tacticalMovesArr.length == 0)
					score = -pVsearch(depth - (razRed + 1)*params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				else {
					score = -pVsearch(depth - (razRed + 1)*params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
					if (score > alpha && score < beta)
						score = -pVsearch(depth - (razRed + 1)*params.fullPly, distFromRoot + 1, -beta, -alpha, true);
				}
				position.unmakeMove();
				searchedMoves++;
				if (score > bestScore) {
					bestMove = move;
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						// Cutoff from a non-material move.
						if (alpha >= beta) {
							// Add to killer moves.
							kT.add(distFromRoot, move);
							// Record success in the relative history table.
							hT.recordSuccessfulMove(move);
							break Search;
						}
					}
				}
				// Record failure in the relative history table.
				hT.recordUnsuccessfulMove(move);
				if (doStopSearch)
					break Search;
			}
		}
		// Add new entry to the transposition table.
		if (useTt)
			insertNodeIntoTt(position.key, origAlpha, beta, bestMove, bestScore, (short) distFromRoot, (short) (depth/params.fullPly));
		// Return the unadjusted best score.
		return bestScore;
	}
	/**
	 * A search algorithm for diminishing the horizon effect once the main search algorithm has reached a leaf node. It keeps searching until
	 * the side to move is not in check and does not have any legal winning captures according to SEE.
	 * 
	 * @param distFromRoot
	 * @param alpha
	 * @param beta
	 * @return
	 */
	private int quiescence(int distFromRoot, int alpha, int beta) {
		final boolean isInCheck = position.isInCheck;
		List<Move> moveList;
		Move[] moves;
		int bestScore, searchScore;
		nodes++;
		if ((!ponder && nodes >= maxNodes) || Thread.currentThread().isInterrupted())
			doStopSearch = true;
		// Limit the maximum depth of the quiescence search.
		if (distFromRoot > maxExpectedSearchDepth)
			return eval.score(position, hashEntryGen, alpha, beta);
		// Fifty-move rule and repetition rule check.
		if (position.fiftyMoveRuleClock >= 100 || position.getNumberOfRepetitions(distFromRoot) >= 2)
			return Termination.DRAW_CLAIMED.score;
		// Evaluate the position statically.
		bestScore = eval.score(position, hashEntryGen, alpha, beta);
		// Fail soft.
		if (bestScore > alpha) {
			alpha = bestScore;
			if (bestScore >= beta)
				return bestScore;
		}
		// Generate all the material moves or all moves if in check.
		moveList = position.getTacticalMoves();
		moves = orderMaterialMovesSEE(position, moveList);
		for (Move move : moves) {
			// If the SEE value is below 0 or the delta pruning limit, break the search because the rest of the moves are even worse.
			if (move.value < 0 || (!isEndgame && !isInCheck && move.value < alpha - params.quiescenceDelta))
				break;
			position.makeMove(move);
			searchScore = -quiescence(distFromRoot + 1, -beta, -alpha);
			position.unmakeMove();
			if (searchScore > bestScore) {
				bestScore = searchScore;
				if (bestScore > alpha) {
					alpha = bestScore;
					if (alpha >= beta)
						break;
				}
			}
			if (doStopSearch)
				break;
		}
		return bestScore;
	}
	/**
	 * Returns a queue of Move objects according to the best line of play extracted form the transposition table.
	 * 
	 * @param ply
	 * @return
	 */
	private List<Move> extractPv(int ply) {
		Move[] pVarr = new Move[ply];
		List<Move> pV = new ArrayList<>();
		TTEntry e;
		Move bestMove;
		int i, j;
		i = j = 0;
		while ((e = tT.get(position.key)) != null && e.bestMove != 0 && i < ply) {
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
	/**
	 * Orders material moves and checks, the former of which according to the SEE swap algorithm.
	 * 
	 * @param pos
	 * @param moves
	 * @return
	 */
	private Move[] orderMaterialMovesSEE(Position pos, List<Move> moves) {
		Move[] arr = new Move[moves.size()];
		int i = 0;
		for (Move move : moves) {
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
		Move[] arr = new Move[moves.size()];
		int i = 0;
		for (Move move : moves) {
			if (move.type >= MoveType.PROMOTION_TO_QUEEN.ind) { // If promotion.
				if (move.type == MoveType.PROMOTION_TO_QUEEN.ind)
					move.value = params.queenValue;
				else if (move.type == MoveType.PROMOTION_TO_ROOK.ind)
					move.value = params.rookValue;
				else if (move.type == MoveType.PROMOTION_TO_BISHOP.ind)
					move.value = params.bishopValue;
				else // PROMOTION_TO_KNIGHT
					move.value = params.knightValue;
				move.value -= params.pawnValue;
				if (move.capturedPiece != Piece.NULL.ind)
					move.value += eval.materialValueByPieceInd(move.capturedPiece);
			} else
				move.value = (short) (eval.materialValueByPieceInd(move.capturedPiece) - eval.materialValueByPieceInd(move.movedPiece));
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
		Move[] arr = new Move[moves.size()];
		int i = 0;
		for (Move move : moves) {
			move.value = hT.score(move);
			arr[i++] = move;
		}
		return QuickSort.sort(arr);
	}
	/**
	 * Updates the observable search info according to the specified parameters.
	 * 
	 * @param ply
	 * @param alpha
	 * @param beta
	 * @param score
	 * @param isCancelled
	 */
	private void updateInfo(Move currentMove, int moveNumber, short ply, int alpha, int beta, int score, boolean isCancelled) {
		int resultScore;
		ScoreType scoreType;
		// Determine score type and value in case it's a mate score.
		if (score >= beta) {
			scoreType = ScoreType.UPPER_BOUND;
			resultScore = score;
		} else if (score <= alpha) {
			scoreType = ScoreType.LOWER_BOUND;
			resultScore = score;
		} else {
			if (score <= lCheckMateLimit) {
				resultScore = (Termination.CHECK_MATE.score - score)/2;
				scoreType = ScoreType.MATE;
			} else if (score >= wCheckMateLimit) {
				resultScore = (-Termination.CHECK_MATE.score - score)/2;
				scoreType = ScoreType.MATE;
			} else {
				resultScore = score;
				scoreType = ScoreType.EXACT;
			}
		}
		List<Move> pV = useTt ? extractPv(ply) : new ArrayList<>(Arrays.asList(currentMove));
		// Update stats.
		stats.set(pV, currentMove, moveNumber, ply, (short) resultScore, scoreType,
				nodes, System.currentTimeMillis() - startTime, isCancelled);
	}
	/**
	 * Inserts a node into the transposition table according to the specified parameters.
	 * 
	 * @param key
	 * @param alpha
	 * @param beta
	 * @param bestMove
	 * @param bestScore
	 * @param distFromRoot
	 * @param depth
	 * @return
	 */
	private boolean insertNodeIntoTt(long key, int alpha, int beta, Move bestMove, int bestScore, short distFromRoot, short depth) {
		int score;
		int bestMoveInt;
		byte type;
		// Adjustment of the best score for TT insertion according to the distance from the mate position in case it's a check mate score.
		if (bestScore <= lCheckMateLimit)
			score = bestScore - distFromRoot;
		else if (bestScore >= wCheckMateLimit)
			score = bestScore + distFromRoot;
		else
			score = bestScore;
		bestMoveInt = bestMove == null ? 0 : bestMove.toInt();
		// Determine node type.
		if (bestScore <= alpha)
			type = NodeType.FAIL_LOW.ind;
		else if (bestScore >= beta)
			type = NodeType.FAIL_HIGH.ind;
		else
			type = NodeType.EXACT.ind;
		//	Add new entry to the transposition table.
		return tT.put(new TTEntry(key, depth, type, (short) score, bestMoveInt, hashEntryGen));
	}
	
}
