package tuning;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import uci.UCIEngine;
import util.PBIL;

/**
 * A class for optimizing chess engine parameters using a PBIL algorithm with a possibly parallel, game play based fitness function.
 * 
 * @author Viktor
 *
 */
public class GamePlayOptimizer extends PBIL implements AutoCloseable {

	/**
	 * The factor of the original number of games to play in addition when assessing the fitness of a parameter set whose fitness 
	 * surpassed the current highest fitness after having played the original number of games.
	 */
	private static final int VALIDATION_FACTOR = 1;
	
	private final OptimizerEngines[] engines;
	private final Arena[] arenas;
	private final int games;
	private final long timePerGame;
	private final long timeIncPerMove;
	private final ExecutorService pool;
	
	/**
	 * Constructs and returns a new EngineParameterOptimizer instance according to the specified parameters.
	 * 
	 * @param engines An array of {@link #OptimizerEngines OptimizerEngines} instances that
	 * each contain the engines needed for one optimization thread. For each non-null element
	 * in the array, a new thread will be utilized for the optimization. E.g. if engines is an
	 * array of four non-null elements, the games in the fitness function will be distributed
	 * and played parallel on four threads. The array's first element cannot be null or a 
	 * {@link #NullPointerException NullPointerException} is thrown.
	 * @param games The number of games to play to assess the fitness of the parameters.
	 * @param timePerGame The time each engine will have per game in milliseconds.
	 * @param timeIncPerMove The number of milliseconds with which the remaining time of an
	 * engine is incremented after each legal move.
	 * @param initialProbabilityVector The starting probability vector for the optimization.
	 * It allows the algorithm to pick up where a previous, terminated optimization process
	 * left off. If the array's length is smaller than the engine to be tuned's parameters' 
	 * binary string's length, it will be extended with elements of the value 0.5d; if the 
	 * length of the array is greater than engine to be tuned's parameters' binary string's 
	 * length, only the first x elements will be considered, where x equals the parameters' 
	 * binary string's length. If it is null, an array with a length equal to the parameters' 
	 * binary string's length, only containing elements that have the value 0.5d will be used.
	 * @param populationSize The number of samples to produce per generation.
	 * @param logger A logger to log the optimization process. If it is null, no logging 
	 * will be done.
	 * @throws Exception If the engines cannot be initialised.
	 */
	public GamePlayOptimizer(OptimizerEngines[] engines, int games, long timePerGame, long timeIncPerMove,
			double[] initialProbabilityVector, int populationSize, Logger logger)
					throws Exception {
		super(engines[0].getEngine().getParameters().toGrayCodeString().length(), populationSize, null, null, null, null, null,
				initialProbabilityVector, logger);
		ArrayList<OptimizerEngines> enginesList = new ArrayList<>();
		for (OptimizerEngines e : engines) {
			if (e != null)
				enginesList.add(e);
		}
		this.engines = enginesList.toArray(new OptimizerEngines[enginesList.size()]);
		arenas = new Arena[this.engines.length];
		for (int i = 0; i < this.engines.length; i++)
			arenas[i] = new Arena(this.engines[i].getController(), Logger.getAnonymousLogger());
		this.games = games;
		this.timePerGame = timePerGame;
		this.timeIncPerMove = timeIncPerMove;
		pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
	}
	/**
	 * Constructs and returns a new EngineParameterOptimizer instance according to the specified parameters.
	 * 
	 * @param engines An array of {@link #OptimizerEngines OptimizerEngines} instances that
	 * each contain the engines needed for one optimization thread. For each non-null element
	 * in the array, a new thread will be utilized for the optimization. E.g. if engines is an
	 * array of four non-null elements, the games in the fitness function will be distributed
	 * and played parallel on four threads. The array's first element cannot be null or a 
	 * {@link #NullPointerException NullPointerException} is thrown.
	 * @param games The number of games to play to assess the fitness of the parameters.
	 * @param timePerGame The time each engine will have per game in milliseconds.
	 * @param timeIncPerMove The number of milliseconds with which the remaining time of an
	 * engine is incremented after each legal move.
	 * @param populationSize The number of samples to produce per generation.
	 * @param logger A logger to log the optimization process. If it is null, no logging 
	 * will be done.
	 * @throws Exception If the engines cannot be initialised.
	 */
	public GamePlayOptimizer(OptimizerEngines[] engines, int games, long timePerGame, long timeIncPerMove, int populationSize,
			Logger logger) throws Exception {
		this(engines, games, timePerGame, timeIncPerMove, null, populationSize, logger);
	}
	@Override
	protected double fitnessFunction(String genotype) {
		int engine1Wins = 0;
		int engine2Wins = 0;
		int draws = 0;
		ArrayList<Future<MatchResult>> futures = new ArrayList<>(engines.length);
		for (int i = 0; i < engines.length; i++) {
			final int index = i;
			futures.add(pool.submit(() -> {
				TunableEngine tunEngine = engines[index].getEngine();
				UCIEngine oppEngine = engines[index].getOpponentEngine();
				if (!tunEngine.isInit())
					tunEngine.init();
				if (!oppEngine.isInit())
					oppEngine.init();
				tunEngine.getParameters().set(genotype);
				tunEngine.reloadParameters();
				return arenas[index].match(tunEngine, oppEngine, games/engines.length, timePerGame, timeIncPerMove);
			}));
		}
		for (Future<MatchResult> f : futures) {
			try {
				MatchResult res = f.get();
				engine1Wins += res.getEngine1Wins();
				engine2Wins += res.getEngine2Wins();
				draws += res.getDraws();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		double fitness = Elo.calculateDifference(engine1Wins, engine2Wins, draws);
		if (fitness > 0) {
			futures = new ArrayList<>(engines.length);
			for (int i = 0; i < engines.length; i++) {
				int index = i;
				futures.add(pool.submit(() -> arenas[index].match(engines[index].getEngine(), engines[index].getOpponentEngine(),
						VALIDATION_FACTOR*games/engines.length, timePerGame, timeIncPerMove)));
			}
			for (Future<MatchResult> f : futures) {
				try {
					MatchResult res = f.get();
					engine1Wins += res.getEngine1Wins();
					engine2Wins += res.getEngine2Wins();
					draws += res.getDraws();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			fitness = Elo.calculateDifference(engine1Wins, engine2Wins, draws);
			if (fitness > 0) {
				for (int i = 0; i < engines.length; i++) {
					TunableEngine oppEngine = engines[i].getOpponentEngine();
					try {
						oppEngine.init();
					} catch (Exception e) {
						e.printStackTrace();
						return -Double.MAX_VALUE;
					}
					oppEngine.getParameters().set(genotype);
					oppEngine.reloadParameters();
				}
			}
		}
		return getHighestFitness() > 0 ? getHighestFitness() + fitness : fitness;
	}
	@Override
	public void close() throws Exception {
		pool.shutdown();
		for (Arena a : arenas)
			a.close();
		for (OptimizerEngines e : engines) {
			e.getEngine().quit();
			e.getOpponentEngine().quit();
			e.getController().quit();
		}
	}

}
