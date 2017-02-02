package net.viktorc.detroid.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An adaptive stochastic gradient descent implementation for supervised learning. It is based on the Nadam (Nesterov-accelerated adaptive 
 * moment estimation) algorithm. It can be employed as a stochastic, mini-batch, or standard batch gradient descent algorithm. There are three 
 * abstract methods to implement for its subclasses, {@link #costFunction(double[], ArrayList) costFunction} which is ideally a differentiable, 
 * smooth, convex function whose (global) minimum is to be found (if the function is not convex, the algorithm might converge to a local minimum), 
 * {@link #sampleTrainingData() sampleTrainingData} which (random) samples the training data, and {@link #getTestData() getTestData} which returns 
 * the test data.
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
	protected final Set<Integer> indicesToIgnore;
	protected final double h;
	protected final double learningRate;
	protected final double epsilon;
	protected final double momentumDecayRate;
	protected final double normDecayRate;
	protected final double annealingRate;
	protected final Integer maxEpoch;
	
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
	 * maxValues array, else the corresponding feature will be ignored.
	 * @param maxValues The maximum allowed values for the features. Each element corresponds to the element at the same index in the 
	 * features array. If the length of the array is greater than that of the features array, the extra elements will be ignored. If the 
	 * length of the array is smaller than that off the features array, the array will be extended by elements of the greatest positive 
	 * double value to match the length of the features array. If it is null, an array of elements of the greatest positive double value 
	 * will be used. Each element has to be greater by at least the absolute value of h times two than the corresponding element in the 
	 * minValues array, else the corresponding feature will be ignored.
	 * @param h A constant employed in the numerical differentiation formula used to derive the derivative of the cost function. If the 
	 * the function is smooth, usually the smaller it is, the more accurate the approximation of the derivatives will be. It should never 
	 * be 0 nonetheless. If it is null, a default value of 1e-3 is used. However, the optimal value is highly application dependent (e.g. 
	 * if the cost function treats the features as integers, a value of less than 1 or any non-integer value whatsoever would make no sense), 
	 * thus it is recommended to provide a non-null value for h.
	 * @param baseLearningRate The base step size for the gradient descent. If it is null, the default base learning rate of 1.0 will 
	 * be used.
	 * @param epsilon A constant used to better condition the denominator when calculating the Root-Mean-Squares. If it is 
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
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws IllegalArgumentException If features is null or its length is 0. If the decay rate is greater than 1 or smaller than 0. If 
	 * an element in minValues is greater than the respective element in maxValues.
	 */
	protected ASGD(double[] features, double[] minValues, double[] maxValues, Double h, Double baseLearningRate, Double epsilon,
			Double momentumDecayRate, Double normDecayRate, Double annealingExponent, Integer maxEpoch, Logger logger)
					throws IllegalArgumentException {
		if (features == null || features.length == 0)
			throw new IllegalArgumentException("The array features cannot be null and its length has to be greater than 0.");
		indicesToIgnore = new HashSet<>();
		if (minValues != null && maxValues != null) {
			int length = Math.min(minValues.length, maxValues.length);
			length = Math.min(length, features.length);
			for (int i = 0; i < length; i++) {
				if (minValues[i] > maxValues[i])
					throw new IllegalArgumentException("The minimum value constraints cannot be greater than the respective maximum " +
							"value constraints.");
				if (maxValues[i] - minValues[i] <= Math.abs(2*h))
					indicesToIgnore.add(i);
			}
		}
		this.features = features.clone();
		this.minValues = new double[features.length];
		this.maxValues = new double[features.length];
		Arrays.fill(this.minValues, -Double.MAX_VALUE);
		Arrays.fill(this.maxValues, Double.MAX_VALUE);
		for (int i = 0; i < features.length; i++) {
			if (indicesToIgnore.contains(i))
				continue;
			this.features[i] = features[i];
			if (minValues != null) {
				this.features[i] = Math.max(this.features[i], minValues[i]);
				this.minValues[i] = minValues[i];
			}
			if (maxValues != null) {
				this.features[i] = Math.min(this.features[i], maxValues[i]);
				this.maxValues[i] = maxValues[i];
			}
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
			List<Entry<Object, Object>> sample = sampleTrainingData();
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
				// Update the norm vector.
				n = normDecayRate*normVector[i] + (1 - normDecayRate)*Math.pow(g, 2);
				normVector[i] = n;
				// Correct the norm vector initialization bias by the norm decay rate to the power of t.
				nPrime = n/(1 - Math.pow(normDecayRate, t));
				// Compute the Nesterov-accelerated learning rate factor.
				mBar = (1 - momDecayRateT)*gPrime + momDecayRateTplus1*mPrime;
				// The current epoch's next annealed decay rate is the next epoch's current annealed decay rate...
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
				// Log cost over the test data set. This is just a sanity check; it is not used for learning!
				logger.info("Epoch: " + t + "; Cost: " + costFunction(features, getTestData()) + System.lineSeparator() + "Greatest deltas: " +
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
	private final double[] computeGradient(List<Entry<Object, Object>> dataSample) {
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
	private final double computeCostFunctionDerivative(int i, List<Entry<Object, Object>> dataSample) {
		double cost1, cost2, denominator;
		double feature = features[i];
		if (indicesToIgnore.contains(i))
			return 0;
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
	 * Returns the test data set as a list of key-value pairs where the key is the data and the value is the ground truth label. The list should 
	 * contain at least one data set. The test data should never be included in the training data samples.
	 * 
	 * @return An list holding the validation data mapped to the correct labels.
	 */
	protected abstract List<Entry<Object, Object>> getTestData();
	/**
	 * Extracts a random sample from the training data set and loads it into a list of key-value pairs where the key is the data and the value is the 
	 * ground truth. The list should contain at least one data set.
	 * 
	 * @return An list holding the training data mapped to the correct labels.
	 */
	protected abstract List<Entry<Object, Object>> sampleTrainingData();
	/**
	 * Calculates the costs associated with the given feature set for the specified data sample. The better the system performs, the lower the costs 
	 * should be. Ideally, the cost function is differentiable, smooth, and convex, but these are not requirements.
	 * 
	 * @param features An array of parameters.
	 * @param dataSample A list of the training data mapped to the correct labels on which the cost function is to be calculated.
	 * @return The cost associated with the given features.
	 */
	protected abstract double costFunction(double[] features, List<Entry<Object, Object>> dataSample);
	
}