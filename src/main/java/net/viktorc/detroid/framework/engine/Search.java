package net.viktorc.detroid.framework.engine;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import net.viktorc.detroid.framework.util.BitOperations;
import net.viktorc.detroid.framework.util.Cache;
import net.viktorc.detroid.framework.util.QuickSort;

/**
 * A chess game tree search based on the PVS algorithm supported by a transposition table within an iterative deepening framework with
 * aspiration windows utilizing heuristics such as null move pruning, late move reductions, futility pruning, extended futility pruning,
 * razoring, IID, quiescence search, and fractional depth extensions. For move ordering, it relies on a table of killer moves, a relative
 * history score table, and the MVVLVA and SEE heuristics.
 *
 * @author Viktor
 */
public class Search implements Runnable, Future<SearchResults> {

  /**
   * The depth to which the position is to be searched at the first iteration of the iterative deepening framework.
   */
  private static final short INITIAL_DEPTH = 1;
  /**
   * The maximum nominal base search depth.
   */
  private static final byte MAX_NOMINAL_SEARCH_DEPTH = 64;
  /**
   * The number of ply fractions that make up a full ply.
   */
  private static final byte FULL_PLY = 8;

  private final Position rootPos;
  private final DetroidParameters params;
  private final Evaluator eval;
  private final EndGameTableBase egtb;
  private final boolean useEgtb;
  private final Set<Integer> availableEgtbs;
  private final DetroidSearchInformation info;
  private final Cache<TTEntry> transTable;
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
  private Map<Move, AtomicLong> movesToNodes;
  private volatile int selDepth;
  private volatile boolean isDone;
  private volatile boolean doStopSearch;
  private volatile SearchResults results;
  private volatile Throwable throwable;

  /**
   * Constructs a new instance using the specified parameters.
   *
   * @param pos The root position to search.
   * @param params The engine parameters.
   * @param eval The evaluator object.
   * @param egtb The endgame tablebase object to use.
   * @param info The object to update with search information.
   * @param numOfSearchThreads The number of search threads to use.
   * @param transTable The transposition table to use.
   * @param hashEntryGen The current hash entry generation.
   * @param analysisMode Whether the search is to be run in analysis mode (e.g. no EGTB moves and even single response root positions are
   * searched).
   * @param ponder Whether the search should run in ponder mode.
   * @param maxDepth The maximum depth to search to.
   * @param maxNodes The maximum number of positions to search.
   * @param moves The only moves from the root position that should be searched. If it is <code>null</code>, all moves are to be searched.
   */
  public Search(Position pos, DetroidParameters params, Evaluator eval, EndGameTableBase egtb, DetroidSearchInformation info,
      int numOfSearchThreads, Cache<TTEntry> transTable, byte hashEntryGen, boolean analysisMode, boolean ponder, int maxDepth,
      long maxNodes, Set<Move> moves) {
    this.params = params;
    this.info = info;
    this.eval = eval;
    this.egtb = egtb;
    useEgtb = !analysisMode && egtb.isProbingLibLoaded() && egtb.isInit();
    if (useEgtb) {
      availableEgtbs = new HashSet<>();
      for (int i = 3; i <= EndGameTableBase.MAX_NUMBER_OF_PIECES; i++) {
        if (egtb.areTableBasesAvailable(i)) {
          availableEgtbs.add(i);
        }
      }
    } else {
      availableEgtbs = null;
    }
    this.rootPos = pos;
    this.analysisMode = analysisMode;
    this.ponder = ponder;
    if (!ponder) {
      maxDepth = Math.max(0, maxDepth);
      this.maxNominalDepth = Math.min(MAX_NOMINAL_SEARCH_DEPTH, maxDepth);
      this.maxNodes = maxNodes;
    } else {
      this.maxNominalDepth = MAX_NOMINAL_SEARCH_DEPTH;
      this.maxNodes = Long.MAX_VALUE;
    }
    allowedRootMoves = moves != null ? new ArrayList<>(moves) : null;
    areMovesRestricted = moves != null && moves.size() > 0;
    /* In case all the extensions are activated during the search and the quiscence search probes 2 fold beyond
     * the main search depth. */
    maxExpectedSearchDepth = 2 * 2 * this.maxNominalDepth;
    lCheckMateLimit = Score.LOSING_CHECK_MATE.value + maxExpectedSearchDepth;
    wCheckMateLimit = -lCheckMateLimit;
    this.transTable = transTable;
    this.hashEntryGen = hashEntryGen;
    this.numOfHelperThreads = this.maxNominalDepth > 1 ? numOfSearchThreads - 1 : 0;
    rootLock = new Object();
  }

  private long getTotalNodes() {
    return searchStats.mainNodes.get() + searchStats.quiescenceNodes.get();
  }

  private List<Move> extractPv(Position position, int ply) {
    List<Move> pv = new ArrayList<>();
    TTEntry e;
    int i = 0;
    while ((e = transTable.get(position.getKey())) != null && e.getBestMove() != 0 && i < ply) {
      Move bestMove = Move.toMove(e.getBestMove());
      position.makeMove(bestMove);
      pv.add(bestMove);
      i++;
    }
    for (int k = 0; k < i; k++) {
      position.unmakeMove();
    }
    return pv;
  }

  private Entry<Short, ScoreType> adjustExactScore(int score) {
    int resultScore;
    ScoreType scoreType;
    if (score <= lCheckMateLimit) {
      resultScore = (int) Math.floor(((double) (Score.LOSING_CHECK_MATE.value - score)) / 2);
      scoreType = ScoreType.MATE;
    } else if (score >= wCheckMateLimit) {
      resultScore = (int) Math.ceil(((double) (Score.WINNING_CHECK_MATE.value - score)) / 2);
      scoreType = ScoreType.MATE;
    } else {
      resultScore = score;
      scoreType = ScoreType.EXACT;
    }
    return new SimpleImmutableEntry<>((short) resultScore, scoreType);
  }

  private Entry<Short, ScoreType> adjustScore(int score, int alpha, int beta) {
    if (score == Score.NULL.value) {
      return null;
    }
    // Determine score type and value in case it's a mate score.
    if (score >= beta) {
      return new SimpleImmutableEntry<>((short) score, ScoreType.LOWER_BOUND);
    }
    if (score <= alpha) {
      return new SimpleImmutableEntry<>((short) score, ScoreType.UPPER_BOUND);
    }
    return adjustExactScore(score);
  }

