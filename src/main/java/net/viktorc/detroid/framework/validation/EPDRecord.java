package net.viktorc.detroid.framework.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A simplified implementation of the Extended Position Description standard.
 *
 * @author Viktor
 */
public class EPDRecord {

  private static final String FIELD_SEPARATOR = " ";
  private static final String OP_SEPARATOR = ";";

  private final String position;
  private final Map<String, String> operations;

  /**
   * @param position The position description in FEN.
   * @param operations A map of the operation codes and operation values.
   */
  public EPDRecord(String position, Map<String, String> operations) {
    this.position = convertFENtoEPDPosition(position);
    this.operations = new HashMap<>(operations);
    for (String key : operations.keySet()) {
      if (!verifyOperationCode(key)) {
        throw new IllegalArgumentException();
      }
    }
  }

  /**
   * Constructs an instance based on the specified EPD record.
   *
   * @param epd The EPD record.
   * @return An EPD record object.
   */
  public static EPDRecord parse(String epd) {
    String[] parts = epd.split(FIELD_SEPARATOR);
    if (parts.length < 4) {
      throw new IllegalArgumentException("Illegal EPD format.");
    }
    String position = String.join(FIELD_SEPARATOR, Arrays.copyOf(parts, 4)).trim();
    Map<String, String> operations = new HashMap<>();
    String allOps = epd.substring(position.length());
    String[] ops = allOps.trim().split(OP_SEPARATOR);
    for (String op : ops) {
      op = op.trim();
      String[] splitOp = op.split(FIELD_SEPARATOR);
      String opCode = splitOp[0];
      String opValue = op.substring(opCode.length()).trim();
      operations.put(opCode, opValue);
    }
    return new EPDRecord(position, operations);
  }

  private String convertFENtoEPDPosition(String fen) {
    return String.join(FIELD_SEPARATOR, Arrays.copyOf(fen.split(FIELD_SEPARATOR), 4)).trim();
  }

  private boolean verifyOperationCode(String operationCode) {
    return operationCode.matches("^[a-zA-Z][a-zA-Z0-9_]{0,14}?");
  }

  /**
   * @return The position description part of the EPD record (FEN minus the fifty-move-rule clock and the half-move counter).
   */
  public String getPosition() {
    return position;
  }

  /**
   * @param operationCode The code of the operation.
   * @return The operand.
   */
  public String getOperand(String operationCode) {
    String op = operations.get(operationCode);
    return op == null ? null : op.replace("\"", "");
  }

  /**
   * @param operationCode The code of the operation.
   * @return The integer operand.
   */
  public Integer getIntegerOperand(String operationCode) {
    String op = getOperand(operationCode);
    return op == null ? null : Integer.parseInt(op);
  }

  /**
   * @param operationCode The code of the operation.
   * @return The long operand.
   */
  public Long getLongOperand(String operationCode) {
    String op = getOperand(operationCode);
    return op == null ? null : Long.parseLong(op);
  }

  /**
   * @param operationCode The code of the operation.
   * @return The double operand.
   */
  public Double getDoubleOperand(String operationCode) {
    String op = getOperand(operationCode);
    return op == null ? null : Double.parseDouble(op);
  }

  /**
   * @param operationCode The code of the operation.
   * @return The double operand.
   */
  public List<String> getListOperand(String operationCode) {
    String string = getOperand(operationCode);
    return string == null ? null : new ArrayList<>(Arrays.asList(string.split(FIELD_SEPARATOR)));
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(position);
    Set<Entry<String, String>> operationSet = operations.entrySet();
    for (Entry<String, String> operation : operationSet) {
      stringBuilder.append(FIELD_SEPARATOR);
      stringBuilder.append(operation.getKey());
      stringBuilder.append(FIELD_SEPARATOR);
      stringBuilder.append(operation.getValue());
      stringBuilder.append(OP_SEPARATOR);
    }
    return stringBuilder.toString();
  }

}
