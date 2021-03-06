package net.viktorc.detroid.framework.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding perft records.
 *
 * @author Viktor
 */
public class PerftSuite {

  public static final String PERFT_DEPTH_OP_CODE = "Pd";
  public static final String PERFT_NODE_COUNT_OP_CODE = "Pnc";

  private final List<EPDRecord> records;

  /**
   * Parses the perft records in the specified file and holds them in a list.
   *
   * @param perftEntriesFilePath The path to the file containing the perft test records.
   * @throws IOException If the file does not exist or cannot be read.
   */
  public PerftSuite(String perftEntriesFilePath) throws IOException {
    records = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(perftEntriesFilePath)))) {
      String line;
      while ((line = reader.readLine()) != null) {
        records.add(EPDRecord.parse(line));
      }
    }
  }

  /**
   * @return A list of perft records.
   */
  public List<EPDRecord> getRecords() {
    return new ArrayList<>(records);
  }

  /**
   * Runs a perft test on the position specified in the record to the depth noted using the provided engine and returns whether the engine
   * counted the same number of positions as stated in the record.
   *
   * @param engine The engine to test.
   * @param record The perft record specifying the position, the depth, and the correct number of nodes.
   * @return Whether the engine returned the same node count as defined in the record.
   * @throws Exception If the engine cannot be initialized.
   */
  public static boolean perft(ControllerEngine engine, EPDRecord record) throws Exception {
    if (!engine.isInit()) {
      engine.init();
    }
    int depth = record.getIntegerOperand(PERFT_DEPTH_OP_CODE);
    long expectedNodes = record.getLongOperand(PERFT_NODE_COUNT_OP_CODE);
    engine.setControllerMode(true);
    engine.newGame();
    engine.setPosition(record.getPosition());
    long start = System.currentTimeMillis();
    long nodes = engine.perft(depth);
    long end = System.currentTimeMillis();
    String log = String.format("%s - %d in %.3fs", record, nodes, ((double) (end - start)) / 1000);
    System.out.println(log);
    return expectedNodes == nodes;
  }

}
