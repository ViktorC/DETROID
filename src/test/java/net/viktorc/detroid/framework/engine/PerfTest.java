package net.viktorc.detroid.framework.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.EPDRecord;
import net.viktorc.detroid.framework.validation.PerftSuite;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Perft result verification test.
 *
 * @author Viktor
 */
@RunWith(Parameterized.class)
public final class PerfTest {

  private static final String PERFT_FILE_PATH = "/perft.epd";
  private static final ControllerEngine CONTROLLER = new Detroid();

  @Parameter
  public EPDRecord record;

  @Parameters
  public static Collection<Object[]> provideData() throws IOException {
    PerftSuite suite = new PerftSuite(PERFT_FILE_PATH);
    Collection<Object[]> data = new ArrayList<>();
    for (EPDRecord r : suite.getRecords()) {
      data.add(new Object[]{r});
    }
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
