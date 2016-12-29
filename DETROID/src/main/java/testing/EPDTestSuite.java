package main.java.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation for an EPD test suite for sanity checks and the rough estimation of the tactical strength of a chess engine.
 * Each instance has a name and holds a list of EPD records.
 * 
 * @author Viktor
 *
 */
public class EPDTestSuite {

	private final String name;
	private final List<EPD> records;
	
	/**
	 * Parses the EPD records in the specified file and holds them in a list.
	 * 
	 * @param name The name of the test suite.
	 * @param testSuiteFilePath The path to the file holding the EPD records.
	 * @throws IOException If the file does not exist or cannot be read.
	 */
	public EPDTestSuite(String name, String testSuiteFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(testSuiteFilePath)))) {
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
	 * Returns the EPD records held in the test suite.
	 * 
	 * @return The EPD records in the test suite.
	 */
	public List<EPD> getRecords() {
		return new ArrayList<>(records);
	}
}
