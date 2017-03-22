package net.viktorc.detroid.framework.tuning;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.util.ASGD;

/**
 * A class for optimizing chess engine evaluation parameters using a stochastic gradient descent 
 * algorithm with a possibly parallel cost function. The cost function is that of the 
 * <a href="https://chessprogramming.wikispaces.com/Texel's+Tuning+Method">Texel Tuning Method</a> 
 * which is the average error of the evaluation scores of a set of positions exported from games 
 * with known outcomes.
 * 
 * @author Viktor
 *
 */
public final class TexelOptimizer extends ASGD<String,Float> implements AutoCloseable {

	/**
	 * The fraction of the complete data set used as a test data set.
	 */
	private static final double TEST_DATA_PROPORTION = 0.2d;
	/**
	 * The base step size for the gradient descent.
	 */
	private static final double BASE_LEARNING_RATE = 1;
	/**
	 * The only parameter type the optimizer is concerned with.
	 */
	private static final Set<ParameterType> TYPE = new HashSet<>(Arrays.asList(ParameterType.STATIC_EVALUATION));
	
	private final TunableEngine[] engines;
	private final double k;
	private final int sampleSize;
	private final int dataSetSize;
	private final String fenFilePath;
	private final List<Entry<String,Float>> testData;
	private final Random rand;
	private final ExecutorService pool;
	
