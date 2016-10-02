package main;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import engine.Detroid;
import tuning.EngineParameterOptimizer;
import tuning.OptimizerEngines;
import tuning.OptimizerObserver;

public class TunerLauncher {

	final static String LOG_FILE_PATH = "log.txt";
	final static int CONCURRENCY = 4;
	
	public static void main(String[] args) {
		OptimizerEngines[] engines = new OptimizerEngines[CONCURRENCY];
		for (int i = 0; i < engines.length; i++)
			engines[i] = new OptimizerEngines(new Detroid(), new Detroid(), new Detroid());
		double[] probVector = OptimizerObserver.getLatestProbabilityVector(LOG_FILE_PATH);
		EngineParameterOptimizer epo = new EngineParameterOptimizer(engines, 24, 1000, probVector, 100, 300, Logger.getAnonymousLogger());
		Logger logger = Logger.getAnonymousLogger();
		try {
			logger.addHandler(new FileHandler(LOG_FILE_PATH));
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		epo.addObserver(new OptimizerObserver(logger));
		epo.optimize();
		try {
			epo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
