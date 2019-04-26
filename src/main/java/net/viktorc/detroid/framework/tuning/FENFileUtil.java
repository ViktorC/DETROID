package net.viktorc.detroid.framework.tuning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A utility class for generating and filtering FEN files used for static evaluation tuning.
 *
 * @author Viktor
 */
public final class FENFileUtil {

  /**
   * The pattern of the first line of every game in PGN.
   */
  private static final String FIRST_PGN_LINE_PATTERN = "(?i)^\\[EVENT (.)+\\]$";

  private FENFileUtil() {
  }

  /**
   * Generates a file of lines of FEN strings with the results of the games the positions occurred in appended to them. This file can then
   * be used for the optimization of engine parameters.
   *
   * @param engine A controller engine that will be responsible for parsing the PGNs and providing FENs for each position.
   * @param pgnFilePath The path to the file containing the games in PGN.
   * @param fenFilePath The output file path.
   * @param maxNumOfGames The maximum number of games that will be parsed and converted into lines of FEN.
   * @throws Exception If the input file does not exist or cannot be read, if the output file path is invalid, or if the engine is not
   * initialized and cannot be initialized.
   */
  public static void generateFENFile(ControllerEngine engine, String pgnFilePath, String fenFilePath, int maxNumOfGames) throws Exception {
    try (BufferedReader reader = new BufferedReader(new FileReader(pgnFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(fenFilePath))) {
      String line;
      StringBuffer pgnBuffer = new StringBuffer();
      int gameCount = 0;
      if (!engine.isInit()) {
        engine.init();
      }
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        boolean doProcess = false;
        if (line.matches(FIRST_PGN_LINE_PATTERN) && pgnBuffer.length() > 0) {
          doProcess = true;
        } else if (!reader.ready()) {
          pgnBuffer.append(line).append("\n");
          doProcess = true;
        }
        if (doProcess) {
          gameCount++;
          String pgn = pgnBuffer.toString().trim();
          engine.setGame(pgn);
          String result;
          GameState state = engine.getGameState();
          if (state != GameState.IN_PROGRESS) {
            if (state == GameState.WHITE_MATES || state == GameState.UNSPECIFIED_WHITE_WIN) {
              result = "1";
            } else if (state == GameState.BLACK_MATES || state == GameState.UNSPECIFIED_BLACK_WIN) {
              result = "0";
            } else {
              result = "0.5";
            }
            do {
              String fen = engine.toFEN();
              writer.write(fen + ";" + result + "\n");
            } while (engine.unplayLastMove() != null);
          }
          pgnBuffer = new StringBuffer();
        }
        if (gameCount >= maxNumOfGames) {
          break;
        }
        pgnBuffer.append(line).append("\n");
      }
    }
  }

