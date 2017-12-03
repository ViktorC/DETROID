package net.viktorc.detroid.framework.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.viktorc.detroid.framework.uci.UCIEngine;

/**
 * A class for a suite of time-to-depth test entries.
 * 
 * @author Viktor
 *
 */
public class TTDSuite {

	private final List<TTDRecord> records;
	
	/**
	 * Parses the time-to-depth entries.
	 * 
	 * @param ttdEntriesFilePath The path to the file containing the TTD entries.
	 * @throws IOException If the file does not exist or cannot be read.
	 */
	public TTDSuite(String ttdEntriesFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream(ttdEntriesFilePath)))) {
			String line;
			while ((line = reader.readLine()) != null)
				records.add(new TTDRecord(line));
		}
	}
	/**
	 * Returns a list of the TTD records.
	 * 
	 * @return A list of the TTD records.
	 */
	public List<TTDRecord> getRecords() {
		return new ArrayList<>(records);
	}
	/**
	 * It has the parameter engine search the position specified in the TTD record to the (potentially adjusted) depth 
	 * specified in the record.
	 * 
	 * @param engine The engine to test.
	 * @param record The time-to-depth record.
	 * @param additionalDepth Optional adjustment to the depth specified in the entry.
	 * @return The number of nanoseconds it took for the engine to search to specified to position to the specified depth 
	 * and the total number of nodes searched during the search if that information is available.
	 * @throws Exception If the engine is not initialized and it cannot be initialized.
	 */
	public static Entry<Long,Long> searchTest(UCIEngine engine, TTDRecord record, int additionalDepth)
			throws Exception {
		if (!engine.isInit())
			engine.init();
		engine.newGame();
		engine.setPosition(record.getPosition());
		long start = System.nanoTime();
		engine.search(null, null, null, null, null, null, null, Math.max(1, record.getDepth() + additionalDepth),
				null, null, null, null);
		long time = System.nanoTime() - start;
		long nodes = engine.getSearchInfo().getNodes();
		double knps = nodes != 0 ? ((double) nodes*1000000)/time : 0;
		String log = String.format("%s - %.2f ms %.2f kNPS", record.toString(), ((double) time)/1000000, knps);
		System.out.println(log);
		return new SimpleImmutableEntry<>(time, nodes);
	}
	
}
