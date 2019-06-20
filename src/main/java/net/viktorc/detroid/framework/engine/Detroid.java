package net.viktorc.detroid.framework.engine;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import net.viktorc.detroid.framework.engine.GaviotaTableBaseJNI.CompressionScheme;
import net.viktorc.detroid.framework.engine.OpeningBook.SelectionModel;
import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.uci.DebugInformation;
import net.viktorc.detroid.framework.uci.Option;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchInformation;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.util.Cache;
import net.viktorc.detroid.framework.util.SizeEstimator;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A UCI compatible, tunable chess engine that utilizes magic bitboards and most search heuristics and supports Polyglot opening books and
 * Gaviota endgame tablebases.
 *
 * @author Viktor
 */
public class Detroid implements ControllerEngine, TunableEngine {

  private static final float VERSION_NUMBER = 1.0f;
  private static final String NAME = "DETROID " + VERSION_NUMBER;
  private static final String AUTHOR = "Viktor Csomor";
  // Search, evaluation, and time control parameters.
  private static final String DEFAULT_PARAMETERS_FILE_PATH = "params.xml";
  // The default path to the Polyglot opening book (compiled using SCID 4.62, PGN-Extract 17-21 and Polyglot 1.4w).
  private static final String DEFAULT_BOOK_FILE_PATH = "book.bin";
  // The default path to the Gaviota probing library.
  private static final String DEFAULT_EGTB_LIB_PATH;

  // Determine the default path to the Gaviota EGTB probing library based on the OS.
  static {
    boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
    boolean arch32 = "32".equals(System.getProperty("sun.arch.data.model"));
    String lib = windows ? arch32 ? "gtb32.dll" : "gtb.dll" :
        arch32 ? "libgtb32.so" : "libgtb.so";
    DEFAULT_EGTB_LIB_PATH = "gtb/" + lib;
  }

  // The default path to the 3 and 4 men Gaviota endgame tablebases.
  private static final String DEFAULT_EGTB_FOLDERS_PATH = "gtb/3;gtb/4";
  // The default compression scheme of the Gaviotat endgame tablebase files.
  private static final CompressionScheme DEFAULT_EGTB_COMP_SCHEME = CompressionScheme.CP4;
  // The minimum allowed number of search threads to use.
  private static final int MIN_SEARCH_THREADS = 1;
  // The maximum allowed number of search threads to use.
  private static final int MAX_SEARCH_THREADS = Runtime.getRuntime().availableProcessors();
  // The default number of search threads to use.
  private static final int DEFAULT_SEARCH_THREADS = Math.max(MIN_SEARCH_THREADS, MAX_SEARCH_THREADS / 2);
  // The minimum allowed hash size in MB.
  private static final int MIN_HASH_SIZE = 1;
  // The maximum allowed hash size in MB.
  private static final int MAX_HASH_SIZE = (int) (Runtime.getRuntime().maxMemory() / (2L << 20));
  // The default hash size in MB.
  private static final int DEFAULT_HASH_SIZE = Math.min(DEFAULT_SEARCH_THREADS * 32, MAX_HASH_SIZE);
  // The minimum allowed endgame tablebase cache size in MB.
  private static final int MIN_EGTB_CACHE_SIZE = 0;
  // The maximum allowed endgame tablebase cache size in MB.
  private static final int MAX_EGTB_CACHE_SIZE = Math.min(256, MAX_HASH_SIZE);
  // The default endgame tablebase cache size in MB.
  private static final int DEFAULT_EGTB_CACHE_SIZE = Math.min(DEFAULT_SEARCH_THREADS * 16, MAX_EGTB_CACHE_SIZE);

  private final Object mainLock;
  private final Object searchLock;
  private final Object stopLock;

  private Option<?> hashSize;
  private Option<?> clearHash;
  private Option<?> ponder;
  private Option<?> ownBook;
  private Option<?> primaryBookPath;
  private Option<?> secondaryBookPath;
  private Option<?> egtbLibPath;
  private Option<?> egtbFilesPath;
  private Option<?> egtbCompScheme;
  private Option<?> egtbCacheSize;
  private Option<?> egtbClearCache;
  private Option<?> numOfSearchThreads;
  private Option<?> parametersPath;
  private Option<?> uciOpponent;
  private Option<?> uciAnalysis;
  private Map<Option<?>, Object> options;

  private DetroidParameters params;
  private DetroidSearchInformation searchInfo;
  private DetroidDebugInfo debugInfo;
  private OpeningBook book;
  private EndGameTableBase egtb;
  private Game game;
  private Evaluator eval;
  private Cache<TTEntry> transTable;
  private Cache<ETEntry> evalTable;
  private ExecutorService executor;
  private Future<SearchResults> search;
  private volatile boolean bookMove;
  private volatile boolean outOfBook;
  private volatile boolean init;
  private volatile boolean debugMode;
  private volatile boolean controllerMode;
  private volatile boolean deterministicEvalMode;
  private volatile boolean stop;
  private volatile boolean ponderHit;
  private volatile boolean newGame;
  private volatile byte gen;

  /**
   * Instantiates the chess engine object.
   */
  public Detroid() {
    mainLock = new Object();
    searchLock = new Object();
    stopLock = new Object();
  }

