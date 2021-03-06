package net.viktorc.detroid.framework.tuning;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A class for pitting two UCI compatible engines against each other, supervised by a controller engine.
 *
 * @author Viktor
 * @see UCIEngine
 * @see ControllerEngine
 */
class Arena implements AutoCloseable {

  /**
   * The arena event's impromptu name.
   */
  public static final String EVENT = "DETROID Computer Chess Arena";

  private static final long MIN_TC = 500;

  private ControllerEngine controller;
  private ExecutorService pool;
  private Random rand;
  private Logger resultLogger;
  private Logger pgnLogger;
  private long id;

  /**
   * Constructs an arena controlled by the specified engine. A logger can be provided that will be used to log game results.
   *
   * @param controller The engine to control the match.
   * @param resultLogger The logger to log the results of the games.
   * @param pgnLogger The logger to log the complete games in PGN format.
   * @throws Exception If the controller engine cannot be initialised.
   */
  public Arena(ControllerEngine controller, Logger resultLogger, Logger pgnLogger) throws Exception {
    this.controller = controller;
    if (!this.controller.isInit()) {
      this.controller.init();
    }
    this.controller.setControllerMode(true);
    pool = Executors.newCachedThreadPool();
    this.resultLogger = resultLogger;
    this.pgnLogger = pgnLogger;
    rand = new Random(System.nanoTime());
    id = rand.nextLong();
  }

  /**
   * Constructs an arena controlled by the specified engine.
   *
   * @param controller The engine to control the match.
   * @throws Exception If the controller engine cannot be initialised.
   */
  public Arena(ControllerEngine controller) throws Exception {
    this(controller, null, null);
  }

  /**
   * Returns the Arena instance's id number.
   *
   * @return The arena's ID number.
   */
  public long getId() {
    return id;
  }

  private SearchResults searchPosition(UCIEngine engine, Timer timer, AtomicLong timeLeft, AtomicLong oppTimeLeft,
      long timeIncPerMove, boolean white) throws Exception {
    timeLeft.set(timeLeft.get() + timeIncPerMove);
    TimerTask task = new TimerTask() {

      @Override
      public void run() {
        engine.stop();
      }
    };
    timer.schedule(task, timeLeft.get());
    long start = System.nanoTime();
    SearchResults res = engine.search(null, null, white ? timeLeft.get() : oppTimeLeft.get(),
        white ? oppTimeLeft.get() : timeLeft.get(), timeIncPerMove, timeIncPerMove, null, null,
        null, null, null, null);
    long end = System.nanoTime();
    task.cancel();
    timeLeft.set(timeLeft.get() - Math.round((double) (end - start) / 1e6));
    if (timeLeft.get() <= 0 || !controller.play(res.getBestMove())) {
      throw new GameOverException(timeLeft.get() <= 0 ?
          "Engine1 lost on time." : "Engine1 returned an illegal move: " + res.getBestMove() + ".");
    }
    return res;
  }

  private void assignEngineNames(boolean engine1White) {
    controller.setPlayers(engine1White ? "Engine1" : "Engine2", engine1White ? "Engine2" : "Engine1");
    controller.setEvent(EVENT);
    controller.setSite("?");
  }

  private void logArenaHeader(String engine1Name, String engine2Name, int games, long timePerGame,
      long timeIncPerMove) {
    if (resultLogger != null) {
      resultLogger.info("--------------------------------------MATCH STARTED" +
          "--------------------------------------\n" + "Arena: " + id + "\n" +
          "Engine1: " + engine1Name + " - Engine2: " + engine2Name + "\n" +
          "Games: " + games + " TC: " + timePerGame + (timeIncPerMove != 0 ? " + " + timeIncPerMove : "") +
          "\n\n");
    }
  }

  private void logResults(GameState outcome, String reason, boolean engine1White, int engine1Wins, int engine2Wins, int draws) {
    if (resultLogger != null) {
      String result;
      String state;
      switch (outcome) {
        case WHITE_MATES:
          result = (engine1White ? "Engine1" : "Engine2") + " WINS";
          state = "Check mate";
          break;
        case BLACK_MATES:
          result = (engine1White ? "Engine2" : "Engine1") + " WINS";
          state = "Check mate";
          break;
        case UNSPECIFIED_WHITE_WIN:
          result = (engine1White ? "Engine1" : "Engine2") + " WINS";
          state = "Black lost";
          break;
        case UNSPECIFIED_BLACK_WIN:
          result = (engine1White ? "Engine2" : "Engine1") + " WINS";
          state = "White lost";
          break;
        case STALE_MATE:
          result = "DRAW";
          state = "Stale mate";
          break;
        case DRAW_BY_INSUFFICIENT_MATERIAL:
          result = "DRAW";
          state = "Insufficient material";
          break;
        case DRAW_BY_3_FOLD_REPETITION:
          result = "DRAW";
          state = "Three fold repetition";
          break;
        case DRAW_BY_50_MOVE_RULE:
          result = "DRAW";
          state = "Fifty move rule";
          break;
        default:
          result = "";
          state = "";
          break;
      }
      state += reason.isEmpty() ? "" : (" - " + reason);
      resultLogger.info("Arena: " + id + "\n" + result + ": " + state + "\n" +
          "STANDINGS: " + engine1Wins + " - " + engine2Wins + " - " + draws + "\n\n");
    }
  }

