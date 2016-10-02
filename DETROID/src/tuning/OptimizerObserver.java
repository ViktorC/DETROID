package tuning;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import util.PBIL;

/**
 * A simple observer for PBIL subclasses.
 * 
 * @author Viktor
 *
 */
public class OptimizerObserver implements Observer {

	private Logger logger;
	
	/**
	 * Tries to parse and return the latest probability vector from a file written to by an {@link #OptimizerObserver(Logger) OptimizerObserver}
	 * instance. If due to an IO or parsing exception, it's not possible, it returns null.
	 * 
	 * @param logFilePath
	 * @return
	 */
	public static double[] getLatestProbabilityVector(String logFilePath) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFilePath)))) {
			String line;
			String probVectorLine = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Probability vector"))
					probVectorLine = line;
			}
			probVectorLine = probVectorLine.trim();
			probVectorLine = probVectorLine.substring(probVectorLine.indexOf('{') + 1, probVectorLine.indexOf('}'));
			String[] values = probVectorLine.split(",");
			double[] probVector = new double[values.length];
			for (int i = 0; i < probVector.length; i++)
				probVector[i] = Double.parseDouble(values[i].trim());
			return probVector;
		} catch (Exception e) {
			return null;
		}
	}
	/**
	 * Constructs an observer with the provided logger used to log the updates.
	 * 
	 * @param logger
	 */
	public OptimizerObserver(Logger logger) {
		this.logger = logger;
	}
	@Override
	public void update(Observable arg0, Object arg1) {
		PBIL pbil = (PBIL) arg0;
		String out = "------------------------------ Generation " + pbil.getCurrentGeneration() + "\n" +
				"Probability vector: {";
		for (double prob : pbil.getProbabilityVector())
			out += " " + prob + "d,";
		if (out.endsWith(","))
			out = out.substring(0, out.length() - 1);
		out += " }\n";
		out += "All time fittest individual: " + pbil.getFittestIndividual().getGenome() + "\n" +
				"Highest fitness level: " + pbil.getFittestIndividual().getFitness() + "\n";
		logger.info(out);
	}

}
