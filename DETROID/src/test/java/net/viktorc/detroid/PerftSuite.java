package test.java.net.viktorc.detroid;

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
	 * Parses the perft records of the format [<FEN>; <depth>; <nodes>;] in the specified file and holds them in a list.
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
	
}
