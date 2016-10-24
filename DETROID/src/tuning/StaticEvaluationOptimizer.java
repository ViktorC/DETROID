package tuning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
	 * The base step size for the gradient descent.
	 */
	private static final double BASE_LEARNING_RATE = 500000000;
	
	private final TunableEngine[] engines;
	private final double k;
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
	 * @throws IllegalArgumentException If an IO or parsing error occurs.
	 * @throws NullPointerException If the parameter engines is null or its first element is
	 * null.
	 */
	public StaticEvaluationOptimizer(TunableEngine[] engines, int sampleSize, String fenFilePath, Double k, Logger logger)
			throws NullPointerException, IOException {
		super(engines[0].getParameters().toDoubleArray(), false, 1d, BASE_LEARNING_RATE, null, null, null, sampleSize, fenFilePath, logger);
		ArrayList<TunableEngine> enginesList = new ArrayList<>();
		for (TunableEngine e : engines) {
			if (e != null) {
				if (!e.isInit())
					e.init();
				enginesList.add(e);
			}
		}
		this.engines = enginesList.toArray(new TunableEngine[enginesList.size()]);
		pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
		if (k == null) {
			this.k = computeOptimalK();
			if (logger != null)
				logger.info("Optimal K: " + this.k + System.lineSeparator());
		} else
			this.k = k;
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
					a.match(e.getTunableEngine(), e.getOpponentEngine(), workLoad, timePerGame, timeIncPerMove);
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
						while ((line = reader.readLine()) != null)
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
	 * @param numOfPositionsToFilter The first x positions to filter from each game.
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
					int halfMoveInd = Integer.parseInt(match.substring(0, match.length() - 1));
					if (halfMoveInd > numOfPositionsToFilter)
						writer.write(line + System.lineSeparator());
				}
			}
		}
	}
	/**
	 * Computes the optimal scaling constant K for the sigmoid function used to calculate the evaluation error.
	 * 
	 * @return The value for K that locally minimizes the average error.
	 */
	private double computeOptimalK() {
		final double resolution = 0.001;
		double k = 0;
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
	private double computeAverageError(double[] parameters, final double k, ArrayList<Object> dataSample) {
		double totalError = 0;
		ArrayList<Future<Double>> futures = new ArrayList<>();
		int startInd = 0;
		int workLoadPerThread = dataSample.size()/engines.length;
		for (int i = 0; i < engines.length; i++) {
			final int finalStartInd = startInd;
			final boolean last = i == engines.length - 1;
			final TunableEngine e = engines[i];
			if (!e.isInit())
				e.init();
			e.getParameters().set(parameters);
			e.reloadParameters();
			futures.add(pool.submit(() -> {
				double subTotalError = 0;
				int endInd = (int) (last ? dataSample.size() : finalStartInd + workLoadPerThread);
				for (int j = finalStartInd; j < endInd; j++) {
					String[] parts = ((String) dataSample.get(j)).split(";");
					String fen = parts[0];
					double result = Double.parseDouble(parts[1]);
					e.position(fen);
					e.search(null, null, null, null, null, null, null, 0, null, null, null, null);
					double score = e.getSearchInfo().getScore();
					double error = computeError(result, score, k);
					subTotalError += error;
				}
				return subTotalError;
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
	@Override
	protected ArrayList<Object> cacheTrainingData(String filePath) throws FileNotFoundException, IOException {
		ArrayList<Object> dataSet = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!"".equals(line))
					dataSet.add(line);
			}
		}
		return dataSet;
	}
	@Override
	protected double costFunction(double[] features, ArrayList<Object> dataSample) {
		return computeAverageError(features, k, dataSample);
	}
	@Override
	public void close() throws Exception {
		pool.shutdown();
		for (TunableEngine e : engines)
			e.quit();
	}
	
}