  /**
   * Pits the two engines against each other playing the specified number of games with the number of milliseconds per game allotted for
   * each engine to make their moves. The engines play alternating their colours after each game so it is recommended to specify an even
   * number for the number of games to play.
   *
   * @param engine1 Contender number one.
   * @param engine2 Contender number two.
   * @param games The number of games to play. Should be an even number.
   * @param timePerGame The number of milliseconds each engine will have to make all their moves during the course of each game. If it is
   * less than 500, it will default to 500.
   * @param timeIncPerMove The number of milliseconds with which the remaining time of an engine is incremented after each legal move.
   * @return The results of the match.
   * @throws Exception If either of the engines is not initialised and an attempt at initialisation fails.
   */
  public synchronized MatchResult match(UCIEngine engine1, UCIEngine engine2, int games, long timePerGame,
      long timeIncPerMove) throws Exception {
    Timer timer = null;
    int engine1Wins = 0;
    int engine2Wins = 0;
    int draws = 0;
    games = Math.max(0, games);
    timePerGame = Math.max(MIN_TC, timePerGame);
    timeIncPerMove = Math.max(0, timeIncPerMove);
    if (!engine1.isInit()) {
      engine1.init();
    }
    if (!engine2.isInit()) {
      engine2.init();
    }
    logArenaHeader(engine1.getName(), engine2.getName(), games, timePerGame, timeIncPerMove);
    boolean engine1White = rand.nextBoolean();
    Games:
    for (int i = 0; i < games; i++, engine1White = !engine1White) {
      SearchResults res;
      AtomicLong engine1Time = new AtomicLong(timePerGame);
      AtomicLong engine2Time = new AtomicLong(timePerGame);
      boolean engine1Turn = engine1White;
      if (timer != null) {
        timer.cancel();
      }
      timer = new Timer();
      engine1.newGame();
      engine2.newGame();
      controller.newGame();
      engine1.setPosition();
      engine2.setPosition();
      controller.setPosition();
      assignEngineNames(engine1White);
      while (controller.getGameState() == GameState.IN_PROGRESS) {
        if (engine1Turn) {
          try {
            res = searchPosition(engine1, timer, engine1Time, engine2Time, timeIncPerMove, engine1White);
          } catch (Exception e) {
            timer.cancel();
            engine2Wins++;
            logResults(GameState.UNSPECIFIED_BLACK_WIN, e.getMessage(), engine1White, engine1Wins, engine2Wins, draws);
            continue Games;
          }
        } else {
          try {
            res = searchPosition(engine2, timer, engine2Time, engine1Time, timeIncPerMove, !engine1White);
          } catch (Exception e) {
            timer.cancel();
            engine1Wins++;
            logResults(GameState.UNSPECIFIED_WHITE_WIN, e.getMessage(), engine1White, engine1Wins, engine2Wins, draws);
            continue Games;
          }
        }
        engine1.play(res.getBestMove());
        engine2.play(res.getBestMove());
        engine1Turn = !engine1Turn;
      }
      GameState state = controller.getGameState();
      if (state == GameState.WHITE_MATES || state == GameState.UNSPECIFIED_WHITE_WIN) {
        if (engine1White) {
          engine1Wins++;
        } else {
          engine2Wins++;
        }
      } else if (state == GameState.BLACK_MATES || state == GameState.UNSPECIFIED_BLACK_WIN) {
        if (engine1White) {
          engine2Wins++;
        } else {
          engine1Wins++;
        }
      } else {
        draws++;
      }
      logResults(state, "", engine1White, engine1Wins, engine2Wins, draws);
      if (pgnLogger != null) {
        String pgn = controller.toPGN();
        pgnLogger.info(pgn);
      }
    }
    if (timer != null) {
      timer.cancel();
    }
    return new MatchResult(engine1Wins, engine2Wins, draws);
  }

  @Override
  public void close() {
    controller.close();
    pool.shutdown();
  }

  /**
   * A simple exception for premature game termination due to illegal moves or time out.
   */
  private static class GameOverException extends Exception {

    private static final long serialVersionUID = 0L;

    GameOverException(String message) {
      super(message);
    }
  }

}
