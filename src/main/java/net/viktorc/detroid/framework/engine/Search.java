package net.viktorc.detroid.framework.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.viktorc.detroid.framework.engine.KillerTable.KillerTableEntry;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.util.LossyHashTable;
import net.viktorc.detroid.framework.util.QuickSort;

/**
 * A chess game tree search based on the PVS algorithm supported by a transposition table and an evaluation hash table within an iterative deepening 
 * framework with aspiration windows utilizing heuristics such as null move pruning; late move reductions; futility pruning; extended futility 
 * pruning; razoring; IID; quiescence search; and fractional depth extensions such as check extension, recapture extension, and one reply extension. 
 * For move ordering, it relies on a table for killer moves, a table for the relative history score of moves, and the MVVLVA and SEE heuristics.
 * 
 * @author Viktor
 *
 */
class Search implements Runnable, Future<SearchResults> {
	
	private final Params params;
	private final Position rootPosition;
	private final Evaluator eval;
	private final SearchInfo info;
	// Killer heuristic table.
	private final KillerTable kT;
	// History heuristic table.
	private final RelativeHistoryTable hT;
	// Transposition table.
	private final LossyHashTable<TTEntry> tT;
	private final byte hashEntryGen;
	// Including extensions and quiescence search.
	private final int maxExpectedSearchDepth;
	private final int lCheckMateLimit;
	private final int wCheckMateLimit;
	private final int maxDepth;
	private final long maxNodes;
	private final boolean ponder;
	private final ArrayList<Move> allowedRootMoves;
	private final boolean areMovesRestricted;
	private final int numOfHelperThreads;

