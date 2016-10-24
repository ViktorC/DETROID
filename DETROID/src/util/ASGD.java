package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * An adaptive stochastic gradient descent implementation for supervised learning. It uses the ADADELTA method to adapt its learning rate. It 
 * can be employed as a stochastic, mini-batch, or standard btach gradient descent algorithm. There are two abstract methods to implement for its 
 * subclasses, {@link #costFunction(double[], ArrayList) costFunction} which is ideally a smooth, convex function whose (global) minimum is to be 
 * found (if the function is not convex, the algorithm might converge to a local minimum) and {@link #cacheTrainingData(String) cacheTrainingData} 
 * which reads, parses, and caches the training data set for further use into an ArrayList of Objects.
 * 
 * ADADELTA: https://arxiv.org/pdf/1212.5701v1.pdf
 * 
 * @author Viktor
 *
 */
public abstract class ASGD {
	
	/**
	 * The default constant employed in the numerical differentiation formulas used to derive the derivatives of the cost function
	 */
	protected static final double H = 1e-3;
	/**
	 * The default fudgy factor used for calculating Root-Mean-Squares.
	 */
	protected static final double EPSILON = 1e-8;
	/**
	 * The default decay fraction of the accumulated Root-Mean-Squares. It serves the purpose of a momentum rate.
	 */
	protected static final double DECAY_RATE = 0.95;
	/**
	 * The default base learning rate.
	 */
	protected static final double LEARNING_RATE = 1;
	
	protected final double[] features;
	protected final boolean enableNegativeFeatures;
	protected final double h;
	protected final double learningRate;
	protected final double epsilon;
	protected final double decayRate;
	protected final Integer maxEpoch;
	protected final Integer sampleSize;
	protected final ArrayList<Object> trainingData;
	
	protected final Logger logger;
	
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param features The features to optimize.
	 * @param enableNegativeFeatures Whether the features can take on negtive values.
	 * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. If the 
	 * the function is smooth, usually the smaller it is, the more accurate the approximation of the derivatives will be. It should never 
	 * be 0 nonetheless. If it is null, a default value of 1e-3 is used. However, the optimal value is highly application dependent (e.g. 
	 * if the cost function treats the features as integers, a value of less than 1 or any non-integer value whatsoever would make no sense), 
	 * thus it is recommended to provide a non-null value for h.
	 * @param baseLearningRate The base step size for the gradient descent. If it is null, the default base learning rate of 1.0 will 
	 * be used.
	 * @param epsilon A constant fudgy factor used to better condition the denominator when calculating the Root-Mean-Squares. If it is 
	 * null, the default value of 1e-8 will be used. It is not recommended to change this value.
	 * @param decayRate A constant that determines the decay rate of the accumulated Root-Mean-Squares of past deltas and gradients used to 
	 * calculate the current delta. If it is null, a default value of 0.95 is used. The lower this value, the faster the decay. It is not 
	 * recommended to change this value. 
	 * @param maxEpoch The maximum number of iterations. If it is null, the optimization process will only stop if the gradient of the 
	 * cost function is 0 for each feature.
	 * @param sampleSize The size of the mini-batches to calculate the cost function on. If it is null, the whole training data set will be 
	 * used.
	 * @param filePath The path to the file holding the training data set.
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws IllegalArgumentException
	 */
	protected ASGD(double[] features, boolean enableNegativeFeatures, Double h, Double baseLearningRate, Double epsilon, Double decayRate,
			Integer maxEpoch, Integer sampleSize, String filePath, Logger logger) throws IllegalArgumentException {
		if (features == null || features.length == 0)
			throw new IllegalArgumentException("The array features cannot be null and its length has to be greater than 0.");
		this.features = features;
		this.enableNegativeFeatures = enableNegativeFeatures;
		if (!enableNegativeFeatures) {
			for (int i = 0; i < features.length; i++)
				features[i] = Math.max(0, features[i]);
		}
		this.h = (h == null ? H : h);
		this.learningRate = (baseLearningRate == null ? LEARNING_RATE : baseLearningRate);
		this.epsilon = (epsilon == null ? EPSILON : epsilon);
		this.decayRate = (decayRate == null ? DECAY_RATE : decayRate);
		this.maxEpoch = maxEpoch;
		this.sampleSize = sampleSize;
		try {
			this.trainingData = cacheTrainingData(filePath);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		this.logger = logger;
	}
	/**
	 * Optimizes the features and returns the set that is associated with the minimum of the cost function (whether it's a local or global one 
	 * depends on the convexity of the function).
	 * 
	 * @return The optimal feature set.
	 */
	public synchronized double[] train() {
		int i = 0;
		double[] accGradSqrs = new double[features.length];
		double[] accDeltaSqrs = new double[features.length];
		// For logging.
		double[] delta = new double[features.length];
		for (;;) {
			// Random sample the training data.
			ArrayList<Object> sample = sampleTrainingData();
			// Compute the gradient.
			double[] gradient = computeGradient(sample);
			boolean changed = false;
			for (int j = 0; j < features.length; j++) {
				double g = gradient[j];
				// If g is not 0, the minimum has not been reached with respect to feature j yet.
				if (g != 0)
					changed = true;
				// Update the accumulated gradient squares according to the momentum constant.
				accGradSqrs[j] = decayRate*accGradSqrs[j] + (1 - decayRate)*Math.pow(g, 2);
				/* Calculate delta by dividing the product of the gradient and the RMS of the accumulated deltas up to the previous epoch by
				 * the RMS of the accumulated gradients up to the current epoch. */
				double d = g*Math.sqrt(accDeltaSqrs[j] + epsilon)/Math.sqrt(accGradSqrs[j] + epsilon);
				// By default, the step size is determined fully dynamically and learningRate is 1. In case the default learningRate is overriden...
				d *= learningRate;
				delta[j] = d;
				// Update the feature.
				features[j] -= d;
				// Constraints.
				if (!enableNegativeFeatures)
					features[j] = Math.max(0, features[j]);
				else
					features[j] = Math.max(features[j], Double.MIN_VALUE);
				// Update the accumulated delta squares using the same method as for the accumulated gradients.
				accDeltaSqrs[j] = decayRate*accDeltaSqrs[j] + (1 - decayRate)*Math.pow(d, 2);
			}
			if (logger != null)
				logger.info("Epoch: " + i + "; Cost: " + costFunction(features, trainingData) + System.lineSeparator() + "Delta: " +
						Arrays.toString(delta) + System.lineSeparator() +"Features: " + Arrays.toString(features) + System.lineSeparator());
			i++;
			if (!changed)
				break;
			if (maxEpoch != null && i >= maxEpoch)
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
		if (sampleSize == null)
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
	 * It uses a two-point numerical differentiation formula (centered difference formula or in corner cases, 
	 * Newton's difference quotient) to approximate the derivative of the cost function for the training data 
	 * sample with respect to the parameter at index i in the features array.
	 * 
	 * @param i The index of the parameter for which the derivative of the cost function is to be computed.
	 * @param dataSample An iterable data set on which the cost function is to be calculated.
	 * @return The derivative of the cost function with respect to the feature at index i in the feature set.
	 */
	private final double computeCostFunctionDerivative(int i, ArrayList<Object> dataSample) {
		double cost1, cost2, denominator;
		double feature = features[i];
		if (feature > Double.MAX_VALUE - h) {
			cost1 = costFunction(features, dataSample);
			features[i] = feature - h;
			cost2 = costFunction(features, dataSample);
			denominator = h;
		} else if (feature < Double.MIN_VALUE + h || (!enableNegativeFeatures && feature - h < 0)) {
			features[i] = feature + h;
			cost1 = costFunction(features, dataSample);
			features[i] = feature;
			cost2 = costFunction(features, dataSample);
			denominator = h;
		} else {
			features[i] = feature + h;
			cost1 = costFunction(features, dataSample);
			features[i] = feature - h;
			cost2 = costFunction(features, dataSample);
			features[i] = feature;
			denominator = 2*h;
		}
		features[i] = feature;
		return (cost1 - cost2)/denominator;
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
