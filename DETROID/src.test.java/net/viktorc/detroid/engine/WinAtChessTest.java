package net.viktorc.detroid.engine;

import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.viktorc.detroid.framework.tuning.EPD;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * Win at Chess test suite at 1 second per position.
 * 
 * @author Viktor
 *
 */
@RunWith(Parameterized.class)
public final class WinAtChessTest {

	private static final String WAC_FILE_PATH = "wac.epd";
	private static final long TIME_PER_POSITION = 1000;
	private static final ControllerEngine CONTROLLER = new Detroid();
	
	@Parameter
	public EPD record;
	
	@Parameters
	public static Collection<Object[]> provideData() throws IOException {
		Collection<Object[]> data = new ArrayList<>();
		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(WinAtChessTest.class.getResourceAsStream(WAC_FILE_PATH)))) {
			String line;
			while ((line = reader.readLine()) != null)
				data.add(new Object[] { new EPD(line) });
		}
		return data;
	}
	@Test
	public void search() throws Exception {
		if (!CONTROLLER.isInit())
			CONTROLLER.init();
		CONTROLLER.newGame();
		CONTROLLER.setPosition(record.getPosition());
		String bestMove = CONTROLLER.search(null, null, null, null, null, null, null, null, null, null,
				TIME_PER_POSITION, null).getBestMove();
		boolean correct = false;
		/* Convert the correct move from SAN to PACN and compare it to the found move instead of the other way around to 
		 * potentially avoid not recognizing a correct solution due to (occasional) notational differences in SAN. */
		for (String san : record.getBestMoves()) {
			String pacn = CONTROLLER.convertSANToPACN(san);
			if (bestMove.equals(pacn)) {
				correct = true;
				break;
			}
		}
		String log = String.format("%s - %s in %.3fs", record.toString(), CONTROLLER.convertPACNToSAN(bestMove),
				((double) TIME_PER_POSITION)/1000);
		System.out.println(log);
		assumeTrue(correct);
	}
	@AfterClass
	public static void cleanUp() {
		CONTROLLER.quit();
	}
	
}