  private void setHashSize(int hashSize) {
    long sizeInBytes = hashSize * 1024L * 1024L;
    SizeEstimator estimator = SizeEstimator.getInstance();
    double transTableShare = ((double) params.transTableShare16th) / 16;
    transTable = new Cache<>(TTEntry::new, (int) (sizeInBytes * transTableShare / estimator.sizeOf(TTEntry.class)));
    evalTable = new Cache<>(ETEntry::new, (int) (sizeInBytes * (1d - transTableShare) / estimator.sizeOf(ETEntry.class)));
    gen = 0;
    // Prompt for garbage collection.
    System.gc();
    if (debugMode) {
      debugInfo.set("Hash capacity data\n" +
          "Transposition table capacity - " + transTable.capacity() + "\n" +
          "Evaluation table capacity - " + evalTable.capacity());
    }
  }

  private void clearHash() {
    transTable.clear();
    evalTable.clear();
    gen = 0;
    if (debugMode) {
      debugInfo.set("Hash tables cleared");
    }
  }

  private void setPlayerNames() {
    if (newGame) {
      if (game.isWhitesTurn()) {
        setPlayers(NAME, (String) options.get(uciOpponent));
      } else {
        setPlayers((String) options.get(uciOpponent), NAME);
      }
      if (debugMode) {
        debugInfo.set("Players' names set\n" +
            "White - " + game.getWhitePlayerName() + "\n" +
            "Black - " + game.getBlackPlayerName());
      }
      newGame = false;
    }
  }

  private Set<Move> getAllowedMoves(Set<String> searchMoves) {
    Set<Move> allowedMoves = null;
    if (searchMoves != null && !searchMoves.isEmpty()) {
      allowedMoves = new HashSet<>();
      try {
        Position pos = game.getPosition();
        for (String s : searchMoves) {
          allowedMoves.add(MoveStringUtils.parsePACN(pos, s));
        }
      } catch (ChessParseException | NullPointerException e) {
        if (debugMode) {
          debugInfo.set("Search moves could not be parsed\n" + e.getMessage());
        }
      }
    }
    return allowedMoves;
  }

  private int movesLeftBasedOnPhaseScore(int phaseScore) {
    double movesToGoInterval = params.maxMovesToGo - params.minMovesToGo;
    double remainingMoves = movesToGoInterval * phaseScore / Position.MAX_PHASE_SCORE;
    return (int) Math.round(params.minMovesToGo + remainingMoves);
  }

  private long computeSearchTime(Long whiteTime, Long blackTime, Integer movesToGo, int phaseScore, boolean extension) {
    Long remainingTime = game.isWhitesTurn() ? whiteTime : blackTime;
    if (remainingTime == null || remainingTime <= 0) {
      return 0;
    }
    if (movesToGo == null) {
      movesToGo = movesLeftBasedOnPhaseScore(phaseScore);
      if (debugMode) {
        debugInfo.set("Search time data\n" +
            "Phase score - " + phaseScore + "/256\n" +
            "Expected number of moves left until end - " + movesToGo);
      }
    }
    double remainingUsableTime = (double) (remainingTime * params.totalTimePortionToUse16th) / 16;
    if (extension) {
      remainingUsableTime *= (double) params.maxTimePortionToUseForExtension16th / 16;
    }
    long target = Math.round(remainingUsableTime / Math.max(1d, movesToGo));
    return Math.max(1, target);
  }

  private boolean doExtendSearch() {
    return searchInfo.getScoreType() == ScoreType.LOWER_BOUND || searchInfo.getScoreType() == ScoreType.UPPER_BOUND;
  }

  private boolean doTerminateSearchPrematurely(long searchTime, long timeLeft) {
    return timeLeft < searchTime * params.minTimePortionNeededForExtraDepth16th / 16 && !doExtendSearch();
  }

