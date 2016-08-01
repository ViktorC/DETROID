package engine;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import engine.ETEntry;
import engine.Evaluator;
import engine.KillerTable;
import engine.NodeType;
import engine.PTEntry;
import engine.Parameters;
import engine.RelativeHistoryTable;
import engine.Termination;
import engine.KillerTable.KillerTableEntry;
import uci.ScoreType;
import util.*;

/**
 * A selectivity based search engine that traverses the game tree from a given position through legal steps until a given nominal depth. It
 * uses principal variation search with a transposition table and MVV-LVA and history heuristics based move ordering within an iterative
 * deepening framework.
 * 
 * @author Viktor
 *
 */
class Search extends Thread {
	
	public final static int MAX_NOMINAL_SEARCH_DEPTH = 64;
	// For fractional ply extensions.
	private final static int FULL_PLY = 8;
	// Including extensions and quiescence search.
	private final int MAX_EXPECTED_TOTAL_SEARCH_DEPTH;
	private final int L_CHECK_MATE_LIMIT;
	private final int W_CHECK_MATE_LIMIT;
	
	private Parameters params;
	private Position position;
	private Long startTime;
	// Whether heuristics based on the null move observation such as stand-pat and NMP are applicable.
	private boolean nullMoveObservHolds;	
	private SearchInformation stats;
	private KillerTable kT;				// Killer heuristic table.
	private RelativeHistoryTable hT;	// History heuristic table.
	private LossyHashTable<TTEntry> tT;		// Transposition table.
	private byte hashEntryGen;			// Entry generation.
	private Evaluator eval;
	private boolean ponder;
	private int maxDepth;
	private long maxNodes;
	private List<Move> allowedRootMoves;	// The only moves to search at the root.
	private boolean areMovesRestricted;
	private AtomicLong nodes;				// Number of searched positions.
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
	public Search(Position position, SearchInformation stats, boolean ponder, int maxDepth, long maxNodes, Set<Move> moves,
			RelativeHistoryTable historyTable, final byte hashEntryGen, LossyHashTable<TTEntry> transposTable,
			LossyHashTable<ETEntry> evalTable, LossyHashTable<PTEntry> pawnTable, Parameters params) {
		this.params = params;
		MAX_EXPECTED_TOTAL_SEARCH_DEPTH =
				8*(params.CHECK_EXT + params.RECAP_EXT + params.SINGLE_REPLY_EXT + params.MATE_THREAT_EXT)*MAX_NOMINAL_SEARCH_DEPTH;
		L_CHECK_MATE_LIMIT = Termination.CHECK_MATE.score + MAX_EXPECTED_TOTAL_SEARCH_DEPTH;
		W_CHECK_MATE_LIMIT = -L_CHECK_MATE_LIMIT;
		this.stats = stats;
		eval = new Evaluator(params, evalTable, pawnTable, hashEntryGen);
		this.position = position.deepCopy();
		nullMoveObservHolds = eval.phaseScore(position) < params.GAME_PHASE_ENDGAME_LOWER;
		this.ponder = ponder;
		if (!ponder) {
			this.maxDepth = Math.min(MAX_NOMINAL_SEARCH_DEPTH, maxDepth);
			this.maxNodes = maxNodes;
		}
		else
			this.maxDepth = MAX_NOMINAL_SEARCH_DEPTH;
		if (moves != null) {
			allowedRootMoves = new Stack<>();
			for (Move m : moves)
				allowedRootMoves.add(m);
		}
		areMovesRestricted = allowedRootMoves != null;
		doStopSearch = new AtomicBoolean(false);
		kT = new KillerTable(3*this.maxDepth);	// In case all the extensions are activated during the search.
		this.hT = historyTable;
		this.tT = transposTable;
		this.hashEntryGen = hashEntryGen;
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
		while ((e = tT.lookUp(position.hashKey())) != null && e.bestMove != 0 && i < ply) {
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
		Move[] arr = new Move[moves.size()];
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
		Move[] arr = new Move[moves.size()];
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
	 * Updates the observable search info according to the specified parameters.
	 * 
	 * @param ply
	 * @param alpha
	 * @param beta
	 * @param score
	 * @param isFinal
	 */
	private void updateInfo(Move currentMove, int moveNumber, short ply, int alpha, int beta, int score) {
		int resultScore;
		ScoreType scoreType;
		// Determine score type and value in case it's a mate score.
		if (score >= beta) {
			scoreType = ScoreType.UPPER_BOUND;
			resultScore = score;
		}
		else if (score <= alpha) {
			scoreType = ScoreType.LOWER_BOUND;
			resultScore = score;
		}
		else {
			if (score <= L_CHECK_MATE_LIMIT) {
				resultScore = Termination.CHECK_MATE.score - score;
				scoreType = ScoreType.MATE;
			}
			else if (score >= W_CHECK_MATE_LIMIT) {
				resultScore = -Termination.CHECK_MATE.score - score;
				scoreType = ScoreType.MATE;
			}
			else {
				resultScore = score;
				scoreType = ScoreType.EXACT;
			}
		}
		// Update stats.
		stats.set(extractPv(ply), currentMove, moveNumber, ply, (short)resultScore, scoreType, nodes.get(),
				  System.currentTimeMillis() - startTime);
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
		if (bestScore <= L_CHECK_MATE_LIMIT)
			score = bestScore - distFromRoot;
		else if (bestScore >= W_CHECK_MATE_LIMIT)
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
		return tT.insert(new TTEntry(key, depth, type, (short) score, bestMoveInt, hashEntryGen));
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
		final int origAlpha = alpha;
		int depth, score, bestScore, tacticalMovesBreakInd, extension, numOfMoves;
		Move hashMove, bestMove, killerMove1, killerMove2, move, lastMove;
		boolean lastMoveIsMaterial, statsUpdated;
		List<Move> tacticalMoves, quietMoves;
		Set<Move> allMoves;
		Move[] tacticalMovesArr, quietMovesArr, allMovesArr;
		TTEntry e;
		KillerTableEntry kE;
		statsUpdated = false;
		bestScore = Integer.MIN_VALUE;
		bestMove = null;
		hashMove = killerMove1 = killerMove2 = null;
		tacticalMoves = quietMoves = null;
		depth = ply*FULL_PLY;
		// Check for the 3-fold repetition rule.
		if (position.repetitions >= 3)
			return Termination.DRAW_CLAIMED.score;
		// Generate moves.
		tacticalMoves = position.getTacticalMoves();
		quietMoves = position.getQuietMoves();
		numOfMoves = tacticalMoves.size() + quietMoves.size();
		// Mate check.
		if (numOfMoves == 0)
			return position.isInCheck ? Termination.CHECK_MATE.score : Termination.STALE_MATE.score;
		// Check for the 50 move rule.
		if (position.fiftyMoveRuleClock >= 100)
			return Termination.DRAW_CLAIMED.score;
		// One reply extension.
		else if (numOfMoves == 1)
			depth += params.SINGLE_REPLY_EXT;
		// Check extension.
		depth = position.isInCheck ? depth + params.CHECK_EXT : depth;
		// Hash look-up.
		e = tT.lookUp(position.key);
		if (e != null) {
			e.generation = hashEntryGen;
			if (e.bestMove != 0) {
				hashMove = Move.toMove(e.bestMove);
				hashMove = position.isLegalSoft(hashMove) ? hashMove : null;
			}
		}
		// Killer moves look-up.
		kE = kT.retrieve(ply);
		if (kE.getMove1() != 0) {
			killerMove1 = Move.toMove(kE.getMove1());
			killerMove1 = position.isLegalSoft(killerMove1) ? killerMove1 : null;
		}
		if (kE.getMove2() != 0) {
			killerMove2 = Move.toMove(kE.getMove2());
			killerMove2 = position.isLegalSoft(killerMove2) ? killerMove2 : null;
		}
		// Sort moves.
		tacticalMovesArr = orderMaterialMovesMVVLVA(tacticalMoves);
		quietMovesArr = orderNonMaterialMoves(quietMoves);
		allMoves = new HashSet<>();
		if (hashMove != null)
			allMoves.add(hashMove);
		for (tacticalMovesBreakInd = 0; tacticalMovesBreakInd < tacticalMovesArr.length; tacticalMovesBreakInd++) {
			if (tacticalMovesArr[tacticalMovesBreakInd].value >= 0)
				allMoves.add(tacticalMovesArr[tacticalMovesBreakInd]);
			else {
				tacticalMovesBreakInd++;
				break;
			}
		}
		if (killerMove1 != null)
			allMoves.add(killerMove1);
		if (killerMove2 != null)
			allMoves.add(killerMove2);
		for (; tacticalMovesBreakInd < tacticalMovesArr.length; tacticalMovesBreakInd++)
			allMoves.add(tacticalMovesArr[tacticalMovesBreakInd]);
		for (int i = 0; i < quietMovesArr.length; i++)
			allMoves.add(quietMovesArr[i]);
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
				params.RECAP_EXT : 0;
			position.makeMove(move);
			// Full window search for the first move...
			if (i == 0)
				score = -new SearchThread(position, 1, depth - FULL_PLY + extension, -beta, -alpha, true, 0).call();
			// PVS for the rest.
			else {
				score = -new SearchThread(position, 1, depth - FULL_PLY + extension, -alpha - 1, -alpha, true, 0).call();
				if (score > alpha && score < beta)
					score = -new SearchThread(position, 1, depth - FULL_PLY + extension, -beta, -alpha, true, 0).call();
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
					if (insertNodeIntoTt(position.hashKey(), origAlpha, beta, move, score, (short) 0, (short) (depth/FULL_PLY)) ||
							(hashMove != null && move.equals(hashMove))) {
						statsUpdated = true;
						updateInfo(move, i + 1, ply, origAlpha, beta, score);
					}
				}
			}
			if (isInterrupted() || doStopSearch.get())
				break;
		}
		// If the seach stats have not been updated yet, try inserting the best move into the TT and update the stats
		if (!statsUpdated) {
			insertNodeIntoTt(position.hashKey(), origAlpha, beta, bestMove, bestScore, (short) 0, (short) (depth/FULL_PLY));
			updateInfo(null, 0, ply, origAlpha, beta, hashMove == null ? bestScore : e.score);
		}
		return bestScore;
	}
	/**
	 * An iterative deepening framework with gradually widening aspiration windows.
	 */
	private void iterativeDeepening() {
		int alpha, beta, score, failHigh, failLow;
		nodes = new AtomicLong(0);
		alpha = Termination.CHECK_MATE.score;
		beta = -alpha;
		failHigh = failLow = 0; // The number of consecutive fail highs/fail lows.
		// Iterative deepening.
		for (short i = 1; i <= maxDepth; i++) {
			score = search(i, alpha, beta);
			if (doStopSearch.get() || isInterrupted())
				break;
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
			}
			else if (score >= beta) {
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
			}
			else {
				failHigh = failLow = 0;
				alpha = score >= W_CHECK_MATE_LIMIT ? alpha : Math.max(score - params.A_DELTA, Termination.CHECK_MATE.score);
				beta = score <= L_CHECK_MATE_LIMIT ? beta : Math.min(score + params.A_DELTA, -Termination.CHECK_MATE.score);
			}
		}
	}
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		iterativeDeepening();
		startTime = null;
	}
	
	/**
	 * A thread-task for parallel search.
	 * 
	 * @author Viktor
	 *
	 */
	private class SearchThread implements Callable<Integer> {
		
		private static final long serialVersionUID = 6351992983920986212L;
		
		private Position pos;
		private int distFromRoot;
		private int depth;
		private int alpha;
		private int beta;
		private boolean nullMoveAllowed;
		private int qDepth;
		private boolean boundsChanged;
		private boolean doStopThread;
		
		SearchThread(Position pos, int distFromRoot, int depth, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
			this.pos = pos;
			this.distFromRoot = distFromRoot;
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
			return pVsearch(depth, distFromRoot, alpha, beta, nullMoveAllowed, qDepth);
		}
		/**
		 * Updates the beta boundary of the search thread in case an improvement to alpha has been found in an ancestor node.
		 * 
		 * @param parentAlpha
		 */
		private void updateBeta(int parentAlpha) {
			this.beta = -parentAlpha;
			boundsChanged = true;
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
		 * @param qDepth The depth with which quiescence search should be called.
		 * @return
		 */
		private int pVsearch(int depth, int distFromRoot, int alpha, int beta, boolean nullMoveAllowed, int qDepth) {
			final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
			final int origAlpha = alpha;
			final boolean isInCheck = pos.isInCheck;
			final boolean isPvNode = beta > origAlpha + 1;
			int bestScore, score, searchedMoves, matMoveBreakInd, kMove, evalScore, razRed, extension;
			Move hashMove, bestMove, killerMove1, killerMove2, move, lastMove;
			boolean isThereHashMove, isThereKM1, isThereKM2, lastMoveIsMaterial;
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
			if ((!ponder && nodes.get() >= maxNodes) || isInterrupted())
				doStopSearch.set(true);
			if (Thread.currentThread().isInterrupted())
				doStopThread = true;
//			if (boundsChanged) {
//				beta = this.beta;
//				boundsChanged = false;
//			}
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
				// Check extension (less than a whole ply because the quiescence search handles checks).
				depth = isInCheck && qDepth == 0 ? depth + params.CHECK_EXT : depth;
				// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
				e = tT.lookUp(pos.key);
				if (e != null) {
					e.generation = hashEntryGen;
					// If the hashed entry's depth is greater than or equal to the current search depth, check if the stored score is usable.
					if (!isPvNode && e.depth >= depth/FULL_PLY && pos.repetitions < 2) {
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
								 * node due to there possibly being siblings to the right of the child node [that raised alpha higher than beta and
								 * caused a cut-off] that could raise alpha even higher. If it was a fail low node, the score is a higher boundary for
								 * the exact score of the node, because all children of a fail low node are fail high nodes (-score <= alpha ->
								 * score >= -alpha [-alpha = beta in the child node]). To keep the interval of values the exact score could take on
								 * out of (alpha, beta), the score has to be lower than or equal to alpha if it is a higher boundary, i.e. fail low
								 * score, and it has to be greater than or equal to beta if it is a lower boundary i.e. fail high score.
								 */
								(e.type == NodeType.FAIL_HIGH.ind && score >= beta) || (e.type == NodeType.FAIL_LOW.ind && score <= alpha))
							return score;
					}
					// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
					if (e.bestMove != 0 && position.isLegalSoft(hashMove = Move.toMove(e.bestMove)))
						isThereHashMove = true;
				}
				// Return the score from the quiescence search in case a leaf node has been reached.
				if (depth/FULL_PLY <= 0) {
					score = quiescence(qDepth, distFromRoot, alpha, beta);
					if (score > bestScore) {
						bestMove = null;
						bestScore = score;
						if (score > alpha)
							alpha = score;
					}
					break Search;
				}
				// If there is no hash entry in a PV node that is to be searched deep, try IID.
				if (isPvNode && !isThereHashMove && depth/FULL_PLY >= params.IID_MIN_ACTIVATION_DEPTH) {
					for (short i = 1; i < depth*params.IID_REL_DEPTH/FULL_PLY; i++) {
						pVsearch(i*FULL_PLY, distFromRoot, alpha, beta, true, qDepth);
					}
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
					// Recapture extension (includes capturing newly promoted pieces).
					extension = lastMoveIsMaterial && hashMove.capturedPiece != Piece.NULL.ind && hashMove.to == lastMove.to ?
						params.RECAP_EXT : 0;
					pos.makeMove(hashMove);
					score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					pos.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = hashMove;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {
								if (!hashMove.isMaterial() && qDepth == 0) {
									kT.add(distFromRoot, hashMove);	// Add to killer moves.
									hT.recordSuccessfulMove(hashMove);	// Record success in the relative history table.
								}
								break Search;
							}
							else if (!hashMove.isMaterial() && qDepth == 0)
								hT.recordUnsuccessfulMove(hashMove);	// Record failure in the relative history table.
						}
					}
				}
				// Generate material moves.
				matMoves = pos.getTacticalMoves();
				// If there was no hash move or material moves, perform a mate check.
				if (!isThereHashMove && matMoves.size() == 0) {
					nonMatMoves = pos.getQuietMoves();
					if (nonMatMoves.size() == 0) {
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
						score = -pVsearch(depth - params.NMR*FULL_PLY, distFromRoot + 1, -beta, -beta + 1, false, qDepth);
						// Mate threat extension.
						if (score <= L_CHECK_MATE_LIMIT)
							depth += params.MATE_THREAT_EXT;
					}
					else
						score = -pVsearch(depth - (params.NMR + 1)*FULL_PLY, distFromRoot + 1, -beta, -beta + 1, false, qDepth);
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
					// If the current move's order-value indicates a losing capture, break the search to check the killer moves.
					if (move.value < 0) {
						matMoveBreakInd = i;
						break;
					}
					// Recapture extension (includes capturing newly promoted pieces).
					extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
							params.RECAP_EXT : 0;
					pos.makeMove(move);
					// PVS.
					if (i == 0)
						score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
//					if (boundsChanged) {
//						beta = this.beta;
//						if (alpha >= beta)
//							break Search;
//						boundsChanged = false;
//					}
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
					if (pos.isLegalSoft(killerMove1) && (!isThereHashMove || !killerMove1.equals(hashMove))) {
						isThereKM1 = true;
						pos.makeMove(killerMove1);
						if (!isThereHashMove && matMoveBreakInd == 0)
							score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
						else {
							score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, qDepth);
							if (score > alpha && score < beta)
								score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
						}
						pos.unmakeMove();
						searchedMoves++;
//						if (boundsChanged) {
//							beta = this.beta;
//							if (alpha >= beta)
//								break Search;
//							boundsChanged = false;
//						}
						if (score > bestScore) {
							bestMove = killerMove1;
							bestScore = score;
							if (score > alpha) {
								alpha = score;
								if (alpha >= beta) {
									if (qDepth == 0)
										hT.recordSuccessfulMove(killerMove1);	// Record success in the relative history table.
									break Search;
								}
								else if (qDepth == 0)
									hT.recordUnsuccessfulMove(killerMove1);	// Record failure in the relative history table.
							}
						}
						if (doStopSearch.get() || doStopThread)
							break Search;
					}
				}
				if ((kMove = kE.getMove2()) != 0) {	// Killer move no. 2.
					killerMove2 = Move.toMove(kMove);
					if (pos.isLegalSoft(killerMove2) && (!isThereHashMove || !killerMove2.equals(hashMove))) {
						isThereKM2 = true;
						pos.makeMove(killerMove2);
						if (!isThereHashMove && !isThereKM1 && matMoveBreakInd == 0)
							score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
						else {
							score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, qDepth);
							if (score > alpha && score < beta)
								score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
						}
						pos.unmakeMove();
						searchedMoves++;
//						if (boundsChanged) {
//							beta = this.beta;
//							if (alpha >= beta)
//								break Search;
//							boundsChanged = false;
//						}
						if (score > bestScore) {
							bestMove = killerMove2;
							bestScore = score;
							if (score > alpha) {
								alpha = score;
								if (alpha >= beta) {
									if (qDepth == 0)
										hT.recordSuccessfulMove(killerMove1);	// Record success in the relative history table.
									break Search;
								}
								else if (qDepth == 0)
									hT.recordUnsuccessfulMove(killerMove1);	// Record failure in the relative history table.
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
					// Recapture extension.
					extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
							params.RECAP_EXT : 0;
					pos.makeMove(move);
					// PVS.
					if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2)
						score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(depth + extension - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
//					if (boundsChanged) {
//						beta = this.beta;
//						if (alpha >= beta)
//							break Search;
//						boundsChanged = false;
//					}
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
				if (matMoves.size() == 0 && nonMatMoves.size() == 1)
					depth += params.SINGLE_REPLY_EXT;
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
							score = -pVsearch(depth - (params.LMR + 1)*FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, qDepth);
						// If it does not fail low, research with full window.
						if (score > alpha)
								score = -pVsearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					}
					// Else PVS.
					else if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2 && matMovesArr.length == 0)
						score = -pVsearch(depth - (razRed + 1)*FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					else {
						score = -pVsearch(depth - (razRed + 1)*FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, qDepth);
						if (score > alpha && score < beta)
							score = -pVsearch(depth - (razRed + 1)*FULL_PLY, distFromRoot + 1, -beta, -alpha, true, qDepth);
					}
					pos.unmakeMove();
					searchedMoves++;
//					if (boundsChanged) {
//						beta = this.beta;
//						if (alpha >= beta)
//							break Search;
//						boundsChanged = false;
//					}
					if (score > bestScore) {
						bestMove = move;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (alpha >= beta) {	// Cutoff from a non-material move.
								if (qDepth == 0) {
									kT.add(distFromRoot, move);	// Add to killer moves.
									hT.recordSuccessfulMove(move);	// Record success in the relative history table.
								}
								break Search;
							}
							else if (qDepth == 0)
								hT.recordUnsuccessfulMove(move);	// Record failure in the relative history table.
						}
					}
					if (doStopSearch.get() || doStopThread)
						break Search;
				}
			}
			// If the search has been invoked from quiescence search, do not store entries in the TT.
			if (qDepth < 0 || pos.repetitions == 2)
				return bestScore;
			// Add new entry to the transposition table.
			insertNodeIntoTt(pos.hashKey(), origAlpha, beta, bestMove, bestScore, (short) distFromRoot, (short) (depth/ FULL_PLY));
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
		 * @param distFromRoot
		 * @param alpha
		 * @param beta
		 * @return
		 */
		private int quiescence(int depth, int distFromRoot, int alpha, int beta) {
			final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
			final boolean isInCheck = pos.isInCheck;
			List<Move> materialMoves, quietMoves;
			Move[] moves;
			Move move;
			int bestScore, searchScore;
			if (depth != 0)
				nodes.incrementAndGet();
			if ((!ponder && nodes.get() >= maxNodes) || isInterrupted())
				doStopSearch.set(true);
			if (Thread.currentThread().isInterrupted())
				doStopThread = true;
//			if (boundsChanged) {
//				beta = this.beta;
//				if (alpha >= beta)
//					break Search;
//				boundsChanged = false;
//			}
			// Check for the repetition rule; return a draw score if it applies.
			if (pos.repetitions >= 3)
				return Termination.DRAW_CLAIMED.score;
			// Just for my peace of mind.
			if (distFromRoot >= MAX_EXPECTED_TOTAL_SEARCH_DEPTH)
				return eval.score(pos, alpha, beta);
			// If check, call the main search for one ply (while keeping the track of the quiescence search depth to avoid resetting it).
			if (isInCheck && qDepth >= 1) {
				// If the side to move is in check, stand-pat does not hold and it's not enough to just search the material moves.
				nodes.decrementAndGet();
				return pVsearch(FULL_PLY, distFromRoot, alpha, beta, false, depth - 1);
			}
			// Quiescence search.
			else {
				materialMoves = pos.getTacticalMoves();
				if (materialMoves.size() == 0) {
					quietMoves = pos.getQuietMoves();
					if (quietMoves.size() == 0)
						return Termination.STALE_MATE.score;
				}
				// Check for the fifty-move rule; return a draw score if it applies.
				if (pos.fiftyMoveRuleClock >= 100)
					return Termination.DRAW_CLAIMED.score;
				// The position is quiet, evaluate it statically.
				if (materialMoves.size() == 0)
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
					searchScore = -quiescence(depth - 1, distFromRoot + 1, -beta, -alpha);
					pos.unmakeMove();
//					if (boundsChanged) {
//						beta = this.beta;
//						if (alpha >= beta)
//							break Search;
//						boundsChanged = false;
//					}
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
	}
}
