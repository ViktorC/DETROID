package tuning;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import tuning.Arena.MatchResult;
import uci.UCIEngine;
import util.PBIL;

/**
 * A class for optimizing chess engine parameters using a PBIL algorithm with a possibly parallel, game play based fitness function.
 * 
 * @author Viktor
 *
 */
public class EngineParameterOptimizer extends PBIL implements AutoCloseable {

	private ExecutorService pool;
	private OptimizerEngines[] engines;
	private Arena[] arenas;
	private int games;
	private long timeControl;
	
	/**
	 * Constructs and returns a new EngineParameterOptimizer instance according to the specified parameters.
	 * 
	 * @param engines An array of {@link #OptimizerEngines OptimizerEngines} instances that
	 * each contain the engines needed for one optimization thread. For each non-null element
	 * in the array, a new thread will be utilized for the optimization. E.g. if engines is an
	 * array of four non-null elements, the games in the fitness function will be distributed
	 * and played parallel on four threads. The array must contain at least one non-null element
	 * or an {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param games The number of games to play to assess the fitness of the parameters.
	 * @param timeControl The time each engine will have per game in milliseconds.
	 * @param initialProbabilityVector The starting probability vector for the optimization.
	 * It allows the algorithm to pick up where a previous, terminated optimization process
	 * left off. If the array's length is smaller than the engine to be tuned's parameters' 
	 * binary string's length, it will be extended with elements of the value 0.5d; if the 
	 * length of the array is greater than engine to be tuned's parameters' binary string's 
	 * length, only the first x elements will be considered, where x equals the parameters' 
	 * binary string's length. If it is null, an array with a length equal to the parameters' 
	 * binary string's length, only containing elements that have the value 0.5d will be used.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param logger The logger to log the game results. If it is null, no logging will be done.
	 * @return
	 * @throws NullPointerException If the parameter engines is null.
	 * @throws IllegalArgumentException If the parameter engines doesn't contain at least one
	 * non-null element.
	 */
	public EngineParameterOptimizer(OptimizerEngines[] engines, int games, long timeControl, double[] initialProbabilityVector,
			int populationSize, int generations, Logger logger) throws NullPointerException, IllegalArgumentException {
		super(initialProbabilityVector, engines[0].getTunableEngine().getParameters().toGrayCodeString().length(), populationSize, generations);
		ArrayList<OptimizerEngines> enginesList = new ArrayList<>();
		for (OptimizerEngines e : engines) {
			if (e != null)
				enginesList.add(e);
		}
		if (enginesList.size() == 0)
			throw new IllegalArgumentException("The parameter engines must contain at least one non-null element.");
		this.engines = enginesList.toArray(new OptimizerEngines[enginesList.size()]);
		arenas = new Arena[this.engines.length];
		for (int i = 0; i < this.engines.length; i++)
			arenas[i] = new Arena(this.engines[i].getController(), logger);
		this.games = games;
		this.timeControl = timeControl;
		pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
	}
	/**
	 * Constructs and returns a new EngineParameterOptimizer instance according to the specified parameters.
	 * 
	 * @param engines An array of {@link #OptimizerEngines OptimizerEngines} instances that
	 * each contain the engines needed for one optimization thread. For each non-null element
	 * in the array, a new thread will be utilized for the optimization. E.g. if engines is an
	 * array of four non-null elements, the games in the fitness function will be distributed
	 * and played parallel on four threads. The array must contain at least one non-null element
	 * or an {@link #IllegalArgumentException IllegalArgumentException} is thrown.
	 * @param games The number of games to play to assess the fitness of the parameters.
	 * @param timeControl The time each engine will have per game in milliseconds.
	 * @param populationSize The number of samples to produce per generation.
	 * @param generations The number of iterations.
	 * @param logger The logger to log the game results. If it is null, no logging will be done.
	 * @return
	 * @throws NullPointerException If the parameter engines is null.
	 * @throws IllegalArgumentException If the parameter engines doesn't contain at least one
	 * non-null element.
	 */
	public EngineParameterOptimizer(OptimizerEngines[] engines, int games, long timeControl, int populationSize, int generations,
			Logger logger) throws NullPointerException, IllegalArgumentException {
		this(engines, games, timeControl, null, populationSize, generations, logger);
	}
	@Override
	protected double fitnessFunction(String genome) {
		double fitness = 0;
		ArrayList<Future<MatchResult>> futures = new ArrayList<>(engines.length);
		for (int i = 0; i < engines.length; i++) {
			final int index = i;
			futures.add(pool.submit(() -> {
				TunableEngine tunEngine = engines[index].getTunableEngine();
				UCIEngine oppEngine = engines[index].getOpponentEngine();
				if (!tunEngine.isInit())
					tunEngine.init();
				if (!oppEngine.isInit())
					oppEngine.init();
				tunEngine.getParameters().initFromGrayCodeString(genome);
				return arenas[index].match(tunEngine, oppEngine, games/engines.length, timeControl);
			}));
		}
		for (Future<MatchResult> f : futures) {
			try {
				MatchResult res = f.get();
				fitness += res.getEngine1Wins()*2 + res.getDraws();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		return fitness;
	}
	@Override
	public void close() throws Exception {
		pool.shutdown();
		for (Arena a : arenas)
			a.close();
		for (OptimizerEngines e : engines) {
			e.getTunableEngine().quit();
			e.getOpponentEngine().quit();
			e.getController().quit();
		}
	}

}
