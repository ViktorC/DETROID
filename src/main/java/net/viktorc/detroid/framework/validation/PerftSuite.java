package net.viktorc.detroid.framework.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding perft records.
 * 
 * @author Viktor
 *
 */
public class PerftSuite {

	private final List<PerftRecord> records;
	
	/**
	 * Parses the perft records of the format "[FEN]; [depth]; [nodes];" in the specified file and holds them in a list.
	 * 
	 * @param perftEntriesFilePath The path to the file containing the perft test records.
	 * @throws IOException If the file does not exist or cannot be read.
	 */
	public PerftSuite(String perftEntriesFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(perftEntriesFilePath)))) {
			String line;
			while ((line = reader.readLine()) != null)
				records.add(new PerftRecord(line));
		}
	}
	/**
	 * Returns a list of perft records.
	 * 
	 * @return A list of perft records.
	 */
	public List<PerftRecord> getRecords() {
		return new ArrayList<>(records);
	}
	/**
	 * Runs a perft test on the position specified in the record to the depth noted using the provided engine and returns whether 
	 * the engine counted the same number of positions as stated in the record.
	 * 
	 * @param engine The engine to test.
	 * @param record The perft record specifying the position, the depth, and the correct number of nodes.
	 * @return Whether the engine returned the same node count as defined in the record.
	 * @throws Exception If the engine cannot be initialized.
	 */
	public static boolean perft(ControllerEngine engine, PerftRecord record) throws Exception {
		long start, end;
		long nodes;
		if (!engine.isInit())
			engine.init();
		engine.setControllerMode(true);
		engine.newGame();
		engine.setPosition(record.getPosition());
		start = System.currentTimeMillis();
		nodes = engine.perft(record.getDepth());
		end = System.currentTimeMillis();
		String log = String.format("%s; %d; %d; - %d in %.3fs", record.getPosition(), record.getDepth(), record.getNodes(),
				nodes, ((double) (end - start))/1000);
		System.out.println(log);
		return record.getNodes() == nodes;
	}
	
}
