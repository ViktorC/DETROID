package main;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import engine.Detroid;
import tuning.EngineParameters;
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
	private static final String DEF_LOG_FILE_PATH = "log.txt";
	/**
	 * The default FENs file path for static evaluation tuning.
	 */
	private static final String DEF_FENS_FILE_PATH = "fens.txt";
	/**
	 * The default path to the file to which the parameters converted from binary strings or double arrays are written.
	 */
	private static final String DEF_CONVERTED_PARAMS_PATH = "params.txt";
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
				// Tuning.
				case "-t": {
					String logFilePath = DEF_LOG_FILE_PATH;
					String arg1 = args[1];
					if ("gameplay".equals(arg1)) {
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
								} break;
								default:
									throw new IllegalArgumentException();
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
						try (GamePlayOptimizer optimizer = new GamePlayOptimizer(engines, games, tc, tcInc, initProbVec, popSize, logger)) {
							optimizer.optimize();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if ("evaluation".equals(arg1)) {
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
								} break;
								default:
									throw new IllegalArgumentException();
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
						try (StaticEvaluationOptimizer optimizer = new StaticEvaluationOptimizer(engines, sampleSize, fensFilePath, k, logger)) {
							optimizer.train();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else
						throw new IllegalArgumentException();
				} break;
				// Generate FENs file.
				case "-g": {
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
							} break;
							default:
								throw new IllegalArgumentException();
						}
					}
					OptimizerEngines[] engines = new OptimizerEngines[concurrency];
					for (int i = 0; i < concurrency; i++) {
						engines[i] = new OptimizerEngines(new Detroid(), new Detroid(), new Detroid());
					}
					try {
						StaticEvaluationOptimizer.generateFENFile(engines, games, tc, tcInc, destFile);
					} catch (NullPointerException | IllegalArgumentException | IOException e) {
						e.printStackTrace();
					}
				} break;
				// Filter FENs file.
				case "-f": {
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
								} break;
								default:
									throw new IllegalArgumentException();
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
								} break;
								default:
									throw new IllegalArgumentException();
							}
						}
						try {
							StaticEvaluationOptimizer.filterOpeningPositions(sourceFile, destFile, numOfPositionsToFilter);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else
						throw new IllegalArgumentException();
				} break;
				// Convert to parameters.
				case "-c": {
					String arg1 = args[1];
					String destFile = DEF_CONVERTED_PARAMS_PATH;
					TunableEngine engine = new Detroid();
					engine.init();
					EngineParameters params = engine.getParameters();
					if ("-binarystring".equals(arg1)) {
						String binaryString = args[2];
						params.set(binaryString);
					} else if ("-probvector".equals(arg1)) {
						String binaryString = "";
						String vec = args[2];
						String[] probs = vec.split(",");
						for (int j = 0; j < probs.length; j++) {
							double prob = Double.parseDouble(probs[j].trim());
							binaryString += (prob >= 0.5 ? "1" : "0");
						}
						params.set(binaryString);
					} else if ("-decimalarray".equals(arg1)) {
						String val = args[2];
						String[] array = val.split(",");
						double[] decimalArray = new double[array.length];
						for (int j = 0; j < array.length; j++)
							decimalArray[j] = Double.parseDouble(array[j].trim());
						params.set(decimalArray);
					} else
						throw new IllegalArgumentException();
					if (args.length > 3) {
						if (!"-paramsfile".equals(args[3]))
							throw new IllegalArgumentException();
						destFile = args[4];
					}
					System.out.println(params.toString());
					params.writeToFile(destFile);
				} break;
				// UCI mode.
				case "-u": {
					try (UCI uci = new UCI(System.in, System.out)) {
						uci.run(new Detroid());
					} catch (IOException e) {
						e.printStackTrace();
					}
				} break;
				default:
					throw new IllegalArgumentException();
			}
		} else {
			// !TODO GUI mode.
		}
	}

}
