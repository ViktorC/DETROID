package main;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import engine.Detroid;
import tuning.StaticEvaluationOptimizer;
import tuning.TunableEngine;

public class EvalTunerLauncher {

	final static int CONCURRENCY = 4;
	
	public static void main(String[] args) throws IOException {
		TunableEngine[] engines = new TunableEngine[CONCURRENCY];
		for (int i = 0; i < CONCURRENCY; i++)
			engines[i] = new Detroid();
		engines[0].init();
		Logger logger = Logger.getAnonymousLogger();
		logger.addHandler(new FileHandler("log", true));
		StaticEvaluationOptimizer optimizer = new StaticEvaluationOptimizer(engines, 131072, "fens", -0.63d , logger);
		optimizer.train();
		try {
			optimizer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