  private void updateInfo(Position position, Move currentMove, int moveNumber, short ply, int alpha, int beta, int score) {
    // Determine score type and value in case it's a mate score.
    Entry<Short, ScoreType> adjustedScore = adjustScore(score, alpha, beta);
    long egtbHits = 0;
    String statInfo = null;
    if (analysisMode) {
      statInfo = searchStats.toString();
    }
    if (useEgtb) {
      EGTBStats egtbStats = egtb.getStats();
      egtbHits += egtbStats.getTotalDriveHits() + egtbStats.getTotalCacheHits();
      if (analysisMode) {
        statInfo += String.format(" egtb_hp %d egtb_sp %d egtb_chits %d egtb_dhits %d",
            egtbStats.getTotalHardProbes(), egtbStats.getTotalSoftProbes(), egtbStats.getTotalCacheHits(),
            egtbStats.getTotalDriveHits());
      }
    }
    // Update stats.
    if (adjustedScore != null) {
      info.set(extractPv(position, ply), currentMove, moveNumber, ply, (short) selDepth, adjustedScore.getKey(),
          adjustedScore.getValue(), getTotalNodes(), System.currentTimeMillis() - startTime, egtbHits,
          statInfo);
    }
  }

  private SearchResults iterativeDeepening() {
    selDepth = 0;
    doStopSearch = false;
    searchStats = new SearchStats();
    List<SearchThread> slaveThreads = null;
    int alpha = Score.MIN.value;
    int beta = Score.MAX.value;
    SearchThread masterThread = new SearchThread(rootPos, null);
    rootMoves = areMovesRestricted ? allowedRootMoves : rootPos.getMoves();
    movesToNodes = new HashMap<>();
    for (Move m : rootMoves) {
      movesToNodes.put(m, new AtomicLong(0));
    }
    if (numOfHelperThreads > 0) {
      slaveThreads = new ArrayList<>();
      for (int i = 0; i < numOfHelperThreads; i++) {
        slaveThreads.add(new SearchThread(new Position(rootPos), masterThread));
      }
    }
    // The number of consecutive fail-highs/fail-lows.
    int failHigh = 0;
    int failLow = 0;
    // Iterative deepening.
    int score;
    short ply;
    for (ply = INITIAL_DEPTH; ; ply++) {
      // Sort moves...
      if (ply == INITIAL_DEPTH) { // First iteration based on SEE.
        rootMoves = new ArrayList<>(Arrays.asList(masterThread.orderMaterialMovesSEE(rootPos, rootMoves)));
      } else { // Subsequent iterations based on cumulative subtree sizes.
        rootMoves.sort((m1, m2) -> (int) (movesToNodes.get(m2).get() - movesToNodes.get(m1).get()));
      }
      masterThread.setPly(ply);
      masterThread.setBounds(alpha, beta);
      // Launch helper threads.
      if (numOfHelperThreads > 0 && ply != INITIAL_DEPTH) {
        latch = new CountDownLatch(numOfHelperThreads);
        boolean odd = true;
        for (int j = 0; j < slaveThreads.size(); j++, odd = !odd) {
          SearchThread slaveThread = slaveThreads.get(j);
          slaveThread.setPly((short) (odd && ply < maxNominalDepth ? ply + 1 : ply));
          slaveThread.setBounds(alpha, beta);
          executor.submit(slaveThread);
        }
      }
      // Launch the master thread.
      score = masterThread.call();
      // Interrupt the helpers thread and wait for them to terminate.
      if (numOfHelperThreads > 0 && ply != INITIAL_DEPTH) {
        for (SearchThread t : slaveThreads) {
          t.stop();
        }
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
      if (doStopSearch || ply == maxNominalDepth || score == Score.NULL.value) {
        break;
      }
      // Aspiration windows with gradual widening.
      if (score <= alpha) {
        if (score <= lCheckMateLimit) {
          alpha = Score.MIN.value;
          failLow = 2;
        } else {
          alpha = failLow <= 1 ? Math.max(score - params.aspirationDelta, Score.MIN.value) : Score.MIN.value;
          failLow++;
        }
        ply--;
      } else if (score >= beta) {
        if (score >= wCheckMateLimit) {
          beta = Score.MAX.value;
          failHigh = 2;
        } else {
          beta = failHigh <= 1 ? Math.min(score + params.aspirationDelta, Score.MAX.value) : Score.MAX.value;
          failHigh++;
        }
        ply--;
      } else {
        alpha = Math.max(score - params.aspirationDelta, Score.MIN.value);
        beta = Math.min(score + params.aspirationDelta, Score.MAX.value);
        failHigh = failLow = 0;
      }
    }
    Move bestMove = null;
    Move ponderMove = null;
    TTEntry entry = transTable.get(rootPos.getKey());
    if (entry != null) {
      score = entry.getScore();
    }
    List<Move> pv = extractPv(rootPos, 2);
    if (pv != null && !pv.isEmpty()) {
      bestMove = pv.get(0);
      if (pv.size() > 1) {
        ponderMove = pv.get(1);
      }
    }
    Entry<Short, ScoreType> adjustedScore = adjustScore(score, alpha, beta);
    Short finalScore;
    ScoreType scoreType;
    if (adjustedScore == null) {
      finalScore = null;
      scoreType = null;
    } else {
      finalScore = adjustedScore.getKey();
      scoreType = adjustedScore.getValue();
    }
    updateInfo(rootPos, null, 0, ply, alpha, beta, score);
    return new SearchResults(bestMove == null ? null : bestMove.toString(), ponderMove == null ? null : ponderMove.toString(), finalScore,
        scoreType);
  }

  @Override
  public synchronized SearchResults get() throws InterruptedException, ExecutionException {
    while (!isDone) {
      wait();
    }
    if (throwable != null) {
      throw new ExecutionException(throwable);
    }
    return results;
  }

  @Override
  public synchronized SearchResults get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
    long timeoutMs = unit.toMillis(timeout);
    while (!isDone && timeoutMs > 0) {
      long start = System.currentTimeMillis();
      wait(timeoutMs);
      timeoutMs -= (System.currentTimeMillis() - start);
    }
    if (throwable != null) {
      throw new ExecutionException(throwable);
    }
    return results;
  }

  @Override
  public synchronized boolean isDone() {
    return isDone;
  }

