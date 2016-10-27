package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * An adaptive stochastic gradient descent implementation for supervised learning. It is based on the Nadam (Nesterov-accelerated adaptive 
 * moment estimation) algorithm. It can be employed as a stochastic, mini-batch, or standard batch gradient descent algorithm. There are two 
 * abstract methods to implement for its subclasses, {@link #costFunction(double[], ArrayList) costFunction} which is ideally a smooth, 
 * convex function whose (global) minimum is to be found (if the function is not convex, the algorithm might converge to a local minimum) 
 * and {@link #cacheTrainingData(String) cacheTrainingData} which reads, parses, and caches the training data set for further use into an 
 * ArrayList of Objects.
 * 
 * Nadam: http://cs229.stanford.edu/proj2015/054_report.pdf
 * 
 * @author Viktor
 *
 */
public abstract class ASGD {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 
	
	/**
	 * The default constant employed in the numerical differentiation formulas used to derive the derivatives of the cost function.
	 */
	protected static final double H = 1e-3;
	/**
	 * The default fudgy factor used for conditioning the Root-Mean-Square of the decaying second moment estimates of the gradient as the 
	 * denominator in the deltas.
	 */
	protected static final double EPSILON = 1e-8;
	/**
	 * The default decay constant of the accumulated Nesterov momentum.
	 */
	protected static final double MOMENTUM_DECAY_RATE = 0.99;
	/**
	 * The default decay constant of the accumulated gradient squares.
	 */
	protected static final double NORM_DECY_RATE = 0.999;
	/**
	 * The default exponent used in the scheduling function of the momentum decay constant.
	 */
	protected static final double ANNEALING_EXPONENT = 0.004;
	/**
	 * The default base learning rate.
	 */
	protected static final double LEARNING_RATE = 0.001;
	
	protected final double[] features;
	protected final double[] minValues;
	protected final double[] maxValues;
	protected final double h;
	protected final double learningRate;
	protected final double epsilon;
	protected final double momentumDecayRate;
	protected final double normDecayRate;
	protected final double annealingRate;
	protected final Integer maxEpoch;
	protected final Integer sampleSize;
	protected final ArrayList<Object> trainingData;
	
	protected final Logger logger;
	
	/**
	 * Constructs an instance according to the specified parameters.
	 * 
	 * @param features The starting values of the features to optimize.
	 * @param minValues The minimum allowed values for the features. Each element corresponds to the element at the same index in the 
	 * features array. If the length of the array is greater than that of the features array, the extra elements will be ignored. If the 
	 * length of the array is smaller than that off the features array, the array will be extended by elements of the greatest negative 
	 * double value to match the length of the features array. If it is null, an array of elements of the greatest negative double value 
	 * will be used. Each element has to be smaller by at least the absolute value of h times two than the corresponding element in the 
	 * maxValues array.
	 * @param maxValues The maximum allowed values for the features. Each element corresponds to the element at the same index in the 
	 * features array. If the length of the array is greater than that of the features array, the extra elements will be ignored. If the 
	 * length of the array is smaller than that off the features array, the array will be extended by elements of the greatest positive 
	 * double value to match the length of the features array. If it is null, an array of elements of the greatest positive double value 
	 * will be used. Each element has to be greater by at least the absolute value of h times two than the corresponding element in the 
	 * minValues array.
	 * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. If the 
	 * the function is smooth, usually the smaller it is, the more accurate the approximation of the derivatives will be. It should never 
	 * be 0 nonetheless. If it is null, a default value of 1e-3 is used. However, the optimal value is highly application dependent (e.g. 
	 * if the cost function treats the features as integers, a value of less than 1 or any non-integer value whatsoever would make no sense), 
	 * thus it is recommended to provide a non-null value for h.
	 * @param baseLearningRate The base step size for the gradient descent. If it is null, the default base learning rate of 1.0 will 
	 * be used.
	 * @param epsilon A constant fudgy factor used to better condition the denominator when calculating the Root-Mean-Squares. If it is 
	 * null, the default value of 1e-8 will be used. It is not recommended to change this value.
	 * @param momentumDecayRate A constant that determines the base decay rate of the accumulated Nesterov momentum. If it is null, a 
	 * default value of 0.99 is used. The lower this value, the faster the decay. It is not recommended to change this value. However, 
	 * if it is changed, the new value has to be within the range of 0 (inclusive) and 1 (inclusive).
	 * @param normDecayRate A constant that determines the base decay rate of the accumulated gradient squares. If it is null, a default 
	 * value of 0.999 is used. The lower this value, the faster the decay. It is not recommended to change this value. However, if it is 
	 * changed, the new value has to be within the range of 0 (inclusive) and 1 (inclusive).
	 * @param annealingExponent A constant that determines the momentum's annealing schedule. If it is null, it defaults to 4e-3. It is 
	 * not recommended to change this value.
	 * @param maxEpoch The maximum number of iterations. If it is null, the optimization process will only stop if the gradient of the 
	 * cost function is 0 for each feature.
	 * @param sampleSize The size of the mini-batches to calculate the cost function on. If it is null, the whole training data set will be 
	 * used.
	 * @param filePath The path to the file holding the training data set.
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws IllegalArgumentException If features is null or its length is 0. If the decay rate is greater than 1 or smaller than 0. If 
	 * an element in minValues is not at least the absolute value of h times two smaller than the respective element in maxValues.
	 */
	protected ASGD(double[] features, double[] minValues, double[] maxValues, Double h, Double baseLearningRate, Double epsilon,
			Double momentumDecayRate, Double normDecayRate, Double annealingExponent, Integer maxEpoch, Integer sampleSize, String filePath,
			Logger logger) throws IllegalArgumentException {
		if (features == null || features.length == 0)
			throw new IllegalArgumentException("The array features cannot be null and its length has to be greater than 0.");
		this.features = features.clone();
		this.minValues = new double[features.length];
		this.maxValues = new double[features.length];
		Arrays.fill(this.minValues, -Double.MAX_VALUE);
		Arrays.fill(this.maxValues, Double.MAX_VALUE);
		if (minValues != null) {
			int length = Math.min(this.features.length, minValues.length);
			for (int i = 0; i < length; i++) {
				this.minValues[i] = minValues[i];
				this.features[i] = Math.max(this.features[i], minValues[i]);
			}
		}
		if (maxValues != null) {
			int length = Math.min(this.features.length, this.maxValues.length);
			for (int i = 0; i < length; i++) {
				this.minValues[i] = minValues[i];
				this.features[i] = Math.min(this.features[i], this.maxValues[i]);
			}
		}
		int length = Math.min(this.minValues.length, this.maxValues.length);
		for (int i = 0; i < length; i++) {
			if (this.minValues[i] >= this.maxValues[i])
				throw new IllegalArgumentException("The minimum value constraints have to be smaller than the respective maximum value constraints.");
			if (this.maxValues[i] - this.minValues[i] <= Math.abs(2*h))
				throw new IllegalArgumentException("The windows between the respective minimum and maximum values have to be at least |2*h| each.");
		}
		this.h = (h == null ? H : h);
		this.learningRate = (baseLearningRate == null ? LEARNING_RATE : baseLearningRate);
		this.epsilon = (epsilon == null ? EPSILON : epsilon);
		if (momentumDecayRate != null && (momentumDecayRate > 1 || momentumDecayRate < 0))
			throw new IllegalArgumentException("The momentum decay rate cannot be greater than 1 or smaller than 0.");
		this.momentumDecayRate = (momentumDecayRate == null ? MOMENTUM_DECAY_RATE : momentumDecayRate);
		if (normDecayRate != null && (normDecayRate > 1 || normDecayRate < 0))
			throw new IllegalArgumentException("The norm decay rate cannot be greater than 1 or smaller than 0.");
		this.normDecayRate = (normDecayRate == null ? NORM_DECY_RATE : normDecayRate);
		this.annealingRate = (annealingExponent == null ? ANNEALING_EXPONENT : annealingExponent);
		this.maxEpoch = maxEpoch;
		if (sampleSize != null && sampleSize <= 0)
			throw new IllegalArgumentException("The sample size has to be greater than 0 or null.");
		this.sampleSize = sampleSize;
		try {
			this.trainingData = cacheTrainingData(filePath);
		} catch (Exception e) {
			throw new IllegalArgumentException("The training data set could not be cached.", e);
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
		double g, gPrime;
		double m, mPrime, mBar;
		double n, nPrime;
		double momDecayRateT, momDecayRateTplus1;
		double d;
		double[] momentumVector = new double[features.length];
		double[] normVector = new double[features.length];
		// For logging.
		double[] delta = new double[features.length];
		// Initialize the momentum decay rate sequence product.
		double momentumPi = computeMomentumSchedule(1);
		momDecayRateT = momentumPi;
		for (int t = 1;; t++) {
			// Random sample the training data.
			ArrayList<Object> sample = sampleTrainingData();
			// Compute the gradient.
			double[] gradient = computeGradient(sample);
			boolean changed = false;
			for (int i = 0; i < features.length; i++) {
				g = gradient[i];
				/* Correct the gradient vector initialization bias by 1 - the product of the sequence of annealed momentum decay 
				 * rates up until t. */
				gPrime = g/(1 - momentumPi);
				// Update the momentum vector, the accumulated first moment estimates of the gradients.
				m = momentumDecayRate*momentumVector[i] + (1 - momentumDecayRate)*g;
				momentumVector[i] = m;
				// Compute the annealed momentum decay rate for the next epoch.
				momDecayRateTplus1 = computeMomentumSchedule(t + 1);
				// Update the total product of annealed momentum decay rates.
				momentumPi *= momDecayRateTplus1;
				// Correct the momentum vector initialization bias by the momentum Pi for t + 1.
				mPrime = m/(1 - momentumPi);
				// Update the norm vector, the accumulated second moment estimates of the gradients.
				n = normDecayRate*normVector[i] + (1 - normDecayRate)*Math.pow(g, 2);
				normVector[i] = n;
				// Correct the norm vector initialization bias by the norm decay rate to the power of t.
				nPrime = n/(1 - Math.pow(normDecayRate, t));
				// Compute the Nesterov-accelerated learning rate adapter.
				mBar = (1 - momDecayRateT)*gPrime + momDecayRateTplus1*mPrime;
				// The current epoch's next annealed decay rate is next epoch's current annealed decay rate...
				momDecayRateT = momDecayRateTplus1;
				// Delta.
				d = learningRate*mBar/(Math.sqrt(nPrime) + epsilon);
				delta[i] = d;
				features[i] -= d;
				// Constraints.
				features[i] = Math.min(features[i], maxValues[i]);
				features[i] = Math.max(features[i], minValues[i]);
				// If g is not 0, the minimum has not been reached with respect to feature j yet.
				if (g != 0)
					changed = true;
			}
			// Log information about the current state.
			if (logger != null) {
				// The greatest absolute deltas of the epoch.
				ArrayList<Double> sortedDelta = new ArrayList<>(delta.length);
				for (double v : delta)
					sortedDelta.add(v);
				sortedDelta.sort((a, b) -> Math.abs(a) > Math.abs(b) ? 1 : (Math.abs(a) == Math.abs(b) ? 0 : -1));
				double[] greatestDelta = new double[Math.min(delta.length, 5)];
				for (int j = 0; j < greatestDelta.length; j++)
					greatestDelta[j] = sortedDelta.get(sortedDelta.size() - (j + 1));
				// Log cost over the complete training data set as well.
				logger.info("Epoch: " + t + "; Cost: " + costFunction(features, trainingData) + System.lineSeparator() + "Greatest deltas: " +
						Arrays.toString(greatestDelta) + System.lineSeparator() + "Deltas: " + Arrays.toString(delta) + System.lineSeparator() + 
						"Features: " + Arrays.toString(features) + System.lineSeparator());
			}
			if (!changed)
				break;
			if (maxEpoch != null && t >= maxEpoch)
				break;
		}
		return features;
	}
	/**
	 * Computes the annealed momentum decay rate according to the momentum schedule.
	 * 
	 * @param t The epoch index.
	 * @return The annealed momentum decay rate.
	 */
	private double computeMomentumSchedule(int t) {
		return momentumDecayRate*(1d - 0.5d*Math.pow(0.96d, t*annealingRate));
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
		if (feature > maxValues[i] - h) {
			cost1 = costFunction(features, dataSample);
			features[i] = feature - h;
			cost2 = costFunction(features, dataSample);
			denominator = h;
		} else if (feature < minValues[i] - h) {
			cost2 = costFunction(features, dataSample);
			features[i] = feature + h;
			cost1 = costFunction(features, dataSample);
			denominator = h;
		} else {
			features[i] = feature + h;
			cost1 = costFunction(features, dataSample);
			features[i] = feature - h;
			cost2 = costFunction(features, dataSample);
			denominator = 2*h;
		}
		features[i] = feature;
		return (cost1 - cost2)/denominator;
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