package net.viktorc.detroid.framework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * An adaptive stochastic gradient descent implementation for supervised learning. It is based on the Nadam (Nesterov-accelerated adaptive
 * moment estimation) algorithm. It can be employed as a stochastic, mini-batch, or standard batch gradient descent algorithm. There are
 * five abstract methods to implement for its subclasses, {@link #computeCost(double[], List)} which is ideally a differentiable, smooth,
 * convex function whose (global) minimum is to be found (if the function is not convex, the algorithm might converge to a local minimum),
 * {@link #getTrainingData(long)} which fetches batches the training data, {@link #resetTrainingDataReader()} which resets the data reader
 * to the beginning of the training data, and {@link #getTestData(long)} and {@link #resetTestDataReader()} which do the same for the test
 * data.
 *
 * Nadam: <a href="http://cs229.stanford.edu/proj2015/054_report.pdf">http://cs229.stanford.edu/proj2015/054_report.pdf</a>
 *
 * @param <E> The type of the data entries.
 * @param <L> The type of the labels for the data entries.
 * @author Viktor
 */
public abstract class NadamSGD<E, L> {

  /**
   * The default constant employed in the numerical differentiation formulas used to derive the derivatives of the cost function.
   */
  protected static final double H = 1e-3;
  /**
   * The default base learning rate.
   */
  protected static final double LEARNING_RATE = 1e-3;
  /**
   * The default factor by which the learning rate is multiplied after every epoch.
   */
  protected static final double LEARNING_ANNEALING_RATE = .99;
  /**
   * The default decay constant of the accumulated Nesterov momentum.
   */
  protected static final double FIRST_MOMENT_DECAY_RATE = .99;
  /**
   * The default decay constant of the accumulated gradient squares.
   */
  protected static final double SECOND_MOMENT_DECAY_RATE = .999;
  /**
   * The default L1 regularization coefficient.
   */
  protected static final double L1_REGULARIZATION_COEFF = 0d;
  /**
   * The default L2 regularization coefficient.
   */
  protected static final double L2_REGULARIZATION_COEFF = 0d;
  /**
   * The default fudgy factor used for conditioning the Root-Mean-Square of the decaying second moment estimates of the gradient as the
   * denominator in the deltas.
   */
  protected static final double EPSILON = 1e-8;

  protected final double[] parameters;
  protected final double[] minValues;
  protected final double[] maxValues;
  protected final Set<Integer> indicesToIgnore;
  protected final double h;
  protected final double learningRate;
  protected final double learningAnnealingRate;
  protected final double firstMomentDecayRate;
  protected final double secondMomentDecayRate;
  protected final double l1RegularizationCoeff;
  protected final double l2RegularizationCoeff;
  protected final double epsilon;
  protected final long trainingBatchSize;
  protected final long costCalculationBatchSize;
  protected final int epochs;

  protected final Logger logger;

  /**
   * Constructs an instance according to the specified parameters.
   *
   * @param parameters The starting values of the parameters to optimize.
   * @param minValues The minimum allowed values for the parameters. Each element corresponds to the element at the same index in the parameters
   * array. If the length of the array is greater than that of the parameters array, the extra elements will be ignored. If the length of the
   * array is smaller than that off the parameters array, the array will be extended by elements of the greatest negative double value to
   * match the length of the parameters array. If it is null, an array of elements of the greatest negative double value will be used. Each
   * element has to be smaller by at least the absolute value of h times two than the corresponding element in the maxValues array, else the
   * corresponding parameter will be ignored.
   * @param maxValues The maximum allowed values for the parameters. Each element corresponds to the element at the same index in the parameters
   * array. If the length of the array is greater than that of the parameters array, the extra elements will be ignored. If the length of the
   * array is smaller than that off the parameters array, the array will be extended by elements of the greatest positive double value to
   * match the length of the parameters array. If it is null, an array of elements of the greatest positive double value will be used. Each
   * element has to be greater by at least the absolute value of h times two than the corresponding element in the minValues array, else the
   * corresponding parameter will be ignored.
   * @param trainingBatchSize The number of samples in the mini-batches used for training.
   * @param costCalculationBatchSize The number of samples in the batches used for calculating the total training and test costs. Using
   * batches allows for the calculation of costs over data sets that do not fit into memory. However, using small batches may incur a
   * significant IO overhead if the data source is in a file system.
   * @param epochs The maximum number of iterations. If it is 0, the loop is endless.
   * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. If the the
   * function is smooth, usually the smaller it is, the more accurate the approximation of the derivatives will be. It should never be 0
   * nonetheless. If it is null, a default value of 1e-3 is used. However, the optimal value is highly application dependent (e.g. if the
   * cost function treats the parameters as integers, a value of less than 1 or any non-integer value whatsoever would make no sense), thus it
   * is recommended to provide a non-null value for h.
   * @param baseLearningRate The base step size for the gradient descent. If it is null, the default base learning rate of 1.0 will be
   * used.
   * @param learningAnnealingRate The factor by which the learning rate is multiplied after every epoch. If it is null, a default value of
   * 0.95 is used.
   * @param firstMomentDecayRate A constant that determines the base decay rate of the accumulated Nesterov momentum. If it is null, a
   * default value of 0.99 is used. The lower this value, the faster the decay. It is not recommended to change this value. However, if it
   * is changed, the new value has to be within the range of 0 (inclusive) and 1 (inclusive).
   * @param secondMomentDecayRate A constant that determines the base decay rate of the accumulated gradient squares. If it is null, a default
   * value of 0.999 is used. The lower this value, the faster the decay. It is not recommended to change this value. However, if it is
   * changed, the new value has to be within the range of 0 (inclusive) and 1 (inclusive).
   * @param l1RegularizationCoeff The coefficient to use for L1 parameter regularization, by default 0.
   * @param l2RegularizationCoeff The coefficient to use for L2 parameter regularization, by default 0.
   * @param epsilon A constant used to better condition the denominator when calculating the Root-Mean-Squares. If it is null, the default
   * value of 1e-8 will be used. It is not recommended to change this value.
   * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
   * @throws IllegalArgumentException If parameters is null or its length is 0. If the decay rate is greater than 1 or smaller than 0. If an
   * element in minValues is greater than the respective element in maxValues.
   */
  protected NadamSGD(double[] parameters, double[] minValues, double[] maxValues, long trainingBatchSize, long costCalculationBatchSize,
      int epochs, Double h, Double baseLearningRate, Double learningAnnealingRate, Double firstMomentDecayRate,
      Double secondMomentDecayRate, Double l1RegularizationCoeff, Double l2RegularizationCoeff, Double epsilon, Logger logger)
      throws IllegalArgumentException {
    if (parameters == null || parameters.length == 0) {
      throw new IllegalArgumentException("The parameters array cannot be null and its length has to be greater than 0.");
    }
    if (trainingBatchSize <= 0) {
      throw new IllegalArgumentException("The training batch size has to be greater than 0.");
    }
    if (costCalculationBatchSize <= 0) {
      throw new IllegalArgumentException("The cost calculation batch size has to be greater than 0.");
    }
    indicesToIgnore = new HashSet<>();
    if (minValues != null && maxValues != null) {
      int length = Math.min(minValues.length, maxValues.length);
      length = Math.min(length, parameters.length);
      for (int i = 0; i < length; i++) {
        if (minValues[i] > maxValues[i]) {
          throw new IllegalArgumentException("The minimum value constraints cannot be greater than the " +
              "respective maximum value constraints.");
        }
        if (maxValues[i] - minValues[i] <= Math.abs(2 * h)) {
          indicesToIgnore.add(i);
        }
      }
    }
    this.parameters = parameters.clone();
    this.minValues = new double[parameters.length];
    this.maxValues = new double[parameters.length];
    Arrays.fill(this.minValues, -Double.MAX_VALUE);
    Arrays.fill(this.maxValues, Double.MAX_VALUE);
    for (int i = 0; i < parameters.length; i++) {
      if (indicesToIgnore.contains(i)) {
        continue;
      }
      this.parameters[i] = parameters[i];
      if (minValues != null) {
        this.parameters[i] = Math.max(this.parameters[i], minValues[i]);
        this.minValues[i] = minValues[i];
      }
      if (maxValues != null) {
        this.parameters[i] = Math.min(this.parameters[i], maxValues[i]);
        this.maxValues[i] = maxValues[i];
      }
    }
    this.trainingBatchSize = trainingBatchSize;
    this.costCalculationBatchSize = costCalculationBatchSize;
    this.epochs = epochs;
    this.h = (h == null ? H : h);
    this.learningRate = (baseLearningRate == null ? LEARNING_RATE : baseLearningRate);
    if (learningAnnealingRate != null && learningAnnealingRate <= 0) {
      throw new IllegalArgumentException("The learning annealing rate cannot be less than or equal to 0.");
    }
    this.learningAnnealingRate = (learningAnnealingRate == null ? LEARNING_ANNEALING_RATE : learningAnnealingRate);
    if (firstMomentDecayRate != null && (firstMomentDecayRate > 1 || firstMomentDecayRate < 0)) {
      throw new IllegalArgumentException("The first momentum decay rate cannot be greater than 1 or smaller than 0.");
    }
    this.firstMomentDecayRate = (firstMomentDecayRate == null ? FIRST_MOMENT_DECAY_RATE : firstMomentDecayRate);
    if (secondMomentDecayRate != null && (secondMomentDecayRate > 1 || secondMomentDecayRate < 0)) {
      throw new IllegalArgumentException("The second momentum decay rate cannot be greater than 1 or smaller than 0.");
    }
    this.secondMomentDecayRate = (secondMomentDecayRate == null ? SECOND_MOMENT_DECAY_RATE : secondMomentDecayRate);
    if (l1RegularizationCoeff != null && (l1RegularizationCoeff < 0)) {
      throw new IllegalArgumentException("The L1 regularization coefficient has to be greater than 0.");
    }
    this.l1RegularizationCoeff = (l1RegularizationCoeff == null ? L1_REGULARIZATION_COEFF : l1RegularizationCoeff);
    if (l2RegularizationCoeff != null && (l2RegularizationCoeff < 0)) {
      throw new IllegalArgumentException("The L2 regularization coefficient has to be greater than 0.");
    }
    this.l2RegularizationCoeff = (l2RegularizationCoeff == null ? L2_REGULARIZATION_COEFF : l2RegularizationCoeff);
    this.epsilon = (epsilon == null ? EPSILON : epsilon);
    this.logger = logger;
  }

  /**
   * Optimizes the parameters and returns the set that is associated with the minimum of the cost function (whether it's a local or global
   * one depends on the convexity of the function).
   *
   * @return The optimal parameter set.
   */
  public synchronized double[] optimize() {
    // Rolling average of the gradient (first moment).
    double[] firstMomentVector = new double[parameters.length];
    // Rolling uncentered variance of the gradient (second moment).
    double[] secondMomentVector = new double[parameters.length];
    // For logging.
    double[] deltas = new double[parameters.length];
    double learningRate = this.learningRate;
    int updates = 1;
    for (int t = 0; epochs <= 0 || t <= epochs; t++) {
      if (t != 0) {
        resetTrainingDataReader();
        int iterations = 0;
        List<Entry<E, L>> batch;
        while (!(batch = getTrainingData(trainingBatchSize)).isEmpty()) {
          // Compute the gradient.
          double[] gradient = computeGradient(batch);
          // Compute the initialization bias correction factors (serves as annealing as well).
          double firstMomentCorrection = 1d / (1d - Math.pow(firstMomentDecayRate, (double) updates));
          double firstMomentNesterovCorrection = 1d / (1d - Math.pow(firstMomentDecayRate, (double) (updates + 1)));
          double secondMomentCorrection = 1d / (1d - Math.pow(secondMomentDecayRate, (double) updates));
          for (int i = 0; i < parameters.length; i++) {
            // Ensure that the magnitude of the gradient is proportional to the batch size.
            double derivative = gradient[i];
            double firstMoment = firstMomentDecayRate * firstMomentVector[i] + (1d - firstMomentDecayRate) * derivative;
            double secondMoment = secondMomentDecayRate * secondMomentVector[i] + (1d - secondMomentDecayRate) * derivative * derivative;
            firstMomentVector[i] = firstMoment;
            secondMomentVector[i] = secondMoment;
            double correctedDerivative = derivative * firstMomentCorrection;
            double nesterovCorrectedFirstMoment = firstMoment * firstMomentNesterovCorrection;
            double correctedSecondMoment = secondMoment * secondMomentCorrection;
            double nesterovMomentum = (1d - firstMomentDecayRate) * correctedDerivative +
                firstMomentDecayRate * nesterovCorrectedFirstMoment;
            double delta = learningRate * nesterovMomentum / (Math.sqrt(correctedSecondMoment) + epsilon);
            deltas[i] = delta;
            // Apply the constraints.
            double updatedParam = parameters[i] - delta;
            parameters[i] = Math.min(Math.max(updatedParam, minValues[i]), maxValues[i]);
          }
          updates++;
          // Log information about the current state.
          if (logger != null) {
            /* Display the greatest absolute deltas of the update to assisst with the tuning of the
             * learning rate hyperparameter. */
            ArrayList<Double> sortedDelta = new ArrayList<>(deltas.length);
            for (double v : deltas) {
              sortedDelta.add(v);
            }
            sortedDelta.sort(Comparator.comparingDouble(Math::abs));
            double[] greatestDelta = new double[Math.min(deltas.length, 5)];
            for (int j = 0; j < greatestDelta.length; j++) {
              greatestDelta[j] = sortedDelta.get(sortedDelta.size() - (j + 1));
            }
            logger.info("Epoch: " + t + "; Update: " + (iterations++) + "; Batch size: " + batch.size() + System.lineSeparator() +
                "Greatest deltas: " + Arrays.toString(greatestDelta) + System.lineSeparator() +
                "Deltas: " + Arrays.toString(deltas) + System.lineSeparator() +
                "Gradient: " + Arrays.toString(gradient) + System.lineSeparator() +
                "Parameters: " + Arrays.toString(parameters));
          }
        }
        learningRate *= learningAnnealingRate;
      }
      /* Calculate the cost over the test data set. This is just to test how well the parameters generalize;
       * it is not used for learning! */
      if (logger != null) {
        logger.info("Epoch: " + t + "; Training cost: " + computeAverageTrainingCost() + "; Test cost: " + computeAverageTestCost());
      }
    }
    return parameters;
  }

  /**
   * Computes the gradient of the cost function for the current parameter values.
   *
   * @param dataSample An iterable data set on which the cost function is to be calculated.
   * @return The gradient of the cost function.
   */
  private double[] computeGradient(List<Entry<E, L>> dataSample) {
    double[] gradient = computeGradient(parameters, dataSample);
    if (gradient == null) {
      gradient = approximateGradient(dataSample);
    }
    double sampleSize = dataSample.size();
    for (int i = 0; i < gradient.length; i++) {
      if (indicesToIgnore.contains(i)) {
        gradient[i] = 0;
      } else {
        double parameter = parameters[i];
        double derivative = gradient[i];
        double l1Reg = parameter >= 0 ? l1RegularizationCoeff : -l1RegularizationCoeff;
        double l2Reg = l2RegularizationCoeff * 2 * parameter;
        gradient[i] = (derivative + l1Reg + l2Reg) / sampleSize;
      }
    }
    return gradient;
  }

  /**
   * It uses a two-point numerical differentiation formula (centered difference formula or in corner cases, Newton's difference quotient) to
   * approximate the derivative of the cost function for the training data sample with respect to the parameters.
   *
   * @param dataSample An iterable data set on which the cost function is to be calculated.
   * @return The estimated gradient of the parameters.
   */
  private double[] approximateGradient(List<Entry<E, L>> dataSample) {
    double[] gradient = new double[parameters.length];
    for (int i = 0; i < gradient.length; i++) {
      if (indicesToIgnore.contains(i)) {
        continue;
      }
      double cost1, cost2, denominator;
      double parameter = parameters[i];
      if (parameter > maxValues[i] - h) {
        cost1 = computeCost(parameters, dataSample);
        parameters[i] = parameter - h;
        cost2 = computeCost(parameters, dataSample);
        denominator = h;
      } else if (parameter < minValues[i] - h) {
        cost2 = computeCost(parameters, dataSample);
        parameters[i] = parameter + h;
        cost1 = computeCost(parameters, dataSample);
        denominator = h;
      } else {
        parameters[i] = parameter + h;
        cost1 = computeCost(parameters, dataSample);
        parameters[i] = parameter - h;
        cost2 = computeCost(parameters, dataSample);
        denominator = 2 * h;
      }
      parameters[i] = parameter;
      gradient[i] = (cost1 - cost2) / denominator;
    }
    return gradient;
  }

  /**
   * Verifies the correctness of the symbolic gradient.
   *
   * @param dataSample The data batch for which the gradients are to be computed.
   * @param absTol The maximum acceptable absolute difference between the symbolic gradient and the numerical gradient.
   * @param relTol The maximum acceptable relative difference between the symbolic gradient and the numerical gradient.
   * @return Whether the symbolic and the numerical gradients are sufficiently close.
   */
  protected boolean verifyGradient(List<Entry<E, L>> dataSample, double absTol, double relTol) {
    double[] symbolicGradient = computeGradient(parameters, dataSample);
    double[] numericalGradient = approximateGradient(dataSample);
    if (symbolicGradient == null) {
      return false;
    } else {
      for (int i = 0; i < symbolicGradient.length; i++) {
        if (indicesToIgnore.contains(i)) {
          symbolicGradient[i] = 0;
        }
      }
    }
    boolean pass = true;
    for (int i = 0; i < parameters.length; i++) {
      double symbolicDerivative = symbolicGradient[i];
      double numericalDerivative = numericalGradient[i];
      boolean match = true;
      if (Math.abs(symbolicDerivative - numericalDerivative) > absTol) {
        match = false;
      } else {
        double absSymbolicDerivative = Math.abs(symbolicDerivative);
        double absNumericalDerivative = Math.abs(numericalDerivative);
        if (absSymbolicDerivative >= absNumericalDerivative) {
          if (absSymbolicDerivative / absNumericalDerivative - 1d > relTol) {
            match = false;
          }
        } else {
          if (absNumericalDerivative / absSymbolicDerivative - 1d > relTol) {
            match = false;
          }
        }
      }
      System.out.println(String.format("Index: %d, Symbolic derivative: %f, Numerical derivative: %f %s",
          i, symbolicDerivative, numericalDerivative, match ? "" : "- MISMATCH"));
      pass = pass && match;
    }
    return pass;
  }

  /**
   * It computes the total average cost over the entire data set supplied by the data provider.
   *
   * @param dataProvider The batch data provider function.
   * @return The average cost.
   */
  private double computeAverageCost(Function<Long, List<Entry<E, L>>> dataProvider) {
    double totalCost = 0;
    long samples = 0;
    List<Entry<E, L>> batch;
    while (!(batch = dataProvider.apply(costCalculationBatchSize)).isEmpty()) {
      double loss = computeCost(parameters, batch);
      totalCost += loss;
      samples += batch.size();
    }
    if (samples == 0) {
      return 0;
    }
    return totalCost / samples;
  }

  /**
   * Computes the average cost over the entire training data set.
   *
   * @return The average training cost.
   */
  private double computeAverageTrainingCost() {
    resetTrainingDataReader();
    return computeAverageCost(this::getTrainingData);
  }

  /**
   * Computes the average cost over the entire test data set.
   *
   * @return The average test cost.
   */
  private double computeAverageTestCost() {
    resetTestDataReader();
    return computeAverageCost(this::getTestData);
  }

  /**
   * Resets the training data reader enanbling the resampling of already sampled data points. E.g. if the data provider reads the data from
   * a file line by line, the invocation of this method should set the file stream back to the first line.
   */
  protected abstract void resetTrainingDataReader();

  /**
   * Resets the test data reader enanbling the resampling of already sampled data points. E.g. if the data provider reads the data from
   * a file line by line, the invocation of this method should set the file stream back to the first line.
   */
  protected abstract void resetTestDataReader();

  /**
   * Extracts a sample from the training data set and loads it into a list of key-value pairs where the key is the data and the value is the
   * ground truth. It should never extract the same data point twice until the {@link #resetTrainingDataReader()} method is called. If there
   * is no more training data left, an empty list should be returned. The list should never be null.
   *
   * @param batchSize The maximum number of entries the returned list is to have. It is never less than 1.
   * @return A list holding the training observation-label pairs.
   */
  protected abstract List<Entry<E, L>> getTrainingData(long batchSize);

  /**
   * Extracts a sample from the test data set and loads it into a list of key-value pairs where the key is the data and the value is the
   * ground truth. It should never extract the same data point twice until the {@link #resetTestDataReader()} method is called. If there
   * is no more test data left, an empty list should be returned. The list should never be null.
   *
   * @param batchSize The maximum number of entries the returned list is to have. It is never less than 1.
   * @return A list holding the test observation-label pairs.
   */
  protected abstract List<Entry<E, L>> getTestData(long batchSize);

  /**
   * Calculates the costs associated with the given parameter set for the specified data sample. The better the system performs, the lower
   * the costs should be. Ideally, the cost function is differentiable, smooth, and convex, but these are not requirements. The cost should
   * also not be averaged, the optimizer ensures that the costs are independent of the batch size.
   *
   * @param parameters An array of parameters.
   * @param dataSample A list of the training data mapped to the correct labels on which the cost function is to be calculated.
   * @return The cost associated with the given parameters.
   */
  protected abstract double computeCost(double[] parameters, List<Entry<E, L>> dataSample);

  /**
   * Calculates the derivative of the cost function with respect to the parameters.
   *
   * @param parameters An array of parameters.
   * @param dataSample A list of the training data mapped to the correct labels on which the cost function is to be calculated.
   * @return The gradient of the parameters.
   */
  protected abstract double[] computeGradient(double[] parameters, List<Entry<E, L>> dataSample);

}
