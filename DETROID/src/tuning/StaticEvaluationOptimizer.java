package tuning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
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

import util.ASGD;

/**
 * A class for optimizing chess engine evaluation parameters using a stochastic gradient descent algorithm 
 * with a possibly parallel cost function. The cost function is defined by "Texel's Tuning Method" which is 
 * the average error of the evaluation scores of a set of positions exported from games with known outcomes.
 * 
 * @author Viktor
 *
 */
public class StaticEvaluationOptimizer extends ASGD implements AutoCloseable {

	/**
	 * The fraction of the complete data set used as a test data set.
	 */
	private static final double TEST_DATA_PROPORTION = 0.2d;
	/**
	 * The base step size for the gradient descent.
	 */
	private static final double BASE_LEARNING_RATE = 2;
	
	private final TunableEngine[] engines;
	private final double k;
	private final int sampleSize;
	private final int dataSetSize;
	private final String fenFilePath;
	private final List<Entry<Object, Object>> testData;
	private final Random rand;
	private final ExecutorService pool;
	
	/**
	 * Constructs and returns a new StaticEvaluationOptimizer instance according to the specified 
	 * parameters.
	 * 
	 * @param engines An array of {@link #TunableEngine TunableEngine} instances (of which 
	 * the parameters' gray code string should have the same length). For each non-null element
	 * in the array, a new thread will be utilized for the optimization. E.g. if engines is an
	 * array of four non-null elements, the fitness function will be distributed and executed 
	 * parallel on four threads. If the array or its first element are null or the method {@link 
	 * #uci.UCIEngine.init() init} hasn't been called on the first element, a {@link 
	 * #NullPointerException NullPointerException} is thrown.
	 * @param sampleSize The number of positions to include in one mini-batch. The higher this number
	 * is, the slower but more stable the convergence will be.
	 * @param fenFilePath The path to the file containing the FEN list of positions to evaluate.
	 * If it doesn't exist an {@link #IOException IOException} is thrown.
	 * @param k A scaling constant for the sigmoid function used calculate the average error.
	 * @param logger A logger to log the status of the optimization. If it is null, no logging is performed.
	 * @throws Exception If the engines cannot be initialised.
	 */
	public StaticEvaluationOptimizer(TunableEngine[] engines, int sampleSize, String fenFilePath, Double k, Logger logger)
			throws Exception {
		super(engines[0].getParameters().values(), (double[]) Array.newInstance(double.class, engines[0].getParameters().values().length),
				engines[0].getParameters().maxValues(), 1d, BASE_LEARNING_RATE, null, null, null, null, null, logger);
		if (sampleSize < 1)
			throw new IllegalArgumentException("The sample size has to be greater than 0.");
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
				e.setStaticEvalTuningMode(true);
				enginesList.add(e);
			}
		}
		this.engines = enginesList.toArray(new TunableEngine[enginesList.size()]);
		rand = new Random(System.nanoTime());
		pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
		if (k == null) {
			this.k = computeOptimalK();
			if (logger != null)
				logger.info("Optimal K: " + this.k + System.lineSeparator());
		} else
			this.k = k;
		testData = cacheData((int) (dataSetSize*(1 - TEST_DATA_PROPORTION)), dataSetSize);
	}
	/**
	 * Generates a file of lines of FEN strings with the results of the games the positions occurred in 
	 * appended to them. This file can then be used for the optimization of engine parameters.
	 * 
	 * @param engines An array of {@link #OptimizerEngines OptimizerEngines} instances that
	 * each contain the engines needed for playing games in the {@link #Arena Arena}. For each 
	 * non-null element in the array, a new thread will be utilized for the optimization. E.g. 
	 * if engines is an array of four non-null elements, the games in the fitness function will 
	 * be distributed and played parallel on four threads. The array's first element cannot be 
	 * null or a {@link #NullPointerException NullPointerException} is thrown.
	 * @param games The number of games to play.
	 * @param timePerGame The time each engine will have per game in milliseconds.
	 * @param timeIncPerMove The number of milliseconds with which the remaining time of an
	 * engine is incremented after each legal move.
	 * be done.
	 * @throws IOException If the file specified by filePath doesn't exist and cannot be created.
	 * @throws NullPointerException If the parameter engines is null.
	 * @throws IllegalArgumentException If engines doesn't contain at least one non-null element.
	 */
	public static void generateFENFile(OptimizerEngines[] engines, int games, long timePerGame, long timeIncPerMove,
			String filePath) throws IOException, NullPointerException, IllegalArgumentException {
		File destinationFile = new File(filePath);
		if (!destinationFile.exists())
			Files.createFile(destinationFile.toPath());
		ArrayList<OptimizerEngines> enginesList = new ArrayList<>();
		for (OptimizerEngines e : engines) {
			if (e != null)
				enginesList.add(e);
		}
		if (enginesList.size() == 0)
			throw new IllegalArgumentException("The parameter engines has to contain at least 1 non-null element.");
		ExecutorService pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), enginesList.size()));
		try {
			ArrayList<Future<Void>> futures = new ArrayList<>();
			int workLoad = games/enginesList.size();
			for (OptimizerEngines e : engines) {
				futures.add(pool.submit(() -> {
					Logger fenLogger = Logger.getAnonymousLogger();
					fenLogger.setUseParentHandlers(false);
					FileHandler handler = null;
					try {
						handler = new FileHandler(filePath, true);
					} catch (SecurityException | IOException e1) { }
					handler.setFormatter(new Formatter() {
						
						@Override
						public String format(LogRecord record) {
							return record.getMessage() + System.lineSeparator();
						}
					});
					fenLogger.addHandler(handler);
					Arena a = new Arena(e.getController(), null, fenLogger);
					a.match(e.getEngine(), e.getOpponentEngine(), workLoad, timePerGame, timeIncPerMove);
					a.close();
					handler.flush();
					handler.close();
					return null;
				}));
			}
			for (Future<?> f : futures) {
				try {
					f.get();
				} catch (InterruptedException | ExecutionException e1) {
					e1.printStackTrace();
				}
			}
		} finally {
			pool.shutdown();
			for (int i = 1; i < enginesList.size(); i++) {
				String fileName = filePath + "." + i;
				File extraFile = new File(fileName);
				if (extraFile.exists()) {
					try (BufferedReader reader = new BufferedReader(new FileReader(fileName));
							BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
						String line;
						while ((line = reader.readLine()) != null && !"".equals(line))
							writer.write(line + System.lineSeparator());
					}
					Files.delete(extraFile.toPath());
				}
			}
		}
	}
	/**
	 * Copies all the lines from the source FEN file to the destination file except for the ones representing positions no 
	 * more than numOfPositionsToFilter full moves into the game.
	 * 
	 * @param sourceFenFile The file path to the source FEN file.
	 * @param destinationFenFile The path to the destination file. If it doesn't exist it will be created.
	 * @param numOfPositionsToFilter The first x full moves to filter from each game.
	 * @throws IOException
	 */
	public static void filterOpeningPositions(String sourceFenFile, String destinationFenFile, int numOfPositionsToFilter)
			throws IOException {
		File destinationFile = new File(sourceFenFile);
		if (!destinationFile.exists())
			Files.createFile(destinationFile.toPath());
		Pattern halfMovePattern = Pattern.compile("[0-9]+;");
		try (BufferedReader reader = new BufferedReader(new FileReader(sourceFenFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFenFile, true))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = halfMovePattern.matcher(line);
				if (matcher.find()) {
					String match = matcher.group();
					int fullMoveInd = Integer.parseInt(match.substring(0, match.length() - 1));
					if (fullMoveInd > numOfPositionsToFilter)
						writer.write(line + System.lineSeparator());
				}
			}
		}
	}
	/**
	 * Copies all the lines from the source FEN file to the destination file except for the ones representing positions from 
	 * drawn games.
	 * 
	 * @param sourceFenFile The file path to the source FEN file.
	 * @param destinationFenFile The path to the destination file. If it doesn't exist it will be created.
	 * @throws IOException
	 */
	public static void filterDraws(String sourceFenFile, String destinationFenFile)
			throws IOException {
		File destinationFile = new File(sourceFenFile);
		if (!destinationFile.exists())
			Files.createFile(destinationFile.toPath());
		try (BufferedReader reader = new BufferedReader(new FileReader(sourceFenFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFenFile, true))) {
			String line;
			while ((line = reader.readLine()) != null) {
				double result = Double.parseDouble(line.split(";")[1]);
				if (result != 0.5)
					writer.write(line + System.lineSeparator());
			}
		}
	}
	/**
	 * Computes the optimal scaling constant K for the sigmoid function used to calculate the evaluation error.
	 * 
	 * @return The value for K that locally minimizes the average error.
	 * @throws IOException If the training data cannot be loaded from the FEN file.
	 */
	private double computeOptimalK() throws IOException {
		final double resolution = 0.01;
		double k = 0;
		List<Entry<Object, Object>> trainingData = cacheData(0, (int) (dataSetSize*(1 - TEST_DATA_PROPORTION)));
		double kPlusErr = computeAverageError(features, k + resolution, trainingData);
		double kMinusErr = computeAverageError(features, k - resolution, trainingData);
		double minAvgErr = Math.min(kPlusErr, kMinusErr);
		double startingValue = computeAverageError(features, k, trainingData);
		if (logger != null)
			logger.info("K: " + k + "; Avg. error: " + startingValue + System.lineSeparator());
		if (startingValue < minAvgErr)
			return k;
		boolean increase = kPlusErr < kMinusErr;
		if (increase) {
			k += resolution;
			if (logger != null)
				logger.info("K: " + k + "; Avg. error: " + kPlusErr + System.lineSeparator());
		} else {
			k -= resolution;
			if (logger != null)
				logger.info("K: " + k + "; Avg. error: " + kMinusErr + System.lineSeparator());
		}
		double lastK = k;
		while (true) {
			k = increase? k + resolution : k - resolution;
			double avgError = computeAverageError(features, k, trainingData);
			if (logger != null)
				logger.info("K: " + k + "; Avg. error: " + avgError + System.lineSeparator());
			if (avgError > minAvgErr)
				break;
			minAvgErr = Math.min(avgError, minAvgErr);
			lastK = k;
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
	 * @param parameters The engine parameters.
	 * @param k The scaling constant K for the sigmoid function.
	 * @return
	 */
	private double computeAverageError(double[] parameters, final double k, List<Entry<Object, Object>> dataSample) {
		double totalError = 0;
		ArrayList<Future<Double>> futures = new ArrayList<>();
		int startInd = 0;
		int workLoadPerThread = dataSample.size()/engines.length;
		int enginesLength = workLoadPerThread < 0 ? 1 : engines.length;
		for (int i = 0; i < enginesLength; i++) {
			final int finalStartInd = startInd;
			final boolean last = i == enginesLength - 1;
			final TunableEngine e = engines[i];
			if (!e.isInit()) {
				try {
					e.init();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			e.getParameters().set(parameters);
			e.refresh();
			futures.add(pool.submit(() -> {
				try {
					double subTotalError = 0;
					int endInd = (int) (last ? dataSample.size() : finalStartInd + workLoadPerThread);
					for (int j = finalStartInd; j < endInd; j++) {
						Entry<Object, Object> dataPair = dataSample.get(j);
						String fen = (String) dataPair.getKey();
						double result = (double) ((Float) dataPair.getValue()).floatValue();
						e.position(fen);
						e.search(null, null, null, null, null, null, null, 0, null, null, null, null);
						double score = e.getSearchInfo().getScore();
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
	private List<Entry<Object, Object>> cacheData(int fromInd, int toInd) throws IOException {
		List<Entry<Object, Object>> data = new ArrayList<>();
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(fenFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (count >= fromInd && count < toInd) {
					String[] parts = line.split(";");
					String fen = parts[0];
					Float result = Float.parseFloat(parts[1]);
					data.add(new SimpleEntry<Object, Object>(fen, result));
				}
				count++;
			}
		}
		return data;
	}
	@Override
	protected List<Entry<Object, Object>> getTestData() {
		return testData;
	}
	@Override
	protected List<Entry<Object, Object>> sampleTrainingData() {
		int[] lines = new int[sampleSize];
		for (int i = 0; i < sampleSize; i++)
			lines[i] = (int) (rand.nextDouble()*(1 - TEST_DATA_PROPORTION)*dataSetSize);
		Arrays.sort(lines);
		List<Entry<Object, Object>> sample = new ArrayList<>();
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
					sample.add(new SimpleEntry<Object, Object>(fen, result));
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
	protected double costFunction(double[] features, List<Entry<Object, Object>> dataSample) {
		return computeAverageError(features, k, dataSample);
	}
	@Override
	public void close() throws Exception {
		pool.shutdown();
		for (TunableEngine e : engines)
			e.quit();
	}
	
}
