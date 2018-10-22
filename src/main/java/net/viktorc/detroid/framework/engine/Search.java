package net.viktorc.detroid.framework.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import net.viktorc.detroid.framework.engine.EndGameTableBase.DTM;
import net.viktorc.detroid.framework.engine.EndGameTableBase.EGTBStats;
import net.viktorc.detroid.framework.engine.KillerTable.KTEntry;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.util.Cache;
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
public class Search implements Runnable, Future<SearchResults> {
	
	/**
	 * The depth to which the position is to be searched at the first iteration of the iterative deepening framework.
	 */
	private static final short INITIAL_DEPTH = 1;
	
	private final Position rootPosition;
	private final DetroidParameters params;
	private final Evaluator eval;
	private final EndGameTableBase egtb;
	private final boolean useEgtb;
	private final Set<Integer> availableEgtbs;
	private final DetroidSearchInformation info;
	private final Cache<TTEntry> tT;
	private final byte hashEntryGen;
	private final int numOfHelperThreads;
	private final boolean analysisMode;
	private final boolean ponder;
	private final long maxNodes;
	private final List<Move> allowedRootMoves;
	private final boolean areMovesRestricted;
	private final int maxNominalDepth;
	private final int maxExpectedSearchDepth;
	private final int lCheckMateLimit, wCheckMateLimit;
	private final Object rootLock;
	private ExecutorService executor;
	private Long startTime;
	private SearchStats searchStats;
	private CountDownLatch latch;
	private List<Move> rootMoves;
	private Map<Move,AtomicLong> movesToNodes;
	private volatile int selDepth;
	private volatile boolean isDone;
	private volatile boolean doStopSearch;
	private volatile SearchResults results;
	
