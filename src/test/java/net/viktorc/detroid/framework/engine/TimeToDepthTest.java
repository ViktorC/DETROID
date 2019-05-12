package net.viktorc.detroid.framework.engine;

import java.util.List;
import java.util.Map.Entry;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.EPDRecord;
import net.viktorc.detroid.framework.validation.TTDSuite;
import org.junit.Assume;
import org.junit.Test;

/**
 * A class for testing the time-to-depth speedup of the engine when using multiple threads.
 *
 * @author Viktor
 */
public final class TimeToDepthTest {

  private static final String TTD_PATH = "/ttd.epd";

  /**
   * It runs a test on the specified TTD suite using the specified engine running the specified number of search threads.
   *
   * @param engine The engine to test.
   * @param suite The TTD test suite.
   * @param threads The number of threads to use.
   * @param additionalDepth Depth adjustment.
   * @return The total time needed for the engine to search the records in nanoseconds.
   * @throws Exception If the engine fails.
   */
  private long test(UCIEngine engine, TTDSuite suite, int threads, int additionalDepth) throws Exception {
    engine.setThreadsOption(threads);
    long time = 0;
    long nodes = 0;
    long totalDepth = 0;
    List<EPDRecord> records = suite.getRecords();
    for (EPDRecord record : records) {
      totalDepth += Math.max(1, record.getIntegerOperand(TTDSuite.TTD_DEPTH_OP_CODE) + additionalDepth);
      Entry<Long, Long> res = TTDSuite.searchTest(engine, record, additionalDepth);
      time += res.getKey();
      if (res.getValue() != null) {
        nodes += res.getValue();
      }
    }
    System.out.printf("%nThreads: %d; average depth: %.2f; total time: %.2f ms; average time: %.2f; " +
            "average speed: %.2f kNPS%n%n", threads, ((double) totalDepth) / records.size(), ((double) time) / 1000000,
        ((double) time) / records.size() / 1000000, ((double) nodes) * 1000000 / time);
    return time;
  }

  @Test
  public void test() throws Exception {
    TTDSuite suite = new TTDSuite(TTD_PATH);
    UCIEngine engine = new Detroid();
    engine.init();
    try {
      int depthAdjustment = -1;
      // Warm up.
      System.out.printf("%n%n%nWARM UP%n%n%n");
      test(engine, suite, 4, depthAdjustment);
      System.out.printf("%n%n%nSTART%n%n%n");
      long time1, time2, time3, time4;
      time1 = time2 = time3 = time4 = 0;
      time1 += test(engine, suite, 1, depthAdjustment);
      time2 += test(engine, suite, 2, depthAdjustment);
      time3 += test(engine, suite, 3, depthAdjustment);
      time4 += test(engine, suite, 4, depthAdjustment);
      Assume.assumeTrue(time1 > time2 && time2 > time3 && time3 > time4);
    } finally {
      engine.close();
    }
  }

}
