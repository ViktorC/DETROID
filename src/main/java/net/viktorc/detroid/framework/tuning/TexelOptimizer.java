package net.viktorc.detroid.framework.tuning;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.util.NadamSGD;

/**
 * A class for optimizing chess engine evaluation parameters using a stochastic gradient descent algorithm with a possibly parallel cost
 * function. The cost function is based on that of the
 * <a href="https://www.chessprogramming.org/Texel%27s_Tuning_Method">Texel Tuning Method</a>
 * which is the mean squared error of the evaluation scores of a set of positions exported from games with known outcomes.
 *
 * @author Viktor
 */
public final class TexelOptimizer extends NadamSGD<String, Float> implements AutoCloseable {

  /**
   * The batch size to use for the calculation of the total training and test costs.
   */
  private static final long DEF_COST_CALC_BATCH_SIZE = 2000000;
  /**
   * The fraction of the complete data set used as a test data set.
   */
  private static final double DEF_TEST_DATA_PROPORTION = .2;
  /**
   * The base step size for the gradient descent.
   */
  private static final double DEF_BASE_LEARNING_RATE = 1d;
  /**
   * The learning rate annealing rate for the gradient descent.
   */
  private static final double DEF_ANNEALING_RATE = .9;
  /**
   * The only parameter type the optimizer is concerned with.
   */
  private static final Set<ParameterType> TYPE = new HashSet<>(Collections.singletonList(ParameterType.STATIC_EVALUATION));
  /**
   * The minimum first derivative of K needed to continue the line search. If the first derivative drops below this threshold, a local
   * minimum is assumed to have been found.
   */
  private static final double MIN_K_1ST_DERIVATIVE = 1e-8;

  private final String fenFilePath;
  private final long dataSetSize;
  private final int testDataStartInd;
  private final TunableEngine[] engines;
  private final ExecutorService pool;
  private double k;
  private long trainingDataReaderHead;
  private long testDataReaderHead;

  /**
   * Constructs and returns a new instance according to the specified parameters.
   *
   * @param engines An array of {@link net.viktorc.detroid.framework.tuning.TunableEngine} instances (of which the parameters' gray code
   * string should have the same length). For each non-null element in the array, a new thread will be utilized for the optimization. E.g.
   * if engines is an array of four non-null elements, the fitness function will be distributed and executed parallel on four threads. If
   * the array or its first element are null or the method {@link net.viktorc.detroid.framework.uci.UCIEngine#init() init} hasn't been
   * called on the first element, a {@link java.lang.NullPointerException} is thrown.
   * @param trainingBatchSize The number of positions to include in one mini-batch. The higher this number is, the slower but more stable
   * the convergence will be.
   * @param epochs The maximum number of iterations. If it is 0, the loop is endless.
   * @param h The step size to use for the numerical differentiation of the cost function. If it is null, it defaults to 1 (if the
   * parameters are integers as they usually are in chess engines, a value of less than 1 or any non-integer value whatsoever would make no
   * sense).
   * @param baseLearningRate The base step size for the gradient descent. If it is null, it defaults to 1.
   * @param learningAnnealingRate The factor by which the learning rate is multiplied after every epoch. If it is null, it defaults to 0.9.
   * @param fenFilePath The path to the file containing the FEN list of positions to evaluate. If it doesn't exist an {@link
   * java.io.IOException} is thrown.
   * @param costCalculationBatchSize The number of samples in the batches used for calculating the total training and test costs. If it is
   * null, it defaults to 4 million.
   * @param k A scaling constant for the sigmoid function used calculate the average error.
   * @param testDataProportion The proportion of the entire data set that should be used as test data. It has to be greater than 0 and less
   * than 1. If it is null, it defaults to {@link #DEF_TEST_DATA_PROPORTION}.
   * @param logger A logger to log the status of the optimization. It cannot be null.
   * @throws Exception If the engines cannot be initialised.
   * @throws IllegalArgumentException If the logger is null, or the batch size is not greater than 0, or the data set is too small.
   */
  public TexelOptimizer(TunableEngine[] engines, long trainingBatchSize, int epochs, Double h, Double baseLearningRate, Double learningAnnealingRate,
      String fenFilePath, Long costCalculationBatchSize, Double k, Double testDataProportion, Logger logger)
      throws Exception, IllegalArgumentException {
    super(engines[0].getParameters().values(TYPE), (double[]) Array.newInstance(double.class,
        engines[0].getParameters().values(TYPE).length), engines[0].getParameters().maxValues(TYPE), trainingBatchSize,
        costCalculationBatchSize == null ? DEF_COST_CALC_BATCH_SIZE : costCalculationBatchSize, epochs, h == null ? 1d : h,
        baseLearningRate == null ? DEF_BASE_LEARNING_RATE : baseLearningRate,
        learningAnnealingRate == null ? DEF_ANNEALING_RATE : learningAnnealingRate, null, null, null, logger);
    if (logger == null) {
      throw new IllegalArgumentException("The logger cannot be null.");
    }
    if (baseLearningRate != null && baseLearningRate <= 0) {
      throw new IllegalArgumentException("The base learning rate has to be greater than 0.");
    }
    if (testDataProportion != null && (testDataProportion >= 1 || testDataProportion <= 0)) {
      throw new IllegalArgumentException("The test data proportion has to be greater than 0 and less than 1.");
    }
    this.fenFilePath = fenFilePath;
    testDataProportion = testDataProportion == null ? DEF_TEST_DATA_PROPORTION : testDataProportion;
    this.dataSetSize = countDataSetSize();
    if (dataSetSize < Math.ceil(1d / testDataProportion)) {
      throw new IllegalArgumentException("The complete data set has to contain at least " +
          Math.ceil(1d / testDataProportion) + " data rows.");
    }
    testDataStartInd = (int) (dataSetSize * (1 - testDataProportion));
    ArrayList<TunableEngine> enginesList = new ArrayList<>();
    for (TunableEngine e : engines) {
      if (e != null) {
        if (!e.isInit()) {
          e.init();
        }
        e.setDeterministicZeroDepthMode(true);
        enginesList.add(e);
      }
    }
    this.engines = enginesList.toArray(new TunableEngine[enginesList.size()]);
    pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
    logger.info("Tuning parameters of type: " + ParameterType.STATIC_EVALUATION);
    if (k == null) {
      computeAndSetOptimalK();
      logger.info("Optimal K: " + this.k + System.lineSeparator());
    } else {
      this.k = k;
    }
  }

