package net.viktorc.detroid.framework.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.viktorc.detroid.framework.uci.UCIEngine;

/**
 * An implementation for an EPD test suite for sanity checks and the rough estimation of the tactical strength of a chess engine.
 * Each instance has a name and holds a list of {@link net.viktorc.detroid.framework.validation.EPDRecord} instances.
 * 
 * @author Viktor
 *
 */
public class EPDSuite {

	private final String name;
	private final List<EPDRecord> records;
	
	/**
	 * Parses the EPD records in the specified file and holds them in a list.
	 * 
	 * @param name The name of the test suite.
	 * @param testSuiteFilePath The path to the file holding the EPDRecord records.
	 * @throws IOException If the file does not exist or cannot be read.
	 */
	public EPDSuite(String name, String testSuiteFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(testSuiteFilePath)))) {
			String line;
			while ((line = reader.readLine()) != null)
				records.add(new EPDRecord(line));
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
	 * Returns the EPDRecord records held in the test suite.
	 * 
	 * @return The EPDRecord records in the test suite.
	 */
	public List<EPDRecord> getRecords() {
		return new ArrayList<>(records);
	}
	/**
	 * Searches the position defined in the EPDRecord record for the specified amount of time using the provided engine and returns 
	 * if the engine found any one of the best moves noted in the record.
	 * 
	 * @param engine The engine to test.
	 * @param controllerEngine The controller engine.
	 * @param record The EPDRecord record specifying the position and the best move(s).
	 * @param timePerPos How long the engine should search the position in milliseconds.
	 * @return Whether the engine found any one of the best moves.
	 * @throws Exception If the engine initialization fails.
	 */
	public static boolean searchTest(UCIEngine engine, ControllerEngine controllerEngine, EPDRecord record, long timePerPos)
			throws Exception {
		if (!controllerEngine.isInit())
			controllerEngine.init();
		controllerEngine.setControllerMode(true);
		controllerEngine.setPosition(record.getPosition());
		if (!engine.isInit())
			engine.init();
		engine.newGame();
		engine.setPosition(record.getPosition());
		String bestMove = engine.search(null, null, null, null, null, null, null, null, null, null,
				timePerPos, null).getBestMove();
		boolean correct = false;
		/* Convert the correct move from SAN to PACN and compare it to the found move instead of the other way around to 
		 * potentially avoid not recognizing a correct solution due to (occasional) notational differences in SAN. */
		for (String san : record.getBestMoves()) {
			String pacn = controllerEngine.convertSANToPACN(san);
			if (bestMove.equals(pacn)) {
				correct = true;
				break;
			}
		}
		String log = String.format("%s - %s in %.3fs", record.toString(), controllerEngine.convertPACNToSAN(bestMove),
				((double) timePerPos)/1000);
		System.out.println(log);
		return correct;
	}
	
}