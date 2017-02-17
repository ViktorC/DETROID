package net.viktorc.detroid.framework;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javafx.application.Application;
import net.viktorc.detroid.framework.gui.GUI;
import net.viktorc.detroid.framework.tuning.EngineParameters;
import net.viktorc.detroid.framework.tuning.FENFileUtil;
import net.viktorc.detroid.framework.tuning.GamePlayOptimizer;
import net.viktorc.detroid.framework.tuning.OptimizerEngines;
import net.viktorc.detroid.framework.tuning.ParameterType;
import net.viktorc.detroid.framework.tuning.StaticEvaluationOptimizer;
import net.viktorc.detroid.framework.tuning.TunableEngine;
import net.viktorc.detroid.framework.uci.UCI;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * The application that serves as a chess engine framework handling communication via the UCI protocol, providing 
 * a GUI, and offering flexible engine parameter tuning methods using machine learning.
 * 
 * The application framework offers six main functionalities: GUI mode, UCI mode, tuning, FEN-file generation, 
 * FEN-file filtering, and parameter conversion. The default launch mode is the GUI mode which provides an interface 
 * that allows for playing chess games against an engine and track its search statistics. The UCI mode implements the 
 * Universal Chess Interface protocol as described at 
 * <a href="http://wbec-ridderkerk.nl/html/UCIProtocol.html">http://wbec-ridderkerk.nl/html/UCIProtocol.html</a> by 
 * Stefan-Meyer Kahlen. Two different tuning methods are supported; an evolutionary algorithm for optimizing all or only 
 * a certain type of engine parameters using self-play to assess the fitness of the different parameter sets generated, 
 * and an adaptive stochastic gradient descent algorithm for training the engine by optimizing the static evaluation 
 * parameters using the "Texel" cost function (
 * <a href="https://chessprogramming.wikispaces.com/Texel's+Tuning+Method">https://chessprogramming.wikispaces.com/Texel's+Tuning+Method</a>)
 * based on so called FEN-files which contain position descriptions in Forsyth-Edwards notation, each labelled by the 
 * side that won the game in which the position occurred. The FEN-file generation mode provides the functionalities 
 * needed to generate the files used for static evaluation tuning. They can be generated either by self-play or by 
 * providing a Portable Game Notation file to convert. The FEN-file filtering mode allows for removing draws or opening 
 * positions from the FEN-file which can, in certain cases, possibly improve the tuning results. Last but not least, the 
 * conversion mode allows for converting the numbers logged by the tuning methods into XML files that the engine can read 
 * its parameters' values from. The game play optimization algorithm logs the probability vector, while the static 
 * evaluation tuning method logs the optimal values of the parameter fields.
 * 
 * @author Viktor
 *
 */
public final class ApplicationFramework implements Runnable {

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
	private static final String DEF_CONVERTED_PARAMS_PATH = "params.xml";
	
	private EngineFactory factory;
	private String[] args;
	
	/**
	 * Constructs an instance of the application framework that is available for running with the specified arguments 
	 * using the engine instances built by the provided engine factory.
	 * 
	 * If there are no program arguments, the application is started in GUI mode.
	 * 
	 * @param factory An instance of a class extending the {@link #EngineFactory EngineFactory} interface. It provides the 
	 * engine instances required for different features of the framework.
	 * @param args The program arguments. If it is null or empty, the engine is started in GUI mode; else: <br>
	 * UCI mode: {@code -u} </br>
	 * Gameplay tuning: {@code -t gameplay -population <integer> -games <integer> -tc <integer> [-paramtype <eval | control | all> 
	 * -inc <integer> -validfactor <decimal> -initprobvector <quoted_comma_separated_decimals> -log <string> 
	 * -concurrency <integer>]} <br>
	 * Static evaluation tuning: {@code -t staticeval -samplesize <integer> [-k <decimal> -fensfile <string> -log <string> 
	 * -concurrency <integer>]} <br>
	 * FEN-file generation by self-play: {@code -g byselfplay -games <integer> -tc <integer> [-inc <integer> -destfile <string> 
	 * -concurrency <integer>]} <br>
	 * FEN-file generation by PGN conversion: {@code -g bypgnconversion -sourcefile <string> [-maxgames <integer> 
	 * -destfile <string>]} <br>
	 * Removing draws from a FEN-file: {@code -f draws -sourcefile <string> [-destfile <string>]} <br>
	 * Removing openings from a FEN-file: {@code -f openings -sourcefile <string> -firstxmoves <integer> [-destfile <string>]} <br>
	 * Probability vector conversion to parameters file: {@code -c probvector -value <quoted_comma_separated_decimals> 
	 * [-paramtype <eval | control | all> -paramsfile <string>]} <br>
	 * Parameter value array conversion to parameters file: {@code -c paramvalues -value <quoted_comma_separated_decimals> 
	 * [-paramsfile <string>]}
	 */
	public ApplicationFramework(EngineFactory factory, String[] args) {
		this.factory = factory;
		this.args = Arrays.copyOf(args, args.length);
	}
	@Override
	public synchronized void run() {
		if (args != null && args.length > 0) {
			int concurrency = DEF_CONCURRENCY;
			String arg0 = args[0];
			switch (arg0) {
				// UCI mode.
				case "-u": {
					try (UCI uci = new UCI(factory.newEngineInstance(), System.in, System.out)) {
						uci.run();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} break;
				// Tuning.
				case "-t": {
					String logFilePath = DEF_LOG_FILE_PATH;
					String arg1 = args[1];
					if ("gameplay".equals(arg1)) {
						int popSize = -1;
						int games = -1;
						long tc = -1;
						long tcInc = 0;
						double validFactor = 0;
						ParameterType paramType = null;
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
								case "-paramtype": {
									String type = args[++i];
									switch (type) {
										case "eval":
											paramType = ParameterType.STATIC_EVALUATION_PARAMETER;
											break;
										case "control":
											paramType = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER;
											break;
										case "all":
											paramType = null;
											break;
										default:
											throw new IllegalArgumentException();
									}
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
								case "-validfactor": {
									validFactor = Double.parseDouble(args[++i]);
								} break;
								case "-initprobvector": {
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
						if (games == -1 || tc == -1 || popSize == -1)
							throw new IllegalArgumentException();
						OptimizerEngines[] engines = new OptimizerEngines[concurrency];
						for (int i = 0; i < concurrency; i++) {
							try {
								engines[i] = new OptimizerEngines(factory.newEngineInstance(),
										factory.newEngineInstance(), factory.newControllerEngineInstance());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						Logger logger = Logger.getAnonymousLogger();
						try {
							logger.addHandler(new FileHandler(logFilePath, true));
						} catch (SecurityException | IOException e) {
							e.printStackTrace();
						}
						try (GamePlayOptimizer optimizer = new GamePlayOptimizer(engines, paramType, games, tc, tcInc, validFactor,
								initProbVec, popSize, logger)) {
							optimizer.optimize();
						} catch (Exception e) {
							throw new IllegalArgumentException(e);
						}
					} else if ("staticeval".equals(arg1)) {
						Double k = null;
						int sampleSize = -1;
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
						if (sampleSize == -1)
							throw new IllegalArgumentException();
						TunableEngine[] engines = new TunableEngine[concurrency];
						for (int i = 0; i < concurrency; i++)
							engines[i] = factory.newEngineInstance();
						try {
							engines[0].init();
						} catch (Exception e) {
							e.printStackTrace();
						}
						Logger logger = Logger.getAnonymousLogger();
						try {
							logger.addHandler(new FileHandler(logFilePath, true));
						} catch (SecurityException | IOException e) {
							e.printStackTrace();
							throw new IllegalArgumentException(e);
						}
						try (StaticEvaluationOptimizer optimizer = new StaticEvaluationOptimizer(engines, sampleSize, fensFilePath,
								k, logger)) {
							optimizer.train();
						} catch (Exception e) {
							e.printStackTrace();
							throw new IllegalArgumentException(e);
						}
					} else
						throw new IllegalArgumentException();
				} break;
				// Generate FENs file.
				case "-g": {
					String destFile = DEF_FENS_FILE_PATH;
					String arg1 = args[1];
					if ("byselfplay".equals(arg1)) {
						int games = -1;
						long tc = -1;
						long tcInc = 0;
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
						if (games == -1 || tc == -1)
							throw new IllegalArgumentException();
						OptimizerEngines[] engines = new OptimizerEngines[concurrency];
						for (int i = 0; i < concurrency; i++) {
							try {
								engines[i] = new OptimizerEngines(factory.newEngineInstance(),
										factory.newEngineInstance(), factory.newControllerEngineInstance());
							} catch (Exception e) {
								throw new IllegalArgumentException(e);
							}
						}
						try {
							FENFileUtil.generateFENFile(engines, games, tc, tcInc, destFile);
							for (OptimizerEngines e : engines) {
								e.getEngine().quit();
								e.getOpponentEngine().quit();
								e.getController().quit();
							}
						} catch (NullPointerException | IOException e) {
							throw new IllegalArgumentException(e);
						}
					} else if ("bypgnconversion".equals(arg1)) {
						String sourceFile = null;
						int maxNumOfGames = Integer.MAX_VALUE;
						for (int i = 2; i < args.length; i++) {
							String arg = args[i];
							switch (arg) {
								case "-maxgames": {
									maxNumOfGames = Integer.parseInt(args[++i]);
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
							ControllerEngine engine = factory.newControllerEngineInstance();
							FENFileUtil.generateFENFile(engine, sourceFile, destFile, maxNumOfGames);
							engine.quit();
						} catch (Exception e) {
							throw new IllegalArgumentException(e);
						}
					} else
						throw new IllegalArgumentException();
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
						if (sourceFile == null)
							throw new IllegalArgumentException();
						try {
							FENFileUtil.filterDraws(sourceFile, destFile);
						} catch (IOException e) {
							throw new IllegalArgumentException(e);
						}
					} else if ("openings".equals(arg1)) {
						int numOfPositionsToFilter = -1;
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
						if (sourceFile == null || numOfPositionsToFilter <= 0)
							throw new IllegalArgumentException();
						try {
							FENFileUtil.filterOpeningPositions(sourceFile, destFile, numOfPositionsToFilter);
						} catch (IOException e) {
							throw new IllegalArgumentException(e);
						}
					} else
						throw new IllegalArgumentException();
				} break;
				// Convert to parameters.
				case "-c": {
					String arg1 = args[1];
					String destFile = DEF_CONVERTED_PARAMS_PATH;
					TunableEngine engine = factory.newEngineInstance();
					try {
						engine.init();
					} catch (Exception e) {
						e.printStackTrace();
					}
					EngineParameters params = engine.getParameters();
					if (!"-value".equals(args[2]))
						throw new IllegalArgumentException();
					if ("probvector".equals(arg1)) {
						String binaryString = "";
						String vec = args[3];
						ParameterType paramType = null;
						String[] probs = vec.split(",");
						for (int j = 0; j < probs.length; j++) {
							double prob = Double.parseDouble(probs[j].trim());
							binaryString += (prob >= 0.5 ? "1" : "0");
						}
						if (args.length > 4) {
							if ("-paramtype".equals(args[4])) {
								switch (args[5]) {
									case "eval":
										paramType = ParameterType.STATIC_EVALUATION_PARAMETER;
										break;
									case "control":
										paramType = ParameterType.ENGINE_OR_SEARCH_CONTROL_PARAMETER;
										break;
									case "all":
										paramType = null;
										break;
									default:
										throw new IllegalArgumentException();
								}
							} else
								throw new IllegalArgumentException();
						}
						params.set(binaryString, paramType);
					} else if ("paramvalues".equals(arg1)) {
						String val = args[3];
						String[] array = val.split(",");
						double[] decimalArray = new double[array.length];
						for (int j = 0; j < array.length; j++)
							decimalArray[j] = Double.parseDouble(array[j].trim());
						params.set(decimalArray, ParameterType.STATIC_EVALUATION_PARAMETER);
					} else
						throw new IllegalArgumentException();
					List<String> argList = Arrays.asList(args);
					int ind;
					if ((ind = argList.indexOf("-paramsfile")) != -1)
						destFile = argList.get(ind + 1);
					params.writeToFile(destFile);
					engine.quit();
				} break;
				default:
					throw new IllegalArgumentException();
			}
		} else {
			// GUI mode.
			ControllerEngine controller = factory.newControllerEngineInstance();
			TunableEngine searchEngine = factory.newEngineInstance();
			GUI.setEngines(controller, searchEngine);
			Application.launch(GUI.class);
			controller.quit();
			searchEngine.quit();
		}
	}
	
}
