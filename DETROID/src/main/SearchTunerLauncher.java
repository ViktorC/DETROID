package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import engine.Detroid;
import tuning.GamePlayOptimizer;
import tuning.OptimizerEngines;

public class SearchTunerLauncher {

final static int CONCURRENCY = 4;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		OptimizerEngines[] engines = new OptimizerEngines[CONCURRENCY];
		for (int i = 0; i < CONCURRENCY; i++)
			engines[i] = new OptimizerEngines(new Detroid(), new Detroid(), new Detroid());
		Logger logger = Logger.getAnonymousLogger();
		logger.addHandler(new FileHandler("log", true));
		GamePlayOptimizer optimizer = new GamePlayOptimizer(engines, 32, 10000, 20, 48, logger);
		optimizer.optimize();
		try {
			optimizer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
