package net.viktorc.detroid.framework.engine;

import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.viktorc.detroid.framework.engine.Detroid;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.PerftRecord;
import net.viktorc.detroid.framework.validation.PerftSuite;

/**
 * Perft result verification test.
 * 
 * @author Viktor
 *
 */
@RunWith(Parameterized.class)
public final class PerfTest {

	private static final String PERFT_FILE_PATH = "/perft.txt";
	private static final ControllerEngine CONTROLLER = new Detroid();
	
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
		Assert.assertTrue(PerftSuite.perft(CONTROLLER, record));
	}
	@AfterClass
	public static void cleanUp() {
		CONTROLLER.close();
	}
	
}
