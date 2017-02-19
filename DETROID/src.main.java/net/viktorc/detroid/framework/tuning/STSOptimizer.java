package net.viktorc.detroid.framework.tuning;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.logging.Logger;

import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.util.ASGD;

public class STSOptimizer extends ASGD<String,Set<Entry<String,Integer>>> implements AutoCloseable {

	/**
	 * The number of EPD test suites STS comprises.
	 */
	private static final int NUMBER_OF_SUITES = 15;
	/**
	 * The number of EPD records each of the STS suites holds.
	 */
	private static final int RECORD_PER_SUITE = 100;
	/**
	 * The prefix of the STS EPD files.
	 */
	private static final String SUITE_PREFIX = "sts";
	/**
	 * The fraction of the complete data set used as a test data set.
	 */
	private static final double TEST_DATA_PROPORTION = 0.2d;
	/**
	 * The base step size for the gradient descent.
	 */
	private static final double BASE_LEARNING_RATE = 2;
	
	private final TunableEngine[] engines;
	private final Set<ParameterType> parameterTypes;
	private final int trainingRecordsPerSuite;
	private final int sampleSizePerSuite;
	private final List<List<Entry<String,Set<Entry<String,Integer>>>>> trainingData;
	private final List<Entry<String,Set<Entry<String,Integer>>>> testData;
	private final Random rand;
	private final ExecutorService pool;
	
	protected STSOptimizer(TunableEngine[] engines, ControllerEngine controllerEngine, int sampleSizePerSuite,
			long searchTime, Logger logger, Set<ParameterType> parameterTypes) throws Exception {
		super(engines[0].getParameters().values(parameterTypes), (double[]) Array.newInstance(double.class,
				engines[0].getParameters().values(parameterTypes).length), engines[0].getParameters().maxValues(parameterTypes),
				1d, BASE_LEARNING_RATE, null, null, null, null, null, logger);
		if (logger == null)
			throw new IllegalArgumentException("The logger cannot be null.");
		trainingRecordsPerSuite = (int) (RECORD_PER_SUITE*(1 - TEST_DATA_PROPORTION));
		if (sampleSizePerSuite < 1 || sampleSizePerSuite > trainingRecordsPerSuite)
			throw new IllegalArgumentException("The sample size per suite has to be greater than 0 and less than " +
					trainingRecordsPerSuite + ".");
		ArrayList<TunableEngine> enginesList = new ArrayList<>();
		for (TunableEngine e : engines) {
			if (e != null) {
				if (!e.isInit())
					e.init();
				e.setDeterminism(true);
				enginesList.add(e);
			}
		}
		this.engines = enginesList.toArray(new TunableEngine[enginesList.size()]);
		rand = new Random(System.nanoTime());
		pool = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), this.engines.length));
		this.parameterTypes = parameterTypes;
		logger.info("Tuning parameters of type: " + this.parameterTypes);
		this.sampleSizePerSuite = sampleSizePerSuite;
		int testDataSize = RECORD_PER_SUITE - trainingRecordsPerSuite;
		trainingData = new ArrayList<>(NUMBER_OF_SUITES);
		testData = new ArrayList<>(testDataSize*NUMBER_OF_SUITES);
		for (int i = 0; i < NUMBER_OF_SUITES; i++) {
			List<STSEPD> records = new ArrayList<>();
			String filePath = SUITE_PREFIX + (i + 1) + ".epd";
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filePath)))) {
				String line;
				while ((line = reader.readLine()) != null)
					records.add(new STSEPD(line));
			}
			Collections.shuffle(records);
			List<Entry<String,Set<Entry<String,Integer>>>> trainingPortion = new ArrayList<>(trainingRecordsPerSuite);
			for (int j = 0; j < records.size(); j++) {
				STSEPD record = records.get(j);
				controllerEngine.setPosition(record.getPosition());
				Set<Entry<String,Integer>> moveScorePairs = new HashSet<>();
				for (Entry<String,Integer> moveScorePair : record.getMoveScorePairs())
					moveScorePairs.add(new SimpleImmutableEntry<>(controllerEngine.convertSANToPACN(moveScorePair.getKey()),
							moveScorePair.getValue()));
				Entry<String,Set<Entry<String,Integer>>> entry = new SimpleImmutableEntry<>(record.getPosition(), moveScorePairs);
				if (j < testDataSize)
					testData.add(entry);
				else
					trainingPortion.add(entry);
			}
			trainingData.add(trainingPortion);
		}
	}
	@Override
	protected List<Entry<String,Set<Entry<String, Integer>>>> getTestData() {
		return testData;
	}
	@Override
	protected List<Entry<String,Set<Entry<String, Integer>>>> sampleTrainingData() {
		List<Entry<String,Set<Entry<String, Integer>>>> sample = new ArrayList<>();
		for (int i = 0; i < NUMBER_OF_SUITES; i++) {
			Set<Integer> indices = new HashSet<>();
			while (indices.size() < sampleSizePerSuite)
				indices.add((int) (rand.nextDouble()*trainingRecordsPerSuite));
			for (Integer ind : indices)
				sample.add(trainingData.get(i).get(ind));
		}
		return sample;
	}
	@Override
	protected double costFunction(double[] features, List<Entry<String,Set<Entry<String, Integer>>>> dataSample) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void close() throws Exception {
		pool.shutdown();
		for (TunableEngine e : engines)
			e.quit();
	}

}
