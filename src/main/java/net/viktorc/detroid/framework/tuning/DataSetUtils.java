package net.viktorc.detroid.framework.tuning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
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
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.EPDRecord;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * A utility class for generating and filtering EPD files used for static evaluation tuning.
 *
 * @author Viktor
 */
public final class DataSetUtils {

  /**
   * The pattern of the first line of every game in PGN.
   */
  private static final String FIRST_PGN_LINE_REGEX = "(?i)^\\[EVENT (.)+\\]$";
  private static final Pattern WHITE_ELO_PATTERN = Pattern.compile("\\[WhiteElo \"([0-9]+)\"\\]");
  private static final Pattern BLACK_ELO_PATTERN = Pattern.compile("\\[BlackElo \"([0-9]+)\"\\]");

  private DataSetUtils() {
  }

  /**
   * Generates a PGN file of self-play games.
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
   * @param pgnFilePath The path to the output PGN file.
   * @throws IOException If the file specified by filePath doesn't exist and cannot be created.
   * @throws NullPointerException If the parameter engines is null.
   * @throws IllegalArgumentException If engines doesn't contain at least one non-null element.
   * @throws ExecutionException If an execution exception occurs in one of the threads.
   * @throws InterruptedException If the current thread is interrupted while waiting for the worker threads to finish.
   */
  public static void generatePGNFile(List<SelfPlayEngines<UCIEngine>> engines, int games, long timePerGame, long timeIncPerMove,
      String pgnFilePath) throws IOException, InterruptedException, ExecutionException {
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
      int remainingGames = games;
      int remainingEngines = engines.size();
      for (SelfPlayEngines<UCIEngine> e : engines) {
        int workLoad = remainingGames / remainingEngines;
        remainingGames -= workLoad;
        remainingEngines--;
        futures.add(pool.submit(() -> {
          Logger pgnLogger = Logger.getAnonymousLogger();
          pgnLogger.setUseParentHandlers(false);
          FileHandler handler = new FileHandler(pgnFilePath, true);
          handler.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord record) {
              return record.getMessage() + System.lineSeparator() + System.lineSeparator();
            }
          });
          pgnLogger.addHandler(handler);
          Arena a = new Arena(e.getController(), null, pgnLogger);
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
        String fileName = pgnFilePath + "." + i;
        File extraFile = new File(fileName);
        if (extraFile.exists()) {
          try (BufferedReader reader = new BufferedReader(new FileReader(fileName));
              BufferedWriter writer = new BufferedWriter(new FileWriter(pgnFilePath, true))) {
            String line;
            while ((line = reader.readLine()) != null) {
              writer.write(line + System.lineSeparator());
            }
          }
          Files.delete(extraFile.toPath());
        }
      }
    }
  }

  /**
   * Generates an EPD file of positions labelled by the results of the games the positions occurred. This file can then be used for the
   * optimization of engine parameters.
   *
   * @param engine A controller engine that will be responsible for parsing the PGNs and providing FEN descriptions for each position.
   * @param pgnFilePath The path to the file containing the games in PGN.
   * @param epdFilePath The output file path.
   * @param gameResultOpCode The operation code of the label.
   * @param maxNumOfGames The maximum number of games that will be parsed and converted into EPD records.
   * @param minElo The minimum Elo rating required for each party to process the game.
   * @param minHalfMoveIndex The half move index from which positions of a game are saved in the EPD file.
   * @return The number of games processed.
   * @throws Exception If the input file does not exist or cannot be read, if the output file path is invalid, or if the engine is not
   * initialized and cannot be initialized.
   */
  public static int generateEPDFile(ControllerEngine engine, String pgnFilePath, String epdFilePath, String gameResultOpCode,
      long maxNumOfGames, Integer minElo, Integer minHalfMoveIndex) throws Exception {
    try (BufferedReader reader = new BufferedReader(new FileReader(pgnFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(epdFilePath))) {
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
        if (line.matches(FIRST_PGN_LINE_REGEX) && pgnBuffer.length() > 0) {
          doProcess = true;
        } else if (!reader.ready()) {
          pgnBuffer.append(line).append("\n");
          doProcess = true;
        }
        if (doProcess) {
          String pgn = pgnBuffer.toString().trim();
          boolean skip = false;
          if (minElo != null) {
            Matcher whiteEloMatcher = WHITE_ELO_PATTERN.matcher(pgn);
            Matcher blackEloMatcher = BLACK_ELO_PATTERN.matcher(pgn);
            if (whiteEloMatcher.find() && blackEloMatcher.find()) {
              try {
                int whiteElo = Integer.parseInt(whiteEloMatcher.group(1));
                int blackElo = Integer.parseInt(blackEloMatcher.group(1));
                skip = whiteElo < minElo || blackElo < minElo;
              } catch (Exception e) {
                skip = true;
              }
            } else {
              skip = true;
            }
          }
          if (!skip) {
            gameCount++;
            engine.setGame(pgn);
            GameState state = engine.getGameState();
            if (state != GameState.IN_PROGRESS) {
              String result = state.getPGNCode();
              List<EPDRecord> records = new ArrayList<>();
              do {
                String fen = engine.toFEN();
                records.add(new EPDRecord(fen, Collections.singletonMap(gameResultOpCode, "\"" + result + "\"")));
              } while (engine.unplayLastMove() != null);
              if (minHalfMoveIndex != null) {
                records = records.subList(0, Math.max(0, records.size() - Math.max(0, minHalfMoveIndex)));
              }
              for (EPDRecord record : records) {
                writer.write(record + System.lineSeparator());
              }
            }
          }
          pgnBuffer = new StringBuffer();
        }
        if (gameCount >= maxNumOfGames) {
          break;
        }
        pgnBuffer.append(line).append("\n");
      }
      return gameCount;
    }
  }

  /**
   * Copies all the lines from the source EPD file to the destination file except for the ones representing either too unbalanced or too
   * balanced positions based on the engine to tune's evaluation function.
   *
   * @param sourceEpdFile The file path to the source EPD file.
   * @param destinationEpdFile The path to the destination file. If it doesn't exist it will be created.
   * @param imbalance The maximum allowed absolute score difference in centi-pawns. If it is negative, the minimum necessary score
   * difference.
   * @param engine The engine to use for unbalanced position detection.
   * @throws Exception If the source and destination paths are the same, there is an I/O issue or the engine cannot be initialized.
   */
  public static void filterUnbalancedPositions(String sourceEpdFile, String destinationEpdFile, short imbalance, TunableEngine engine)
      throws Exception {
    if (sourceEpdFile.equals(destinationEpdFile)) {
      throw new IllegalArgumentException();
    }
    if (!engine.isInit()) {
      engine.init();
    }
    engine.setDeterministicEvaluationMode(true);
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceEpdFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationEpdFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        EPDRecord record = EPDRecord.parse(line);
        engine.setPosition(record.getPosition());
        short score = engine.eval(null);
        if ((imbalance >= 0 && Math.abs(score) < imbalance) || (imbalance < 0 && Math.abs(score) > -imbalance)) {
          writer.write(line + System.lineSeparator());
        }
      }
    }
  }

  /**
   * Copies all the lines from the source EPD file to the destination file except for the ones representing positions from drawn games.
   *
   * @param sourceEpdFile The file path to the source EPD file.
   * @param destinationEpdFile The path to the destination file. If it doesn't exist it will be created.
   * @param gameResultOpCode The operation code of the label.
   * @throws IOException If the source cannot be read from and the destination cannot be created or written to.
   */
  public static void filterDraws(String sourceEpdFile, String destinationEpdFile, String gameResultOpCode) throws IOException {
    if (sourceEpdFile.equals(destinationEpdFile)) {
      throw new IllegalArgumentException();
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceEpdFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationEpdFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        EPDRecord record = EPDRecord.parse(line);
        String result = record.getOperand(gameResultOpCode);
        if (result != null) {
          result = result.trim();
        }
        if (!GameState.STALE_MATE.getPGNCode().equals(result)) {
          writer.write(line + System.lineSeparator());
        }
      }
    }
  }

  /**
   * Copies all the lines from the source EPD file to the destination file except the positions that allow for legal tactical moves.
   *
   * @param sourceEpdFile The file path to the source EPD file.
   * @param destinationEpdFile The path to the destination file. If it doesn't exist it will be created.
   * @param engine The engine to use for tactical position detection.
   * @throws Exception If the source and destination paths are the same, there is an I/O issue or the engine cannot be initialized.
   */
  public static void filterTacticalPositions(String sourceEpdFile, String destinationEpdFile, ControllerEngine engine)
      throws Exception {
    if (sourceEpdFile.equals(destinationEpdFile)) {
      throw new IllegalArgumentException();
    }
    if (!engine.isInit()) {
      engine.init();
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceEpdFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationEpdFile, true))) {
      String line;
      while ((line = reader.readLine()) != null) {
        EPDRecord record = EPDRecord.parse(line);
        engine.setPosition(record.getPosition());
        if (engine.isQuiet()) {
          writer.write(line + System.lineSeparator());
        }
      }
    }
  }

}