  /**
   * Generates a file of lines of FEN strings with the results of the games the positions occurred in appended to them. This file can then
   * be used for the optimization of engine parameters.
   *
   * @param engines A list of {@link net.viktorc.detroid.framework.tuning.SelfPlayEngines} instances that each contain the engines needed
   * for playing games in the {@link net.viktorc.detroid.framework.tuning.Arena}. For each non-null element in the list, a new thread will
   * be utilized for the optimization. E.g. if engines is a list of four non-null elements, the games in the fitness function will be
   * distributed and played parallel on four threads. The list's first element cannot be null or a {@link java.lang.NullPointerException} is
   * thrown.
   * @param games The number of games to play.
   * @param timePerGame The time each engine will have per game in milliseconds.
   * @param timeIncPerMove The number of milliseconds with which the remaining time of an engine is incremented after each legal move. be
   * done.
   * @param fenFilePath The path to the FEN-file.
   * @throws IOException If the file specified by filePath doesn't exist and cannot be created.
   * @throws NullPointerException If the parameter engines is null.
   * @throws IllegalArgumentException If engines doesn't contain at least one non-null element.
   * @throws ExecutionException If an execution exception occurs in one of the threads.
   * @throws InterruptedException If the current thread is interrupted while waiting for the worker threads to finish.
   */
  public static void generateFENFile(List<SelfPlayEngines<UCIEngine>> engines, int games, long timePerGame, long timeIncPerMove,
      String fenFilePath) throws IOException, NullPointerException, IllegalArgumentException, InterruptedException, ExecutionException {
    ArrayList<SelfPlayEngines<UCIEngine>> enginesList = new ArrayList<>();
    for (SelfPlayEngines<UCIEngine> e : engines) {
      if (e != null) {
        enginesList.add(e);
      }
    }
    if (enginesList.size() == 0) {
      throw new IllegalArgumentException("The parameter engines has to contain at least 1 non-null element.");
    }
    ExecutorService pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(),
        enginesList.size()));
    try {
      ArrayList<Future<Void>> futures = new ArrayList<>();
      int workLoad = games / enginesList.size();
      for (SelfPlayEngines<UCIEngine> e : engines) {
        futures.add(pool.submit(() -> {
          Logger fenLogger = Logger.getAnonymousLogger();
          fenLogger.setUseParentHandlers(false);
          FileHandler handler = new FileHandler(fenFilePath, true);
          handler.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord record) {
              return record.getMessage() + System.lineSeparator();
            }
          });
          fenLogger.addHandler(handler);
          Arena a = new Arena(e.getController(), null, fenLogger);
          a.match(e.getEngine(), e.getOpponentEngine(), workLoad, timePerGame, timeIncPerMove);
          a.close();
          handler.flush();
          handler.close();
          return null;
        }));
      }
      for (Future<?> f : futures) {
        f.get();
      }
    } finally {
      pool.shutdown();
      for (int i = 1; i < enginesList.size(); i++) {
        String fileName = fenFilePath + "." + i;
        File extraFile = new File(fileName);
        if (extraFile.exists()) {
          try (BufferedReader reader = new BufferedReader(new FileReader(fileName));
              BufferedWriter writer = new BufferedWriter(new FileWriter(fenFilePath, true))) {
            String line;
            while ((line = reader.readLine()) != null && !"".equals(line)) {
              writer.write(line + System.lineSeparator());
            }
          }
          Files.delete(extraFile.toPath());
        }
      }
    }
  }

  /**
   * Copies all the lines from the source FEN file to the destination file except for the ones representing positions no more than
   * numOfPositionsToFilter full moves into the game.
   *
   * @param sourceFenFile The file path to the source FEN file.
   * @param destinationFenFile The path to the destination file. If it doesn't exist it will be created.
   * @param numOfPositionsToFilter The first x full moves to filter from each game.
   * @throws IOException If the source cannot be read from and the destination cannot be created or written to.
   */
  public static void filterOpeningPositions(String sourceFenFile, String destinationFenFile, int numOfPositionsToFilter)
      throws IOException {
    if (sourceFenFile.equals(destinationFenFile)) {
      throw new IllegalArgumentException();
    }
    Pattern halfMovePattern = Pattern.compile("[0-9]+;");
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceFenFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFenFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = halfMovePattern.matcher(line);
        if (matcher.find()) {
          String match = matcher.group();
          int fullMoveInd = Integer.parseInt(match.substring(0, match.length() - 1));
          if (fullMoveInd > numOfPositionsToFilter) {
            writer.write(line + System.lineSeparator());
          }
        }
      }
    }
  }

  /**
   * Copies all the lines from the source FEN file to the destination file except for the obvious mates based on a 0-depth quiescence
   * search.
   *
   * @param sourceFenFile The file path to the source FEN file.
   * @param destinationFenFile The path to the destination file. If it doesn't exist it will be created.
   * @param engine The engine to use for obvious mate detection.
   * @throws Exception If the source and destination paths are the same, there is an I/O issue or the engine cannot be initialized.
   */
  public static void filterObviousMates(String sourceFenFile, String destinationFenFile, TunableEngine engine)
      throws Exception {
    if (sourceFenFile.equals(destinationFenFile)) {
      throw new IllegalArgumentException();
    }
    if (!engine.isInit()) {
      engine.init();
    }
    engine.setDeterministicEvaluationMode(true);
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceFenFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFenFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String fen = line.split(";")[0];
        engine.setPosition(fen);
        SearchResults res = engine.search(null, null, null, null, null, null, null, 0, null, null, null, null);
        if (res.getScoreType().isPresent()) {
          ScoreType scoreType = res.getScoreType().get();
          if (scoreType != ScoreType.MATE) {
            writer.write(line + System.lineSeparator());
          }
        }
      }
    }
  }

  /**
   * Copies all the lines from the source FEN file to the destination file except for the ones representing positions from drawn games.
   *
   * @param sourceFenFile The file path to the source FEN file.
   * @param destinationFenFile The path to the destination file. If it doesn't exist it will be created.
   * @throws IOException If the source cannot be read from and the destination cannot be created or written to.
   */
  public static void filterDraws(String sourceFenFile, String destinationFenFile) throws IOException {
    if (sourceFenFile.equals(destinationFenFile)) {
      throw new IllegalArgumentException();
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceFenFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFenFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String res = line.split(";")[1];
        if (res != null) {
          res = res.trim();
        }
        if (!"0.5".equals(res)) {
          writer.write(line + System.lineSeparator());
        }
      }
    }
  }

  /**
   * Copies all the lines from the source FEN file to the destination file except the positions that allow for legal tactical moves.
   *
   * @param sourceFenFile The file path to the source FEN file.
   * @param destinationFenFile The path to the destination file. If it doesn't exist it will be created.
   * @param engine The engine to use for obvious mate detection.
   * @throws Exception If the source and destination paths are the same, there is an I/O issue or the engine cannot be initialized.
   */
  public static void filterTacticalPositions(String sourceFenFile, String destinationFenFile, ControllerEngine engine)
      throws Exception {
    if (sourceFenFile.equals(destinationFenFile)) {
      throw new IllegalArgumentException();
    }
    if (!engine.isInit()) {
      engine.init();
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceFenFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFenFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String fen = line.split(";")[0];
        engine.setPosition(fen);
        if (engine.isQuiet()) {
          writer.write(line + System.lineSeparator());
        }
      }
    }
  }

}
