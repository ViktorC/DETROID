package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * A stochastic, mini-batch gradient descent algorithm implementation with learning momentum. It can also be instantiated as a standard, batch 
 * gradient descent algorithm.
 * 
 * @author Viktor
 *
 */
public abstract class SGD {
	
	/**
	 * The value used for the maxEpoch field in case no limit has been specified.
	 */
	protected static final int NO_EPOCH_CAP = -1;
	/**
	 * The value used for the sampleSize field in case no limit has been specified.
	 */
	protected static final int NO_MINI_BATCH = -1;
	/**
	 * The fraction of the previous gradient values that will be added to the current one.
	 */
	protected static final double MOMENTUM = 0.9;
	
	protected final double[] features;
	protected final boolean enableNegativeFeatures;
	protected final double h;
	protected final int maxEpoch;
	protected final int sampleSize;
	protected final double learningRate;
	protected final ArrayList<Object> trainingData;
	
	protected final Logger logger;
	
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param features The features to optimize.
	 * @param enableNegativeFeatures Whether the features can take on negtive values.
	 * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. The 
	 * smaller it is, the more accurate the approximation of the derivatives will be.
	 * @param learningRate The step size for the gradient descent.
	 * @param maxEpoch The maximum number of iterations. If it equals {@link #NO_EPOCH_CAP NO_EPOCH_CAP}, the optimization process will 
	 * only stop if the gradient of the cost function is 0 for each feature.
	 * @param sampleSize The size of the mini-batches to calculate the cost function on. If it equals {@link #NO_MINI_BATCH NO_MINI_BATCH}, 
	 * the whole training data set will be used.
	 * @param filePath The path to the file holding the training data set.
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws IllegalArgumentException
	 */
	protected SGD(double[] features, boolean enableNegativeFeatures, double h, double learningRate, int maxEpoch,
			int sampleSize, String filePath, Logger logger) throws IllegalArgumentException {
		if (features == null || features.length == 0)
			throw new IllegalArgumentException("The array features cannot be null and its length has to be greater than 0.");
		this.features = features;
		this.enableNegativeFeatures = enableNegativeFeatures;
		if (!enableNegativeFeatures) {
			for (int i = 0; i < features.length; i++)
				features[i] = Math.max(0, features[i]);
		}
		this.h = h;
		this.maxEpoch = maxEpoch;
		this.sampleSize = sampleSize;
		try {
			this.trainingData = cacheTrainingData(filePath);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		this.logger = logger;
		this.learningRate = learningRate;
	}
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param features The features to optimize.
	 * @param enableNegativeFeatures Whether the features can take on negtive values.
	 * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. The 
	 * smaller it is, the more accurate the approximation of the derivatives will be.
	 * @param learningRate The step size for the gradient descent.
	 * @param sampleSize The size of the mini-batches to calculate the cost function on. If it equals {@link #NO_MINI_BATCH NO_MINI_BATCH}, 
	 * the whole training data set will be used.
	 * @param filePath The path to the file holding the training data set.
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws IllegalArgumentException
	 */
	protected SGD(double[] features, boolean enableNegativeFeatures, double h, double learningRate, int sampleSize,
			String filePath, Logger logger) throws IllegalArgumentException {
		this(features, enableNegativeFeatures, h, learningRate, NO_EPOCH_CAP, sampleSize, filePath, logger);
	}
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param features The features to optimize.
	 * @param enableNegativeFeatures Whether the features can take on negtive values.
	 * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. The 
	 * smaller it is, the more accurate the approximation of the derivatives will be.
	 * @param initialLearningRate The initial step size for the gradient descent.
	 * @param filePath The path to the file holding the training data set.
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws IllegalArgumentException
	 */
	protected SGD(double[] features, boolean enableNegativeFeatures, double h, double initialLearningRate, String filePath,
			Logger logger) throws IllegalArgumentException {
		this(features, enableNegativeFeatures, h, initialLearningRate, NO_EPOCH_CAP, NO_MINI_BATCH, filePath, logger);
	}
	/**
	 * Optimizes the features and returns the set that is associated with the minimum of the cost function (whether it's a local or global one 
	 * depends on the convexity of the function).
	 * 
	 * @return The optimal feature set.
	 */
	public double[] train() {
		int i = 0;
		double[] deltas = new double[features.length];
		for (;;) {
			ArrayList<Object> sample = sampleTrainingData();
			double[] gradient = computeGradient(sample);
			boolean changed = false;
			for (int j = 0; j < features.length; j++) {
				double delta = learningRate*gradient[j];
				if (delta != 0)
					changed = true;
				deltas[j] = MOMENTUM*deltas[j] + delta;
				features[j] -= deltas[j];
				if (!enableNegativeFeatures)
					features[j] = Math.max(0, features[j]);
			}
			if (logger != null)
				logger.info("Epoch: " + i + "; Cost: " + costFunction(features, trainingData) + System.lineSeparator() + "Deltas: " +
						Arrays.toString(deltas) + System.lineSeparator() +"Features: " + Arrays.toString(features) + System.lineSeparator());
			i++;
			if (!changed)
				break;
			if (maxEpoch != NO_EPOCH_CAP && i >= maxEpoch)
				break;
		}
		return features;
	}
	/**
	 * Extracts a random sample from the training data set. The size of the sample is determined by the value of the field sampleSize.
	 * 
	 * @return An iterable data set on which the cost function is to be calculated.
	 */
	private final ArrayList<Object> sampleTrainingData() {
		if (sampleSize == NO_MINI_BATCH)
			return trainingData;
		Random rand = new Random(System.currentTimeMillis());
		ArrayList<Object> sample = new ArrayList<>(sampleSize);
		for (int i = 0; i < sampleSize; i++) {
			int ind = (int) (rand.nextDouble()*trainingData.size());
			sample.add(trainingData.get(ind));
		}
		return sample;
	}
	/**
	 * Computes the gradient of the cost function for the current parameter values.
	 * 
	 * @param dataSample An iterable data set on which the cost function is to be calculated.
	 * @return The gradient of the cost function.
	 */
	private final double[] computeGradient(ArrayList<Object> dataSample) {
		double[] gradient = new double[features.length];
		for (int i = 0; i < gradient.length; i++)
			gradient[i] = computeCostFunctionDerivative(i, dataSample);
		return gradient;
	}
	/**
	 * It uses a two-point numerical differentiation formula (centered difference formula) to approximate the derivative of the cost function 
	 * for the training data sample with respect to the parameter at index i in the features array.
	 * 
	 * @param i The index of the parameter for which the derivative of the cost function is to be computed.
	 * @param dataSample An iterable data set on which the cost function is to be calculated.
	 * @return The derivative of the cost function with respect to the feature at index i in the feature set.
	 */
	private final double computeCostFunctionDerivative(int i, ArrayList<Object> dataSample) {
		double cost1, cost2;
		double feature = features[i];
		features[i] = feature + h;
		cost1 = costFunction(features, dataSample);
		features[i] = feature - h;
		cost2 = costFunction(features, dataSample);
		features[i] = feature;
		return (cost1 - cost2)/(2*h);
	}
	/**
	 * Loads all the data sets from the file into an ArrayList of Objects.
	 * 
	 * @param filePath The path to the file containing the training data.
	 * @return An ArrayList holding the training data.
	 * @throws Exception If an IO or parsing error occurs.
	 */
	protected abstract ArrayList<Object> cacheTrainingData(String filePath) throws Exception;
	/**
	 * Calculates the costs associated with the given feature set for the specified data sample. The better the system performs, the lower the costs 
	 * should be. Ideally, the cost function is convex.
	 * 
	 * @param features An array of parameters.
	 * @param dataSample An iterable data set on which the cost function is to be calculated.
	 * @return The cost associated with the given features.
	 */
	protected abstract double costFunction(double[] features, ArrayList<Object> dataSample);
}
