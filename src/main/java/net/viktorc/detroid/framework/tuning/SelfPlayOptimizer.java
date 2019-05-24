package net.viktorc.detroid.framework.tuning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.util.PBIL;
import net.viktorc.detroid.framework.validation.Elo;

/**
 * A class for optimizing chess engine parameters using a PBIL algorithm with a possibly parallel, game play based fitness function.
 *
 * @author Viktor
 */
public final class SelfPlayOptimizer extends PBIL implements AutoCloseable {

  /**
   * The learning rate of the evolutionary algorithm.
   */
  private static final double DEF_LEARNING_RATE = .1d;
  /**
   * The negative learning rate of the evolutionary algorithm.
   */
  private static final double DEF_NEGATIVE_LEARNING_RATE = .075d;
  /**
   * The mutation probability of the genotypes.
   */
  private static final double DEF_MUTATION_PROB = .025d;
  /**
   * The mutation shift of the genotypes.
   */
  private static final double DEF_MUTATION_SHIFT = .05d;

  private final List<SelfPlayEngines<TunableEngine>> engines;
  private final Set<ParameterType> parameterTypes;
  private final int games;
  private final long timePerGame;
  private final long timeIncPerMove;
  private final double validationFactor;
  private final Arena[] arenas;
  private final ExecutorService pool;
  private int tempGeneration;

  /**
   * Constructs a new instance according to the specified parameters.
   *
   * @param engines A list of {@link net.viktorc.detroid.framework.tuning.SelfPlayEngines} instances that each contain the engines needed
   * for one optimization thread. For each non-null element in the list, a new thread will be utilized for the optimization. E.g. if engines
   * is a list of four non-null elements, the games in the fitness function will be distributed and played parallel on four threads. The
   * list's first element cannot be null or a {@link java.lang.NullPointerException} is thrown.
   * @param parameterTypes The set of chess engine parameter types to tune with game play. If it is null, all parameters will be tuned.
   * @param games The number of games to play to assess the fitness of the parameters.
   * @param timePerGame The time each engine will have per game in milliseconds.
   * @param timeIncPerMove The number of milliseconds with which the remaining time of an engine is incremented after each legal move.
   * @param validationFactor The factor of the original number of games to play in addition when assessing the fitness of a parameter set
   * whose fitness surpassed the current highest fitness after having played the original number of games.
   * @param initialProbabilityVector The starting probability vector for the optimization. It allows the algorithm to pick up where a
   * previous, terminated optimization process left off. If the array's length is smaller than the engine to be tuned's parameters' binary
   * string's length, it will be extended with elements of the value 0.5d; if the length of the array is greater than engine to be tuned's
   * parameters' binary string's length, only the first x elements will be considered, where x equals the parameters' binary string's
   * length. If it is null, an array with a length equal to the parameters' binary string's length, only containing elements that have the
   * value 0.5d will be used.
   * @param populationSize The number of samples to produce per generation.
   * @param learningRate The learning rate of the evolutionary algorithm.
   * @param negativeLearningRate The negative learning rate of the evolutionary algorithm.
   * @param mutationProbability The mutation probability of the genotypes.
   * @param mutationShift The mutation shift of the genotypes.
   * @param generations The number of generations to complete. If it is null, the training will go on until convergence or until it's
   * manually stopped.
   * @param logger A logger to log the optimization process. It cannot be null.
   * @throws Exception If the engines cannot be initialized.
   * @throws IllegalArgumentException If logger is null.
   */
  public SelfPlayOptimizer(List<SelfPlayEngines<TunableEngine>> engines, Set<ParameterType> parameterTypes, int games, long timePerGame,
      long timeIncPerMove, double validationFactor, double[] initialProbabilityVector, int populationSize, Double learningRate,
      Double negativeLearningRate, Double mutationProbability, Double mutationShift, Integer generations, Logger logger)
      throws Exception, IllegalArgumentException {
    super(engines.get(0).getEngine().getParameters().toGrayCodeString(parameterTypes).length(), populationSize,
        mutationProbability == null ? DEF_MUTATION_PROB : mutationProbability, mutationShift == null ? DEF_MUTATION_SHIFT : mutationShift,
        learningRate == null ? DEF_LEARNING_RATE : learningRate,
        negativeLearningRate == null ? DEF_NEGATIVE_LEARNING_RATE : negativeLearningRate, generations, null, initialProbabilityVector,
        logger);
    if (logger == null) {
      throw new IllegalArgumentException("The logger cannot be null.");
    }
    int engineCount = 0;
    this.engines = new ArrayList<>();
    for (SelfPlayEngines<TunableEngine> e : engines) {
      if (e != null && engineCount++ < games) {
        this.engines.add(e);
      }
    }
    this.parameterTypes = parameterTypes;
    logger.info("Tuning parameters of type: " + this.parameterTypes);
    this.games = games;
    this.timePerGame = timePerGame;
    this.timeIncPerMove = timeIncPerMove;
    this.validationFactor = validationFactor;
    arenas = new Arena[this.engines.size()];
    for (int i = 0; i < this.engines.size(); i++) {
      arenas[i] = new Arena(this.engines.get(i).getController(), Logger.getAnonymousLogger(), null);
    }
    pool = Executors.newFixedThreadPool(Math.min(Math.max(1, Runtime.getRuntime().availableProcessors()), this.engines.size()));
    tempGeneration = -1;
  }

