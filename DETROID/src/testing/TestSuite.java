package testing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import control.ControllerEngine;
import uci.SearchResults;

/**
 * An implementation for an EPD test suite for sanity checks and the rough estimation of the tactical strength of a chess engine.
 * Each instance has a name and holds a list of EPD records.
 * 
 * @author Viktor
 *
 */
public class TestSuite {

	private final String name;
	private final List<EPD> records;
	
	public TestSuite(String name, String testSuiteFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(testSuiteFilePath))) {
			String line;
			while ((line = reader.readLine()) != null)
				records.add(new EPD(line));
		}
		this.name = name;
	}
	/**
	 * Returns the name of the test suite.
	 * 
	 * @return The name of the test suite.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Returns the number of records held in the test suite.
	 * 
	 * @return The number of records in the test suite.
	 */
	public int getRecordCount() {
		return records.size();
	}
	/**
	 * Runs a test on the records in the test suite. Each position defined by an EPD record is searched for the specified amount of time.
	 * The test can be executed on as many parallel threads as many engines are provided.
	 * 
	 * @param engines An array of tunable engines. It cannot be null, it has to have a length greater than 0, and it cannot contain null 
	 * elements. The test will be run on as many parallel threads as many elements the array contains.
	 * @param timePerRecord The time to spend on searching each position in milliseconds.
	 * @param logger A logger to log intermediate results. It can be null.
	 * @return The number of successfully solved positions.
	 * @throws Exception If e.g. an engine cannot be initialized.
	 */
	public int test(ControllerEngine[] engines, long timePerRecord, Logger logger) throws Exception {
		int totalSolved = 0;
		int recordInd = 0;
		int recordPerEngine = (int) Math.ceil(records.size()/engines.length);
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<Integer>> futures = new ArrayList<>();
		for (int i = 0; i < engines.length && recordInd < records.size(); i++) {
			ControllerEngine engine = engines[i];
			if (!engine.isInit())
				engine.init();
			int currRecordInd = recordInd;
			int range = Math.min(currRecordInd + recordPerEngine, records.size());
			futures.add(executor.submit(() -> {
				int solved = 0;
				for (int j = currRecordInd; j < range; j++) {
					boolean correct = false;
					EPD record = records.get(j);
					engine.newGame();
					engine.position(record.getPosition());
					SearchResults results = engine.search(null, null, null, null, null, null, null, null, null, null, timePerRecord, null);
					String bestMove = results.getBestMove();
					/* Convert the correct move from SAN to PACN and compare it to the found move instead of the other way around to potentially avoid 
					 * not recognizing a correct solution due to (erroneous) notational differences in SAN. */
					for (String san : record.getBestMoves()) {
						String pacn = engine.convertSANToPACN(san);
						if (bestMove.equals(pacn)) {
							solved++;
							correct = true;
							break;
						}
					}
					if (logger != null) {
						String res = record.getId() + ": " + engine.convertPACNToSAN(bestMove);
						if (correct)
							logger.info(res + " - correct");
						else
							logger.warning(res + " - incorrect");
					}
				}
				return solved;
			}));
			recordInd += recordPerEngine;
		}
		for (Future<Integer> f : futures)
			totalSolved += f.get();
		executor.shutdown();
		return totalSolved;
	}
}
