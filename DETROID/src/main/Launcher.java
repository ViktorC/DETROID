package main;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import engine.Detroid;
import tuning.GamePlayOptimizer;
import tuning.OptimizerEngines;
import tuning.StaticEvaluationOptimizer;
import tuning.TunableEngine;
import uci.UCI;

/**
 * A launcher for the engine and tuning framework.
 * 
 * @author Viktor
 *
 */
public class Launcher {

	/**
	 * The default number of threads to use.
	 */
	private static final int DEF_CONCURRENCY = 1;
	/**
	 * The default log file path.
	 */
	private static final String DEF_LOG_FILE_PATH = "log";
	/**
	 * The default FENs file path for static evaluation tuning.
	 */
	private static final String DEF_FENS_FILE_PATH = "fens";
	/**
	 * The default number of games to play for engine and search control parameter tuning.
	 */
	private static final int DEF_GAMES = 100;
	/**
	 * The default base length of games to play for engine and search control parameter tuning and FENs file generation in milliseconds.
	 */
	private static final long DEF_TC = 2500;
	/**
	 * The default time increment per move for games to play for engine and search control parameter tuning and FENs file generation in 
	 * milliseconds.
	 */
	private static final long DEF_TC_INC = 0;
	/**
	 * The default population size for the genotypes in the PBIL algorithm behind the engine and search control parameter optimizer.
	 */
	private static final int DEF_POPULATION_SIZE = 100;
	
	/**
	 * !TODO Documentation.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args != null && args.length > 0) {
			int concurrency = DEF_CONCURRENCY;
			String arg0 = args[0];
			switch (arg0) {
				case "-tune": {
					String logFilePath = DEF_LOG_FILE_PATH;
					String arg1 = args[1];
					if ("search".equals(arg1)) {
						int games = DEF_GAMES;
						long tc = DEF_TC;
						long tcInc = DEF_TC_INC;
						int popSize = DEF_POPULATION_SIZE;
						double[] initProbVec = null;
						for (int i = 2; i < args.length; i++) {
							String arg = args[i];
							switch (arg) {
								case "-log": {
									logFilePath = args[++i];
								} break;
								case "-concurrency": {
									concurrency = Integer.parseInt(args[++i]);
								} break;
								case "-games": {
									games = Integer.parseInt(args[++i]);
								} break;
								case "-population": {
									popSize = Integer.parseInt(args[++i]);
								} break;
								case "-tc": {
									tc = Long.parseLong(args[++i]);
								} break;
								case "-inc": {
									tcInc = Long.parseLong(args[++i]);
								} break;
								case "-initprobvec": {
									String vec = args[++i];
									String[] probs = vec.split(",");
									initProbVec = new double[probs.length];
									for (int j = 0; j < probs.length; j++)
										initProbVec[j] = Double.parseDouble(probs[j].trim());
								}
							}
						}
						OptimizerEngines[] engines = new OptimizerEngines[concurrency];
						for (int i = 0; i < concurrency; i++)
							engines[i] = new OptimizerEngines(new Detroid(), new Detroid(), new Detroid());
						Logger logger = Logger.getAnonymousLogger();
						try {
							logger.addHandler(new FileHandler(logFilePath, true));
						} catch (SecurityException | IOException e) {
							e.printStackTrace();
						}
						GamePlayOptimizer optimizer = new GamePlayOptimizer(engines, games, tc, tcInc, initProbVec, popSize, logger);
						optimizer.optimize();
						try {
							optimizer.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if ("eval".equals(arg1)) {
						Double k = null;
						Integer sampleSize = null;
						String fensFilePath = DEF_FENS_FILE_PATH;
						for (int i = 2; i < args.length; i++) {
							String arg = args[i];
							switch (arg) {
								case "-log": {
									logFilePath = args[++i];
								} break;
								case "-concurrency": {
									concurrency = Integer.parseInt(args[++i]);
								} break;
								case "-k": {
									k = Double.parseDouble(args[++i]);
								} break;
								case "-samplesize": {
									sampleSize = Integer.parseInt(args[++i]);
								} break;
								case "-fensfile": {
									fensFilePath = args[++i];
								}
							}
						}
						TunableEngine[] engines = new TunableEngine[concurrency];
						for (int i = 0; i < concurrency; i++)
							engines[i] = new Detroid();
						engines[0].init();
						Logger logger = Logger.getAnonymousLogger();
						try {
							logger.addHandler(new FileHandler(logFilePath, true));
						} catch (SecurityException | IOException e) {
							e.printStackTrace();
						}
						StaticEvaluationOptimizer optimizer;
						try {
							optimizer = new StaticEvaluationOptimizer(engines, sampleSize, fensFilePath, k, logger);
							optimizer.train();
							try {
								optimizer.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} catch (NullPointerException | IllegalArgumentException | IOException e) {
							e.printStackTrace();
						}
					}
				} break;
				case "-genfens": {
					int games = DEF_GAMES;
					long tc = DEF_TC;
					long tcInc = DEF_TC_INC;
					String destFile = DEF_FENS_FILE_PATH;
					for (int i = 2; i < args.length; i++) {
						String arg = args[i];
						switch (arg) {
							case "-concurrency": {
								concurrency = Integer.parseInt(args[++i]);
							} break;
							case "-games": {
								games = Integer.parseInt(args[++i]);
							} break;
							case "-tc": {
								tc = Long.parseLong(args[++i]);
							} break;
							case "-inc": {
								tcInc = Long.parseLong(args[++i]);
							} break;
							case "-destfile": {
								destFile = args[++i];
							}
						}
					}
					OptimizerEngines[] engines = new OptimizerEngines[concurrency];
					for (int i = 0; i < concurrency; i++)
						engines[i] = new OptimizerEngines(new Detroid(), new Detroid(), new Detroid());
					try {
						StaticEvaluationOptimizer.generateFENFile(engines, games, tc, tcInc, destFile);
					} catch (NullPointerException | IllegalArgumentException | IOException e) {
						e.printStackTrace();
					}
				} break;
				case "-filterfens": {
					String arg1 = args[1];
					String sourceFile = null;
					String destFile = DEF_FENS_FILE_PATH;
					if ("draws".equals(arg1)) {
						for (int i = 2; i < args.length; i++) {
							String arg = args[i];
							switch (arg) {
								case "-sourcefile": {
									sourceFile = args[++i];
								} break;
								case "-destfile": {
									destFile = args[++i];
								}
							}
						}
						try {
							StaticEvaluationOptimizer.filterDraws(sourceFile, destFile);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if ("openings".equals(arg1)) {
						int numOfPositionsToFilter = 0;
						for (int i = 2; i < args.length; i++) {
							String arg = args[i];
							switch (arg) {
								case "-firstxmoves": {
									numOfPositionsToFilter = Integer.parseInt(args[++i]);
								} break;
								case "-sourcefile": {
									sourceFile = args[++i];
								} break;
								case "-destfile": {
									destFile = args[++i];
								}
							}
						}
						try {
							StaticEvaluationOptimizer.filterOpeningPositions(sourceFile, destFile, numOfPositionsToFilter);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} break;
				case "-gui": {
					
				}
			}
		} else {
			UCI uci = new UCI(System.in, System.out);
			uci.run(new Detroid());
			try {
				uci.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