  private void manageSearchTime(Long searchTime, Long whiteTime, Long blackTime, Integer movesToGo)
      throws ExecutionException, InterruptedException {
    try {
      boolean fixed = searchTime != null && searchTime > 0;
      if (debugMode) {
        debugInfo.set("Search time fixed: " + fixed);
      }
      if (fixed) {
        try {
          search.get(searchTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
          if (debugMode) {
            debugInfo.set("Fixed search time up");
          }
        }
      } else {
        int phaseScore = game.getPosition().getPhaseScore();
        long time = computeSearchTime(whiteTime, blackTime, movesToGo, phaseScore, false);
        long timeLeft = time;
        boolean doTerminate = false;
        if (debugMode) {
          debugInfo.set("Base search time - " + time);
        }
        synchronized (search) {
          while (!search.isDone() && timeLeft > 0 && !doTerminate) {
            long start = System.currentTimeMillis();
            search.wait(timeLeft);
            timeLeft -= (System.currentTimeMillis() - start);
            doTerminate = !search.isDone() && timeLeft > 0 && doTerminateSearchPrematurely(time, timeLeft);
          }
        }
        if (debugMode) {
          String reason = search.isDone() ? "search terminated" : doTerminate ?
              "search cancelled to save time" : "time up";
          debugInfo.set(String.format("Base search over. Reason: %s", reason));
        }
        // If time was up, check if the search should be extended.
        if (!doTerminate && !search.isDone() && doExtendSearch()) {
          try {
            if (debugMode) {
              debugInfo.set("Search extended.");
            }
            search.get(computeSearchTime(game.isWhitesTurn() ? whiteTime - time : whiteTime,
                game.isWhitesTurn() ? blackTime : blackTime - time, movesToGo, phaseScore, true), TimeUnit.MILLISECONDS);
          } catch (TimeoutException e) {
            if (debugMode) {
              debugInfo.set("Extra time up");
            }
          } catch (CancellationException e) {
            // Let the block terminate.
          }
        }
      }
    } finally {
      // Time is up, cancel the search.
      if (!search.isDone()) {
        search.cancel(true);
      }
    }
  }

  private void waitForSearch() throws ExecutionException, InterruptedException {
    try {
      search.get();
    } catch (InterruptedException e) {
      if (debugMode) {
        debugInfo.set(e.getMessage());
      }
      Thread.currentThread().interrupt();
      throw e;
    } catch (ExecutionException e) {
      if (debugMode) {
        debugInfo.set(e.getMessage());
      }
      throw e;
    }
  }

  private String getRandomMove() {
    int i = 0;
    List<Move> moves = game.getPosition().getMoves();
    if (moves.size() == 0) {
      return null;
    }
    String[] arr = new String[moves.size()];
    for (Move m : moves) {
      arr[i++] = m.toString();
    }
    return arr[(int) (Math.random() * arr.length)];
  }

  private SearchResults searchBook(Boolean ponder, Long whiteTime, Long blackTime, Integer movesToGo, Boolean infinite) {
    long bookSearchStart = System.currentTimeMillis();
    synchronized (searchLock) {
      if (stop) {
        return null;
      }
      search = executor.submit(() -> {
        Move bookMove;
        try {
          bookMove = book.getMove(game.getPosition(), SelectionModel.STOCHASTIC);
        } catch (Exception e) {
          bookMove = null;
        }
        return new SearchResults(bookMove == null ? null : bookMove.toString(), null, null, null);
      });
    }
    if (debugMode) {
      debugInfo.set("Book search started");
    }
    SearchResults results = null;
    try {
      results = search.get();
      bookMove = results != null && results.getBestMove() != null;
    } catch (InterruptedException | ExecutionException e) {
      if (debugMode) {
        debugInfo.set(e.getMessage());
      }
      Thread.currentThread().interrupt();
    } catch (CancellationException e) {
      // Let the method finish.
    }
    if (debugMode) {
      debugInfo.set("Book search done");
    }
    // If the book search has not been externally stopped, use the remaining time for a normal search if no move was found.
    if (!stop && !bookMove) {
      if (debugMode) {
        debugInfo.set("No book move found. Out of book.");
      }
      outOfBook = true;
      results = searchGameTree(null, ponder, game.isWhitesTurn() ? (whiteTime != null ?
              whiteTime - (System.currentTimeMillis() - bookSearchStart) : null) : whiteTime,
          game.isWhitesTurn() ? blackTime : (blackTime != null ? blackTime - (System.currentTimeMillis() - bookSearchStart) : null),
          movesToGo, null, null, null, null, infinite);
    }
    return results;
  }

  private void startGameTreeSearch(Set<Move> allowedMoves, boolean doPonder, boolean doInfinite, Integer depth, Long nodes,
      Integer mateDistance) {
    if (egtb.isProbingLibLoaded() && egtb.isInit()) {
      egtb.resetStats();
    }
    boolean analysisMode = (Boolean) options.get(uciAnalysis);
    Search gameTreeSearch = new Search(game.getPosition(), params, eval, egtb, searchInfo, (int) options.get(numOfSearchThreads),
        transTable, gen, analysisMode, doPonder || doInfinite,
        depth == null ? (mateDistance == null ? Integer.MAX_VALUE : mateDistance) : depth, nodes == null ? Long.MAX_VALUE : nodes,
        allowedMoves);
    search = gameTreeSearch;
    executor.submit(gameTreeSearch);
    if (debugMode) {
      debugInfo.set("Search started");
    }
  }

  private void waitForGameTreeSearchInPonderingMode(Long searchTime, Long whiteTime, Long blackTime, Integer movesToGo)
      throws ExecutionException, InterruptedException {
    // If in ponder mode, run the search until the ponder hit signal or until it is externally stopped.
    if (debugMode) {
      debugInfo.set("In ponder mode");
    }
    synchronized (searchLock) {
      while (!stop && !ponderHit) {
        searchLock.wait();
      }
    }
    if (debugMode) {
      debugInfo.set("Ponder stopped");
    }
    // If the search terminated due to a ponder hit, keep searching...
    if (ponderHit) {
      if (debugMode) {
        debugInfo.set("Ponderhit acknowledged");
      }
      ponderHit = false;
      // Time based search.
      manageSearchTime(searchTime, whiteTime, blackTime, movesToGo);
    } else {
      // If it did not, return the best move found.
      waitForSearch();
    }
  }

  private void waitForGameTreeSearchInInfiniteMode() throws ExecutionException, InterruptedException {
    // If in infinite mode, let the search run until it is externally stopped.
    if (debugMode) {
      debugInfo.set("In infinite mode");
    }
    waitForSearch();
  }

  private void waitForGameTreeSearchInFixedMode() throws ExecutionException, InterruptedException {
    // Fixed node or depth search
    if (debugMode) {
      debugInfo.set("In fixed mode");
    }
    waitForSearch();
  }

  private SearchResults collectGameTreeSearchResults() {
    try {
      return search.get();
    } catch (InterruptedException e) {
      if (debugMode) {
        debugInfo.set(e.getMessage());
      }
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException e) {
      if (debugMode) {
        debugInfo.set(e.getMessage());
      }
      return null;
    }
  }

  private SearchResults searchGameTree(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime, Integer movesToGo,
      Integer depth, Long nodes, Integer mateDistance, Long searchTime, Boolean infinite) {
    boolean doPonder = false;
    // Check if pondering is possible.
    if (ponder != null && ponder) {
      if (!(Boolean) options.get(this.ponder)) {
        if (debugMode) {
          debugInfo.set("Ponder mode started with ponder option disallowed - Abort");
        }
        return null;
      } else {
        doPonder = true;
      }
    }
    boolean doInfinite = (infinite != null && infinite) || (!doPonder && whiteTime == null && blackTime == null && depth == null &&
        nodes == null && mateDistance == null && searchTime == null);
    // Set root move restrictions if there are any.
    Set<Move> allowedMoves = getAllowedMoves(searchMoves);
    // Start the search.
    synchronized (searchLock) {
      if (stop) {
        return null;
      }
      startGameTreeSearch(allowedMoves, doPonder, doInfinite, depth, nodes, mateDistance);
    }
    try {
      if (doPonder) {
        waitForGameTreeSearchInPonderingMode(searchTime, whiteTime, blackTime, movesToGo);
      } else if (doInfinite) {
        waitForGameTreeSearchInInfiniteMode();
      } else if (whiteTime == null && blackTime == null && searchTime == null) {
        waitForGameTreeSearchInFixedMode();
      } else {
        manageSearchTime(searchTime, whiteTime, blackTime, movesToGo);
      }
    } catch (Exception e) {
      if (debugMode) {
        debugInfo.set(e.getMessage());
      }
      return null;
    }
    if (debugMode) {
      debugInfo.set("Search stopped");
    }
    // Return the final results.
    return collectGameTreeSearchResults();
  }

  private long perft(Position pos, int depth) {
    long leafNodes = 0;
    List<Move> moves = pos.getMoves();
    if (depth == 1) {
      return moves.size();
    }
    for (Move m : moves) {
      pos.makeMove(m);
      leafNodes += perft(pos, depth - 1);
      pos.unmakeMove();
    }
    return leafNodes;
  }

  @Override
  public void init() throws Exception {
    synchronized (mainLock) {
      params = new DetroidParameters();
      params.loadFrom(DEFAULT_PARAMETERS_FILE_PATH);
      try {
        book = new PolyglotBook(DEFAULT_BOOK_FILE_PATH);
      } catch (Exception e) {
        // It's okay if the opening book can't be initialized.
      }
      egtb = GaviotaTableBaseJNI.getInstance();
      egtb.loadProbingLibrary(DEFAULT_EGTB_LIB_PATH);
      if (egtb.isProbingLibLoaded()) {
        egtb.init(DEFAULT_EGTB_FOLDERS_PATH, DEFAULT_EGTB_CACHE_SIZE * 1024L * 1024L,
            DEFAULT_EGTB_COMP_SCHEME);
      }
      debugInfo = new DetroidDebugInfo();
      debugMode = false;
      controllerMode = false;
      deterministicEvalMode = false;
      ponderHit = false;
      stop = false;
      game = new Game();
      options = new LinkedHashMap<>();
      parametersPath = new Option.StringOption("ParametersPath", DEFAULT_PARAMETERS_FILE_PATH);
      numOfSearchThreads = new Option.SpinOption(THREADS_OPTION_NAME, DEFAULT_SEARCH_THREADS, MIN_SEARCH_THREADS,
          MAX_SEARCH_THREADS);
      hashSize = new Option.SpinOption(HASH_OPTION_NAME, DEFAULT_HASH_SIZE, MIN_HASH_SIZE, MAX_HASH_SIZE);
      clearHash = new Option.ButtonOption("ClearHash");
      ownBook = new Option.CheckOption(OWN_BOOK_OPTION_NAME, true);
      primaryBookPath = new Option.StringOption("PolyglotBookPrimaryPath", DEFAULT_BOOK_FILE_PATH);
      secondaryBookPath = new Option.StringOption("PolyglotBookSecondaryPath", "");
      egtbLibPath = new Option.StringOption("GaviotaTbLibPath", DEFAULT_EGTB_LIB_PATH);
      egtbFilesPath = new Option.StringOption("GaviotaTbPath", DEFAULT_EGTB_FOLDERS_PATH);
      egtbCompScheme = new Option.ComboOption("GaviotaTbCompScheme", DEFAULT_EGTB_COMP_SCHEME.toString(),
          new TreeSet<>(Arrays.stream(CompressionScheme.values()).map(Object::toString)
              .collect(Collectors.toList())));
      egtbCacheSize = new Option.SpinOption("GaviotaTbCache", DEFAULT_EGTB_CACHE_SIZE, MIN_EGTB_CACHE_SIZE,
          MAX_EGTB_CACHE_SIZE);
      egtbClearCache = new Option.ButtonOption("GaviotaTbClearCache");
      ponder = new Option.CheckOption("Ponder", true);
      uciOpponent = new Option.StringOption("UCI_Opponent", "?");
      uciAnalysis = new Option.CheckOption("UCI_AnalyseMode", false);
      options.put(parametersPath, parametersPath.getDefaultValue().get());
      options.put(numOfSearchThreads, numOfSearchThreads.getDefaultValue().get());
      options.put(hashSize, hashSize.getDefaultValue().get());
      options.put(clearHash, null);
      options.put(ownBook, ownBook.getDefaultValue().get());
      options.put(primaryBookPath, primaryBookPath.getDefaultValue().get());
      options.put(secondaryBookPath, secondaryBookPath.getDefaultValue().get());
      options.put(egtbLibPath, egtbLibPath.getDefaultValue().get());
      options.put(egtbFilesPath, egtbFilesPath.getDefaultValue().get());
      options.put(egtbCompScheme, DEFAULT_EGTB_COMP_SCHEME);
      options.put(egtbCacheSize, egtbCacheSize.getDefaultValue().get());
      options.put(egtbClearCache, null);
      options.put(ponder, ponder.getDefaultValue().get());
      options.put(uciOpponent, uciOpponent.getDefaultValue().get());
      options.put(uciAnalysis, uciAnalysis.getDefaultValue().get());
      searchInfo = new DetroidSearchInformation();
      setHashSize(controllerMode || deterministicEvalMode ? MIN_HASH_SIZE : DEFAULT_HASH_SIZE);
      eval = new Evaluator(params, controllerMode || deterministicEvalMode ? null : evalTable);
      executor = Executors.newSingleThreadExecutor();
      init = true;
    }
  }

  @Override
  public boolean isInit() {
    synchronized (mainLock) {
      return init;
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getAuthor() {
    return AUTHOR;
  }

  @Override
  public Map<Option<?>, Object> getOptions() {
    synchronized (mainLock) {
      return new LinkedHashMap<>(options);
    }
  }

  @Override
  public <T> boolean setOption(Option<T> setting, T value) {
    synchronized (mainLock) {
      try {
        if (hashSize.equals(setting)) {
          int val = (Integer) value;
          if (MIN_HASH_SIZE <= val && MAX_HASH_SIZE >= val) {
            if (val != (Integer) options.get(hashSize)) {
              setHashSize(val);
              eval = new Evaluator(params, controllerMode || deterministicEvalMode ? null : evalTable);
              options.put(hashSize, value);
            }
            if (debugMode) {
              debugInfo.set("Hash size successfully set to " + value);
            }
            return true;
          }
        } else if (clearHash.equals(setting)) {
          clearHash();
          return true;
        } else if (ponder.equals(setting)) {
          options.put(ponder, value);
          if (debugMode) {
            debugInfo.set("Ponder successfully set to " + value);
          }
          return true;
        } else if (ownBook.equals(setting)) {
          if (book != null) {
            options.put(ownBook, value);
            outOfBook = false;
            if (debugMode) {
              debugInfo.set("Use of own book successfully set to " + value);
            }
            return true;
          }
          if (debugMode) {
            debugInfo.set("No book file found at " + options.get(primaryBookPath));
          }
        } else if (primaryBookPath.equals(setting)) {
          try {
            String secondaryFilePath = book == null ? null : book.getSecondaryFilePath();
            PolyglotBook newBook = new PolyglotBook((String) value, secondaryFilePath);
            if (book != null) {
              try {
                book.close();
              } catch (IOException e) {
                if (debugMode) {
                  debugInfo.set(e.getMessage());
                }
              }
            }
            book = newBook;
            options.put(primaryBookPath, book.getPrimaryFilePath());
            outOfBook = false;
            if (debugMode) {
              debugInfo.set("Primary book file path successfully set to " + value);
            }
            return true;
          } catch (IOException e) {
            if (debugMode) {
              debugInfo.set(e.getMessage());
            }
          }
        } else if (secondaryBookPath.equals(setting)) {
          if (book != null) {
            try {
              PolyglotBook newBook = new PolyglotBook(book.getPrimaryFilePath(), (String) value);
              try {
                book.close();
              } catch (IOException e) {
                if (debugMode) {
                  debugInfo.set(e.getMessage());
                }
              }
              book = newBook;
              options.put(secondaryBookPath, book.getSecondaryFilePath());
              outOfBook = false;
              if (debugMode) {
                debugInfo.set("Secondary book file path successfully set to " + value);
              }
              return true;
            } catch (IOException e) {
              if (debugMode) {
                debugInfo.set(e.getMessage());
              }
            }
          }
        } else if (egtbLibPath.equals(setting)) {
          try {
            egtb.loadProbingLibrary((String) value);
            if (egtb.isProbingLibLoaded()) {
              options.put(egtbLibPath, value);
              if (debugMode) {
                debugInfo.set("EGTB library path successfully set to " + value);
              }
              return true;
            }
          } catch (Exception e) {
            if (debugMode) {
              debugInfo.set(e.getMessage());
            }
          }
        } else if (egtbFilesPath.equals(setting)) {
          if (egtb.isProbingLibLoaded()) {
            egtb.init((String) value, ((Integer) options.get(egtbCacheSize)) * 1024L * 1024L,
                ((CompressionScheme) options.get(egtbCompScheme)));
            if (egtb.isInit()) {
              options.put(egtbFilesPath, value);
              if (debugMode) {
                debugInfo.set("EGTB files path successfully set to " + value +
                    "; available tablebases: " + egtb.areTableBasesAvailable(1) + ", " +
                    egtb.areTableBasesAvailable(2) + ", " + egtb.areTableBasesAvailable(3) + ", " +
                    egtb.areTableBasesAvailable(4) + ", " + egtb.areTableBasesAvailable(5));
              }
              return true;
            } else {
              egtb.init((String) options.get(egtbFilesPath), ((Integer) options
                  .get(egtbCacheSize)) * 1024L * 1024L, ((CompressionScheme) options
                  .get(egtbCompScheme)));
            }
          }
        } else if (egtbCompScheme.equals(setting)) {
          if (egtb.isProbingLibLoaded()) {
            String comp = (String) value;
            CompressionScheme scheme = Arrays.stream(CompressionScheme.values())
                .filter(c -> comp.equals(c.name()))
                .collect(Collectors.toList()).get(0);
            egtb.init((String) options.get(egtbFilesPath), ((Integer) options
                .get(egtbCacheSize)) * 1024L * 1024L, scheme);
            if (egtb.isInit()) {
              options.put(egtbCompScheme, scheme);
              if (debugMode) {
                debugInfo.set("EGTB compression scheme successfully set to " + value);
              }
              return true;
            } else {
              egtb.init((String) options.get(egtbFilesPath), ((Integer) options
                  .get(egtbCacheSize)) * 1024L * 1024L, ((CompressionScheme) options
                  .get(egtbCompScheme)));
            }
          }
        } else if (egtbCacheSize.equals(setting)) {
          if (egtb.isProbingLibLoaded()) {
            int val = (Integer) value;
            if (MIN_EGTB_CACHE_SIZE <= val && MAX_EGTB_CACHE_SIZE >= val) {
              if (val != (Integer) options.get(egtbCacheSize)) {
                egtb.init((String) options.get(egtbFilesPath), val * 1024L * 1024L,
                    ((CompressionScheme) options.get(egtbCompScheme)));
                options.put(egtbCacheSize, value);
              }
              if (egtb.isInit()) {
                if (debugMode) {
                  debugInfo.set("EGTB cache size successfully set to " + value);
                }
                return true;
              }
            }
          }
        } else if (egtbClearCache.equals(setting)) {
          if (egtb.isProbingLibLoaded()) {
            egtb.clearCache();
            return true;
          }
        } else if (numOfSearchThreads.equals(setting)) {
          if (value != null && MIN_SEARCH_THREADS <= (Integer) value &&
              MAX_SEARCH_THREADS >= (Integer) value) {
            options.put(numOfSearchThreads, value);
            if (debugMode) {
              debugInfo.set("Number of search threads successfully set to " + value);
            }
            return true;
          }
        } else if (parametersPath.equals(setting)) {
          try {
            String filePath = (String) value;
            params.loadFrom(filePath);
            options.put(parametersPath, filePath);
            notifyParametersChanged();
            if (debugMode) {
              debugInfo.set("Parameters file path successfully set to " + value);
            }
            return true;
          } catch (IOException e) {
            if (debugMode) {
              debugInfo.set(e.getMessage());
            }
          }
        } else if (uciOpponent.equals(setting)) {
          options.put(uciOpponent, value);
          if (debugMode) {
            debugInfo.set("Opponent name successfully set to " + value);
          }
          return true;
        } else if (uciAnalysis.equals(setting)) {
          options.put(uciAnalysis, value);
          if (debugMode) {
            debugInfo.set("Analysis mode successfully set to " + value);
          }
          return true;
        }
        if (debugMode) {
          debugInfo.set("The setting was not accepted");
        }
        return false;
      } catch (Exception e) {
        if (debugMode) {
          debugInfo.set("The setting was not accepted\n" + e.getMessage());
        }
        return false;
      }
    }
  }

  @Override
  public void setDebugMode(boolean on) {
    synchronized (mainLock) {
      debugMode = on;
    }
  }

  @Override
  public void newGame() {
    synchronized (mainLock) {
      newGame = true;
      outOfBook = false;
      if (!controllerMode && !deterministicEvalMode) {
        clearHash();
      }
      if (egtb.isProbingLibLoaded()) {
        egtb.clearCache();
      }
    }
  }

  @Override
  public boolean setPosition(String fen) {
    synchronized (mainLock) {
      try {
        Position pos = fen.equals(START_POSITION) ? Position.parse(Position.START_POSITION_FEN) :
            Position.parse(fen);
        /* If the start position of the game is different or the engine got the new game signal, reset the game
         * and the hash tables. */
        if (newGame) {
          if (debugMode) {
            debugInfo.set("New game set");
          }
          game = new Game(pos);
        } else if (!game.getStartPos().toString().equals(pos.toString())) {
          newGame();
          if (debugMode) {
            debugInfo.set("New game set due to new start position");
          }
          game = new Game(pos);
        }
        // Otherwise just clear the obsolete entries from the hash tables.
        else {
          if (debugMode) {
            debugInfo.set("Position0 set within the same game");
          }
          gen++;
          if (!controllerMode && !deterministicEvalMode) {
            if (gen == 127) {
              transTable.clear();
              evalTable.clear();
              gen = 0;
            } else {
              transTable.remove(e -> e.getGeneration() < gen - params.transTableEntryLifeCycle);
              evalTable.remove(e -> e.getGeneration() < gen - params.evalTableEntryLifeCycle);
            }
          }
          game = new Game(game.getStartPos(), game.getEvent(), game.getSite(), game.getWhitePlayerName(), game.getBlackPlayerName());
        }
        return true;
      } catch (ChessParseException | NullPointerException e) {
        if (debugMode) {
          debugInfo.set(e.getMessage());
        }
        return false;
      }
    }
  }

  @Override
  public boolean play(String pacn) {
    synchronized (mainLock) {
      if (game.play(pacn)) {
        if (debugMode) {
          debugInfo.set("Move \"" + pacn + "\" played successfully");
        }
        return true;
      }
      if (debugMode) {
        debugInfo.set("Move \"" + pacn + "\" could not be played");
      }
      return false;
    }
  }

  @Override
  public SearchResults search(Set<String> searchMoves, Boolean ponder, Long whiteTime, Long blackTime,
      Long whiteIncrement, Long blackIncrement, Integer movesToGo, Integer depth, Long nodes,
      Integer mateDistance, Long searchTime, Boolean infinite) {
    synchronized (mainLock) {
      bookMove = false;
      ponderHit = false;
      // Set the names of the players once it is known which colour we are playing.
      setPlayerNames();
      boolean analysisMode = (Boolean) options.get(uciAnalysis);
      boolean searchBook = book != null && !analysisMode && !outOfBook && searchMoves == null && (ponder == null || !ponder) &&
          depth == null && nodes == null && mateDistance == null && searchTime == null && (infinite == null || !infinite) &&
          (Boolean) options.get(ownBook);
      SearchResults results;
      // Search the book if possible.
      if (searchBook) {
        results = searchBook(ponder, whiteTime, blackTime, movesToGo, infinite);
      } else {
        results = searchGameTree(searchMoves, ponder, whiteTime, blackTime, movesToGo, depth, nodes, mateDistance, searchTime, infinite);
      }
      // If there are no results, pick a random move.
      if (results == null || results.getBestMove() == null) {
        results = new SearchResults(getRandomMove(), null, null, null);
      }
      return results;
    }
  }

  @Override
  public void stop() {
    synchronized (stopLock) {
      synchronized (searchLock) {
        if (search != null) {
          if (debugMode) {
            debugInfo.set("Stopping search...");
          }
          stop = true;
          search.cancel(true);
          searchLock.notifyAll();
        }
      }
      synchronized (mainLock) {
        // Wait for the search to finish.
      }
      synchronized (searchLock) {
        stop = false;
      }
    }
  }

  @Override
  public void ponderHit() {
    synchronized (mainLock) {
      if (debugMode) {
        debugInfo.set("Signaling ponderhit...");
      }
      ponderHit = true;
      mainLock.notifyAll();
    }
  }

  @Override
  public SearchInformation getSearchInfo() {
    synchronized (mainLock) {
      return searchInfo;
    }
  }

  @Override
  public short getHashLoadPermill() {
    long transLoad = transTable.size();
    long evalLoad = evalTable.size();
    long totalLoad = transLoad + evalLoad;
    long transCapacity = transTable.capacity();
    long evalCapacity = evalTable.capacity();
    long totalCapacity = transCapacity + evalCapacity;
    if (debugMode) {
      debugInfo.set(String.format("TT load factor - %.2f%nET load factor - %.2f",
          ((float) transLoad) / transCapacity, ((float) evalLoad) / evalCapacity));
      debugInfo.set(String.format("Total hash size in MB - %.2f", (float) ((double) transTable.memorySize() +
          evalTable.memorySize()) / (1L << 20)));
    }
    /* Due to the non-thread-safe nature of the hash tables, incorrect size values may be returned; ensure
     * the load does not exceed 1000. */
    return (short) Math.min(1000, 1000 * totalLoad / totalCapacity);
  }

  @Override
  public DebugInformation getDebugInfo() {
    synchronized (mainLock) {
      return debugInfo;
    }
  }

  @Override
  public void quit() {
    synchronized (mainLock) {
      if (!init) {
        if (debugMode) {
          debugInfo.set("The engine has not been initialized yet; cannot shut down.");
        }
        return;
      }
      stop();
      if (debugMode) {
        debugInfo.set("Shutting down...");
      }
      if (book != null) {
        try {
          book.close();
        } catch (IOException e) {
          if (debugMode) {
            debugInfo.set(e.getMessage());
          }
        }
      }
      if (egtb.isProbingLibLoaded()) {
        try {
          egtb.close();
        } catch (IOException e) {
          if (debugMode) {
            debugInfo.set(e.getMessage());
          }
        }
      }
      executor.shutdown();
      searchInfo.deleteObservers();
      transTable = null;
      evalTable = null;
      init = false;
    }
  }

  @Override
  public boolean isWhitesTurn() {
    synchronized (mainLock) {
      return game.isWhitesTurn();
    }
  }

  @Override
  public String getStartPosition() {
    synchronized (mainLock) {
      return game.getStartPos().toString();
    }
  }

  @Override
  public GameState getGameState() {
    synchronized (mainLock) {
      return game.getState();
    }
  }

  @Override
  public List<String> getMoveHistory() {
    synchronized (mainLock) {
      List<String> moves = game.getPosition().getMoveHistory().stream()
          .map(Object::toString).collect(Collectors.toList());
      Collections.reverse(moves);
      return moves;
    }
  }

  @Override
  public List<String> getLegalMoves() {
    synchronized (mainLock) {
      return game.getPosition().getMoves().stream().map(Object::toString).collect(Collectors.toList());
    }
  }

  @Override
  public boolean isQuiet() {
    synchronized (mainLock) {
      return game.getPosition().getTacticalMoves().size() == 0;
    }
  }

  @Override
  public boolean setGame(String pgn) {
    synchronized (mainLock) {
      try {
        newGame();
        game = Game.parse(pgn);
        if (debugMode) {
          debugInfo.set("Game successfully set to:\n" + pgn);
        }
        return true;
      } catch (ChessParseException e) {
        if (debugMode) {
          debugInfo.set("Game could not be set. Invalid PGN:\n" + pgn);
        }
        return false;
      }
    }
  }

  @Override
  public void setPlayers(String whitePlayer, String blackPlayer) {
    synchronized (mainLock) {
      game.setWhitePlayerName(whitePlayer);
      game.setBlackPlayerName(blackPlayer);
    }
  }

  @Override
  public void setEvent(String event) {
    synchronized (mainLock) {
      game.setEvent(event);
    }
  }

  @Override
  public void setSite(String site) {
    synchronized (mainLock) {
      game.setSite(site);
    }
  }

  @Override
  public void setControllerMode(boolean on) {
    synchronized (mainLock) {
      controllerMode = on;
      if (init) {
        setHashSize(on ? MIN_HASH_SIZE : DEFAULT_HASH_SIZE);
      }
    }
  }

  @Override
  public void drawByAgreement() {
    synchronized (mainLock) {
      game.setState(GameState.DRAW_BY_AGREEMENT);
    }
  }

  @Override
  public void whiteForfeit() {
    synchronized (mainLock) {
      game.setState(GameState.UNSPECIFIED_BLACK_WIN);
    }
  }

  @Override
  public void blackForfeit() {
    synchronized (mainLock) {
      game.setState(GameState.UNSPECIFIED_WHITE_WIN);
    }
  }

  @Override
  public String unplayLastMove() {
    synchronized (mainLock) {
      return game.unplay();
    }
  }

  @Override
  public String convertPACNToSAN(String move) {
    synchronized (mainLock) {
      try {
        Position pos = game.getPosition();
        return MoveStringUtils.toSAN(pos, MoveStringUtils.parsePACN(pos, move));
      } catch (ChessParseException | NullPointerException e) {
        if (debugMode) {
          debugInfo.set("Error while converting PACN to SAN: " + e.getMessage());
        }
        throw new IllegalArgumentException("The parameter move cannot be converted to SAN.", e);
      }
    }
  }

  @Override
  public String convertSANToPACN(String move) {
    synchronized (mainLock) {
      try {
        return MoveStringUtils.parseSAN(game.getPosition(), move).toString();
      } catch (ChessParseException | NullPointerException e) {
        if (debugMode) {
          debugInfo.set("Error while converting SAN to PACN: " + e.getMessage());
        }
        throw new IllegalArgumentException("The parameter move cannot be converted to PACN.", e);
      }
    }
  }

  @Override
  public String toPGN() {
    synchronized (mainLock) {
      return game.toString();
    }
  }

  @Override
  public String toFEN() {
    synchronized (mainLock) {
      return game.getPosition().toString();
    }
  }

  @Override
  public long perft(int depth) {
    synchronized (mainLock) {
      return perft(game.getPosition(), depth);
    }
  }

  @Override
  public EngineParameters getParameters() {
    synchronized (mainLock) {
      return params;
    }
  }

  @Override
  public void notifyParametersChanged() {
    synchronized (mainLock) {
      if (init) {
        eval = new Evaluator(params, controllerMode || deterministicEvalMode ? null : evalTable);
      }
    }
  }

  @Override
  public void setDeterministicEvaluationMode(boolean on) {
    synchronized (mainLock) {
      deterministicEvalMode = on;
      if (init) {
        setHashSize(on ? MIN_HASH_SIZE : DEFAULT_HASH_SIZE);
      }
    }
  }

  @Override
  public double eval(Map<String, Double> gradientCache) {
    synchronized (mainLock) {
      return eval.score(game.getPosition(), gen, new ETEntry(), gradientCache);
    }
  }

}
