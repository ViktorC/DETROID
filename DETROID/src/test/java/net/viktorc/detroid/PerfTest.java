package test.java.net.viktorc.detroid;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import main.java.net.viktorc.detroid.EngineFactory;
import main.java.net.viktorc.detroid.framework.control.ControllerEngine;

/**
 * Perft result verification test.
 * 
 * @author Viktor
 *
 */
@RunWith(Parameterized.class)
public class PerfTest {

	private static final String PERFT_FILE_PATH = "/test/resources/perft.txt";
	private static final ControllerEngine CONTROLLER = EngineFactory.getInstance().newControllerEngineInstance();
	
	@Parameter
	public PerftRecord record;
	
	@Parameters
	public static Collection<Object[]> provideData() throws IOException {
		PerftSuite suite = new PerftSuite(PERFT_FILE_PATH);
		Collection<Object[]> data = new ArrayList<>();
		for (PerftRecord r : suite.getRecords())
			data.add(new Object[] { r });
		return data;
	}
	@Test
	public void perft() throws Exception {
		long start, end;
		long nodes;
		if (!CONTROLLER.isInit())
			CONTROLLER.init();
		CONTROLLER.setControllerMode(true);
		CONTROLLER.newGame();
		CONTROLLER.position(record.getPosition());
		start = System.currentTimeMillis();
		nodes = CONTROLLER.perft(record.getDepth());
		end = System.currentTimeMillis();
		String log = String.format("%s; %d; %d; - %d in %.3fs", record.getPosition(), record.getDepth(), record.getNodes(),
				nodes, ((double) (end - start))/1000);
		System.out.println(log);
		assertEquals(record.getNodes(), nodes);
	}
	@AfterClass
	public static void cleanUp() {
		CONTROLLER.quit();
	}
	
}