	/**
	 * Constructs a new instance using the specified parameters.
	 * 
	 * @param position The root position to search.
	 * @param params The engine parameters.
	 * @param eval The evaluator object.
	 * @param egtb The endgame tablebase object to use.
	 * @param info The object to update with search information.
	 * @param numOfSearchThreads The number of search threads to use.
	 * @param tT1 The primary transposition table to use.
	 * @param tT2 The secondary transposition table to use.
	 * @param hashEntryGen The current hash entry generation.
	 * @param ponder Whether the search should run in ponder mode.
	 * @param maxNominalDepth The maximum depth to search to.
	 * @param maxNodes The maximum number of positions to search.
	 * @param moves The only moves from the root position that should be searched. If it is <code>null</code>, 
	 * all moves are to be searched.
	 * @param analysisMode Whether the search should run in analysis mode. In analysis mode, the endgame 
	 * tablebases are not used and single move root positions are searched, too.
	 */
	Search(Position0 position, DetroidParameters params, Evaluator eval, EndGameTableBase egtb, DetroidSearchInformation info,
		   int numOfSearchThreads, Cache<TTEntry> tT, byte hashEntryGen, boolean analysisMode, boolean ponder, int maxDepth,
		   long maxNodes, Set<Move> moves) {
		this.params = params;
		this.info = info;
		this.eval = eval;
		this.egtb = egtb;
		useEgtb = !analysisMode && egtb.isProbingLibLoaded() && egtb.isInit();
		if (useEgtb) {
			availableEgtbs = new HashSet<>();
			for (int i = 3; i <= EndGameTableBase.MAX_NUMBER_OF_PIECES; i++) {
				if (egtb.areTableBasesAvailable(i))
					availableEgtbs.add(i);
			}
		} else
			availableEgtbs = null;
		this.rootPosition = position;
		this.analysisMode = analysisMode;
		this.ponder = ponder;
		if (!ponder) {
			maxDepth = Math.max(0, maxDepth);
			this.maxNominalDepth = Math.min(this.params.maxNominalSearchDepth, maxDepth);
			this.maxNodes = maxNodes;
		} else {
			this.maxNominalDepth = this.params.maxNominalSearchDepth;
			this.maxNodes = Long.MAX_VALUE;
		}
		allowedRootMoves = moves != null ? new ArrayList<>(moves) : null;
		areMovesRestricted = moves != null && moves.size() > 0;
 		/* In case all the extensions are activated during the search and the quiscence search probes 2 fold beyond
 		 * the main search depth. */
		maxExpectedSearchDepth = 2*2*this.maxNominalDepth;
		lCheckMateLimit = Score.LOSING_CHECK_MATE.value + maxExpectedSearchDepth;
		wCheckMateLimit = -lCheckMateLimit;
		this.tT = tT;
		this.hashEntryGen = hashEntryGen;
		this.numOfHelperThreads = this.maxNominalDepth > 1 ? numOfSearchThreads - 1 : 0;
		rootLock = new Object();
	}
	/**
	 * Returns the cumulative total number of nodes searched.
	 * 
	 * @return
	 */
	private long getTotalNodes() {
		return searchStats.mainNodes.get() + searchStats.quiescenceNodes.get();
	}
	/**
	 * Returns a list of Move objects according to the best line of play extracted form the transposition table.
	 * 
	 * @param position
	 * @param ply
	 * @return
	 */
	private List<Move> extractPv(Position0 position, int ply) {
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
		if (score == Score.NULL.value)
			return null;
		// Determine score type and value in case it's a mate score.
		else if (score >= beta) {
			scoreType = ScoreType.LOWER_BOUND;
			resultScore = score;
		} else if (score <= alpha) {
			scoreType = ScoreType.UPPER_BOUND;
			resultScore = score;
		} else {
			if (score <= lCheckMateLimit) {
				resultScore = (int) Math.floor(((double) (Score.LOSING_CHECK_MATE.value - score))/2);
				scoreType = ScoreType.MATE;
			} else if (score >= wCheckMateLimit) {
				resultScore = (int) Math.ceil(((double) (Score.WINNING_CHECK_MATE.value - score))/2);
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
	private void updateInfo(Position0 position, Move currentMove, int moveNumber, short ply, int alpha, int beta, int score) {
		// Determine score type and value in case it's a mate score.
		Entry<Short,ScoreType> adjustedScore = adjustScore(score, alpha, beta);
		long egtbHits = 0;
		String statInfo = null;
		if (analysisMode)
			statInfo = searchStats.toString();
		if (useEgtb) {
			EGTBStats egtbStats = egtb.getStats();
			egtbHits += egtbStats.getTotalDriveHits() + egtbStats.getTotalCacheHits();
			if (analysisMode)
				statInfo += String.format(" egtb_hp %d egtb_sp %d egtb_chits %d egtb_dhits %d", egtbStats.getTotalHardProbes(),
						egtbStats.getTotalSoftProbes(), egtbStats.getTotalCacheHits(), egtbStats.getTotalDriveHits());
		}
		// Update stats.
		if (adjustedScore != null)
			info.set(extractPv(position, ply), currentMove, moveNumber, ply, (short) selDepth, adjustedScore.getKey(),
					adjustedScore.getValue(), getTotalNodes(), System.currentTimeMillis() - startTime, egtbHits, statInfo);
	}
	/**
	 * An iterative deepening framework with gradually widening aspiration windows.
	 * 
	 * @return
	 */
	private SearchResults iterativeDeepening() {
		short ply;
		int alpha, beta, score, failHigh, failLow;
		boolean odd;
		SearchThread masterThread, slaveThread;
		TTEntry entry;
		List<SearchThread> slaveThreads;
		Entry<Short,ScoreType> adjustedScore;
		List<Move> pV;
		Move bestMove, ponderMove;
		Short finalScore;
		ScoreType scoreType;
		selDepth = 0;
		doStopSearch = false;
		searchStats = new SearchStats();
		slaveThreads = null;
		alpha = Score.MIN.value;
		beta = Score.MAX.value;
		masterThread = new SearchThread(rootPosition, null);
		// Zero-depth mode.
		if (maxNominalDepth == 0) {
			masterThread.setPly((short) 0);
			masterThread.setBounds(alpha, beta);
			return new SearchResults(null, null, (short) masterThread.call().intValue(), ScoreType.EXACT);
		}
		rootMoves = areMovesRestricted ? allowedRootMoves : rootPosition.getMoves();
		movesToNodes = new HashMap<>();
		for (Move m : rootMoves)
			movesToNodes.put(m, new AtomicLong(0));
		if (numOfHelperThreads > 0) {
			slaveThreads = new ArrayList<>();
			for (int i = 0; i < numOfHelperThreads; i++)
				slaveThreads.add(new SearchThread(rootPosition.deepCopy(), masterThread));
		}
		// The number of consecutive fail-highs/fail-lows.
		failHigh = failLow = 0;
		// Iterative deepening.
		for (ply = INITIAL_DEPTH;; ply++) {
			// Sort moves...
			if (ply == INITIAL_DEPTH) // First iteration based on SEE.
				rootMoves = new ArrayList<>(Arrays.asList(masterThread.orderMaterialMovesSEE(rootPosition, rootMoves)));
			else // Subsequent iterations based on cumulative subtree sizes.
				rootMoves.sort((m1, m2) -> (int) (movesToNodes.get(m2).get() - movesToNodes.get(m1).get()));
			masterThread.setPly(ply);
			masterThread.setBounds(alpha, beta);
			// Launch helper threads.
			if (numOfHelperThreads > 0 && ply != INITIAL_DEPTH) {
				latch = new CountDownLatch(numOfHelperThreads);
				odd = true;
				for (int j = 0; j < slaveThreads.size(); j++, odd = !odd) {
					slaveThread = slaveThreads.get(j);
					slaveThread.setPly((short) (odd && ply < maxNominalDepth ? ply + 1 : ply));
					slaveThread.setBounds(alpha, beta);
					executor.submit(slaveThread);
				}
			}
			// Launch the master thread.
			score = masterThread.call();
			// Interrupt the helpers thread and wait for them to terminate.
			if (numOfHelperThreads > 0 && ply != INITIAL_DEPTH) {
				for (SearchThread t : slaveThreads)
					t.stop();
				try {
					latch.await();
				} catch (InterruptedException e) {
					doStopSearch = true;
				}
			}
			// Let the engine know that the search at the current ply has completed.
			synchronized (this) {
				notifyAll();
			}
			if (doStopSearch || ply == maxNominalDepth || score == Score.NULL.value)
				break;
			// Aspiration windows with gradual widening.
			if (score <= alpha) {
				if (score <= lCheckMateLimit) {
					alpha = Score.MIN.value;
					failLow = 2;
				} else {
					alpha = failLow <= 1 ? Math.max(score - params.aspirationDelta, Score.MIN.value) :
							Score.MIN.value;
					failLow++;
				}
				ply--;
			} else if (score >= beta) {
				if (score >= wCheckMateLimit) {
					beta = Score.MAX.value;
					failHigh = 2;
				} else {
					beta = failHigh <= 1 ? Math.min(score + params.aspirationDelta, Score.MAX.value) :
							Score.MAX.value;
					failHigh++;
				}
				ply--;
			} else {
				alpha = Math.max(score - params.aspirationDelta, Score.MIN.value);
				beta = Math.min(score + params.aspirationDelta, Score.MAX.value);
				failHigh = failLow = 0;
			}
		}
		bestMove = ponderMove = null;
		entry = tT.get(rootPosition.key);
		if (entry != null)
			score = entry.score;
		pV = extractPv(rootPosition, 2);
		if (pV != null && !pV.isEmpty()) {
			bestMove = pV.get(0);
			if (pV.size() > 1)
				ponderMove = pV.get(1);
		}
		adjustedScore = adjustScore(score, alpha, beta);
		if (adjustedScore == null) {
			finalScore = null;
			scoreType = null;
		} else {
			finalScore = adjustedScore.getKey();
			scoreType = adjustedScore.getValue();
		}
		updateInfo(rootPosition, null, 0, ply, alpha, beta, score);
		return new SearchResults(bestMove == null ? null : bestMove.toString(), ponderMove == null ? null :
				ponderMove.toString(), finalScore, scoreType);
	}
	@Override
	public synchronized SearchResults get() throws InterruptedException {
		while (!isDone) {
			wait();
		}
		return results;
	}
	@Override
	public synchronized SearchResults get(long timeout, TimeUnit unit) throws InterruptedException {
		long timeoutMs = unit.toMillis(timeout);
		while (!isDone && timeoutMs > 0) {
			long start = System.currentTimeMillis();
			wait(timeoutMs);
			timeoutMs -= (System.currentTimeMillis() - start);
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
		synchronized (this) {
			isDone = false;
		}
		doStopSearch = false;
		if (numOfHelperThreads > 0)
			executor = Executors.newFixedThreadPool(numOfHelperThreads);
		results = iterativeDeepening();
		startTime = null;
		if (executor != null)
			executor.shutdown();
		doStopSearch = false;
		synchronized (this) {
			isDone = true;
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

		/**
		 * The score to return when a node is already being searched by another thread.
		 */
		private static final int BUSY_SCORE = -2*Short.MAX_VALUE;
		
		private final Position0 origPosition; // The original position to search.
		private final SearchThread master;
		private final boolean isMainSearchThread;
		private final KillerTable kT;
		private final RelativeHistoryTable hT;
		private final TTEntry tTentry;
		private final ETEntry eTentry;
		private Position0 position; // The position instance to use for the search.
		private AtomicLong nodes;
		private int alpha;
		private int beta;
		private short ply;
		private volatile short result;
		private volatile boolean doStopSearchThread;
		
		/**
		 * Constructs an instance using the specified arguments. If the parameter master is null, 
		 * the search thread will be constructed as a master search thread itself.
		 * 
		 * @param position
		 * @param master
		 */
		SearchThread(Position0 position, SearchThread master) {
			origPosition = position;
			this.master = master;
			this.isMainSearchThread = master == null;
			if (maxNominalDepth > 0) {
				kT = new KillerTable(maxNominalDepth*2);
				hT = new RelativeHistoryTable();
			} else {
				kT = null;
				hT = null;
			}
			tTentry = new TTEntry();
			eTentry = new ETEntry();
		}
		/**
		 * Sets the depth to which the position is to be searched.
		 * 
		 * @param ply
		 */
		void setPly(short ply) {
			this.ply = ply;
		}
		/**
		 * Sets the result of the search thread.
		 * 
		 * @param result The result score to return.
		 */
		void setResult(short result) {
			this.result = result;
		}
		/**
		 * Sets the alpha-beta bounds of the search thread.
		 * 
		 * @param alpha The new alpha bound.
		 * @param beta The new beta bound.
		 */
		void setBounds(int alpha, int beta) {
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
		private Move[] orderMaterialMovesSEE(Position0 pos, List<Move> moves) {
			Move[] arr = new Move[moves.size()];
			int i = 0;
			for (Move move : moves) {
				move.value = eval.SEE(pos, move); // Static exchange evaluation.
				arr[i++] = move;
			}
			return QuickSort.sort(arr);
		}
		/**
		 * Orders captures and promotions according to the MVV-LVA principle.
		 * 
		 * @param moves
		 * @return
		 */
		private Move[] orderMaterialMovesMVVLVA(List<Move> moves) {
			Move[] arr = new Move[moves.size()];
			int i = 0;
			for (Move move : moves) {
				move.value = eval.MVVLVA(move);
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
		 * Tries to insert an entry into one of the transposition tables based on the specified parameters.
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
		private boolean insertIntoTt(long key, int alpha, int beta, Move bestMove, int bestScore, short distFromRoot,
				short depth) {
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
			// Add new entry to the transposition table.
			// First try the primary table.
			tTentry.set(key, depth, type, (short) score, bestMoveInt, hashEntryGen, false);
			tTentry.setupKey();
			return tT.put(tTentry);
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
			List<Move> moves;
			Move[] sortedMoves;
			int bestScore, searchScore, moveValue;
			int mateValue = Score.LOSING_CHECK_MATE.value + distFromRoot;
			boolean inCheck = position.inCheck;
			if (!ponder && getTotalNodes() >= maxNodes)
				doStopSearch = true;
			if (doStopSearch || doStopSearchThread)
				throw new AbnormalSearchTerminationException();
			if (nodes != null)
				nodes.incrementAndGet();
			searchStats.quiescenceNodes.incrementAndGet();
			// Fifty-move rule and repetition rule check.
			if (position.fiftyMoveRuleClock >= 100 || position.hasRepeated(distFromRoot > 2 ? 1 : 2))
				return Score.DRAW_CLAIMED.value;
			// Evaluate the position statically.
			bestScore = inCheck ? mateValue : eval.score(position, hashEntryGen, alpha, beta, eTentry);
			// Fail soft.
			if (bestScore > alpha) {
				alpha = bestScore;
				if (bestScore >= beta)
					return bestScore;
			}
			// Limit the maximum depth of the quiescence search.
			if (distFromRoot > maxExpectedSearchDepth)
				return alpha;
			// Generate all the material moves or if in check, all moves.
			moves = inCheck ? position.getMoves() : position.getTacticalMoves();
			sortedMoves = orderMaterialMovesMVVLVA(moves);
			for (Move move : sortedMoves) {
				if (!inCheck) {
					// If the SEE value is below 0 or the delta pruning limit, skip.
					moveValue = eval.SEE(position, move);
					if (moveValue < 0 || moveValue <= alpha - params.deltaPruningMargin)
						continue;
				}
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
		 * @param exclusive
		 * @return
		 * @throws AbnormalSearchTerminationException
		 */
		private int pVsearch(int depth, int distFromRoot, int alpha, int beta, boolean nullMoveAllowed, boolean exclusive)
				throws AbnormalSearchTerminationException {
			// Do not allow negative depths for the full effect of check extensions.
			depth = Math.max(depth, 0);
			final int mateScore = Score.LOSING_CHECK_MATE.value + distFromRoot;
			final int origAlpha = alpha;
			final int depthLimit = distFromRoot >= maxNominalDepth ? depth : depth + params.fullPly;
			final boolean inCheck = position.inCheck;
			final boolean pvNode = beta > origAlpha + 1;
			final long posKey = position.key;
			int hashDepth, hashType, hashScore, hashMoveInt;
			int bestScore, score, evalScore;
			int nullMoveReduction, lateMoveReduction, extension, searchDepth;
			int numOfPieces;
			int searchedMoves;
			int killerMoveInt;
			int razMargin, futMargin;
			Move hashMove, bestMove, killerMove, killerMove1, killerMove2, move, lastMove;
			boolean hashBusy, nodeBlocked, doQuiescence;
			boolean isThereHashMove, isThereKM1, isThereKM2;
			boolean lastMoveIsTactical, pawnPushed;
			boolean dangerous, prunable;
			boolean isMaterial, isKiller;
			List<Move> tacticalMoves, quietMoves, losingCaptures, deferredMoves;
			Move[] tacticalMovesArr, quietMovesArr, losingCapturesArr;
			TTEntry hashEntry;
			KTEntry killerEntry;
			bestScore = mateScore;
			bestMove = null;
			searchedMoves = 0;
			hashMove = killerMove1 = killerMove2 = null;
			nodeBlocked = false;
			isThereHashMove = isThereKM1 = isThereKM2 = false;
			tacticalMoves = quietMoves = null;
			killerEntry = null;
			if (!ponder && getTotalNodes() >= maxNodes)
				doStopSearch = true;
			if (doStopSearch || doStopSearchThread)
				throw new AbnormalSearchTerminationException();
			nodes.incrementAndGet();
			searchStats.mainNodes.incrementAndGet();
			if (distFromRoot > selDepth)
				selDepth = distFromRoot;
			Search: {
				// Check for the repetition rule; return a draw score if it applies.
				if (position.hasRepeated(distFromRoot > 2 ? 1 : 2))
					return Score.DRAW_CLAIMED.value;
				// Mate distance pruning.
				if (-mateScore < beta && alpha >= -mateScore)
					return -mateScore;
				if (mateScore > alpha && beta <= mateScore)
					return mateScore;
				// Probe endgame tablebases if applicable.
				if (useEgtb && distFromRoot <= params.maxDistFromRootForEgtbProbe) {
					numOfPieces = position.getNumberOfPieces();
					// Check if there are tablebases loaded for the current number of pieces on the board.
					if (numOfPieces <= EndGameTableBase.MAX_NUMBER_OF_PIECES && availableEgtbs.contains(numOfPieces)) {
						DTM dtm = egtb.probeDTM(position, distFromRoot > params.maxDistFromRootForHardEgtbProbe);
						// If the position is found...
						if (dtm != null && dtm.getWdl() != null) {
							// Return the appropriate score.
							switch (dtm.getWdl()) {
								case DRAW:
									return Score.STALE_MATE.value;
								case LOSS:
									return mateScore + dtm.getDistance();
								case WIN:
									return -mateScore - dtm.getDistance();
								default:
									break;
							}
						}
					}
				}
				// Check extension.
				depth = inCheck ? Math.min(depthLimit, depth + params.checkExtension) : depth;
				// Pawn push extension
				lastMove = position.getLastMove();
				pawnPushed = isPawnPush(lastMove);
				depth = pawnPushed ? Math.min(depthLimit, depth + params.pawnPushExtension) : depth;
				// Check the conditions for quiescence search.
				doQuiescence = depth/params.fullPly <= 0;
				// Check the hash move and return its score for the position if it is exact or set alpha or beta according to its score if it is not.
				hashEntry = tT.get(posKey);
				if (hashEntry != null) {
					hashDepth = hashEntry.depth;
					hashType = hashEntry.type;
					hashScore = hashEntry.score;
					hashMoveInt = hashEntry.bestMove;
					hashBusy = hashEntry.busy;
					// Make sure that entry was not overwritten in another thread.
					if (hashEntry.hashKey() == posKey) {
						hashEntry.generation = hashEntryGen;
						searchStats.hashHits.incrementAndGet();
						// If the hashed entry's depth is greater than or equal to the current search depth, check if the stored score is usable.
						if (hashDepth >= depth/params.fullPly && hashScore != Score.NULL.value) {
							// Mate score adjustment to root distance.
							if (hashScore <= lCheckMateLimit)
								score = hashScore + distFromRoot;
							else if (hashEntry.score >= wCheckMateLimit)
								score = hashScore - distFromRoot;
							else
								score = hashScore;
							/* If the score was exact, or it was the score of an all node and is smaller than or equal to alpha, or it is that 
							 * of a cut node and is greater than or equal to beta, return the score. If it is a PV node and the score is exact, 
							 * make sure that either the score is outside the bounds or the search is about to drop into quiescence search 
							 * to avoid a PV cut. */
							if ((hashType == NodeType.EXACT.ind && (!pvNode || doQuiescence || hashScore <= alpha || hashScore >= beta)) ||
									/* To make sure that a score that might not have been the exact score for the subtree below the node regardless 
									 * of the alpha-beta boundaries is not treated as an exact score in the current context, we can not allow it to 
									 * fall between the current alpha and beta. If it was a fail high node, the score is a lower boundary of the 
									 * exact score of the node due to there possibly being siblings to the right of the child node [that raised 
									 * alpha higher than beta and caused a cut-off] that could raise alpha even higher. If it was a fail low node, 
									 * the score is a higher boundary for the exact score of the node, because all children of a fail low node are 
									 * fail high nodes (-score <= alpha -> score >= -alpha [-alpha = beta in the child node]). To keep the interval 
									 * of values the exact score could take on out of (alpha, beta), the score has to be lower than or equal to 
									 * alpha if it is a higher boundary, i.e. fail low score, and it has to be greater than or equal to beta if it 
									 * is a lower boundary i.e. fail high score. */
									(hashType == NodeType.FAIL_HIGH.ind && score >= beta) ||
									(hashType == NodeType.FAIL_LOW.ind && score <= alpha)) {
								searchStats.hashScoreHits.incrementAndGet();
								return score;
							}
						}
						// Check if the node should be put to the end of the list in the current thread due to another one already searching it.
						if (numOfHelperThreads > 0 && depth/params.fullPly >= params.nodeBusinessCheckMinDepthLeft) {
							if (exclusive && hashBusy) {
								searchStats.busyNodes.incrementAndGet();
								return BUSY_SCORE;
							}
							// If it is still the right entry, set it to busy.
							if (hashEntry.hashKey() == posKey) {
								hashEntry.busy = true;
								nodeBlocked = true;
							}
						}
						// Check for the stored move and make it the best guess if it is not null and the node is not fail low.
						if (hashMoveInt != 0 && !doQuiescence) {
							hashMove = Move.toMove(hashMoveInt);
							isThereHashMove = position.isLegalSoft(hashMove);
						}
					}
				}
				// Perform quiescence search.
				if (doQuiescence) {
					nodes.decrementAndGet();
					searchStats.mainNodes.decrementAndGet();
					bestScore = quiescence(distFromRoot, alpha, beta);
					bestMove = null;
					break Search;
				}
				score = evalScore = Score.NULL.value;
				// Assess if the position is 'safe'...
				dangerous = inCheck || pawnPushed;
				if (!pvNode && !dangerous && Math.abs(beta) < wCheckMateLimit && position.areTherePiecesOtherThanKingsAndPawns()) {
					// Try reverse futility pruning or 'static null move pruning' if the conditions are met.
					if (depth/params.fullPly <= 3 && params.doRazor) {
						switch (depth/params.fullPly) {
							case 1: // Reverse pre-frontier futility pruning.
								razMargin = params.revFutitilityMargin1;
								break;
							case 2: // Reverse limited futility pruning.
								razMargin = params.revFutitilityMargin2;
								break;
							case 3: // Reverse deep futility pruning.
								razMargin = params.revFutitilityMargin3;
								break;
							default:
								razMargin = 0;
						}
						if (evalScore == Score.NULL.value)
							evalScore = eval.score(position, hashEntryGen, alpha, beta, eTentry);
						if (evalScore - razMargin >= beta) {
							searchStats.reverseRazorCutoffs.incrementAndGet();
							// Reset the busy flag.
							if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
								hashEntry.busy = false;
							return evalScore - razMargin;
						}
					}
					// Try null move pruning if the conditions are met.
					if (nullMoveAllowed && depth/ply >= params.nullMoveReductionMinDepthLeft) {
						if (evalScore == Score.NULL.value)
							evalScore = eval.score(position, hashEntryGen, alpha, beta, eTentry);
						if (evalScore > alpha) {
							searchStats.nullMoveReductions.incrementAndGet();
							// Dynamic depth reduction.
							nullMoveReduction = params.nullMoveReduction*params.fullPly +
									params.extraNullMoveReduction*depth/params.extraNullMoveReductionDepthLimit;
							position.makeNullMove();
							/* Use separate, tight-scoped try-blocks for the parts where exceptions may be thrown to allow the compiler to 
							 * make as many optimizations as possible. */
							try {
								// Do not allow consecutive null moves.
								score = -pVsearch(depth - (params.fullPly + nullMoveReduction), distFromRoot + 1, -beta, -beta + 1, false, false);
							} catch (AbnormalSearchTerminationException e) {
								// If an exception is thrown, make sure that the busy flag is set to false in the TT.
								if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
									hashEntry.busy = false;
								throw e;
							}
							position.unmakeMove();
							if (score >= beta) {
								searchStats.nullMoveCutoffs.incrementAndGet();
								if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
									hashEntry.busy = false;
								return score;
							}
						}
					}
				}
				// If there is no hash entry in a PV node that is to be searched deep, try IID.
				if (params.doIid && pvNode && !isThereHashMove && depth/params.fullPly >= params.iidMinDepthLeft) {
					searchStats.iids.incrementAndGet();
					try {
						pVsearch(depth*params.iidRelDepth64th/64, distFromRoot, alpha, beta, true, false);
					} catch (AbnormalSearchTerminationException e) {
						// Reset the busy flag.
						if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
							hashEntry.busy = false;
						throw e;
					}
					hashEntry = tT.get(position.key);
					if (hashEntry != null && (hashMoveInt = hashEntry.bestMove) != 0 && hashEntry.hashKey() == posKey) {
						searchStats.successfulIids.incrementAndGet();
						hashMove = Move.toMove(hashMoveInt);
						isThereHashMove = position.isLegalSoft(hashMove);
					}
				}
				// If there was no hash move...
				if (!isThereHashMove) {
					// Generate material moves.
					tacticalMoves = position.getTacticalMoves();
					// If there were no material moves, check killer moves...
					if (tacticalMoves.size() == 0) {
						killerEntry = kT.retrieve(distFromRoot);
						if (killerEntry.getMove1() != 0 && position.isLegalSoft(killerMove1 = Move.toMove(killerEntry.getMove1())))
							isThereKM1 = true;
						else if (killerEntry.getMove2() != 0 && position.isLegalSoft(killerMove2 = Move.toMove(killerEntry.getMove2())))
							isThereKM2 = true;
						// If there were no legal killer moves, generate the quiet moves and perform a mate check.
						if (!isThereKM1 && !isThereKM2) {
							quietMoves = position.getQuietMoves();
							if (quietMoves.size() == 0) {
								score = inCheck ? mateScore : Score.STALE_MATE.value;
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
				if (position.fiftyMoveRuleClock >= 100) {
					// Reset the busy flag.
					if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
						hashEntry.busy = false;
					return Score.DRAW_CLAIMED.value;
				}
				// Check if a recapture extension could possibly be applied.
				lastMove = position.getLastMove();
				lastMoveIsTactical = lastMove != null && lastMove.isTactical();
				// If there is a hash move, search that first.
				if (isThereHashMove) {
					// Recapture extension (includes capturing newly promoted pieces).
					extension = lastMoveIsTactical && hashMove.capturedPiece != Piece.NULL.ind && hashMove.to == lastMove.to ?
							params.recapExtension : 0;
					position.makeMove(hashMove);
					try {
						score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, distFromRoot + 1, -beta, -alpha, true, false);
					} catch (AbnormalSearchTerminationException e) {
						if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
							hashEntry.busy = false;
						throw e;
					}
					position.unmakeMove();
					searchedMoves++;
					if (score > bestScore) {
						bestMove = hashMove;
						bestScore = score;
						if (score > alpha) {
							alpha = score;
							if (score >= beta) {
								searchStats.hashMoveCutoffs.incrementAndGet();
								if (!hashMove.isTactical()) {
									// Add to killer moves.
									kT.add(distFromRoot, hashMove);
									// Record success in the relative history table.
									hT.recordSuccessfulMove(hashMove);
								}
								break Search;
							}
						}
					}
					if (!hashMove.isTactical())
						// Record failure in the relative history table.
						hT.recordUnsuccessfulMove(hashMove);
				}
				// Generate the material moves if they are not generated yet.
				if (tacticalMoves == null)
					tacticalMoves = position.getTacticalMoves();
				losingCaptures = deferredMoves = null;
				// Sort the material moves.
				tacticalMovesArr = orderMaterialMovesMVVLVA(tacticalMoves);
				// Search winning and equal captures.
				for (int i = 0; i < tacticalMovesArr.length; i++) {
					move = tacticalMovesArr[i];
					// If this move was the hash move, skip it.
					if (isThereHashMove && move.equals(hashMove)) {
						isThereHashMove = false;
						continue;
					}
					// If the current move's SEE value indicates a losing capture, put it in the losing captures list.
					move.value = eval.SEE(position, move);
					if (move.value < 0) {
						if (losingCaptures == null)
							losingCaptures = new ArrayList<>();
						losingCaptures.add(move);
						continue;
					}
					// Recapture extension (includes capturing newly promoted pieces).
					extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
							params.recapExtension : 0;
					searchDepth = Math.min(depthLimit, depth + extension) - params.fullPly;
					position.makeMove(move);
					try {
						// PVS.
						if (searchedMoves == 0)
							score = -pVsearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
						else {
							score = -pVsearch(searchDepth, distFromRoot + 1, -alpha - 1, -alpha, true, true);
							/* If the null-window search fails, the research is not exclusive to allow other threads search the 
							 * line as it is likely to be the best move. */
							if (score != -BUSY_SCORE && score > alpha && score < beta)
								score = -pVsearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
						}
					} catch (AbnormalSearchTerminationException e) {
						if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
							hashEntry.busy = false;
						throw e;
					}
					position.unmakeMove();
					// If the position is currently searched by another thread, add it to the list of moves to search later.
					if (score == -BUSY_SCORE) {
						if (deferredMoves == null)
							deferredMoves = new ArrayList<>();
						deferredMoves.add(move);
					} else { // Do the usual PVS routine.
						searchedMoves++;
						if (score > bestScore) {
							bestMove = move;
							bestScore = score;
							if (score > alpha) {
								alpha = score;
								if (score >= beta) {
									searchStats.winningTacticalCutoffs.incrementAndGet();
									break Search;
								}
							}
						}
					}
				}
				// If there are no more winning or equal captures, check and search the killer moves.
				killerEntry = killerEntry == null ? kT.retrieve(distFromRoot) : killerEntry;
				if (killerEntry != null) {
					for (int i = 0; i < 2; i++) {
						// Assess if the first killer moves is legal.
						if (i == 0) {
							// If the killer move has not yet been verified during the mate check and the entry is not empty.
							if (!isThereKM1 && killerMove1 == null && (killerMoveInt = killerEntry.getMove1()) != 0) {
								killerMove1 = Move.toMove(killerMoveInt);
								if (isThereHashMove && killerMove1.equals(hashMove))
									isThereHashMove = false;
								else
									isThereKM1 = position.isLegalSoft(killerMove1);
							}
							// The killer move is not legal.
							if (!isThereKM1)
								continue;
							killerMove = killerMove1;
						} else { // Assess the legality of the second killer move.
							if (!isThereKM2 && killerMove2 == null && (killerMoveInt = killerEntry.getMove2()) != 0) {
								killerMove2 = Move.toMove(killerMoveInt);
								if (isThereHashMove && killerMove2.equals(hashMove))
									isThereHashMove = false;
								else
									isThereKM2 = position.isLegalSoft(killerMove2);
							}
							if (!isThereKM2)
								continue;
							killerMove = killerMove2;
						}
						position.makeMove(killerMove);
						try {
							// Full window PVS.
							if (searchedMoves == 0)
								score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true, false);
							// Null-window PVS.
							else {
								score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true, true);
								if (score != -BUSY_SCORE && score > alpha && score < beta)
									score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true, false);
							}
						} catch (AbnormalSearchTerminationException e) {
							if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
								hashEntry.busy = false;
							throw e;
						}
						position.unmakeMove();
						if (score == -BUSY_SCORE) {
							if (deferredMoves == null)
								deferredMoves = new ArrayList<>();
							deferredMoves.add(killerMove);
						} else {
							searchedMoves++;
							if (score > bestScore) {
								bestMove = killerMove;
								bestScore = score;
								if (score > alpha) {
									alpha = score;
									if (score >= beta) {
										searchStats.killerCutoffs.incrementAndGet();
										if (i == 1) // Make it killer move no. 1.
											kT.add(distFromRoot, killerMove);
										// Record success in the relative history table.
										hT.recordSuccessfulMove(killerMove);
										break Search;
									}
								}
							}
							// Record failure in the relative history table.
							hT.recordUnsuccessfulMove(killerMove);
						}
					}
				}
				// If there are any losing captures...
				if (losingCaptures != null) {
					// Sort them based on their already assigned SEE scores.
					losingCapturesArr = QuickSort.sort(losingCaptures.toArray(new Move[losingCaptures.size()]));
					// And search them.
					for (int i = 0; i < losingCapturesArr.length; i++) {
						move = losingCapturesArr[i];
						// No need to check if it is the hash move as that was done before it was added to the losing captures.
						// Recapture extension.
						extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
								params.recapExtension : 0;
						searchDepth = Math.min(depthLimit, depth + extension) - params.fullPly;
						position.makeMove(move);
						try {
							// Left-most move.
							if (searchedMoves == 0)
								score = -pVsearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
							 // Null-window PVS.
							else {
								score = -pVsearch(searchDepth, distFromRoot + 1, -alpha - 1, -alpha, true, true);
								if (score != -BUSY_SCORE && score > alpha && score < beta)
									score = -pVsearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
							}
						} catch (AbnormalSearchTerminationException e) {
							if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
								hashEntry.busy = false;
							throw e;
						}
						position.unmakeMove();
						if (score == -BUSY_SCORE) {
							if (deferredMoves == null)
								deferredMoves = new ArrayList<>();
							deferredMoves.add(move);
						} else {
							searchedMoves++;
							if (score > bestScore) {
								bestMove = move;
								bestScore = score;
								if (score > alpha) {
									alpha = score;
									if (score >= beta) {
										searchStats.losingTacticalCutoffs.incrementAndGet();
										break Search;
									}
								}
							}
						}
					}
				}
				// Generate the non-material legal moves if they are not generated yet.
				if (quietMoves == null)
					quietMoves = position.getQuietMoves();
				// One reply extension.
				if (tacticalMoves.size() == 0 && quietMoves.size() == 1) {
					dangerous = true;
					depth = Math.min(depthLimit, depth + params.singleReplyExtension);
				}
				lateMoveReduction = params.lateMoveReduction*params.fullPly +
						params.extraLateMoveReduction*depth/params.extraLateMoveReductionDepthLimit;
				prunable = !pvNode && !dangerous && Math.abs(alpha) < wCheckMateLimit;
				// Futility pruning margin calculation.
				if (prunable) {
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
							break;
						default:
							futMargin = 0;
					}
				} else
					futMargin = 0;
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
					// Futility pruning.
					if (prunable && depth/params.fullPly <= 5 && !position.givesCheck(move)) {
						if (evalScore == Score.NULL.value)
							evalScore = eval.score(position, hashEntryGen, alpha, beta, eTentry);
						if (evalScore <= alpha - futMargin) {
							searchStats.futilityPrunes.incrementAndGet();
							// Record failure in the relative history table.
							hT.recordUnsuccessfulMove(move);
							continue;
						}
					}
					position.makeMove(move);
					try {
						// Left-most move.
						if (searchedMoves == 0)
							score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true, false);
						// Try late move reduction.
						else if (!dangerous && !position.inCheck && searchedMoves > params.minMovesSearchedForLmr &&
								depth/params.fullPly >= params.lateMoveReductionMinDepthLeft) {
							score = -pVsearch(depth - (params.fullPly + lateMoveReduction), distFromRoot + 1, -alpha - 1, -alpha, true, true);
							if (score != -BUSY_SCORE) {
								searchStats.lateMoveReductions.incrementAndGet();
								// If it does not fail low, research with full window.
								if (score > alpha)
									score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true, false);
								else
									searchStats.successfulLateMoveReductions.incrementAndGet();
							}
						} else { // Null-window PVS.
							score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -alpha - 1, -alpha, true, true);
							if (score != -BUSY_SCORE && score > alpha && score < beta) // Full-window research.
								score = -pVsearch(depth - params.fullPly, distFromRoot + 1, -beta, -alpha, true, false);
						}
					} catch (AbnormalSearchTerminationException e) {
						if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
							hashEntry.busy = false;
						throw e;
					}
					position.unmakeMove();
					if (score == -BUSY_SCORE) {
						if (deferredMoves == null)
							deferredMoves = new ArrayList<>();
						deferredMoves.add(move);
					} else {
						searchedMoves++;
						if (score > bestScore) {
							bestMove = move;
							bestScore = score;
							if (score > alpha) {
								alpha = score;
								// Cutoff from a non-material move.
								if (score >= beta) {
									searchStats.quietCutoffs.incrementAndGet();
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
				// If moves searched by other threads were rescheduled, search them now.
				if (deferredMoves != null) {
					for (Move deferredMove : deferredMoves) {
						isMaterial = deferredMove.isTactical();
						if (isMaterial) {
							isKiller = false;
							// Recapture extension.
							extension = isMaterial && lastMoveIsTactical && deferredMove.capturedPiece != Piece.NULL.ind &&
									deferredMove.to == lastMove.to ? params.recapExtension : 0;
							searchDepth = Math.min(depthLimit, depth + extension) - params.fullPly;
						} else {
							// Check if it is a killer move.
							isKiller = true;
							if (isThereKM1 && deferredMove.equals(killerMove1))
								isThereKM1 = false;
							else if (isThereKM2 && deferredMove.equals(killerMove2))
								isThereKM2 = false;
							else
								isKiller = false;
							searchDepth = depth - params.fullPly;
								
						}
						position.makeMove(deferredMove);
						try {
							// LMR if possible.
							if (!isMaterial && !isKiller && !dangerous && !position.inCheck && searchedMoves > params.minMovesSearchedForLmr &&
									depth/params.fullPly >= params.lateMoveReductionMinDepthLeft) {
								score = -pVsearch(searchDepth - lateMoveReduction, distFromRoot + 1, -alpha - 1, -alpha, true, false);
								searchStats.lateMoveReductions.incrementAndGet();
								// If it does not fail low, research with full window.
								if (score > alpha)
									score = -pVsearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
								else
									searchStats.successfulLateMoveReductions.incrementAndGet();
							} else { // PVS.
								score = -pVsearch(searchDepth, distFromRoot + 1, -alpha - 1, -alpha, true, false);
								if (score > alpha && score < beta)
									score = -pVsearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
							}
						} catch (AbnormalSearchTerminationException e) {
							if (nodeBlocked && (hashEntry = tT.get(posKey)) != null)
								hashEntry.busy = false;
							throw e;
						}
						position.unmakeMove();
						searchStats.delayedNodes.incrementAndGet();
						searchedMoves++;
						if (score > bestScore) {
							bestMove = deferredMove;
							bestScore = score;
							if (score > alpha) {
								alpha = score;
								// Cutoff from a non-material move.
								if (score >= beta) {
									if (!isMaterial) {
										(isKiller ? searchStats.killerCutoffs : searchStats.quietCutoffs).incrementAndGet();
										// Add to killer moves.
										kT.add(distFromRoot, deferredMove);
										// Record success in the relative history table.
										hT.recordSuccessfulMove(deferredMove);
									} else
										(deferredMove.value >= 0 ? searchStats.winningTacticalCutoffs :
												searchStats.losingTacticalCutoffs).incrementAndGet();
									break Search;
								}
							}
						}
						if (!isMaterial) // Record failure in the relative history table.
							hT.recordUnsuccessfulMove(deferredMove);
					}
				}
			}
			// Add new entry to the transposition table.
			if (!insertIntoTt(posKey, origAlpha, beta, bestMove, bestScore, (short) distFromRoot,
					(short) (depth/params.fullPly)) &&
					// If it is not good enough, make sure to reset the busy flag if it was set.
					nodeBlocked && (hashEntry = tT.get(posKey)) != null)
				hashEntry.busy = false;
			// Return the unadjusted best score.
			return bestScore;
		}
		@Override
		public Integer call() {
			int depth = ply*params.fullPly;
			final int depthLimit = depth + params.fullPly;
			final int origAlpha = alpha;
			int alpha = this.alpha;
			int beta = this.beta;
			int hashDepth, hashType, hashScore, hashMoveInt;
			int moveInd, searchedMoves;
			int score, bestScore, extension;
			Move bestMove, hashMove, move, lastMove;
			boolean infoUpdated, lastMoveIsMaterial;
			boolean[] searched;
			List<Move> moves;
			TTEntry entry;
			bestScore = Score.MIN.value;
			bestMove = hashMove = null;
			infoUpdated = false;
			try {
				doStopSearchThread = false;
				position = origPosition.deepCopy();
				// If ply equals 0, perform quiescence search only.
				if (ply == 0)
					return quiescence(0, alpha, beta);
				searchStats.mainNodes.incrementAndGet();
				// Check for the 3-fold repetition rule.
				if (position.hasRepeated(2))
					return (int) Score.DRAW_CLAIMED.value;
				// Generate moves.
				moves = new ArrayList<>(rootMoves);
				// Mate check.
				if (moves.isEmpty())
					return (int) (position.inCheck ? Score.LOSING_CHECK_MATE.value : Score.STALE_MATE.value);
				// Check for the 50 move rule.
				if (position.fiftyMoveRuleClock >= 100)
					return (int) Score.DRAW_CLAIMED.value;
				// In non-analysis mode, terminate prematurely if there is only one legal response at the root.
				if (!analysisMode && !ponder && moves.size() == 1) {
					bestScore = Score.NULL.value;
					synchronized (rootLock) {
						insertIntoTt(position.key, origAlpha, beta, moves.get(0), bestScore, (short) 0, (short) (depth/params.fullPly));
					}
					return bestScore;
				}
				// Check extension.
				depth = position.inCheck ? Math.min(depthLimit, depth + params.checkExtension) : depth;
				// Pawn push extension
				lastMove = position.getLastMove();
				depth = lastMove != null && isPawnPush(lastMove) ? Math.min(depthLimit, depth + params.pawnPushExtension) : depth;
				// Hash look-up.
				entry = tT.get(position.key);
				if (entry != null) {
					hashDepth = entry.depth;
					hashType = entry.type;
					hashScore = entry.score;
					hashMoveInt = entry.bestMove;
					// Make sure the entry was not modified while retrieving the information contained in it.
					if (entry.hashKey() == position.key) {
						entry.generation = hashEntryGen;
						searchStats.hashHits.incrementAndGet();
						// If the hashed entry's depth is greater than or equal to the current search depth, check if the stored score is usable.
						if (hashDepth >= depth/params.fullPly && hashScore != Score.NULL.value) {
							/* If the score was exact, or it was the score of an all node and is smaller than or equal to alpha, or it is that of a 
							 * cut node and is greater than or equal to beta, return the score. Only take an exact score if is outside the bounds to 
							 * avoid the truncation of the PV line. */
							if ((hashType == NodeType.EXACT.ind && (hashScore <= alpha || hashScore >= beta)) ||
									(hashType == NodeType.FAIL_HIGH.ind && hashScore >= beta) || (hashType == NodeType.FAIL_LOW.ind &&
											hashScore <= alpha)) {
								if (hashDepth > selDepth)
									selDepth = hashDepth;
								bestScore = hashScore;
								searchStats.hashScoreHits.incrementAndGet();
								return bestScore;
							}
						}
						if (hashMoveInt != 0)
							hashMove = Move.toMove(hashMoveInt);
					}
				}
				// If there is a hash move, make sure it is the first element.
				if (hashMove != null && moves.contains(hashMove)) {
					moves.remove(hashMove);
					moves.add(0, hashMove);
				}
				lastMoveIsMaterial = lastMove != null && lastMove.isTactical();
				searched = new boolean[moves.size()];
				searchedMoves = 0;
				RootSearch: for (int i = 0; i < 2; i++) {
					// Iterate over moves.
					for (moveInd = 0; moveInd < moves.size(); moveInd++) {
						if (searched[moveInd])
							continue;
						move = moves.get(moveInd);
						nodes = movesToNodes.get(move);
						// Recapture extension.
						extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
								params.recapExtension : 0;
						position.makeMove(move);
						// Full window search for the first move...
						if (i == 0 && moveInd == 0)
							score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, 1, -beta, -alpha, true, false);
						// PVS for the rest.
						else {
							score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, 1, -alpha - 1, -alpha, true, i == 0);
							if (score != -BUSY_SCORE && score > alpha && score < beta)
								score = -pVsearch(Math.min(depthLimit, depth + extension) - params.fullPly, 1, -beta, -alpha, true, false);
						}
						position.unmakeMove();
						if (score == -BUSY_SCORE)
							continue;
						if (i == 0)
							searched[moveInd] = true;
						else
							searchStats.delayedNodes.incrementAndGet();
						searchedMoves++;
						// Score check.
						if (score > bestScore) {
							bestScore = score;
							bestMove = move;
							if (score > alpha) {
								alpha = score;
								// If it is the master thread, insert the entry into the TT and update the stats.
								if (isMainSearchThread) {
									synchronized (rootLock) {
										insertIntoTt(position.key, origAlpha, beta, move, score, (short) 0,
												(short) (depth/params.fullPly));
										updateInfo(position, move, searchedMoves, ply, origAlpha, beta, score);
										infoUpdated = true;
									}
								}
								if (score >= beta)
									break RootSearch;
							}
						}
					}
				}
				// If a slave thread finished first or if the main thread failed low, try to insert the results into the TT.
				if (!isMainSearchThread || bestScore <= origAlpha) {
					synchronized (rootLock) {
						insertIntoTt(position.key, origAlpha, beta, bestMove, bestScore, (short) 0, (short) (depth/params.fullPly));
						// If it is the main thread, update the search info with the fail low score.
						if (isMainSearchThread) {
							updateInfo(position, null, 0, ply, origAlpha, beta, bestScore);
							infoUpdated = true;
						} else { // If it is an early finisher slave thread, set the result of the master thread and stop it.
							master.setResult((short) bestScore);
							master.stop();
						}
					}
				}
				return bestScore;
			} catch (AbnormalSearchTerminationException e) {
				// If the main thread was cancelled by a slave thread that finished earlier, use the results of the slave thread.
				if (isMainSearchThread && doStopSearchThread) {
					synchronized (rootLock) {
						bestScore = result;
						// If the search info has not been updated yet, do it now.
						if (!infoUpdated)
							updateInfo(rootPosition, null, 0, ply, origAlpha, beta, bestScore);
					}
				}
				return bestScore;
			} finally {
				if (!isMainSearchThread)
					latch.countDown();
			}
		}
	}
	
	/**
	 * A simple, thread-safe container class for search statistics in the form of atomic counters.
	 * 
	 * @author Viktor
	 *
	 */
	private class SearchStats {
		
		final AtomicLong mainNodes, quiescenceNodes, busyNodes, delayedNodes, hashHits, hashScoreHits, iids, successfulIids,
		nullMoveReductions, nullMoveCutoffs, reverseRazorCutoffs, hashMoveCutoffs, winningTacticalCutoffs, killerCutoffs,
		losingTacticalCutoffs, quietCutoffs, futilityPrunes, lateMoveReductions, successfulLateMoveReductions;
		
		/**
		 * Initializes the counters.
		 */
		SearchStats() {
			mainNodes = new AtomicLong();
			quiescenceNodes = new AtomicLong();
			busyNodes = new AtomicLong();
			delayedNodes = new AtomicLong();
			hashHits = new AtomicLong();
			hashScoreHits = new AtomicLong();
			iids = new AtomicLong();
			successfulIids = new AtomicLong();
			nullMoveReductions = new AtomicLong();
			nullMoveCutoffs = new AtomicLong();
			reverseRazorCutoffs = new AtomicLong();
			hashMoveCutoffs = new AtomicLong();
			winningTacticalCutoffs = new AtomicLong();
			killerCutoffs = new AtomicLong();
			losingTacticalCutoffs = new AtomicLong();
			quietCutoffs = new AtomicLong();
			futilityPrunes = new AtomicLong();
			lateMoveReductions = new AtomicLong();
			successfulLateMoveReductions = new AtomicLong();
		}
		@Override
		public String toString() {
			return String.format("m_nodes %d q_nodes %d b_nodes %d d_nodes %d h_hits %.2f h_score_hits %.2f " +
					"iids %d iid_success_rate %.2f nmrs %d nmr_success_rate %.2f nullmove_cutoffs %.2f razor_cutoffs %.2f " +
					"h_move_cutoffs %.2f wtactical_cutoffs %.2f killer_cutoffs %.2f ltactical_cutoffs %.2f quiet_cutoffs %.2f " +
					"futility_prunes %d lmrs %d lmr_success_rate %.2f",
					mainNodes.get(), quiescenceNodes.get(), busyNodes.get(), delayedNodes.get(), ((double) hashHits.get())/mainNodes.get(),
					((double) hashScoreHits.get())/mainNodes.get(), iids.get(), ((double) successfulIids.get())/iids.get(),
					nullMoveReductions.get(), ((double) nullMoveCutoffs.get())/nullMoveReductions.get(),
					((double) nullMoveCutoffs.get())/mainNodes.get(), ((double) reverseRazorCutoffs.get())/mainNodes.get(),
					((double) hashMoveCutoffs.get())/mainNodes.get(), ((double) winningTacticalCutoffs.get())/mainNodes.get(),
					((double) killerCutoffs.get())/mainNodes.get(), ((double) losingTacticalCutoffs.get())/mainNodes.get(),
					((double) quietCutoffs.get())/mainNodes.get(), futilityPrunes.get(), lateMoveReductions.get(),
					((double) successfulLateMoveReductions.get())/lateMoveReductions.get());
		}
		
	}
	
	/**
	 * A {@link java.lang.RuntimeException} for when a search is cancelled or the maximum number of nodes to search have been exceeded and thus 
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