	/**
	 * Constructs and returns a new instance according to the specified parameters.
	 * 
	 * @param engines An array of {@link #TunableEngine TunableEngine} instances (of which 
	 * the parameters' gray code string should have the same length). For each non-null element
	 * in the array, a new thread will be utilized for the optimization. E.g. if engines is an
	 * array of four non-null elements, the fitness function will be distributed and executed 
	 * parallel on four threads. If the array or its first element are null or the method {@link 
	 * #net.viktorc.detroid.frameworkuci.UCIEngine.init() init} hasn't been called on the first 
	 * element, a {@link #NullPointerException NullPointerException} is thrown.
	 * @param sampleSize The number of positions to include in one mini-batch. The higher this number
	 * is, the slower but more stable the convergence will be.
	 * @param baseLearningRate The base step size for the gradient descent. If it is null, it 
	 * defaults to 1.
	 * @param fenFilePath The path to the file containing the FEN list of positions to evaluate.
	 * If it doesn't exist an {@link #IOException IOException} is thrown.
	 * @param k A scaling constant for the sigmoid function used calculate the average error.
	 * @param logger A logger to log the status of the optimization. It cannot be null.
	 * @throws Exception If the engines cannot be initialised.
	 * @throws IllegalArgumentException If the logger is null, or the sample size is not greater than 0, 
	 * or the data set is too small.
	 */
	public TexelOptimizer(TunableEngine[] engines, int sampleSize, Double baseLearningRate, String fenFilePath,
			Double k, Logger logger) throws Exception, IllegalArgumentException {
		super(engines[0].getParameters().values(TYPE), (double[]) Array.newInstance(double.class,
				engines[0].getParameters().values(TYPE).length), engines[0].getParameters().maxValues(TYPE),
				1d, baseLearningRate == null ? BASE_LEARNING_RATE : baseLearningRate, null, null, null, null,
				null, logger);
		if (logger == null)
			throw new IllegalArgumentException("The logger cannot be null.");
		if (sampleSize < 1)
			throw new IllegalArgumentException("The sample size has to be greater than 0.");
		if (baseLearningRate != null && baseLearningRate <= 0)
			throw new IllegalArgumentException("The base learning rate has to be greater than 0.");
		this.sampleSize = sampleSize;
		this.fenFilePath = fenFilePath;
		dataSetSize = countDataSetSize();
		if (dataSetSize < Math.ceil(1/TEST_DATA_PROPORTION))
			throw new IllegalArgumentException("The complete data set has to contain at least " + Math.ceil(1/TEST_DATA_PROPORTION) +
					" data rows.");
		ArrayList<TunableEngine> enginesList = new ArrayList<>();
		for (TunableEngine e : engines) {
			if (e != null) {
				if (!e.isInit())
					e.init();
				e.setDeterministicZeroDepthMode(true);
				enginesList.add(e);
			}
		}
		this.engines = enginesList.toArray(new TunableEngine[enginesList.size()]);
		rand = new Random(System.nanoTime());
		pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
		logger.info("Tuning parameters of type: " + ParameterType.STATIC_EVALUATION);
		if (k == null) {
			this.k = computeOptimalK();
			if (logger != null)
				logger.info("Optimal K: " + this.k + System.lineSeparator());
		} else
			this.k = k;
		testData = cacheData((int) (dataSetSize*(1 - TEST_DATA_PROPORTION)), dataSetSize);
	}
	/**
	 * Computes the optimal scaling constant K for the sigmoid function used to calculate the evaluation error. It uses 
	 * binary search with a minimum resolution of 0.005.
	 * 
	 * @return The value for K that locally minimizes the average error.
	 * @throws IOException If the training data cannot be loaded from the FEN file.
	 */
	private double computeOptimalK() throws IOException {
		final double minResolution = 0.005;
		double k = 0;
		List<Entry<String,Float>> trainingData = cacheData(0, (int) (dataSetSize*(1 - TEST_DATA_PROPORTION)));
		double avgErr = computeAverageError(features, k, trainingData);
		if (logger != null)
			logger.info("K: " + k + "; Avg. error: " + avgErr);
		double kPlusErr = computeAverageError(features, k + minResolution, trainingData);
		double kMinusErr = computeAverageError(features, k - minResolution, trainingData);
		double minAvgErr = Math.min(kPlusErr, kMinusErr);
		if (avgErr < minAvgErr)
			return k;
		boolean increase = kPlusErr < kMinusErr;
		if (increase) {
			k += minResolution;
			if (logger != null)
				logger.info("K: " + k + "; Avg. error: " + kPlusErr);
		} else {
			k -= minResolution;
			if (logger != null)
				logger.info("K: " + k + "; Avg. error: " + kMinusErr);
		}
		avgErr = minAvgErr;
		double resolution = 1;
		double lastK = k;
		while (true) {
			k += increase ? resolution : -resolution;
			double currAvgErr = computeAverageError(features, k, trainingData);
			if (logger != null)
				logger.info("K: " + k + "; Avg. error: " + currAvgErr);
			if (currAvgErr < avgErr) {
				lastK = k;
			} else {
				increase = !increase;
				resolution /= 2;
				if (resolution < minResolution)
					break;
			}
			avgErr = currAvgErr;
		}
		return lastK;
	}
	/**
	 * Computes the error of the evaluation function given the game's result, the score the 0-depth search returned, 
	 * and the scaling constant k.
	 * 
	 * @param result The game's result. 1 for white win, 0 for black win, 0.5 for draw.
	 * @param score The score the quiescence search returns.
	 * @param k The scaling constant for the sigmoid function.
	 * @return
	 */
	private double computeError(double result, double score, double k) {
		double sigmoid = 1/(1 + Math.pow(10, k*score/400));
		return Math.pow(result - sigmoid, 2);
	}
	/**
	 * Computes the average evaluation error based on the FEN file in relation to the result of the games the
	 * positions occurred in.
	 * 
	 * @param features The engine parameters.
	 * @param k The scaling constant K for the sigmoid function.
	 * @return
	 */
	private double computeAverageError(double[] features, final double k, List<Entry<String,Float>> dataSample) {
		double totalError = 0;
		ArrayList<Future<Double>> futures = new ArrayList<>();
		int startInd = 0;
		int workLoadPerThread = (int) Math.ceil(dataSample.size()/engines.length);
		for (int i = 0; i < engines.length && startInd < dataSample.size(); i++) {
			final int finalStartInd = startInd;
			final TunableEngine e = engines[i];
			if (!e.isInit()) {
				try {
					e.init();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			e.getParameters().set(features, TYPE);
			e.notifyParametersChanged();
			futures.add(pool.submit(() -> {
				try {
					double subTotalError = 0;
					int endInd = Math.min(dataSample.size(), finalStartInd + workLoadPerThread);
					for (int j = finalStartInd; j < endInd; j++) {
						Entry<String,Float> dataPair = dataSample.get(j);
						String fen = dataPair.getKey();
						float result = dataPair.getValue();
						e.setPosition(fen);
						SearchResults res = e.search(null, null, null, null, null, null, null, 0, null, null, null, null);
						double score = res.getScore().get();
						// Check if it's white's turn.
						if (!fen.contains("w"))
							score *= -1;
						double error = computeError(result, score, k);
						subTotalError += error;
					}
					return subTotalError;
				} catch (Exception e1) {
					e1.printStackTrace();
					return 0d;
				}
			}));
			startInd += workLoadPerThread;
		}
		for (Future<Double> f : futures) {
			try {
				totalError += f.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		return totalError/dataSample.size();
	}
	/**
	 * Returns the number of data rows in the labelled data set.
	 * 
	 * @return The number of data rows contained in the file.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IOException If an iO error occurs.
	 */
	private int countDataSetSize() throws FileNotFoundException, IOException {
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(fenFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!"".equals(line))
					count++;
			}
		}
		return count;
	}
	/**
	 * Caches the data into a list of key-pair values where the key is FEN position and the value is the label denoting which side 
	 * won the game in which the position occurred.
	 * 
	 * @param fromInd The line number from which on the lines will be loaded into the data set.
	 * @param toInd The line number up to which (exclusive) the lines will be loaded into the data set.
	 * @return The data held in the lines between the specified indices.
	 * @throws IOException If there is an IO error reading the file.
	 */
	private List<Entry<String,Float>> cacheData(int fromInd, int toInd) throws IOException {
		List<Entry<String,Float>> data = new ArrayList<>();
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(fenFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (count >= fromInd && count < toInd) {
					String[] parts = line.split(";");
					String fen = parts[0];
					Float result = Float.parseFloat(parts[1]);
					data.add(new SimpleEntry<String,Float>(fen, result));
				}
				count++;
			}
		}
		return data;
	}
	@Override
	protected List<Entry<String,Float>> getTestData() {
		return testData;
	}
	@Override
	protected List<Entry<String,Float>> sampleTrainingData() {
		int[] lines = new int[sampleSize];
		for (int i = 0; i < sampleSize; i++)
			lines[i] = (int) (rand.nextDouble()*(1 - TEST_DATA_PROPORTION)*dataSetSize);
		Arrays.sort(lines);
		List<Entry<String,Float>> sample = new ArrayList<>();
		int i = 0;
		int count = 0;
		long nextLine = lines[0];
		try (BufferedReader reader = new BufferedReader(new FileReader(fenFilePath))) {
			String line;
			while ((line = reader.readLine()) != null && i < sampleSize) {
				line = line.trim();
				if (count == nextLine) {
					String[] parts = line.split(";");
					String fen = parts[0];
					Float result = Float.parseFloat(parts[1]);
					sample.add(new SimpleEntry<String,Float>(fen, result));
					nextLine = lines[i++];
				} else
					count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sample;
	}
	@Override
	protected double costFunction(double[] features, List<Entry<String,Float>> dataSample) {
		return computeAverageError(features, k, dataSample);
	}
	@Override
	public void close() throws Exception {
		pool.shutdown();
		for (TunableEngine e : engines)
			e.quit();
	}
	
}