	private Long startTime;
	private ExecutorService executor;
	private CountDownLatch latch;
	private AtomicLong nodes;
	private boolean isDone;
	private volatile int selDepth;
	private volatile boolean doStopSearch;
	private volatile SearchResults results;
	
	
	/**
	 * Constructs a new {@link #Search} instance.
	 * 
	 * @param position
	 * @param info
	 * @param ponder
	 * @param maxDepth
	 * @param maxNodes
	 * @param moves
	 * @param eval
	 * @param historyTable
	 * @param hashEntryGen
	 * @param transposTable
	 * @param params
	 * @param numOfSearchThreads
	 */
	Search(Position position, SearchInfo info, boolean ponder, int maxDepth, long maxNodes, Set<Move> moves,
			Evaluator eval, RelativeHistoryTable historyTable, final byte hashEntryGen,
			LossyHashTable<TTEntry> transposTable, Params params, int numOfSearchThreads) {
		this.params = params;
		this.info = info;
		this.eval = eval;
		this.rootPosition = position;
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
 		/* In case all the extensions are activated during the search and the quiscence search probes 2 fold beyond
 		 * the main search depth. */
		maxExpectedSearchDepth = 2*4*this.maxDepth;
		lCheckMateLimit = Termination.CHECK_MATE.score + maxExpectedSearchDepth;
		wCheckMateLimit = -lCheckMateLimit;
		if (this.maxDepth > 0)
			kT = new KillerTable(this.maxDepth*2);
		else
			kT = null;
		this.hT = historyTable;
		this.tT = transposTable;
		this.hashEntryGen = hashEntryGen;
		this.numOfHelperThreads = this.maxDepth > 1 ? numOfSearchThreads - 1 : 1;
	}
	/**
	 * Returns a list of Move objects according to the best line of play extracted form the transposition table.
	 * 
	 * @param position
	 * @param ply
	 * @return
	 */
	private List<Move> extractPv(Position position, int ply) {
		List<Move> pV = new ArrayList<>();
		TTEntry e;
		Move bestMove;
		int i;
		i = 0;
		while ((e = tT.get(position.key)) != null && e.bestMove != 0 && i < ply) {
			bestMove = Move.toMove(e.bestMove);
			position.makeMove(bestMove);
			pV.add(bestMove);
			i++;
		}
		for (int k = 0; k < i; k++)
			position.unmakeMove();
		return pV;
	}
	/**
	 * Determines the score type and adjusts the score in case it's a mate.
	 * 
	 * @param score
	 * @param alpha
	 * @param beta
	 * @return
	 */
	private Entry<Short,ScoreType> adjustScore(int score, int alpha, int beta) {
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
				resultScore = (int) Math.floor(((double) (Termination.CHECK_MATE.score - score))/2);
				scoreType = ScoreType.MATE;
			} else if (score >= wCheckMateLimit) {
				resultScore = (int) Math.ceil(((double) (-Termination.CHECK_MATE.score - score))/2);
				scoreType = ScoreType.MATE;
			} else {
				resultScore = score;
				scoreType = ScoreType.EXACT;
			}
		}
		return new SimpleImmutableEntry<>((short) resultScore, scoreType);
	}
	/**
	 * Updates the observable search info according to the specified parameters.
	 * 
	 * @param position
	 * @param currentMove
	 * @param moveNumber
	 * @param ply
	 * @param alpha
	 * @param beta
	 * @param score
	 */
	private void updateInfo(Position position, Move currentMove, int moveNumber, short ply, int alpha, int beta, int score) {
		// Determine score type and value in case it's a mate score.
		Entry<Short,ScoreType> adjustedScore = adjustScore(score, alpha, beta);
		// Update stats.
		info.set(extractPv(position, ply), currentMove, moveNumber, ply, (short) selDepth, adjustedScore.getKey(),
				adjustedScore.getValue(), nodes.get(), System.currentTimeMillis() - startTime);
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
	/**
	 * An iterative deepening framework with gradually widening aspiration windows.
	 * 
	 * @return
	 */
	private SearchResults iterativeDeepening() {
		int alpha, beta, score, failHigh, failLow;
		boolean even;
		SearchThread masterThread;
		SearchThread slaveThread;
		List<SearchThread> slaveThreads;
		Entry<Short,ScoreType> adjustedScore;
		TTEntry entry;
		List<Move> pV;
		Move bestMove, ponderMove;
		nodes = new AtomicLong(0);
		selDepth = 0;
		doStopSearch = false;
		slaveThreads = null;
		alpha = Termination.CHECK_MATE.score;
		beta = -alpha;
		if (maxDepth == 0) {
			masterThread = new SearchThread(rootPosition, (short) 0, alpha, beta, null);
			score = masterThread.quiescence(0, alpha, beta);
			return new SearchResults(null, null, (short) score, ScoreType.EXACT);
		}
		// The number of consecutive fail-highs/fail-lows.
		failHigh = failLow = 0;
		// Iterative deepening.
		for (short i = 1;; i++) {
			masterThread = new SearchThread(rootPosition.deepCopy(), i, alpha, beta, null);
			// Launch helper threads.
			if (numOfHelperThreads > 0 && i != 1) {
				latch = new CountDownLatch(numOfHelperThreads);
				slaveThreads = new ArrayList<>();
				even = true;
				for (int j = 0; j < numOfHelperThreads; j++, even = !even) {
					slaveThread = new SearchThread(rootPosition.deepCopy(), (short) (even && i != maxDepth ? i + 1 : i),
							alpha, beta, masterThread);
					slaveThreads.add(slaveThread);
					executor.submit(slaveThread);
				}
			}
			// Launch the master thread.
			score = masterThread.call();
			// Interrupt the helpers thread and wait for them to terminate.
			if (numOfHelperThreads > 0 && i != 1) {
				for (SearchThread t : slaveThreads)
					t.stop();
				try {
					latch.await();
				} catch (InterruptedException e) { }
			}
			if (doStopSearch || i == maxDepth)
				break;
			// Aspiration windows with gradual widening.
			if (score <= alpha && alpha != Termination.CHECK_MATE.score) {
				if (score <= lCheckMateLimit) {
					alpha = Termination.CHECK_MATE.score;
					failLow = 2;
				} else {
					alpha = failLow == 0 ? Math.max(score - 2*params.aspirationDelta, Termination.CHECK_MATE.score) :
							failLow == 1 ? Math.max(score - 4*params.aspirationDelta, Termination.CHECK_MATE.score) :
							Termination.CHECK_MATE.score;
					failLow++;
				}
				i--;
			} else if (score >= beta && beta != -Termination.CHECK_MATE.score) {
				if (score >= wCheckMateLimit) {
					beta = -Termination.CHECK_MATE.score;
					failHigh = 2;
				} else {
					beta = failHigh == 0 ? Math.min(score + 2*params.aspirationDelta, -Termination.CHECK_MATE.score) :
							failHigh == 1 ? Math.min(score + 4*params.aspirationDelta, -Termination.CHECK_MATE.score) :
							-Termination.CHECK_MATE.score;
					failHigh++;
				}
				i--;
			} else {
				if (Math.abs(beta) < wCheckMateLimit && Math.abs(alpha) < wCheckMateLimit)
					failHigh = failLow = 0;
				alpha = Math.max(score - params.aspirationDelta, Termination.CHECK_MATE.score);
				beta = Math.min(score + params.aspirationDelta, -Termination.CHECK_MATE.score);
			}
		}
		bestMove = ponderMove = null;
		entry = tT.get(rootPosition.key);
		pV = extractPv(rootPosition, 2);
		if (pV != null && pV.size() > 0) {
			bestMove = pV.get(0);
			if (pV.size() > 1)
				ponderMove = pV.get(1);
		}
		if (entry != null)
			adjustedScore = adjustScore(entry.score, alpha, beta);
		else
			adjustedScore = adjustScore(score, alpha, beta);
		return new SearchResults(bestMove == null ? null : bestMove.toString(), ponderMove == null ? null : ponderMove.toString(),
				adjustedScore.getKey(), adjustedScore.getValue());
	}
	@Override
	public synchronized SearchResults get() {
		while (!isDone) {
			try {
				wait();
			} catch (InterruptedException e) { }
		}
		return results;
	}
	@Override
	public synchronized SearchResults get(long timeout, TimeUnit unit) {
		long timeoutMs = unit.toMillis(timeout);
		long start = System.currentTimeMillis();
		while (!isDone && timeoutMs > 0) {
			try {
				wait(timeoutMs);
				break;
			} catch (InterruptedException e) {
				timeoutMs -= (System.currentTimeMillis() - start);
			}
		}
		return results;
	}
	@Override
	public boolean isDone() {
		return isDone;
	}
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (mayInterruptIfRunning && !isDone) {
			doStopSearch = true;
			return true;
		}
		return false;
	}
	@Override
	public boolean isCancelled() {
		return doStopSearch;
	}
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		isDone = doStopSearch = false;
		if (numOfHelperThreads > 0)
			executor = Executors.newFixedThreadPool(numOfHelperThreads);
		results = iterativeDeepening();
		startTime = null;
		if (executor != null)
			executor.shutdown();
		doStopSearch = false;
		isDone = true;
		synchronized (this) {
			notifyAll();
		}
	}
	
	/**
	 * A search thread for searching a position within an iterative deepening framework.
	 * 
	 * @author Viktor
	 *
	 */
	private class SearchThread implements Callable<Integer> {

		private final Position origPosition; // The original position to search.
		private final short ply;
		private final SearchThread master;
		private final boolean isMainSearchThread;
		private final int alpha;
		private final int beta;
		private Position position; // The position instance to use for the search.
		private volatile boolean doStopSearchThread;
		
		/**
		 * Constructs a {@link #SearchThread} instance using the specified arguments. If the parameter master is null, 
		 * the search thread will be constructed as a master search thread itself.
		 * 
		 * @param position
		 * @param ply
		 * @param alpha
		 * @param beta
		 * @param master
		 */
		SearchThread(Position position, short ply, int alpha, int beta, SearchThread master) {
			origPosition = position;
			this.ply = ply;
			this.master = master;
			this.isMainSearchThread = master == null;
			this.alpha = alpha;
			this.beta = beta;
		}
		/**
		 * Interrupts the search thread.
		 */
		void stop() {
			doStopSearchThread = true;
		}
		/**
		 * Returns whether a move is a pawn push. A pawn push is a pawn move to the last or the one before the last rank.
		 * 
		 * @param move
		 * @return
		 */
		private boolean isPawnPush(Move move) {
			return (move.movedPiece == Piece.W_PAWN.ind && move.to >= 48) ||
					(move.movedPiece == Piece.B_PAWN.ind && move.to < 16);
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
		 * A search algorithm for diminishing the horizon effect once the main search algorithm has reached a leaf node. It keeps searching until
		 * the side to move does not have any legal winning captures or even exchanges according to SEE; or the absolute maximmum search depth is 
		 * reached.
		 * 
		 * @param distFromRoot
		 * @param alpha
		 * @param beta
		 * @return
		 * @throws AbnormalSearchTerminationException
		 */
		private int quiescence(int distFromRoot, int alpha, int beta) throws AbnormalSearchTerminationException {
			List<Move> moveList;
			Move[] moves;
			int bestScore, searchScore;
			if (!ponder && nodes.get() >= maxNodes)
				doStopSearch = true;
			if (doStopSearch|| doStopSearchThread)
				throw new AbnormalSearchTerminationException();
			nodes.incrementAndGet();
			// Fifty-move rule and repetition rule check.
			if (position.fiftyMoveRuleClock >= 100 || position.hasRepeated(distFromRoot > 2 ? 1 : 2))
				return Termination.DRAW_CLAIMED.score;
			// Evaluate the position statically.
			bestScore = eval.score(position, hashEntryGen, alpha, beta);
			// Fail soft.
			if (bestScore > alpha) {
				alpha = bestScore;
				if (bestScore >= beta)
					return bestScore;
			}
			// Limit the maximum depth of the quiescence search.
			if (distFromRoot > maxExpectedSearchDepth)
				return eval.score(position, hashEntryGen, alpha, beta);
			// Generate all the material moves or all moves if in check.
			moveList = position.getTacticalMoves();
			moves = orderMaterialMovesSEE(position, moveList);
			for (Move move : moves) {
				// If the SEE value is below 0 or the delta pruning limit, break the search because the rest of the moves are even worse.
				if (move.value < 0 || (maxDepth != 0 && move.value <= alpha - params.deltaPruningMargin))
					break;
				position.makeMove(move);
				searchScore = -quiescence(distFromRoot + 1, -beta, -alpha);
				position.unmakeMove();
				if (searchScore > bestScore) {
					bestScore = searchScore;
					if (searchScore > alpha) {
						alpha = searchScore;
						if (searchScore >= beta)
							break;
					}
				}
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
		 * @throws AbnormalSearchTerminationException
		 */
		private int pVsearch(int depth, int distFromRoot, int alpha, int beta, boolean nullMoveAllowed)
				throws AbnormalSearchTerminationException {
			// Do not allow negative depths for the full effect of check extensions.
			depth = Math.max(depth, 0);
			final int mateScore = Termination.CHECK_MATE.score + distFromRoot;
			final int origAlpha = alpha;
			final int depthLimit = distFromRoot >= maxDepth ? depth : depth + 2*params.fullPly;
			final boolean isInCheck = position.isInCheck;
			final boolean isPvNode = beta > origAlpha + 1;
			int bestScore, score, evalScore;
			int reduction, extension;
			int matMoveBreakInd;
			int searchedMoves;
			int kMove;
			int razMargin, futMargin;
			Move hashMove, bestMove, killerMove1, killerMove2, move, lastMove;
			boolean doQuiescence;
			boolean isThereHashMove, isThereKM1, isThereKM2;
			boolean lastMoveIsTactical, pawnPushed;
			boolean isDangerous, isReducible, mateThreat;
			List<Move> tacticalMoves, quietMoves;
			Move[] tacticalMovesArr, quietMovesArr;
			TTEntry e;
			KillerTableEntry kE;
			bestScore = mateScore;
			bestMove = null;
			searchedMoves = 0;
			hashMove = killerMove1 = killerMove2 = null;
			doQuiescence = false;
			isThereHashMove = isThereKM1 = isThereKM2 = false;
			tacticalMoves = quietMoves = null;
			kE = null;
			if (!ponder && nodes.get() >= maxNodes)
				doStopSearch = true;
			if (doStopSearch|| doStopSearchThread)
				throw new AbnormalSearchTerminationException();
			nodes.incrementAndGet();
			if (distFromRoot > selDepth)
				selDepth = distFromRoot;
			Search: {
				// Check for the repetition rule; return a draw score if it applies.
				if (position.hasRepeated(distFromRoot > 2 ? 1 : 2))
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
				depth = isInCheck ? Math.min(depthLimit, depth + params.checkExtension) : depth;
				// Pawn push extension
				lastMove = position.getLastMove();
				pawnPushed = isPawnPush(lastMove);
				depth = pawnPushed ? Math.min(depthLimit, depth + params.pawnPushExtension) : depth;
				// Check the conditions for quiescence search.
				doQuiescence = depth/params.fullPly <= 0;
				// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
				e = tT.get(position.key);
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
						if (e.type == NodeType.EXACT.ind || (e.type == NodeType.FAIL_HIGH.ind && score >= beta) ||
								(e.type == NodeType.FAIL_LOW.ind && score <= alpha))
							return score;
					}
					// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
					if (e.bestMove != 0 && !doQuiescence) {
						hashMove = Move.toMove(e.bestMove);
						isThereHashMove = position.isLegalSoft(hashMove);
					}
				}
				// Perform quiescence search.
				if (doQuiescence) {
					nodes.decrementAndGet();
					score = quiescence(distFromRoot, alpha, beta);
					if (score > bestScore) {
						bestMove = null;
						bestScore = score;
					}
					break Search;
				}
				evalScore = Integer.MIN_VALUE;
				// Assess if the position is 'dangerous'.
				isDangerous =  isPvNode || isInCheck || pawnPushed || !position.areTherePiecesOtherThanKingsAndPawns();
				mateThreat = Math.abs(beta) >= wCheckMateLimit;
				// Try null move pruning if it is allowed and the position is 'safe'.
				if (nullMoveAllowed && !isDangerous && !mateThreat && depth/ply >= params.nullMoveReductionMinActivationDepth) {
					evalScore = eval.score(position, hashEntryGen, alpha, beta);
					if (evalScore > alpha) {
						// Dynamic depth reduction.
						reduction = params.nullMoveReduction*params.fullPly +
								params.extraNullMoveReduction*depth/params.extraNullMoveReductionDepthLimit;
						position.makeNullMove();
						// Do not allow consecutive null moves.
						score = -pVsearch(depth - (params.fullPly + reduction), distFromRoot + 1, -beta, -beta + 1, false);
						position.unmakeMove();
						if (score >= beta)
							return score;
						if (score <= lCheckMateLimit)
							mateThreat = true;
					}
				}
				// Try razoring if it is allowed and the position is 'safe'.
				if (params.doRazor && !isDangerous && !mateThreat && Math.abs(alpha) < wCheckMateLimit && depth/params.fullPly <= 3) {
					switch (depth/params.fullPly) {
						case 1: // Retrograde pre-frontier razoring.
							razMargin = params.razoringMargin1;
							break;
						case 2: // Retrograde limited razoring.
							razMargin = params.razoringMargin2;
							break;
						case 3: // Retrograde deep razoring.
							razMargin = params.razoringMargin3;
							break;
						default:
							razMargin = 0;
					}
					if (evalScore == Integer.MIN_VALUE)
						evalScore = eval.score(position, hashEntryGen, alpha, beta);
					if (evalScore < beta - razMargin) {
						score = quiescence(distFromRoot, alpha - razMargin, beta - razMargin);
						if (score <= alpha - razMargin)
							return score;
					}
				}
				// If there is no hash entry in a PV node that is to be searched deep, try IID.
				if (params.doIid && isPvNode && !isThereHashMove && depth/params.fullPly >= params.iidMinActivationDepth) {
					pVsearch(depth*params.iidRelDepthHth/100, distFromRoot, alpha, beta, true);
					e = tT.get(position.key);
					if (e != null && e.bestMove != 0) {
						hashMove = Move.toMove(e.bestMove);
						isThereHashMove = position.isLegalSoft(hashMove);
					}
				}
				// If there was no hash move...
				if (!isThereHashMove) {
					// Generate material moves.
					tacticalMoves = position.getTacticalMoves();
					// If there were no material moves, check killer moves...
					if (tacticalMoves.size() == 0) {
						kE = kT.retrieve(distFromRoot);
						if (kE.getMove1() != 0 && position.isLegalSoft(killerMove1 = Move.toMove(kE.getMove1())))
							isThereKM1 = true;
						else if (kE.getMove2() != 0 && position.isLegalSoft(killerMove2 = Move.toMove(kE.getMove2())))
							isThereKM2 = true;
						// If there were no legal killer moves, generate the quiet moves and perform a mate check.
						if (!isThereKM1 && !isThereKM2) {
							quietMoves = position.getQuietMoves();
							if (quietMoves.size() == 0) {
								score = isInCheck ? mateScore : Termination.STALE_MATE.score;
								if (score > bestScore) {
									bestMove = null;
									bestScore = score;
								}
								break Search;
							}
						}
					}
				}
				// Check for the fifty-move rule; return a draw score if it applies.
				if (position.fiftyMoveRuleClock >= 100)
					return Termination.DRAW_CLAIMED.score;
				// Check if a recapture extension could possibly be applied.
				lastMove = position.getLastMove();
				lastMoveIsTactical = lastMove != null && lastMove.isMaterial();
				// If there is a hash move, search that first.
				if (isThereHashMove) {
					// Recapture extension (includes capturing newly promoted pieces).
					extension = lastMoveIsTactical && hashMove.capturedPiece != Piece.NULL.ind && hashMove.to == lastMove.to ? params.recapExtension : 0;
					position.makeMove(hashMove);
					score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					position.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = hashMove;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (score >= beta) {
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
				// Generate the material moves if they are not generated yet.
				if (tacticalMoves == null)
					tacticalMoves = position.getTacticalMoves();
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
							params.recapExtension : 0;
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
				}
				// If there are no more winning or equal captures, check and search the killer moves if legal in this position.
				kE = kE == null ? kT.retrieve(distFromRoot) : kE;
				if ((kMove = kE.getMove1()) != 0) {	// Killer move no. 1.
					killerMove1 = killerMove1 != null ? killerMove1 : Move.toMove(kMove);
					if (isThereKM1 || (position.isLegalSoft(killerMove1) && (!isThereHashMove || !killerMove1.equals(hashMove)))) {
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
					}
				}
				if ((kMove = kE.getMove2()) != 0) {
					killerMove2 = killerMove2 != null ? killerMove2 : Move.toMove(kMove);
					if (isThereKM2 || (position.isLegalSoft(killerMove2) && (!isThereHashMove || !killerMove2.equals(hashMove)))) {
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
					}
				}
				// Search losing captures if there are any.
				for (int i = matMoveBreakInd; i < tacticalMovesArr.length; i++) {
					move = tacticalMovesArr[i];
					// If this move was the hash move, skip it.
					if (isThereHashMove && move.equals(hashMove)) {
						isThereHashMove = false;
						continue;
					}
					// Recapture extension.
					extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? params.recapExtension : 0;
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
				}
				// Generate the non-material legal moves if they are not generated yet.
				if (quietMoves == null)
					quietMoves = position.getQuietMoves();
				// One reply extension.
				if (tacticalMoves.size() == 0 && quietMoves.size() == 1) {
					isDangerous = true;
					depth = Math.min(depthLimit, depth + params.singleReplyExtension);
				}
				// Futility pruning margin calculation.
				futMargin = 0;
				if (!isDangerous) {
					switch (depth/params.fullPly) {
						case 1:
							// Frontier futility pruning.
							futMargin = params.futilityMargin1;
							break;
						case 2:
							// Extended futility pruning.
							futMargin = params.futilityMargin2;
							break;
						case 3:
							// Deep futility pruning.
							futMargin = params.futilityMargin3;
							break;
						case 4:
							// Deep+ futility pruning.
							futMargin = params.futilityMargin4;
							break;
						case 5:
							// Deep++ futility pruning.
							futMargin = params.futilityMargin5;
					}
				}
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
					// Futility pruning, extended futility pruning, and razoring.
					if (isReducible && !mateThreat && depth/params.fullPly <= 5) {
						if (evalScore == Integer.MIN_VALUE)
							evalScore = eval.score(position, hashEntryGen, alpha - futMargin, beta - futMargin);
						if (evalScore <= alpha - futMargin) {
							// Record failure in the relative history table.
							hT.recordUnsuccessfulMove(move);
							continue;
						}
					}
					position.makeMove(move);
					// Try late move reduction.
					if (isReducible && depth/params.fullPly >= params.lateMoveReductionMinActivationDepth &&
							searchedMoves > params.minMovesSearchedForLmr) {
						reduction = params.lateMoveReduction*params.fullPly +
								params.extraLateMoveReduction*depth/params.extraLateMoveReductionDepthLimit;
						score = -pVsearch(depth - (params.fullPly + reduction), distFromRoot + 1, -alpha - 1, -alpha, true);
						// If it does not fail low, research with full window.
						if (score > alpha)
							score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					}
					// Else simple PVS.
					else if (i == 0 && !isThereHashMove && !isThereKM1 && !isThereKM2 && tacticalMovesArr.length == 0)
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
					else {
						score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true);
						if (score > alpha && score < beta)
							score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true);
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
				}
			}
			// Add new entry to the transposition table.
			insertNodeIntoTt(position.key, origAlpha, beta, bestMove, bestScore, (short) distFromRoot, (short) (depth/params.fullPly));
			// Return the unadjusted best score.
			return bestScore;
		}
		@Override
		public Integer call() {
			int depth = ply*params.fullPly;
			final int depthLimit = depth + 2*params.fullPly;
			final int origAlpha = alpha;
			int alpha = this.alpha;
			int beta = this.beta;
			int moveInd;
			int score, bestScore, extension, numOfMoves;
			Move hashMove, bestMove, move, lastMove;
			boolean lastMoveIsMaterial, statsUpdated;
			List<Move> tacticalMoves, quietMoves, allMoves;
			Move[] tacticalMovesArr, quietMovesArr, allMovesArr;
			TTEntry entry;
			bestScore = Integer.MIN_VALUE;
			bestMove = hashMove = null;
			statsUpdated = false;
			try {
				position = origPosition.deepCopy();
				nodes.incrementAndGet();
				// If ply equals 0, perform only quiescence search.
				tacticalMoves = quietMoves = null;
				// Check for the 3-fold repetition rule.
				if (position.hasRepeated(2))
					return (int) Termination.DRAW_CLAIMED.score;
				// Generate moves.
				tacticalMoves = position.getTacticalMoves();
				quietMoves = position.getQuietMoves();
				numOfMoves = tacticalMoves.size() + quietMoves.size();
				// Mate check.
				if (numOfMoves == 0)
					return (int) (position.isInCheck ? Termination.CHECK_MATE.score : Termination.STALE_MATE.score);
				// One reply extension.
				else if (numOfMoves == 1)
					depth = Math.min(depthLimit, depth + params.singleReplyExtension);
				// Check for the 50 move rule.
				if (position.fiftyMoveRuleClock >= 100)
					return (int) Termination.DRAW_CLAIMED.score;
				// Check extension.
				depth = position.isInCheck ? Math.min(depthLimit, depth + params.checkExtension) : depth;
				// Pawn push extension
				lastMove = position.getLastMove();
				depth = lastMove != null && isPawnPush(lastMove) ? Math.min(depthLimit, depth + params.pawnPushExtension) : depth;
				// Hash look-up.
				entry = tT.get(position.key);
				if (entry != null) {
					entry.generation = hashEntryGen;
					// If the hashed entry's depth is greater than or equal to the current search depth, check if the stored score is usable.
					if (entry.depth > depth/params.fullPly) {
						score = entry.score;
						/* If the score was exact, or it was the score of an all node and is smaller than or equal to alpha, or it is that of a cut
						 * node and is greater than or equal to beta, return the score. */
						if (entry.type == NodeType.EXACT.ind ||
								/* To make sure that a score that might not have been the exact score for the subtree below the node regardless of the
								 * alpha-beta boundaries is not treated as an exact score in the current context, we can not allow it to fall between
								 * the current alpha and beta. If it was a fail high node, the score is a lower boundary of the exact score of the
								 * node due to there possibly being siblings to the right of the child node [that raised alpha higher than beta and
								 * caused a cut-off] that could raise alpha even higher. If it was a fail low node, the score is a higher boundary for
								 * the exact score of the node, because all children of a fail low node are fail high nodes (-score <= alpha ->
								 * score >= -alpha [-alpha = beta in the child node]). To keep the interval of values the exact score could take on
								 * out of (alpha, beta), the score has to be lower than or equal to alpha if it is a higher boundary, i.e. fail low
								 * score, and it has to be greater than or equal to beta if it is a lower boundary i.e. fail high score. */
								(entry.type == NodeType.FAIL_HIGH.ind && score >= beta) || (entry.type == NodeType.FAIL_LOW.ind && score <= alpha)) {
							if (entry.depth > selDepth)
								selDepth = entry.depth;
							if (isMainSearchThread) {
								updateInfo(position, null, 0, ply, origAlpha, beta, score);
								statsUpdated = true;
							}
							return score;
						}
					}
					if (entry.bestMove != 0)
						hashMove = Move.toMove(entry.bestMove);
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
				lastMoveIsMaterial = lastMove != null && lastMove.isMaterial();
				move = null;
				// Iterate over moves.
				for (moveInd = 0; moveInd < allMovesArr.length; moveInd++) {
					move = allMovesArr[moveInd];
					// Recapture extension.
					extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
						params.recapExtension : 0;
					position.makeMove(move);
					// Full window search for the first move...
					if (moveInd == 0)
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
						if (bestScore > alpha) {
							alpha = bestScore;
							// Insert into TT and update stats if applicable.
							insertNodeIntoTt(position.key, origAlpha, beta, move, score, (short) 0, (short) (depth/params.fullPly));
							if (isMainSearchThread) {
								updateInfo(position, move, moveInd + 1, ply, origAlpha, beta, score);
								statsUpdated = true;
							}
							if (bestScore >= beta)
								break;
						}
					}
				}
				// If the search stats have not been updated for the current ply yet, due to failing low, do it now.
				if (bestScore <= origAlpha) {
					insertNodeIntoTt(position.key, origAlpha, beta, bestMove, bestScore, (short) 0, (short) (depth/params.fullPly));
					if (isMainSearchThread) {
						updateInfo(position, move, move != null ? Math.min(allMovesArr.length, moveInd + 1) : 0, ply, origAlpha, beta, bestScore);
						statsUpdated = true;
					}
				}
				return bestScore;
			} catch (AbnormalSearchTerminationException e) {
				entry = tT.get(origPosition.key);
				if (entry != null)
					bestScore = entry.score;
				if (isMainSearchThread && !statsUpdated)
					updateInfo(origPosition, null, 0, ply, origAlpha, beta, bestScore);
				return bestScore;
			} finally {
				if (!isMainSearchThread) {
					latch.countDown();
					master.stop();
				}
			}
		}
		
	}
	
	/**
	 * A {@link #RuntimeException} for when a search is cancelled or the maximum number of nodes to search have been exceeded and thus 
	 * the search is interrupted resulting in an abrupt, disorderly termination.
	 * 
	 * @author Viktor
	 *
	 */
	private class AbnormalSearchTerminationException extends RuntimeException {

		/**
		 * Default serial version ID.
		 */
		private static final long serialVersionUID = 1L;
		
	}
	
	
}