  private double assessResults(List<Future<MatchResult>> futures, int initEngine1Wins, int initEngine2Wins,
      int initDraws) {
    for (Future<MatchResult> f : futures) {
      try {
        MatchResult res = f.get();
        initEngine1Wins += res.getEngine1Wins();
        initEngine2Wins += res.getEngine2Wins();
        initDraws += res.getDraws();
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    return Elo.calculateDifference(initEngine1Wins, initEngine2Wins, initDraws);
  }

  @Override
  protected double computeFitness(String genotype) {
    int currGen = getCurrentGeneration();
    if (tempGeneration != currGen) {
      tempGeneration = currGen;
      double[] probVec = getProbabilityVector();
      StringBuilder setBuilder = new StringBuilder();
      for (double prob : probVec) {
        setBuilder.append(prob >= 0.5 ? "1" : "0");
      }
      for (SelfPlayEngines<TunableEngine> tunableEngines : engines) {
        TunableEngine oppEngine = tunableEngines.getOpponentEngine();
        if (!oppEngine.isInit()) {
          try {
            oppEngine.init();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        oppEngine.getParameters().set(setBuilder.toString(), parameterTypes);
        oppEngine.notifyParametersChanged();
      }
    }
    int engine1Wins = 0;
    int engine2Wins = 0;
    int draws = 0;
    ArrayList<Future<MatchResult>> futures = new ArrayList<>(engines.size());
    int remainingGames = games;
    for (int i = 0; i < engines.size(); i++) {
      final int index = i;
      final int workLoad = remainingGames / (engines.size() - i);
      remainingGames -= workLoad;
      futures.add(pool.submit(() -> {
        TunableEngine tunEngine = engines.get(index).getEngine();
        UCIEngine oppEngine = engines.get(index).getOpponentEngine();
        if (!tunEngine.isInit()) {
          tunEngine.init();
        }
        if (!oppEngine.isInit()) {
          oppEngine.init();
        }
        tunEngine.getParameters().set(genotype, parameterTypes);
        tunEngine.notifyParametersChanged();
        return arenas[index].match(tunEngine, oppEngine, workLoad, timePerGame, timeIncPerMove);
      }));
    }
    double fitness = assessResults(futures, engine1Wins, engine2Wins, draws);
    int encore = (int) (validationFactor * games);
    if (fitness > getCurrentHighestFitness() && encore > 0) {
      int addGamesPlayed = 0;
      futures = new ArrayList<>(engines.size());
      for (int i = 0; i < engines.size() && addGamesPlayed < encore; i++) {
        int index = i;
        int gamesToPlay = (int) Math.min((double) encore - addGamesPlayed, Math.ceil(((double) encore) / engines.size()));
        futures.add(pool.submit(() -> arenas[index].match(engines.get(index).getEngine(), engines.get(index).getOpponentEngine(),
            gamesToPlay, timePerGame, timeIncPerMove)));
        addGamesPlayed += gamesToPlay;
      }
      fitness = assessResults(futures, engine1Wins, engine2Wins, draws);
    }
    return fitness;
  }

  @Override
  public void close() {
    pool.shutdown();
    for (Arena a : arenas) {
      a.close();
    }
    for (SelfPlayEngines<TunableEngine> e : engines) {
      e.getEngine().close();
      e.getOpponentEngine().close();
      e.getController().close();
    }
  }

}