  @Override
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (mayInterruptIfRunning && !isDone) {
      doStopSearch = true;
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean isCancelled() {
    return doStopSearch;
  }

  @Override
  public void run() {
    startTime = System.currentTimeMillis();
    synchronized (this) {
      isDone = false;
      doStopSearch = false;
      throwable = null;
    }
    if (numOfHelperThreads > 0) {
      executor = Executors.newFixedThreadPool(numOfHelperThreads);
    }
    try {
      results = iterativeDeepening();
    } catch (Throwable e) {
      synchronized (this) {
        throwable = e;
      }
    }
    startTime = null;
    if (executor != null) {
      executor.shutdown();
    }
    synchronized (this) {
      doStopSearch = false;
      isDone = true;
      notifyAll();
    }
  }

  /**
   * A search thread for searching a position within an iterative deepening framework.
   *
   * @author Viktor
   */
  private class SearchThread implements Callable<Integer> {

    /**
     * The score to return when a node is already being searched by another thread.
     */
    private static final int BUSY_SCORE = -2 * Short.MAX_VALUE;

    private final Position origPos; // The original pos to search.
    private final SearchThread master;
    private final boolean isMainSearchThread;
    private final KillerTable killerTable;
    private final RelativeHistoryTable historyTable;
    private final TTEntry transTableEntry;
    private final ETEntry evalTableEntry;
    private Position pos; // The pos instance to use for the search.
    private AtomicLong nodes;
    private int alpha;
    private int beta;
    private short ply;
    private volatile short result;
    private volatile boolean doStopSearchThread;

    /**
     * Constructs an instance using the specified arguments. If the parameter master is null, the search thread will be constructed as a
     * master search thread itself.
     *
     * @param pos The position to search.
     * @param master The master search thread.
     */
    SearchThread(Position pos, SearchThread master) {
      origPos = pos;
      this.master = master;
      this.isMainSearchThread = master == null;
      if (maxNominalDepth > 0) {
        killerTable = new KillerTable(maxNominalDepth * 2);
        historyTable = new RelativeHistoryTable();
      } else {
        killerTable = null;
        historyTable = null;
      }
      transTableEntry = new TTEntry();
      evalTableEntry = new ETEntry();
    }

    /**
     * @param ply The depth to which the position is to be searched.
     */
    void setPly(short ply) {
      this.ply = ply;
    }

    /**
     * @param result The result score to return.
     */
    void setResult(short result) {
      this.result = result;
    }

    /**
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
     * @param move The move to test.
     * @return Whether a move is a pawn push. A pawn push is a pawn move to the last or the one before the last rank.
     */
    private boolean isPawnPush(Move move) {
      return (move.movedPiece == Piece.W_PAWN.ind && move.to >= 48) ||
          (move.movedPiece == Piece.B_PAWN.ind && move.to < 16);
    }

    /**
     * Orders material moves and checks, the former of which according to the SEE swap algorithm.
     *
     * @param pos The current position.
     * @param moves The moves to order.
     * @return The ordered moves.
     */
    private Move[] orderMaterialMovesSEE(Position pos, List<Move> moves) {
      Move[] arr = new Move[moves.size()];
      int i = 0;
      for (Move move : moves) {
        move.setValue(eval.SEE(pos, move)); // Static exchange evaluation.
        arr[i++] = move;
      }
      return QuickSort.sort(arr);
    }

    /**
     * Orders captures and promotions according to the MVV-LVA principle.
     *
     * @param moves The moves to order.
     * @return The ordered moves.
     */
    private Move[] orderMaterialMovesMVVLVA(List<Move> moves) {
      Move[] arr = new Move[moves.size()];
      int i = 0;
      for (Move move : moves) {
        move.setValue(Evaluator.MVVLVA(move));
        arr[i++] = move;
      }
      return QuickSort.sort(arr);
    }

    /**
     * Orders non-material moves according to the relative history heuristic.
     *
     * @param moves The moves to order.
     * @return The ordered moves.
     */
    private Move[] orderNonMaterialMoves(List<Move> moves) {
      Move[] arr = new Move[moves.size()];
      int i = 0;
      for (Move move : moves) {
        move.setValue(historyTable.score(move));
        arr[i++] = move;
      }
      return QuickSort.sort(arr);
    }

    /**
     * Tries to insert an entry into one of the transposition tables based on the specified parameters.
     *
     * @param key The key of the position.
     * @param alpha The alpha bound.
     * @param beta The beta bound.
     * @param bestMove The best move found.
     * @param bestScore The best score.
     * @param distFromRoot The distance from the root position in plies.
     * @param depth The depth to which the position has been searched.
     * @return Whether the entry was stored in the transposition table.
     */
    private boolean insertIntoTt(long key, int alpha, int beta, Move bestMove, int bestScore, short distFromRoot, short depth) {
      /* Adjustment of the best score for TT insertion according to the distance from the mate pos in case it's a
       * check mate score. */
      int score;
      if (bestScore <= lCheckMateLimit) {
        score = bestScore - distFromRoot;
      } else if (bestScore >= wCheckMateLimit) {
        score = bestScore + distFromRoot;
      } else {
        score = bestScore;
      }
      int bestMoveInt = bestMove == null ? 0 : bestMove.toInt();
      // Determine node type.
      byte type;
      if (bestScore <= alpha) {
        type = NodeType.FAIL_LOW.ind;
      } else if (bestScore >= beta) {
        type = NodeType.FAIL_HIGH.ind;
      } else {
        type = NodeType.EXACT.ind;
      }
      // Add new entry to the transposition table.
      // First try the primary table.
      transTableEntry.set(key, depth, type, (short) score, bestMoveInt, hashEntryGen, false);
      transTableEntry.setupKey();
      return transTable.put(transTableEntry);
    }

    /**
     * A search algorithm for diminishing the horizon effect once the main search algorithm has reached a leaf node. It keeps searching
     * until the side to move does not have any legal winning captures or even exchanges according to SEE (or the absolute maximum search
     * depth is reached).
     *
     * @param distFromRoot The distance from the root position in plies.
     * @param alpha The alpha bound.
     * @param beta The beta bound.
     * @return The score of the leaf node.
     * @throws AbnormalSearchTerminationException If the search is cancelled or the maximum allowed number of nodes have been searched.
     */
    private int quiescence(int distFromRoot, int alpha, int beta) throws AbnormalSearchTerminationException {
      int mateValue = Score.LOSING_CHECK_MATE.value + distFromRoot;
      if (!ponder && getTotalNodes() >= maxNodes) {
        doStopSearch = true;
      }
      if (doStopSearch || doStopSearchThread) {
        throw new AbnormalSearchTerminationException();
      }
      if (nodes != null) {
        nodes.incrementAndGet();
      }
      searchStats.quiescenceNodes.incrementAndGet();
      // Fifty-move rule and repetition rule check.
      if (pos.getFiftyMoveRuleClock() >= 100 || pos.hasRepeated(distFromRoot > 2 ? 1 : 2)) {
        return Score.DRAW_CLAIMED.value;
      }
      // Evaluate the pos statically.
      int bestScore = pos.isInCheck() ? mateValue : eval.score(pos, hashEntryGen, evalTableEntry);
      // Fail soft.
      if (bestScore > alpha) {
        alpha = bestScore;
        if (bestScore >= beta) {
          return bestScore;
        }
      }
      // Limit the maximum depth of the quiescence search.
      if (distFromRoot > maxExpectedSearchDepth) {
        return alpha;
      }
      // Generate all the material moves or if in check, all moves.
      List<Move> moves = pos.isInCheck() ? pos.getMoves() : pos.getTacticalMoves();
      Move[] sortedMoves = orderMaterialMovesMVVLVA(moves);
      for (Move move : sortedMoves) {
        if (!pos.isInCheck()) {
          // If the SEE value is below 0 or the delta pruning limit, skip.
          int moveValue = eval.SEE(pos, move);
          if (moveValue < 0 || moveValue <= alpha - params.deltaPruningMargin) {
            continue;
          }
        }
        pos.makeMove(move);
        int searchScore = -quiescence(distFromRoot + 1, -beta, -alpha);
        pos.unmakeMove();
        if (searchScore > bestScore) {
          bestScore = searchScore;
          if (searchScore > alpha) {
            alpha = searchScore;
            if (searchScore >= beta) {
              break;
            }
          }
        }
      }
      return bestScore;
    }

    /**
     * Sets the 'busy' flag of the current node to false in the corresponding transposition table entry.
     * 
     * @param nodeBlocked Whether the node is blocked.
     */
    private void resetBusyFlag(boolean nodeBlocked) {
      if (!nodeBlocked) {
        return;
      }
      TTEntry hashEntry = transTable.get(pos.getKey());
      if (hashEntry != null) {
        hashEntry.setBusy(false);
      }
    }
    
    /**
     * Performs a PV search on the specified move.
     *
     * @param move The move to search.
     * @param depthLimit The maximum allowed remaining search depth.
     * @param depth The current remaining search depth.
     * @param extension The extension to apply to the search depth.
     * @param distFromRoot The current distance from the root node.
     * @param searchedMoves The number of searched moves.
     * @param alpha The lower bound.
     * @param beta The upper bound
     * @param nullWindowSearchExclusive Whether the null-window search should be exclusive (if it is not the first move searched).
     * @param nodeBlocked Whether the current node is blocked.
     * @return The search score of the move.
     */
    private int pvSearchMove(Move move, int depthLimit, int depth, int extension, int distFromRoot, int searchedMoves,
        int alpha, int beta, boolean nullWindowSearchExclusive, boolean nodeBlocked) {
      int score;
      int searchDepth = Math.min(depthLimit, depth + extension) - FULL_PLY;
      pos.makeMove(move);
      try {
        // PVS.
        if (searchedMoves == 0) {
          // Full window.
          score = -pvSearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
        } else {
          // Null window.
          score = -pvSearch(searchDepth, distFromRoot + 1, -alpha - 1, -alpha, true, nullWindowSearchExclusive);
          /* If the null-window search fails, the research is not exclusive to allow other threads to search the
           * line as it is likely to be the best move. */
          if (score != -BUSY_SCORE && score > alpha && score < beta) {
            score = -pvSearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
          }
        }
      } catch (AbnormalSearchTerminationException e) {
        resetBusyFlag(nodeBlocked);
        throw e;
      }
      pos.unmakeMove();
      return score;
    }

    /**
     * A principal variation search algorithm utilizing a transposition table. It returns only the score for the searched position, but the
     * principal variation can be extracted from the transposition table after a search has been run.
     *
     * @param depth The depth to which the position is to be searched.
     * @param distFromRoot The distance from the root position in plies.
     * @param alpha The alpha bound.
     * @param beta The beta bound.
     * @param nullMoveAllowed Whether null moves are allowed.
     * @param exclusive Whether only a single thread should search the position.
     * @return The score of the searched position.
     * @throws AbnormalSearchTerminationException If the search is cancelled or the maximum allowed number of nodes have been searched.
     */
    private int pvSearch(int depth, int distFromRoot, int alpha, int beta, boolean nullMoveAllowed, boolean exclusive)
        throws AbnormalSearchTerminationException {
      // Do not allow negative depths for the full effect of check extensions.
      depth = Math.max(depth, 0);
      final int mateScore = Score.LOSING_CHECK_MATE.value + distFromRoot;
      final int origAlpha = alpha;
      final int depthLimit = distFromRoot >= maxNominalDepth ? depth : depth + FULL_PLY;
      final boolean pvNode = beta > origAlpha + 1;
      int bestScore = mateScore;
      Move bestMove = null;
      Move hashMove = null;
      Move killerMove1 = null;
      Move killerMove2 = null;
      int searchedMoves = 0;
      boolean nodeBlocked = false;
      boolean isThereHashMove = false;
      boolean isThereKM1 = false;
      boolean isThereKM2 = false;
      List<Move> tacticalMoves = null;
      List<Move> quietMoves = null;
      KTEntry killerEntry = null;
      if (!ponder && getTotalNodes() >= maxNodes) {
        doStopSearch = true;
      }
      if (doStopSearch || doStopSearchThread) {
        throw new AbnormalSearchTerminationException();
      }
      nodes.incrementAndGet();
      searchStats.mainNodes.incrementAndGet();
      if (distFromRoot > selDepth) {
        selDepth = distFromRoot;
      }
      TTEntry hashEntry;
      Search:
      {
        int score;
        // Check for the repetition rule; return a draw score if it applies.
        if (pos.hasRepeated(distFromRoot > 2 ? 1 : 2)) {
          return Score.DRAW_CLAIMED.value;
        }
        // Mate distance pruning.
        if (-mateScore < beta && alpha >= -mateScore) {
          return -mateScore;
        }
        if (mateScore > alpha && beta <= mateScore) {
          return mateScore;
        }
        // Probe endgame tablebases if applicable.
        if (useEgtb && distFromRoot <= params.maxDistFromRootForEgtbProbe) {
          int numOfPieces = BitOperations.hammingWeight(pos.getAllOccupied());
          // Check if there are tablebases loaded for the current number of pieces on the board.
          if (numOfPieces <= EndGameTableBase.MAX_NUMBER_OF_PIECES && availableEgtbs.contains(numOfPieces)) {
            DTM dtm = egtb.probeDTM(pos, distFromRoot > params.maxDistFromRootForHardEgtbProbe);
            // If the pos is found...
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
        depth = pos.isInCheck() ? Math.min(depthLimit, depth + params.checkExtension) : depth;
        // Pawn push extension
        Move lastMove = pos.getLastMove();
        boolean pawnPushed = isPawnPush(lastMove);
        depth = pawnPushed ? Math.min(depthLimit, depth + params.pawnPushExtension) : depth;
        // Check the conditions for quiescence search.
        boolean doQuiescence = depth / FULL_PLY <= 0;
        /* Check the hash move and return its score for the pos if it is exact or set alpha or beta according
         * to its score if it is not. */
        hashEntry = transTable.get(pos.getKey());
        if (hashEntry != null) {
          int hashDepth = hashEntry.getDepth();
          int hashType = hashEntry.getType();
          int hashScore = hashEntry.getScore();
          int hashMoveInt = hashEntry.getBestMove();
          boolean hashBusy = hashEntry.isBusy();
          // Make sure that entry was not overwritten in another thread.
          if (hashEntry.hashKey() == pos.getKey()) {
            hashEntry.setGeneration(hashEntryGen);
            searchStats.hashHits.incrementAndGet();
            /* If the hashed entry's depth is greater than or equal to the current search depth, check if
             * the stored score is usable. */
            if (hashDepth >= depth / FULL_PLY && hashScore != Score.NULL.value) {
              // Mate score adjustment to root distance.
              if (hashScore <= lCheckMateLimit) {
                score = hashScore + distFromRoot;
              } else if (hashEntry.getScore() >= wCheckMateLimit) {
                score = hashScore - distFromRoot;
              } else {
                score = hashScore;
              }
              /* If the score was exact, or it was the score of an all node and is smaller than or equal
               * to alpha, or it is that of a cut node and is greater than or equal to beta, return the
               * score. If it is a PV node and the score is exact, make sure that either the score is
               * outside the bounds or the search is about to drop into quiescence search to avoid a PV
               * cut. */
              if ((hashType == NodeType.EXACT.ind &&
                  (!pvNode || doQuiescence || hashScore <= alpha || hashScore >= beta)) ||
                  /* To make sure that a score that might not have been the exact score for the
                   * subtree below the node regardless of the alpha-beta boundaries is not treated
                   * as an exact score in the current context, we can not allow it to fall between
                   * the current alpha and beta. If it was a fail high node, the score is a lower
                   * boundary of the exact score of the node due to there possibly being siblings to
                   * the right of the child node [that raised alpha higher than beta and caused a
                   * cut-off] that could raise alpha even higher. If it was a fail low node, the
                   * score is a higher boundary for the exact score of the node, because all children
                   * of a fail low node are fail high nodes (-score <= alpha -> score >= -alpha
                   * [-alpha = beta in the child node]). To keep the interval of values the exact
                   * score could take on out of (alpha, beta), the score has to be lower than or
                   * equal to alpha if it is a higher boundary, i.e. fail low score, and it has to be
                   * greater than or equal to beta if it is a lower boundary i.e. fail high score. */
                  (hashType == NodeType.FAIL_HIGH.ind && score >= beta) ||
                  (hashType == NodeType.FAIL_LOW.ind && score <= alpha)) {
                searchStats.hashScoreHits.incrementAndGet();
                return score;
              }
            }
            /* Check if the node should be put to the end of the list in the current thread due to another
             * one already searching it. */
            if (numOfHelperThreads > 0 && depth / FULL_PLY >= params.nodeBusinessCheckMinDepthLeft) {
              if (exclusive && hashBusy) {
                searchStats.busyNodes.incrementAndGet();
                return BUSY_SCORE;
              }
              // If it is still the right entry, set it to busy.
              if (hashEntry.hashKey() == pos.getKey()) {
                hashEntry.setBusy(true);
                nodeBlocked = true;
              }
            }
            /* Check for the stored move and make it the best guess if it is not null and the node is not
             * fail low. */
            if (hashMoveInt != 0 && !doQuiescence) {
              hashMove = Move.toMove(hashMoveInt);
              isThereHashMove = pos.isLegal(hashMove);
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
        score = Score.NULL.value;
        int evalScore = score;
        // Assess if the pos is 'safe'...
        boolean dangerous = pos.isInCheck() || pawnPushed;
        if (!pvNode && !dangerous && Math.abs(beta) < wCheckMateLimit &&
            (pos.getWhiteKing() | pos.getWhitePawns() | pos.getBlackKing() | pos.getBlackPawns()) !=
                pos.getAllOccupied()) {
          // Try reverse futility pruning or 'static null move pruning' if the conditions are met.
          if (depth / FULL_PLY <= 3 && params.doRazor) {
            int razMargin;
            switch (depth / FULL_PLY) {
              case 1: // Reverse pre-frontier futility pruning.
                razMargin = params.revFutilityMargin1;
                break;
              case 2: // Reverse limited futility pruning.
                razMargin = params.revFutilityMargin2;
                break;
              case 3: // Reverse deep futility pruning.
                razMargin = params.revFutilityMargin3;
                break;
              default:
                razMargin = 0;
            }
            if (evalScore == Score.NULL.value) {
              evalScore = eval.score(pos, hashEntryGen, evalTableEntry);
            }
            if (evalScore - razMargin >= beta) {
              searchStats.reverseRazorCutoffs.incrementAndGet();
              resetBusyFlag(nodeBlocked);
              return evalScore - razMargin;
            }
          }
          // Try null move pruning if the conditions are met.
          if (nullMoveAllowed && depth / ply >= params.nullMoveReductionMinDepthLeft) {
            if (evalScore == Score.NULL.value) {
              evalScore = eval.score(pos, hashEntryGen, evalTableEntry);
            }
            if (evalScore > alpha) {
              searchStats.nullMoveReductions.incrementAndGet();
              // Dynamic depth reduction.
              int nullMoveReduction = params.nullMoveReduction * FULL_PLY +
                  params.extraNullMoveReduction * depth / (params.extraNullMoveReductionDepthLimit + 1);
              pos.makeNullMove();
              /* Use separate, tight-scoped try-blocks for the parts where exceptions may be thrown to
               * allow the compiler to make as many optimizations as possible. */
              try {
                // Do not allow consecutive null moves.
                score = -pvSearch(depth - (FULL_PLY + nullMoveReduction), distFromRoot + 1,
                    -beta, -beta + 1, false, false);
              } catch (AbnormalSearchTerminationException e) {
                resetBusyFlag(nodeBlocked);
                throw e;
              }
              pos.unmakeMove();
              if (score >= beta) {
                searchStats.nullMoveCutoffs.incrementAndGet();
                resetBusyFlag(nodeBlocked);
                return score;
              }
            }
          }
        }
        // If there is no hash entry in a PV node that is to be searched deep, try IID.
        if (params.doIid && pvNode && !isThereHashMove && depth / FULL_PLY >= params.iidMinDepthLeft) {
          searchStats.iids.incrementAndGet();
          try {
            pvSearch(depth * params.iidRelDepth16th / 16, distFromRoot, alpha, beta, true, false);
          } catch (AbnormalSearchTerminationException e) {
            resetBusyFlag(nodeBlocked);
            throw e;
          }
          hashEntry = transTable.get(pos.getKey());
          int hashMoveInt;
          if (hashEntry != null && (hashMoveInt = hashEntry.getBestMove()) != 0 &&
              hashEntry.hashKey() == pos.getKey()) {
            searchStats.successfulIids.incrementAndGet();
            hashMove = Move.toMove(hashMoveInt);
            isThereHashMove = pos.isLegal(hashMove);
          }
        }
        // If there was no hash move...
        if (!isThereHashMove) {
          // Generate material moves.
          tacticalMoves = pos.getTacticalMoves();
          // If there were no material moves, check killer moves...
          if (tacticalMoves.size() == 0) {
            killerEntry = killerTable.retrieve(distFromRoot);
            if (killerEntry.getMove1() != 0 &&
                pos.isLegal(killerMove1 = Move.toMove(killerEntry.getMove1()))) {
              isThereKM1 = true;
            } else if (killerEntry.getMove2() != 0 &&
                pos.isLegal(killerMove2 = Move.toMove(killerEntry.getMove2()))) {
              isThereKM2 = true;
            }
            // If there were no legal killer moves, generate the quiet moves and perform a mate check.
            if (!isThereKM1 && !isThereKM2) {
              quietMoves = pos.getQuietMoves();
              if (quietMoves.size() == 0) {
                score = pos.isInCheck() ? mateScore : Score.STALE_MATE.value;
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
        if (pos.getFiftyMoveRuleClock() >= 100) {
          resetBusyFlag(nodeBlocked);
          return Score.DRAW_CLAIMED.value;
        }
        // Check if a recapture extension could possibly be applied.
        lastMove = pos.getLastMove();
        boolean lastMoveIsTactical = lastMove != null && lastMove.isTactical();
        // If there is a hash move, search that first.
        if (isThereHashMove) {
          // Recapture extension (includes capturing newly promoted pieces).
          int extension = lastMoveIsTactical && hashMove.capturedPiece != Piece.NULL.ind &&
              hashMove.to == lastMove.to ? params.recapExtension : 0;
          score = pvSearchMove(hashMove, depthLimit, depth, extension, distFromRoot, 0, alpha, beta, true, nodeBlocked);
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
                  killerTable.add(distFromRoot, hashMove);
                  // Record success in the relative history table.
                  historyTable.recordSuccessfulMove(hashMove);
                }
                break Search;
              }
            }
          }
          if (!hashMove.isTactical()) {
            // Record failure in the relative history table.
            historyTable.recordUnsuccessfulMove(hashMove);
          }
        }
        // Generate the material moves if they are not generated yet.
        if (tacticalMoves == null) {
          tacticalMoves = pos.getTacticalMoves();
        }
        List<Move> losingCaptures = null;
        List<Move> deferredMoves = null;
        // Sort the material moves.
        Move[] tacticalMovesArr = orderMaterialMovesMVVLVA(tacticalMoves);
        // Search winning and equal captures.
        for (Move move : tacticalMovesArr) {
          // If this move was the hash move, skip it.
          if (isThereHashMove && move.equals(hashMove)) {
            isThereHashMove = false;
            continue;
          }
          // If the current move's SEE value indicates a losing capture, put it in the losing captures list.
          move.setValue(eval.SEE(pos, move));
          if (move.getValue() < 0) {
            if (losingCaptures == null) {
              losingCaptures = new ArrayList<>();
            }
            losingCaptures.add(move);
            continue;
          }
          // Recapture extension (includes capturing newly promoted pieces).
          int extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ? params.recapExtension : 0;
          score = pvSearchMove(move, depthLimit, depth, extension, distFromRoot, searchedMoves, alpha, beta, true, nodeBlocked);
          // If the pos is currently searched by another thread, add it to the list of moves to search later.
          if (score == -BUSY_SCORE) {
            if (deferredMoves == null) {
              deferredMoves = new ArrayList<>();
            }
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
        killerEntry = killerEntry == null ? killerTable.retrieve(distFromRoot) : killerEntry;
        if (killerEntry != null) {
          for (int i = 0; i < 2; i++) {
            int killerMoveInt;
            Move killerMove;
            // Assess if the first killer moves is legal.
            if (i == 0) {
              // If the killer move has not yet been verified during the mate check and the entry is not empty.
              if (!isThereKM1 && killerMove1 == null && (killerMoveInt = killerEntry.getMove1()) != 0) {
                killerMove1 = Move.toMove(killerMoveInt);
                if (isThereHashMove && killerMove1.equals(hashMove)) {
                  isThereHashMove = false;
                } else {
                  isThereKM1 = pos.isLegal(killerMove1);
                }
              }
              // The killer move is not legal.
              if (!isThereKM1) {
                continue;
              }
              killerMove = killerMove1;
            } else { // Assess the legality of the second killer move.
              if (!isThereKM2 && killerMove2 == null && (killerMoveInt = killerEntry.getMove2()) != 0) {
                killerMove2 = Move.toMove(killerMoveInt);
                if (isThereHashMove && killerMove2.equals(hashMove)) {
                  isThereHashMove = false;
                } else {
                  isThereKM2 = pos.isLegal(killerMove2);
                }
              }
              if (!isThereKM2) {
                continue;
              }
              killerMove = killerMove2;
            }
            score = pvSearchMove(killerMove, depthLimit, depth, 0, distFromRoot, searchedMoves, alpha, beta, true, nodeBlocked);
            if (score == -BUSY_SCORE) {
              if (deferredMoves == null) {
                deferredMoves = new ArrayList<>();
              }
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
                    {
                      killerTable.add(distFromRoot, killerMove);
                    }
                    // Record success in the relative history table.
                    historyTable.recordSuccessfulMove(killerMove);
                    break Search;
                  }
                }
              }
              // Record failure in the relative history table.
              historyTable.recordUnsuccessfulMove(killerMove);
            }
          }
        }
        // If there are any losing captures...
        if (losingCaptures != null) {
          // Sort them based on their already assigned SEE scores.
          Move[] losingCapturesArr = QuickSort.sort(losingCaptures.toArray(new Move[losingCaptures.size()]));
          // And search them.
          for (Move move : losingCapturesArr) {
            // No need to check if it is the hash move as that was done before it was added to the losing captures.
            // Recapture extension.
            int extension = lastMoveIsTactical && move.capturedPiece != Piece.NULL.ind &&
                move.to == lastMove.to ? params.recapExtension : 0;
            score = pvSearchMove(move, depthLimit, depth, extension, distFromRoot, searchedMoves, alpha, beta, true, nodeBlocked);
            if (score == -BUSY_SCORE) {
              if (deferredMoves == null) {
                deferredMoves = new ArrayList<>();
              }
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
        if (quietMoves == null) {
          quietMoves = pos.getQuietMoves();
        }
        // One reply extension.
        if (tacticalMoves.size() == 0 && quietMoves.size() == 1) {
          dangerous = true;
          depth = Math.min(depthLimit, depth + params.singleReplyExtension);
        }
        int lateMoveReduction = params.lateMoveReduction * FULL_PLY +
            params.extraLateMoveReduction * depth / (params.extraLateMoveReductionDepthLimit + 1);
        int futMargin;
        boolean prunable = !pvNode && !dangerous && Math.abs(alpha) < wCheckMateLimit;
        // Futility pruning margin calculation.
        if (prunable) {
          switch (depth / FULL_PLY) {
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
        } else {
          futMargin = 0;
        }
        // Order and search the non-material moves.
        Move[] quietMovesArr = orderNonMaterialMoves(quietMoves);
        for (Move move : quietMovesArr) {
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
          if (prunable && depth / FULL_PLY <= 5 && !pos.givesCheck(move)) {
            if (evalScore == Score.NULL.value) {
              evalScore = eval.score(pos, hashEntryGen, evalTableEntry);
            }
            if (evalScore <= alpha - futMargin) {
              searchStats.futilityPrunes.incrementAndGet();
              // Record failure in the relative history table.
              historyTable.recordUnsuccessfulMove(move);
              continue;
            }
          }
          pos.makeMove(move);
          try {
            // Left-most move.
            if (searchedMoves == 0) {
              score = -pvSearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, false);
            }
            // Try late move reduction.
            else if (!dangerous && !pos.isInCheck() && searchedMoves > params.minMovesSearchedForLmr &&
                depth / FULL_PLY >= params.lateMoveReductionMinDepthLeft) {
              score = -pvSearch(depth - (FULL_PLY + lateMoveReduction), distFromRoot + 1, -alpha - 1, -alpha, true, true);
              if (score != -BUSY_SCORE) {
                searchStats.lateMoveReductions.incrementAndGet();
                // If it does not fail low, research with full window.
                if (score > alpha) {
                  score = -pvSearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, false);
                } else {
                  searchStats.successfulLateMoveReductions.incrementAndGet();
                }
              }
            } else { // Null-window PVS.
              score = -pvSearch(depth - FULL_PLY, distFromRoot + 1, -alpha - 1, -alpha, true, true);
              if (score != -BUSY_SCORE && score > alpha && score < beta) {
                score = -pvSearch(depth - FULL_PLY, distFromRoot + 1, -beta, -alpha, true, false);
              }
            }
          } catch (AbnormalSearchTerminationException e) {
            resetBusyFlag(nodeBlocked);
            throw e;
          }
          pos.unmakeMove();
          if (score == -BUSY_SCORE) {
            if (deferredMoves == null) {
              deferredMoves = new ArrayList<>();
            }
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
                  killerTable.add(distFromRoot, move);
                  // Record success in the relative history table.
                  historyTable.recordSuccessfulMove(move);
                  break Search;
                }
              }
            }
            // Record failure in the relative history table.
            historyTable.recordUnsuccessfulMove(move);
          }
        }
        // If moves searched by other threads were rescheduled, search them now.
        if (deferredMoves != null) {
          for (Move deferredMove : deferredMoves) {
            int searchDepth;
            boolean isKiller;
            boolean isMaterial = deferredMove.isTactical();
            if (isMaterial) {
              isKiller = false;
              // Recapture extension.
              int extension = isMaterial && lastMoveIsTactical &&
                  deferredMove.capturedPiece != Piece.NULL.ind &&
                  deferredMove.to == lastMove.to ? params.recapExtension : 0;
              searchDepth = Math.min(depthLimit, depth + extension) - FULL_PLY;
            } else {
              // Check if it is a killer move.
              isKiller = true;
              if (isThereKM1 && deferredMove.equals(killerMove1)) {
                isThereKM1 = false;
              } else if (isThereKM2 && deferredMove.equals(killerMove2)) {
                isThereKM2 = false;
              } else {
                isKiller = false;
              }
              searchDepth = depth - FULL_PLY;

            }
            pos.makeMove(deferredMove);
            try {
              // LMR if possible.
              if (!isMaterial && !isKiller && !dangerous && !pos.isInCheck() &&
                  searchedMoves > params.minMovesSearchedForLmr &&
                  depth / FULL_PLY >= params.lateMoveReductionMinDepthLeft) {
                score = -pvSearch(searchDepth - lateMoveReduction, distFromRoot + 1, -alpha - 1, -alpha, true, false);
                searchStats.lateMoveReductions.incrementAndGet();
                // If it does not fail low, research with full window.
                if (score > alpha) {
                  score = -pvSearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
                } else {
                  searchStats.successfulLateMoveReductions.incrementAndGet();
                }
              } else { // PVS.
                score = -pvSearch(searchDepth, distFromRoot + 1, -alpha - 1, -alpha, true, false);
                if (score > alpha && score < beta) {
                  score = -pvSearch(searchDepth, distFromRoot + 1, -beta, -alpha, true, false);
                }
              }
            } catch (AbnormalSearchTerminationException e) {
              resetBusyFlag(nodeBlocked);
              throw e;
            }
            pos.unmakeMove();
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
                    killerTable.add(distFromRoot, deferredMove);
                    // Record success in the relative history table.
                    historyTable.recordSuccessfulMove(deferredMove);
                  } else {
                    (deferredMove.getValue() >= 0 ? searchStats.winningTacticalCutoffs :
                        searchStats.losingTacticalCutoffs).incrementAndGet();
                  }
                  break Search;
                }
              }
            }
            // Record failure in the relative history table.
            if (!isMaterial) {
              historyTable.recordUnsuccessfulMove(deferredMove);
            }
          }
        }
      }
      // Add new entry to the transposition table.
      if (!insertIntoTt(pos.getKey(), origAlpha, beta, bestMove, bestScore, (short) distFromRoot, (short) (depth / FULL_PLY))) {
        // If it is not good enough, make sure to reset the busy flag if it was set.
        resetBusyFlag(nodeBlocked);
      }
      // Return the unadjusted best score.
      return bestScore;
    }

    @Override
    public Integer call() {
      int depth = ply * FULL_PLY;
      final int depthLimit = depth + FULL_PLY;
      final int origAlpha = alpha;
      int alpha = this.alpha;
      int beta = this.beta;
      int bestScore = Score.MIN.value;
      Move bestMove = null;
      Move hashMove = null;
      boolean infoUpdated = false;
      try {
        doStopSearchThread = false;
        pos = new Position(origPos);
        // If ply equals 0, perform quiescence search only.
        if (ply == 0) {
          return quiescence(0, alpha, beta);
        }
        searchStats.mainNodes.incrementAndGet();
        // Check for the 3-fold repetition rule.
        if (pos.hasRepeated(2)) {
          return (int) Score.DRAW_CLAIMED.value;
        }
        // Generate moves.
        List<Move> moves = new ArrayList<>(rootMoves);
        // Mate check.
        if (moves.isEmpty()) {
          return (int) (pos.isInCheck() ? Score.LOSING_CHECK_MATE.value : Score.STALE_MATE.value);
        }
        // Check for the 50 move rule.
        if (pos.getFiftyMoveRuleClock() >= 100) {
          return (int) Score.DRAW_CLAIMED.value;
        }
        // In non-analysis mode, terminate prematurely if there is only one legal response at the root.
        if (!analysisMode && !ponder && moves.size() == 1) {
          bestScore = Score.NULL.value;
          synchronized (rootLock) {
            insertIntoTt(pos.getKey(), origAlpha, beta, moves.get(0), bestScore, (short) 0, (short) (depth / FULL_PLY));
          }
          return bestScore;
        }
        // Check extension.
        depth = pos.isInCheck() ? depth + params.checkExtension : depth;
        // Pawn push extension
        Move lastMove = pos.getLastMove();
        depth = lastMove != null && isPawnPush(lastMove) ? depth + params.pawnPushExtension : depth;
        // Hash look-up.
        TTEntry entry = transTable.get(pos.getKey());
        if (entry != null) {
          int hashDepth = entry.getDepth();
          int hashType = entry.getType();
          int hashScore = entry.getScore();
          int hashMoveInt = entry.getBestMove();
          // Make sure the entry was not modified while retrieving the information contained in it.
          if (entry.hashKey() == pos.getKey()) {
            entry.setGeneration(hashEntryGen);
            searchStats.hashHits.incrementAndGet();
            /* If the hashed entry's depth is greater than or equal to the current search depth, check if
             * the stored score is usable. */
            if (hashDepth >= depth / FULL_PLY && hashScore != Score.NULL.value) {
              /* If the score was exact, or it was the score of an all node and is smaller than or equal
               * to alpha, or it is that of a cut node and is greater than or equal to beta, return the
               * score. Only take an exact score if is outside the bounds to avoid the truncation of the
               * PV line. */
              if ((hashType == NodeType.EXACT.ind && (hashScore <= alpha || hashScore >= beta)) ||
                  (hashType == NodeType.FAIL_HIGH.ind && hashScore >= beta) ||
                  (hashType == NodeType.FAIL_LOW.ind && hashScore <= alpha)) {
                if (hashDepth > selDepth) {
                  selDepth = hashDepth;
                }
                bestScore = hashScore;
                searchStats.hashScoreHits.incrementAndGet();
                return bestScore;
              }
            }
            if (hashMoveInt != 0) {
              hashMove = Move.toMove(hashMoveInt);
            }
          }
        }
        // If there is a hash move, make sure it is the first element.
        if (hashMove != null && moves.contains(hashMove)) {
          moves.remove(hashMove);
          moves.add(0, hashMove);
        }
        boolean lastMoveIsMaterial = lastMove != null && lastMove.isTactical();
        boolean[] searched = new boolean[moves.size()];
        int searchedMoves = 0;
        RootSearch:
        for (int i = 0; i < 2; i++) {
          // Iterate over moves.
          for (int moveInd = 0; moveInd < moves.size(); moveInd++) {
            if (searched[moveInd]) {
              continue;
            }
            Move move = moves.get(moveInd);
            nodes = movesToNodes.get(move);
            // Recapture extension.
            int extension = lastMoveIsMaterial && move.capturedPiece != Piece.NULL.ind && move.to == lastMove.to ?
                params.recapExtension : 0;
            int score = pvSearchMove(move, depthLimit, depth, extension, 0, searchedMoves, alpha, beta, i == 0, false);
            if (score == -BUSY_SCORE) {
              continue;
            }
            if (i == 0) {
              searched[moveInd] = true;
            } else {
              searchStats.delayedNodes.incrementAndGet();
            }
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
                    insertIntoTt(pos.getKey(), origAlpha, beta, move, score, (short) 0,
                        (short) (depth / FULL_PLY));
                    updateInfo(pos, move, searchedMoves, ply, origAlpha, beta, score);
                    infoUpdated = true;
                  }
                }
                if (score >= beta) {
                  break RootSearch;
                }
              }
            }
          }
        }
        /* If a slave thread finished first or if the main thread failed low, try to insert the results into
         * the TT. */
        if (!isMainSearchThread || bestScore <= origAlpha) {
          synchronized (rootLock) {
            insertIntoTt(pos.getKey(), origAlpha, beta, bestMove, bestScore, (short) 0, (short) (depth / FULL_PLY));
            // If it is the main thread, update the search info with the fail low score.
            if (isMainSearchThread) {
              updateInfo(pos, null, 0, ply, origAlpha, beta, bestScore);
              infoUpdated = true;
            } else {
              // If it is an early finisher slave thread, set the result of the master thread and stop it.
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
            if (!infoUpdated) {
              updateInfo(rootPos, null, 0, ply, origAlpha, beta, bestScore);
            }
          }
        }
        return bestScore;
      } finally {
        if (!isMainSearchThread) {
          latch.countDown();
        }
      }
    }
  }

  /**
   * A simple, thread-safe container class for search statistics in the form of atomic counters.
   *
   * @author Viktor
   */
  private class SearchStats {

    final AtomicLong mainNodes, quiescenceNodes, busyNodes, delayedNodes, hashHits, hashScoreHits, iids,
        successfulIids, nullMoveReductions, nullMoveCutoffs, reverseRazorCutoffs, hashMoveCutoffs,
        winningTacticalCutoffs, killerCutoffs, losingTacticalCutoffs, quietCutoffs, futilityPrunes,
        lateMoveReductions, successfulLateMoveReductions;

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
              "iids %d iid_success_rate %.2f nmrs %d nmr_success_rate %.2f nullmove_cutoffs %.2f " +
              "razor_cutoffs %.2f h_move_cutoffs %.2f wtactical_cutoffs %.2f killer_cutoffs %.2f " +
              "ltactical_cutoffs %.2f quiet_cutoffs %.2f futility_prunes %d lmrs %d lmr_success_rate %.2f",
          mainNodes.get(), quiescenceNodes.get(), busyNodes.get(), delayedNodes.get(),
          ((double) hashHits.get()) / mainNodes.get(), ((double) hashScoreHits.get()) / mainNodes.get(),
          iids.get(), ((double) successfulIids.get()) / iids.get(), nullMoveReductions.get(),
          ((double) nullMoveCutoffs.get()) / nullMoveReductions.get(),
          ((double) nullMoveCutoffs.get()) / mainNodes.get(),
          ((double) reverseRazorCutoffs.get()) / mainNodes.get(),
          ((double) hashMoveCutoffs.get()) / mainNodes.get(),
          ((double) winningTacticalCutoffs.get()) / mainNodes.get(),
          ((double) killerCutoffs.get()) / mainNodes.get(),
          ((double) losingTacticalCutoffs.get()) / mainNodes.get(),
          ((double) quietCutoffs.get()) / mainNodes.get(), futilityPrunes.get(), lateMoveReductions.get(),
          ((double) successfulLateMoveReductions.get()) / lateMoveReductions.get());
    }

  }

  /**
   * A {@link java.lang.RuntimeException} for when a search is cancelled or the maximum number of nodes to search have been exceeded and
   * thus the search is interrupted resulting in an abrupt, disorderly termination.
   *
   * @author Viktor
   */
  private class AbnormalSearchTerminationException extends RuntimeException {

    /**
     * Default serial version ID.
     */
    private static final long serialVersionUID = 1L;

  }

}
