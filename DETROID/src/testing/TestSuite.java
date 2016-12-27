package testing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestSuite {

	private final List<EPD> records;
	
	public TestSuite(String testSuiteFilePath) throws IOException {
		records = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(testSuiteFilePath))) {
			String line;
			while ((line = reader.readLine()) != null)
				records.add(new EPD(line));
		}
	}
}
