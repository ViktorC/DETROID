package net.viktorc.detroid.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import javafx.application.Application;
import net.viktorc.detroid.framework.gui.GUI;
import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.FENFileUtil;
import net.viktorc.detroid.framework.tuning.ParameterType;
import net.viktorc.detroid.framework.tuning.SelfPlayEngines;
import net.viktorc.detroid.framework.tuning.SelfPlayOptimizer;
import net.viktorc.detroid.framework.tuning.TexelOptimizer;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.uci.UCI;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * The application that serves as a chess engine framework handling communication via the UCI protocol, providing a GUI, and offering
 * flexible engine parameter tuning methods using machine learning.
 *
 * The application framework offers six main functionalities: GUI mode, UCI mode, tuning, FEN-file generation, FEN-file filtering, and
 * parameter conversion. The default launch mode is the GUI mode which provides an interface that allows for playing chess games against an
 * engine and track its search statistics. The UCI mode implements the Universal Chess Interface protocol as described at
 * <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">http://wbec-ridderkerk.nl/html/UCIProtocol.html</a> by
 * Stefan-Meyer Kahlen. Two different tuning methods are supported; an evolutionary algorithm for optimizing all or only a certain type of
 * engine parameters using self-play to assess the fitness of the different parameter sets generated, and an adaptive stochastic gradient
 * descent algorithm for training the engine by optimizing the static evaluation parameters using the "Texel" cost function (
 * <a href="https://chessprogramming.wikispaces.com/Texel's+Tuning+Method">https://chessprogramming.wikispaces.com/Texel's+Tuning+Method</a>)
 * based on so called FEN-files which contain position descriptions in Forsyth-Edwards notation, each labelled by the side that won the game
 * in which the position occurred. The FEN-file generation mode provides the functionalities needed to generate the files used for static
 * evaluation tuning. They can be generated either by self-play or by providing a Portable Game Notation file to convert. The FEN-file
 * filtering mode allows for removing draws or opening positions from the FEN-file which can, in certain cases, possibly improve the tuning
 * results. Last but not least, the conversion mode allows for converting the numbers logged by the tuning methods into XML files that the
 * engine can read its parameters' values from. The game play optimization algorithm logs the probability vector, while the static
 * evaluation tuning method logs the optimal values of the parameter fields.
 *
 * @author Viktor
 */
public final class EngineFramework implements Runnable {

  /**
   * The default number of threads to use.
   */
  private static final int DEF_CONCURRENCY = 1;
  /**
   * The default log file path.
   */
  private static final String DEF_LOG_FILE_PATH = "log.txt";
  /**
   * The default FENs file path for static evaluation tuning.
   */
  private static final String DEF_FENS_FILE_PATH = "fens.txt";
  /**
   * The default path to the file to which the parameters converted from binary strings or double arrays are written.
   */
  private static final String DEF_CONVERTED_PARAMS_PATH = "params.xml";

  private EngineFactory factory;
  private String[] args;

  /**
   * Constructs an instance of the application framework that is available for running with the specified arguments using the engine
   * instances built by the provided engine factory.
   *
   * If there are no program arguments, the application is started in GUI mode.
   *
   * @param factory An instance of a class extending the {@link net.viktorc.detroid.framework.EngineFactory} interface. It provides the
   * engine instances required for different parameters of the framework.
   * @param args The program arguments. If it is null or empty, the engine is started in GUI mode; else: <br> UCI mode: {@code -u} <br> Self
   * play tuning: {@code -t selfplay -population <integer> -games <integer> -tc <integer> [--paramtype <eval | control | management |
   * eval+control | control+management | all> {all}] [--inc <integer> {0}] [--validfactor <decimal> {0}] [--initprobvector
   * <quoted_comma_separated_decimals>] [--trybook <bool> {false}] [--tryhash <integer>] [--trythreads <integer>] [--log <string>
   * {log.txt}] [--concurrency <integer>] {1}]} <br> Texel tuning: {@code -t texel -batchsize <integer> [--epochs <integer>]
   * [--testdataprop <decimal> {0.2}] [--h <decimal> {1}] [--learningrate <decimal> {1}] [--annealingrate <decimal> {.95}]
   * [--costbatchsize <integer>] [--k <decimal>] [--fensfile <string> {fens.txt}] [--log <string> {log.txt}] [--concurrency <integer> {1}]}
   * <br> FEN-file generation by self-play: {@code -g byselfplay -games <integer> -tc <integer> [--inc <integer> {0}]
   * [--trybook <bool> {false}] [--tryhash <integer>] [--trythreads <integer>] [--destfile <string> {fens.txt}]
   * [--concurrency <integer> {1}]} <br> FEN-file generation by PGN conversion: {@code -g bypgnconversion -sourcefile <string>
   * [--maxgames <integer>] [--destfile <string> {fens.txt}]} <br> Removing draws from a FEN-file: {@code -f draws -sourcefile <string>
   * [--destfile <string> {fens.txt}]} <br> Removing obvious mates from a FEN-file: {@code -f mates -sourcefile <string>
   * [--destfile <string> {fens.txt}]} <br> Removing tactical positions from a FEN-file: {@code -f tacticals -sourcefile <string>
   * [--destfile <string> {fens.txt}]} <br> Removing openings from a FEN-file: {@code -f openings -sourcefile <string>
   * -firstxmoves <integer> [--destfile <string> {fens.txt}]} <br> Probability vector conversion to parameters file: {@code -c probvector
   * -value <quoted_comma_separated_decimals> [--paramtype <eval | control | management | eval+control | control+management | all> {all}]
   * [--paramsfile <string> {params.xml}]} <br> Parameter value array conversion to parameters file: {@code -c parameters
   * -value <quoted_comma_separated_decimals> [--paramsfile <string> {params.xml}]}
   */
  public EngineFramework(EngineFactory factory, String[] args) {
    this.factory = factory;
    this.args = Arrays.copyOf(args, args.length);
  }

  private static Set<ParameterType> resolveParamTypes(String arg) {
    Set<ParameterType> paramTypes = null;
    switch (arg) {
      case "eval":
        paramTypes = new HashSet<>(Collections.singletonList(ParameterType.STATIC_EVALUATION));
        break;
      case "control":
        paramTypes = new HashSet<>(Collections.singletonList(ParameterType.SEARCH_CONTROL));
        break;
      case "management":
        paramTypes = new HashSet<>(Collections.singletonList(ParameterType.ENGINE_MANAGEMENT));
        break;
      case "eval+control":
        paramTypes = new HashSet<>(Arrays.asList(ParameterType.STATIC_EVALUATION, ParameterType.SEARCH_CONTROL));
        break;
      case "control+management":
        paramTypes = new HashSet<>(Arrays.asList(ParameterType.SEARCH_CONTROL, ParameterType.ENGINE_MANAGEMENT));
        break;
      case "all":
        break;
      default:
        throw new IllegalArgumentException();
    }
    return paramTypes;
  }

  private static void trySetOptions(UCIEngine engine, Boolean tryUseBook, Integer hash, Integer threads) {
    if (tryUseBook != null) {
      engine.setOwnBookOption(tryUseBook);
    }
    if (hash != null) {
      engine.setHashSizeOption(hash);
    }
    if (threads != null) {
      engine.setThreadsOption(threads);
    }
  }

  private void runInUCIMode() {
    try (UCI uci = new UCI(factory.newEngineInstance(), System.in, System.out)) {
      uci.run();
    }
  }

  private void runInSelfPlayTuningMode(String logFilePath, int concurrency, int popSize,
      int games, long tc, long tcInc, double validFactor, Set<ParameterType> paramTypes,
      double[] initProbVec, Boolean useBook, Integer hash, Integer threads) {
    List<SelfPlayEngines<TunableEngine>> engines = new ArrayList<>(concurrency);
    for (int i = 0; i < concurrency; i++) {
      try {
        TunableEngine engine1 = factory.newTunableEngineInstance();
        TunableEngine engine2 = factory.newTunableEngineInstance();
        engine1.init();
        engine2.init();
        trySetOptions(engine1, useBook, hash, threads);
        trySetOptions(engine2, useBook, hash, threads);
        engines.add(new SelfPlayEngines<>(engine1, engine2,
            factory.newControllerEngineInstance()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    Logger logger = Logger.getAnonymousLogger();
    try {
      logger.addHandler(new FileHandler(logFilePath, true));
    } catch (SecurityException | IOException e) {
      throw new IllegalArgumentException(e);
    }
    try (SelfPlayOptimizer optimizer = new SelfPlayOptimizer(engines, games, tc, tcInc,
        validFactor, initProbVec, popSize, logger, paramTypes)) {
      optimizer.optimize();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void runInSelfPlayTuningMode(String[] args) {
    String logFilePath = DEF_LOG_FILE_PATH;
    int concurrency = DEF_CONCURRENCY;
    int popSize = -1;
    int games = -1;
    long tc = -1;
    long tcInc = 0;
    double validFactor = 0;
    Set<ParameterType> paramTypes = null;
    double[] initProbVec = null;
    Boolean useBook = null;
    Integer hash = null;
    Integer threads = null;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-games":
          games = Integer.parseInt(args[++i]);
          break;
        case "-population":
          popSize = Integer.parseInt(args[++i]);
          break;
        case "-tc":
          tc = Long.parseLong(args[++i]);
          break;
        case "--log":
          logFilePath = args[++i];
          break;
        case "--concurrency":
          concurrency = Integer.parseInt(args[++i]);
          break;
        case "--paramtype":
          String type = args[++i];
          paramTypes = resolveParamTypes(type);
          break;
        case "--inc":
          tcInc = Long.parseLong(args[++i]);
          break;
        case "--validfactor":
          validFactor = Double.parseDouble(args[++i]);
          break;
        case "--trybook":
          useBook = Boolean.parseBoolean(args[++i]);
          break;
        case "--tryhash":
          hash = Integer.parseInt(args[++i]);
          break;
        case "--trythreads":
          threads = Integer.parseInt(args[++i]);
          break;
        case "--initprobvector":
          String vec = args[++i];
          String[] probs = vec.split(",");
          initProbVec = new double[probs.length];
          for (int j = 0; j < probs.length; j++) {
            initProbVec[j] = Double.parseDouble(probs[j].trim());
          }
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    if (games == -1 || tc == -1 || popSize == -1) {
      throw new IllegalArgumentException();
    }
    runInSelfPlayTuningMode(logFilePath, concurrency, popSize, games, tc, tcInc, validFactor, paramTypes,
        initProbVec, useBook, hash, threads);
  }

  private void runInTexelTuningMode(String logFilePath, String fensFilePath, int concurrency, long trainingBatchSize,
      int epochs, Long costCalcBatchSize, Double k, Double h, Double learningRate, Double annealingRate, Double testDataProp) {
    TunableEngine[] engines = new TunableEngine[concurrency];
    for (int i = 0; i < concurrency; i++) {
      engines[i] = factory.newTunableEngineInstance();
    }
    try {
      engines[0].init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Logger logger = Logger.getAnonymousLogger();
    try {
      logger.addHandler(new FileHandler(logFilePath, true));
    } catch (SecurityException | IOException e) {
      throw new IllegalArgumentException(e);
    }
    try (TexelOptimizer optimizer = new TexelOptimizer(engines, trainingBatchSize, epochs, h, learningRate, annealingRate, fensFilePath,
        costCalcBatchSize, k, testDataProp, logger)) {
      optimizer.optimize();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runInTexelTuningMode(String[] args) {
    String logFilePath = DEF_LOG_FILE_PATH;
    String fensFilePath = DEF_FENS_FILE_PATH;
    int concurrency = DEF_CONCURRENCY;
    long batchSize = -1;
    int epochs = 0;
    Long costCalcBatchSize = null;
    Double k = null;
    Double h = null;
    Double learningRate = null;
    Double annealingRate = null;
    Double testDataProp = null;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-batchsize":
          batchSize = Long.parseLong(args[++i]);
          break;
        case "--log":
          logFilePath = args[++i];
          break;
        case "--concurrency":
          concurrency = Integer.parseInt(args[++i]);
          break;
        case "--epochs":
          epochs = Integer.parseInt(args[++i]);
          break;
        case "--h":
          h = Double.parseDouble(args[++i]);
          break;
        case "--learningrate":
          learningRate = Double.parseDouble(args[++i]);
          break;
        case "--annealingrate":
          annealingRate = Double.parseDouble(args[++i]);
          break;
        case "--costbatchsize":
          costCalcBatchSize = Long.parseLong(args[++i]);
          break;
        case "--k":
          k = Double.parseDouble(args[++i]);
          break;
        case "--testdataprop":
          testDataProp = Double.parseDouble(args[++i]);
          break;
        case "--fensfile":
          fensFilePath = args[++i];
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    if (batchSize == -1) {
      throw new IllegalArgumentException();
    }
    runInTexelTuningMode(logFilePath, fensFilePath, concurrency, batchSize, epochs, costCalcBatchSize, k, h,
        learningRate, annealingRate, testDataProp);
  }

  private void runInTuningMode(String[] args) {
    String arg0 = args[0];
    if ("selfplay".equals(arg0)) {
      runInSelfPlayTuningMode(Arrays.copyOfRange(args, 1, args.length));
    } else if ("texel".equals(arg0)) {
      runInTexelTuningMode(Arrays.copyOfRange(args, 1, args.length));
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void runInSelfPlayGenerationMode(String destFile, int concurrency, int games, long tc, long tcInc,
      Boolean useBook, Integer hash, Integer threads) {
    List<SelfPlayEngines<UCIEngine>> engines = new ArrayList<>(concurrency);
    for (int i = 0; i < concurrency; i++) {
      try {
        UCIEngine engine1 = factory.newEngineInstance();
        UCIEngine engine2 = factory.newEngineInstance();
        engine1.init();
        engine2.init();
        trySetOptions(engine1, useBook, hash, threads);
        trySetOptions(engine2, useBook, hash, threads);
        engines.add(new SelfPlayEngines<>(engine1, engine2,
            factory.newControllerEngineInstance()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    try {
      FENFileUtil.generateFENFile(engines, games, tc, tcInc, destFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      for (SelfPlayEngines<UCIEngine> e : engines) {
        e.getEngine().close();
        e.getOpponentEngine().close();
        e.getController().close();
      }
    }
  }

  private void runInSelfPlayGenerationMode(String[] args) {
    String destFile = DEF_FENS_FILE_PATH;
    int concurrency = DEF_CONCURRENCY;
    int games = -1;
    long tc = -1;
    long tcInc = 0;
    Boolean useBook = null;
    Integer hash = null;
    Integer threads = null;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-games":
          games = Integer.parseInt(args[++i]);
          break;
        case "-tc":
          tc = Long.parseLong(args[++i]);
          break;
        case "--destfile":
          destFile = args[++i];
          break;
        case "--concurrency":
          concurrency = Integer.parseInt(args[++i]);
          break;
        case "--inc":
          tcInc = Long.parseLong(args[++i]);
          break;
        case "--trybook":
          useBook = Boolean.parseBoolean(args[++i]);
          break;
        case "--tryhash":
          hash = Integer.parseInt(args[++i]);
          break;
        case "--trythreads":
          threads = Integer.parseInt(args[++i]);
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    if (games == -1 || tc == -1) {
      throw new IllegalArgumentException();
    }
    runInSelfPlayGenerationMode(destFile, concurrency, games, tc, tcInc, useBook, hash, threads);
  }

  private void runInPGNGenerationMode(String sourceFile, String destFile, int maxNumOfGames) {
    try (ControllerEngine engine = factory.newControllerEngineInstance()) {
      FENFileUtil.generateFENFile(engine, sourceFile, destFile, maxNumOfGames);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runInPGNGenerationMode(String[] args) {
    String destFile = DEF_FENS_FILE_PATH;
    String sourceFile = null;
    int maxNumOfGames = Integer.MAX_VALUE;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-sourcefile":
          sourceFile = args[++i];
          break;
        case "--maxgames":
          maxNumOfGames = Integer.parseInt(args[++i]);
          break;
        case "--destfile":
          destFile = args[++i];
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    runInPGNGenerationMode(sourceFile, destFile, maxNumOfGames);
  }

  private void runInGenerationMode(String[] args) {
    String arg0 = args[0];
    if ("byselfplay".equals(arg0)) {
      runInSelfPlayGenerationMode(Arrays.copyOfRange(args, 1, args.length));
    } else if ("bypgnconversion".equals(arg0)) {
      runInPGNGenerationMode(Arrays.copyOfRange(args, 1, args.length));
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void runInDrawFiltrationMode(String sourceFile, String destFile) {
    try {
      FENFileUtil.filterDraws(sourceFile, destFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void runInMateFiltrationMode(String sourceFile, String destFile) {
    try (TunableEngine tunableEngine = factory.newTunableEngineInstance()) {
      FENFileUtil.filterObviousMates(sourceFile, destFile, tunableEngine);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runInTacticalPositionFiltrationMode(String sourceFile, String destFile) {
    try (ControllerEngine controllerEngine = factory.newControllerEngineInstance()) {
      FENFileUtil.filterTacticalPositions(sourceFile, destFile, controllerEngine);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runInNoParamFiltrationMode(String[] args, BiConsumer<String,String> filtrationFunction) {
    String sourceFile = null;
    String destFile = DEF_FENS_FILE_PATH;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-sourcefile":
          sourceFile = args[++i];
          break;
        case "--destfile":
          destFile = args[++i];
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    if (sourceFile == null) {
      throw new IllegalArgumentException();
    }
    filtrationFunction.accept(sourceFile, destFile);
  }

  private void runInDrawFiltrationMode(String[] args) {
    runInNoParamFiltrationMode(args, this::runInDrawFiltrationMode);
  }

  private void runInMateFiltrationMode(String[] args) {
    runInNoParamFiltrationMode(args, this::runInMateFiltrationMode);
  }

  private void runInTacticalPositionFiltrationMode(String[] args) {
    runInNoParamFiltrationMode(args, this::runInTacticalPositionFiltrationMode);
  }

  private void runInOpeningFiltrationMode(String sourceFile, String destFile, int numOfPositionsToFilter) {
    try {
      FENFileUtil.filterOpeningPositions(sourceFile, destFile, numOfPositionsToFilter);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void runInOpeningFiltrationMode(String[] args) {
    String sourceFile = null;
    String destFile = DEF_FENS_FILE_PATH;
    int numOfPositionsToFilter = -1;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "-firstxmoves":
          numOfPositionsToFilter = Integer.parseInt(args[++i]);
          break;
        case "-sourcefile":
          sourceFile = args[++i];
          break;
        case "--destfile":
          destFile = args[++i];
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    if (sourceFile == null || numOfPositionsToFilter <= 0) {
      throw new IllegalArgumentException();
    }
    runInOpeningFiltrationMode(sourceFile, destFile, numOfPositionsToFilter);
  }

  private void runInFiltrationMode(String[] args) {
    String arg0 = args[0];
    if ("draws".equals(arg0)) {
      runInDrawFiltrationMode(Arrays.copyOfRange(args, 1, args.length));
    } else if ("mates".equals(arg0)) {
      runInMateFiltrationMode(Arrays.copyOfRange(args, 1, args.length));
    } else if ("tacticals".equals(arg0)) {
      runInTacticalPositionFiltrationMode(Arrays.copyOfRange(args, 1, args.length));
    } else if ("openings".equals(arg0)) {
      runInOpeningFiltrationMode(Arrays.copyOfRange(args, 1, args.length));
    } else {
      throw new IllegalArgumentException();
    }
  }

  private String buildBinaryString(String arg) {
    String[] probs = arg.split(",");
    StringBuilder binaryStringBuffer = new StringBuilder();
    for (String probString : probs) {
      double prob = Double.parseDouble(probString.trim());
      binaryStringBuffer.append(prob >= 0.5 ? "1" : "0");
    }
    return binaryStringBuffer.toString();
  }

  private double[] buildFeatureArray(String arg) {
    String[] array = arg.split(",");
    double[] decimalArray = new double[array.length];
    for (int j = 0; j < array.length; j++) {
      decimalArray[j] = Double.parseDouble(array[j].trim());
    }
    return decimalArray;
  }

  private void runInBinaryStringConversionMode(String binaryString, Set<ParameterType> paramTypes,
      String destFile) {
    try (TunableEngine engine = factory.newTunableEngineInstance()) {
      engine.init();
      EngineParameters params = engine.getParameters();
      params.set(binaryString, paramTypes);
      params.writeToFile(destFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runInFeatureArrayConversionMode(double[] features, String destFile) {
    try (TunableEngine engine = factory.newTunableEngineInstance()) {
      engine.init();
      EngineParameters params = engine.getParameters();
      params.set(features, new HashSet<>(Collections.singletonList(ParameterType.STATIC_EVALUATION)));
      params.writeToFile(destFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runInConversionMode(String[] args) {
    String arg0 = args[0];
    String destFile = DEF_CONVERTED_PARAMS_PATH;
    if (!"-value".equals(args[1])) {
      throw new IllegalArgumentException();
    }
    String val = args[2];
    if ("probvector".equals(arg0)) {
      Set<ParameterType> paramTypes = null;
      if (args.length > 3) {
        if ("--paramtype".equals(args[3])) {
          paramTypes = resolveParamTypes(args[4]);
        } else {
          throw new IllegalArgumentException();
        }
      }
      runInBinaryStringConversionMode(buildBinaryString(val), paramTypes, destFile);
    } else if ("parameters".equals(arg0)) {
      runInFeatureArrayConversionMode(buildFeatureArray(val), destFile);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void runInGUIMode() {
    try (ControllerEngine controller = factory.newControllerEngineInstance();
        UCIEngine searchEngine = factory.newEngineInstance()) {
      GUI.setEngines(controller, searchEngine);
      Application.launch(GUI.class);
    }
  }

  @Override
  public synchronized void run() {
    if (args != null && args.length > 0) {
      String arg0 = args[0];
      switch (arg0) {
        // UCI mode.
        case "-u":
          runInUCIMode();
          break;
        // Tuning.
        case "-t":
          runInTuningMode(Arrays.copyOfRange(args, 1, args.length));
          break;
        // Generate FENs file.
        case "-g":
          runInGenerationMode(Arrays.copyOfRange(args, 1, args.length));
          break;
        // Filter FENs file.
        case "-f":
          runInFiltrationMode(Arrays.copyOfRange(args, 1, args.length));
          break;
        // Convert to parameters.
        case "-c":
          runInConversionMode(Arrays.copyOfRange(args, 1, args.length));
          break;
        default:
          throw new IllegalArgumentException();
      }
    } else {
      // GUI mode.
      runInGUIMode();
    }
  }

}
