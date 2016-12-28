package testing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding perft records.
 * 
 * @author Viktor
 *
 */
public class PerftRegister {

	private final List<PerftRecord> records;
	
	/**
	 * Parses the perft records of the format <FEN>;<depth>;<nodes> in the specified file and holds them in a list.
	 * 
	 * @param perftEntriesFilePath The path to the file containing the perft test records.
	 * @throws IOException If the file does not exist or cannot be read.
	 */
	public PerftRegister(String perftEntriesFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(perftEntriesFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(";");
				if (parts.length < 3)
					continue;
				String fen = parts[0].trim();
				int depth = Integer.parseInt(parts[1]);
				int nodes = Integer.parseInt(parts[2].trim());
				records.add(new PerftRecord(fen, depth, nodes));
			}
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