  /**
   * Returns the number of data rows in the labelled data set.
   *
   * @return The number of data rows contained in the file.
   * @throws FileNotFoundException If the file does not exist.
   * @throws IOException If an IO error occurs.
   */
  private long countDataSetSize() throws FileNotFoundException, IOException {
    long count = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(fenFilePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!"".equals(line)) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Reads the data into a list of key-pair values where the key is FEN position and the value is the label denoting which side won the
   * game in which the position occurred.
   *
   * @param fromInd The line number from which on the lines will be loaded into the data set.
   * @param toInd The line number up to which (exclusive) the lines will be loaded into the data set.
   * @return The data held in the lines between the specified indices.
   * @throws IOException If there is an IO error reading the file.
   */
  private List<Entry<String, Float>> loadData(long fromInd, long toInd) throws IOException {
    List<Entry<String, Float>> data = new ArrayList<>();
    if (fromInd == toInd) {
      return data;
    }
    long count = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(fenFilePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (count >= fromInd && count < toInd) {
          line = line.trim();
          if (!line.isEmpty()) {
            String[] parts = line.split(";");
            String fen = parts[0];
            Float result = Float.parseFloat(parts[1]);
            data.add(new SimpleEntry<>(fen, result));
          }
        }
        count++;
      }
    }
    return data;
  }

  /**
   * It applies the sigmoid function to the input value using the instance's scaling value K.
   *
   * @param output The value to squash.
   * @return The squashed output value.
   */
  private double sigmoid(double output) {
    return 1d / (1d + Math.exp(k * -output));
  }

  /**
   * Computes the squared error of the evaluation given the game's result and the squashed score of the 0-depth search.
   *
   * @param label The game's result. 1 for white win, 0 for black win, 0.5 for draw.
   * @param prediction The sigmoid of the score of the quiescence search returns.
   * @return The cross entropy loss.
   */
  private static double squaredError(double label, double prediction) {
    return Math.pow(label - prediction, 2d);
  }

  /**
   * Evaluates the position instances given the specified parameters using a 0-depth quiescence search.
   *
   * @param parameters The engine parameters.
   * @param dataSample A data sample containing the chess position strings and their corresponding labels representing the outcome of the
   * game in which the position occurred.
   * @return A list of scores corresponding to the chess positions.
   * @throws ExecutionException If an execution error happens in one of the threads.
   * @throws InterruptedException If the current thread is interrupted while waiting for the worker threads to finish.
   */
  private List<Short> predict(double[] parameters, List<Entry<String, Float>> dataSample)
      throws InterruptedException, ExecutionException {
    ArrayList<Future<List<Short>>> futures = new ArrayList<>();
    int startInd = 0;
    int workLoadPerThread = (int) Math.ceil(((double) dataSample.size()) / engines.length);
    for (int i = 0; i < engines.length && startInd < dataSample.size(); i++) {
      final int finalStartInd = startInd;
      final TunableEngine e = engines[i];
      e.getParameters().set(parameters, TYPE);
      e.notifyParametersChanged();
      futures.add(pool.submit(() -> {
        try {
          List<Short> predictions = new ArrayList<>();
          int endInd = Math.min(dataSample.size(), finalStartInd + workLoadPerThread);
          for (int j = finalStartInd; j < endInd; j++) {
            Entry<String, Float> dataPair = dataSample.get(j);
            String fen = dataPair.getKey();
            e.setPosition(fen);
            SearchResults res = e.search(null, null, null, null, null, null, null, 0,
                null, null, null, null);
            Short score = res.getScore().orElse((short) 0);
            // Check if it's white's turn.
            if (!fen.contains("w")) {
              score = (short) -score;
            }
            predictions.add(score);
          }
          return predictions;
        } catch (Exception e1) {
          throw new RuntimeException(e1);
        }
      }));
      startInd += workLoadPerThread;
    }
    List<Short> allPredictions = new ArrayList<>();
    for (Future<List<Short>> f : futures) {
      allPredictions.addAll(f.get());
    }
    return allPredictions;
  }

