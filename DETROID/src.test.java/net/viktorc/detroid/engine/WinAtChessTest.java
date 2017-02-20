package net.viktorc.detroid.engine;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.EPD;
import net.viktorc.detroid.framework.validation.EPDTestSuite;

/**
 * Win at Chess test suite at half a second per position.
 * 
 * @author Viktor
 *
 */
@RunWith(Parameterized.class)
public final class WinAtChessTest {

	private static final String SUITE_NAME = "Win at Chess";
	private static final String WAC_FILE_PATH = "wac.epd";
	private static final long TIME_PER_POSITION = 500;
	private static final UCIEngine ENGINE = new Detroid();
	private static final ControllerEngine CONTROLLER = new Detroid();
	
	@Parameter
	public EPD record;
	
	@Parameters
	public static Collection<Object[]> provideData() throws IOException {
		EPDTestSuite suite = new EPDTestSuite(SUITE_NAME, WAC_FILE_PATH);
		Collection<Object[]> data = new ArrayList<>();
		for (EPD r : suite.getRecords())
			data.add(new Object[] { r });
		return data;
	}
	@Test
	public void search() throws Exception {
		assumeTrue(EPDTestSuite.searchTest(ENGINE, CONTROLLER, record, TIME_PER_POSITION));
	}
	@AfterClass
	public static void cleanUp() {
		CONTROLLER.quit();
		ENGINE.quit();
	}
	
}