  /**
   * Computes the total cost, the first derivative of K, and the second derivative of K over the training data set.
   *
   * @return The total cost, the 1st derivative of K, and the 2nd derivative of K.
   */
  private double[] computeCostAndDerivativesOfK() {
    double cost = 0;
    double firstDerivative = 0;
    double secondDerivative = 0;
    long samples = 0;
    resetTrainingDataReader();
    List<Entry<String, Float>> batch;
    while (!(batch = getTrainingData(costCalculationBatchSize)).isEmpty()) {
      try {
        List<Short> scores = predict(parameters, batch);
        for (int i = 0; i < batch.size(); i++) {
          double result = batch.get(i).getValue();
          double score = scores.get(i);
          double sigmoid = sigmoid(score);
          double dSquaredErrorWrtSigmoid = 2d * (sigmoid - result);
          double dSigmoidWrtK = score * sigmoid * (1d - sigmoid);
          cost += squaredError(result, sigmoid);
          firstDerivative += dSquaredErrorWrtSigmoid * dSigmoidWrtK;
          secondDerivative += (2d * dSigmoidWrtK + dSquaredErrorWrtSigmoid * (score * (1d - 2d * sigmoid))) * dSigmoidWrtK;
        }
        samples += batch.size();
      } catch (InterruptedException | ExecutionException e) {
        if (logger != null) {
          logger.log(Level.SEVERE, e, e::getMessage);
        }
        Thread.currentThread().interrupt();
      }
    }
    cost /= samples;
    firstDerivative /= samples;
    secondDerivative /= samples;
    return new double[]{cost, firstDerivative, secondDerivative};
  }

  /**
   * Computes the optimal scaling constant K for the sigmoid function used to calculate the prediction loss. It employs Newton's method.
   */
  private void computeAndSetOptimalK() {
    k = 0;
    for (;;) {
      double[] values = computeCostAndDerivativesOfK();
      double cost = values[0];
      double firstDerivative = values[1];
      if (logger != null) {
        logger.info("K: " + k + "; Total cost: " + cost + "; 1st derivative of K: " + firstDerivative);
      }
      if (Math.abs(firstDerivative) < MIN_K_1ST_DERIVATIVE) {
        break;
      }
      double secondDerivative = values[2];
      k -= firstDerivative / secondDerivative;
    }
  }

  @Override
  protected void resetTrainingDataReader() {
    trainingDataReaderHead = 0;
  }

  @Override
  protected void resetTestDataReader() {
    testDataReaderHead = testDataStartInd;
  }

  @Override
  protected List<Entry<String, Float>> getTrainingData(long batchSize) {
    long correctedBatchSize = Math.min(testDataStartInd - trainingDataReaderHead, batchSize);
    try {
      List<Entry<String, Float>> data = loadData(trainingDataReaderHead, trainingDataReaderHead + correctedBatchSize);
      trainingDataReaderHead += correctedBatchSize;
      return data;
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  protected List<Entry<String, Float>> getTestData(long batchSize) {
    long correctedBatchSize = Math.min(dataSetSize - testDataReaderHead, batchSize);
    try {
      List<Entry<String, Float>> data = loadData(testDataReaderHead, testDataReaderHead + correctedBatchSize);
      testDataReaderHead += correctedBatchSize;
      return data;
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  protected double costFunction(double[] parameters, List<Entry<String, Float>> dataSample) {
    try {
      double totalCost = 0;
      List<Short> scores = predict(parameters, dataSample);
      for (int i = 0; i < dataSample.size(); i++) {
        double result = dataSample.get(i).getValue();
        double score = scores.get(i);
        totalCost += squaredError(result, sigmoid(score));
      }
      return totalCost;
    } catch (InterruptedException | ExecutionException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    pool.shutdown();
    for (TunableEngine e : engines) {
      e.close();
    }
  }

}
