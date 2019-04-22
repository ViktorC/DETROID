package net.viktorc.detroid.framework.tuning;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.viktorc.detroid.framework.util.BitOperations;
import net.viktorc.detroid.framework.util.GrayCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An abstract base class for engine parameter definitions that influence the performance of the chess engine and thus should be highly
 * optimized. It provides methods for the retrieval of the values of the fields of its sub classes which are utilized in the methods
 * provided for writing the contents of an instance to a file or reading, parsing, and setting the fields of an instance based on such a
 * file. It is possible to retrieve the values as an array of doubles or as a binary string that represents the genotype of the instance
 * with each field's value gray coded and concatenated into a binary string. An array or string like that can be then used to set the values
 * of the fields of instances with the same procedure reversed. Only those fields of the subclasses are handled by the class that are
 * annotated with the {@link net.viktorc.detroid.framework.tuning.Parameter} annotation. The annotation also has an optional attribute
 * {@link net.viktorc.detroid.framework.tuning.Parameter#binaryLengthLimit() binaryLengthLimit} which marks the number of bits to consider
 * when tuning the parameter. When applied to floating point values, its effects are less intuitive as instead of ignoring bits of the
 * number's binary representation, it just sets a maximum for the values it can take on which will be equal to what this maximum would be
 * for integer numbers (2^[limit] - 1).
 *
 * WARNING: No negative values are supported, thus the most significant bit of each signed primitive (all except boolean and char) field
 * will be ignored when generating the binary string or setting the values of the fields based on a binary string and negative values will
 * default to 0 when setting the fields based on a double array.
 *
 * @author Viktor
 */
public abstract class EngineParameters {

  private static final String XML_ROOT_ELEMENT_NAME = "parameters";

  private final transient List<Field> allParamFields;
  private final transient List<Field> staticEvalParamFields;
  private final transient List<Field> searchControlParamFields;
  private final transient List<Field> engineManagementParamFields;

  /**
   * Constructs an instance and scans the fields of the (extending) class for ones annotated as {@link
   * net.viktorc.detroid.framework.tuning.Parameter}.
   *
   * @throws ParameterException If a static or non-primitive field is annotated as {@link net.viktorc.detroid.framework.tuning.Parameter}.
   */
  protected EngineParameters() throws ParameterException {
    allParamFields = new ArrayList<>();
    staticEvalParamFields = new ArrayList<>();
    searchControlParamFields = new ArrayList<>();
    engineManagementParamFields = new ArrayList<>();
    int modifiers;
    Parameter param;
    Class<?> fieldType;
    Class<?> clazz = getClass();
    ArrayList<Field> allfields = new ArrayList<>();
    while (clazz != null) {
      allfields.addAll(Arrays.asList(clazz.getDeclaredFields()));
      clazz = clazz.getSuperclass();
    }
    for (Field f : allfields) {
      f.setAccessible(true);
      modifiers = f.getModifiers();
      fieldType = f.getType();
      param = f.getAnnotation(Parameter.class);
      if (param != null) {
        if (!fieldType.isPrimitive() || Modifier.isStatic(modifiers)) {
          throw new ParameterException("Illegal field: " + f.getName() + "; Only non-static primitive, " +
              "that is boolean, byte, short, int, long, float, double, and char fields are allowed " +
              "to be annotated as Parameters.");
        }
        allParamFields.add(f);
        if (param.type() == ParameterType.STATIC_EVALUATION) {
          staticEvalParamFields.add(f);
        } else if (param.type() == ParameterType.SEARCH_CONTROL) {
          searchControlParamFields.add(f);
        } else if (param.type() == ParameterType.ENGINE_MANAGEMENT) {
          engineManagementParamFields.add(f);
        }
      }
    }
  }

  /**
   * Reads the parameter values from an XML file and sets the instance's fields accordingly.
   *
   * @param filePath The path to the file.
   * @throws Exception If an error occurs from which it is not possible to recover.
   */
  public final void loadFrom(String filePath) throws Exception {
    File file;
    String name;
    String value;
    Class<?> clazz = getClass();
    Field field;
    Class<?> fieldType;
    file = new File(filePath);
    try (InputStream input = file.exists() ? new FileInputStream(filePath) : ClassLoader.getSystemClassLoader()
        .getResourceAsStream(filePath)) {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.parse(input);
      document.getDocumentElement().normalize();
      NodeList parameters = document.getDocumentElement().getChildNodes();
      for (int i = 0; i < parameters.getLength(); i++) {
        Node node = parameters.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        Element parameter = (Element) node;
        name = parameter.getNodeName();
        try {
          field = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException | SecurityException e1) {
          continue;
        }
        if (!allParamFields.contains(field)) {
          continue;
        }
        field.setAccessible(true);
        value = parameter.getTextContent();
        if (value.length() == 0) {
          continue;
        }
        fieldType = field.getType();
        if (fieldType.equals(boolean.class)) {
          field.set(this, Boolean.parseBoolean(value));
        } else if (fieldType.equals(byte.class)) {
          field.set(this, Byte.parseByte(value));
        } else if (fieldType.equals(short.class)) {
          field.set(this, Short.parseShort(value));
        } else if (fieldType.equals(int.class)) {
          field.set(this, Integer.parseInt(value));
        } else if (fieldType.equals(long.class)) {
          field.set(this, Long.parseLong(value));
        } else if (fieldType.equals(float.class)) {
          field.set(this, Float.parseFloat(value));
        } else if (fieldType.equals(double.class)) {
          field.set(this, Double.parseDouble(value));
        } else if (fieldType.equals(char.class)) {
          field.set(this, value.charAt(0));
        }
      }
    }
  }

  /**
   * Writes the parameters to the specified file. If it does not exist, this method will attempt to create it. The method returns whether
   * writing to the file was successful.
   *
   * @param filePath The path to the file.
   * @return Whether the parameters could be successfully written to the file.
   */
  public final boolean writeToFile(String filePath) {
    try {
      File file = new File(filePath);
      if (!file.exists()) {
        Files.createFile(file.toPath());
      }
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.newDocument();
      Element root = document.createElement(XML_ROOT_ELEMENT_NAME);
      document.appendChild(root);
      for (Field f : allParamFields) {
        f.setAccessible(true);
        Element parameter = document.createElement(f.getName());
        parameter.setTextContent("" + f.get(this));
        root.appendChild(parameter);
      }
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file);
      transformer.transform(source, result);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Returns a reference to all parameter fields of the specified types. If types is null, all parameter fields are returned.
   *
   * @param types A set of the types of the parameter fields to be returned.
   * @return All parameter fields of the specified types.
   */
  private List<Field> getParamFields(Set<ParameterType> types) {
    if (types == null) {
      return allParamFields;
    }
    List<Field> fields = new ArrayList<>();
    if (types.contains(ParameterType.STATIC_EVALUATION)) {
      fields.addAll(staticEvalParamFields);
    }
    if (types.contains(ParameterType.SEARCH_CONTROL)) {
      fields.addAll(searchControlParamFields);
    }
    if (types.contains(ParameterType.ENGINE_MANAGEMENT)) {
      fields.addAll(engineManagementParamFields);
    }
    return fields;
  }

  /**
   * Sets the values of the parameter fields of the instance. If a value in the array is negative, it will be taken for 0; if a value is
   * greater than what the respective field's type or the bit limit specified by the {@link net.viktorc.detroid.framework.tuning.Parameter}
   * annotation's attribute would allow for, the field will be set to its maximum allowed value. If the array is longer than the number of
   * parameter fields, the extra elements will be ignored; if it is shorter, the fields indexed higher than the length of the array will not
   * be set. For boolean fields, a value greater than or equal to 1 will default to true, everything else will default to false.
   *
   * @param values An array of values for the parameter fields of the instance in the order of declaration.
   * @return Whether the fields could be successfully set.
   */
  public final boolean set(double[] values) {
    return set(values, null);
  }

  /**
   * Sets the values of the parameter fields of the specified type. If a value in the array is negative, it will be taken for 0; if a value
   * is greater than what the respective field's type or the bit limit specified by the {@link net.viktorc.detroid.framework.tuning.Parameter}
   * annotation's attribute would allow for, the field will be set to its maximum allowed value. If the array is longer than the number of
   * parameter fields of the specified type, the extra elements will be ignored; if it is shorter, the fields indexed higher than the length
   * of the array will not be set. For boolean fields, a value greater than or equal to 1 will default to true, everything else will default
   * to false.
   *
   * @param values An array of values for the parameter fields of the specified type in the order of declaration.
   * @param types A set of the types of parameter fields whose values should be set based on the input array. If it is null, the values will
   * be applied to the set of all parameter fields.
   * @return Whether the fields could be successfully set.
   */
  public final boolean set(double[] values, Set<ParameterType> types) {
    try {
      List<Field> params = getParamFields(types);
      if (values == null) {
        return false;
      }
      int lim = Math.min(params.size(), values.length);
      for (int i = 0; i < lim; i++) {
        Field f = params.get(i);
        Parameter ann = f.getAnnotation(Parameter.class);
        byte bitLimit = ann != null && ann.binaryLengthLimit() >= 0 ? ann.binaryLengthLimit() : 63;
        if (bitLimit == 0) {
          continue;
        }
        Class<?> fieldType = f.getType();
        double value = values[i];
        value = Math.max(value, 0);
        value = Math.min(value, (1L << bitLimit) - 1);
        if (fieldType.equals(boolean.class)) {
          f.setBoolean(this, value >= 1);
        } else if (fieldType.equals(byte.class)) {
          f.setByte(this, (byte) Math.min(value, Byte.MAX_VALUE));
        } else if (fieldType.equals(short.class)) {
          f.setShort(this, (short) Math.min(value, Short.MAX_VALUE));
        } else if (fieldType.equals(int.class)) {
          f.setInt(this, (int) Math.min(value, Integer.MAX_VALUE));
        } else if (fieldType.equals(long.class)) {
          f.setLong(this, (long) Math.min(value, Long.MAX_VALUE));
        } else if (fieldType.equals(float.class)) {
          f.setFloat(this, (float) Math.min(value, Float.MAX_VALUE));
        } else if (fieldType.equals(double.class)) {
          f.setDouble(this, Math.min(value, Double.MAX_VALUE));
        } else if (fieldType.equals(char.class)) {
          f.setChar(this, (char) Math.min(value, Character.MAX_VALUE));
        }
      }
      return true;
    } catch (IllegalAccessException e) {
      return false;
    }
  }

  /**
   * Sets the values of the parameter fields based on the binaryString in which each character represents a bit in the string of the
   * individual gray code strings of the values of the parameter fields of the specified type in the order of declaration.
   *
   * @param binaryString A binary string of gray code such as the output of {@link #toGrayCodeString() toGrayCodeString}.
   * @return Whether the binary string could be successfully applied to the fields.
   */
  public final boolean set(String binaryString) {
    return set(binaryString, null);
  }

  /**
   * Sets the values of the parameter fields of the specified type based on the binaryString in which each character represents a bit in the
   * string of the individual gray code strings of the values of the parameter fields of the specified type in the order of declaration.
   *
   * @param binaryString A binary string of gray code such as the output of {@link #toGrayCodeString() toGrayCodeString}.
   * @param types A set of the types of parameter fields whose values should be derived from the binary string and set. If it is null, the
   * values derived will be applied to all parameter fields.
   * @return Whether the binary string could be successfully applied to the fields.
   */
  public final boolean set(String binaryString, Set<ParameterType> types) {
    try {
      List<Field> params = getParamFields(types);
      int i = 0;
      for (Field f : params) {
        Class<?> fieldType = f.getType();
        Parameter ann = f.getAnnotation(Parameter.class);
        byte bitLimit = ann.binaryLengthLimit() >= 0 ? ann.binaryLengthLimit() : 63;
        if (bitLimit == 0) {
          continue;
        }
        if (fieldType.equals(boolean.class)) {
          f.set(this, "1".equals(binaryString.substring(i, ++i)));
        } else if (fieldType.equals(byte.class)) {
          f.set(this, (byte) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 7))), 2)));
        } else if (fieldType.equals(short.class)) {
          f.set(this, (short) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 15))), 2)));
        } else if (fieldType.equals(int.class)) {
          f.set(this, (int) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 31))), 2)));
        } else if (fieldType.equals(long.class)) {
          f.set(this, GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 63))), 2)));
        } else if (fieldType.equals(float.class)) {
          f.set(this, (float) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 31))), 2)));
        } else if (fieldType.equals(double.class)) {
          f.set(this, GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 63))), 2)));
        } else if (fieldType.equals(char.class)) {
          f.set(this, (char) GrayCode.decode(Long.parseUnsignedLong(binaryString.substring(i,
              (i += Math.min(bitLimit, 16))), 2)));
        }
      }
      return true;
    } catch (IllegalAccessException e) {
      return false;
    }
  }

  /**
   * Returns an array of doubles holding the values of all the fields declared as parameters by the {@link
   * net.viktorc.detroid.framework.tuning.Parameter} annotation. Boolean values will be converted to either 1 or 0 depending on whether they
   * are true or false.
   *
   * @return An array of doubles holding the values of all the parameter fields.
   */
  public final double[] values() {
    return values(null);
  }

  /**
   * Returns an array of doubles holding the values of all the fields declared as parameters of the specified type by the {@link
   * net.viktorc.detroid.framework.tuning.Parameter} annotation. Boolean values will be converted to either 1 or 0 depending on whether they
   * are true or false.
   *
   * @param types A set of the types of parameter fields whose max values are to be returned. If it is null the max values for all parameter
   * fields will be returned.
   * @return An array of doubles holding the values of all the parameter fields of the specified type.
   */
  public final double[] values(Set<ParameterType> types) {
    List<Field> params = getParamFields(types);
    double[] arr = new double[params.size()];
    int i = 0;
    for (Field f : params) {
      try {
        if (f.getType().equals(boolean.class)) {
          arr[i++] = (f.getBoolean(this) ? 1 : 0);
        } else {
          arr[i++] = f.getDouble(this);
        }
      } catch (IllegalArgumentException | IllegalAccessException e) {
        // Ignore.
      }
    }
    return arr;
  }

  /**
   * Returns an array of doubles holding the maximum allowed values for all the fields declared as parameters by the {@link
   * net.viktorc.detroid.framework.tuning.Parameter} annotation. The values are determined by the field type and the binaryLengthLimit
   * attribute of the annotation.
   *
   * @return An array of doubles holding the maximum allowed values for all parameters.
   */
  public final double[] maxValues() {
    return maxValues(null);
  }

  /**
   * Returns an array of doubles holding the maximum allowed values for all the fields declared as parameters of the specified type by the
   * {@link net.viktorc.detroid.framework.tuning.Parameter} annotation. The values are determined by the field type and the
   * binaryLengthLimit attribute of the annotation.
   *
   * @param types A set of the types of parameter fields whose max values are to be returned. If it is null the max values for all parameter
   * fields will be returned.
   * @return An array of doubles holding the maximum allowed values for the parameters of the specified type.
   */
  public final double[] maxValues(Set<ParameterType> types) {
    List<Field> params = getParamFields(types);
    double[] arr = new double[params.size()];
    int i = 0;
    for (Field f : params) {
      Class<?> fieldType = f.getType();
      Parameter ann = f.getAnnotation(Parameter.class);
      long max = ann.binaryLengthLimit() < 0 || ann.binaryLengthLimit() > 63 ? Long.MAX_VALUE :
          ((1L << ann.binaryLengthLimit()) - 1);
      if (fieldType.equals(boolean.class)) {
        arr[i++] = 1;
      } else if (fieldType.equals(byte.class)) {
        arr[i++] = Math.min(max, Byte.MAX_VALUE);
      } else if (fieldType.equals(short.class)) {
        arr[i++] = Math.min(max, Short.MAX_VALUE);
      } else if (fieldType.equals(int.class)) {
        arr[i++] = Math.min(max, Integer.MAX_VALUE);
      } else if (fieldType.equals(long.class)) {
        arr[i++] = max;
      } else if (fieldType.equals(float.class)) {
        arr[i++] = Math.min(max, Integer.MAX_VALUE);
      } else if (fieldType.equals(double.class)) {
        arr[i++] = max;
      } else if (fieldType.equals(char.class)) {
        arr[i++] = Math.min(max, Character.MAX_VALUE);
      }
    }
    return arr;
  }

  /**
   * Returns a binary string of all the bits of all the fields annotated as a {@link net.viktorc.detroid.framework.tuning.Parameter}
   * concatenated field by field. Floating point values will be cast to integer values (float to int, double to long) for their binary
   * representation which may result in information loss.
   *
   * @return A binary string of all the bits of all parameter fields concatenated field by field.
   */
  public final String toGrayCodeString() {
    return toGrayCodeString(null);
  }

  /**
   * Returns a binary string of all the bits of the fields annotated as a {@link net.viktorc.detroid.framework.tuning.Parameter} of the
   * specified type concatenated field by field. Floating point values will be cast to integer values (float to int, double to long) for
   * their binary representation which may result in information loss.
   *
   * @param types A set of the types of parameters whose values are to be included in the string. If it is null, all parameters' values will
   * be included.
   * @return A binary string of all the bits of the parameter fields of the specified type concatenated field by field.
   */
  public final String toGrayCodeString(Set<ParameterType> types) {
    List<Field> params = getParamFields(types);
    StringBuilder genomeStringBuilder = new StringBuilder();
    try {
      for (Field f : params) {
        Object fieldValue = f.get(this);
        Parameter ann = f.getAnnotation(Parameter.class);
        byte bitLimit = (byte) (ann.binaryLengthLimit() >= 0 ? 64 - ann.binaryLengthLimit() : 0);
        if (bitLimit == 64) {
          continue;
        }
        if (fieldValue instanceof Boolean) {
          genomeStringBuilder.append(f.getBoolean(this) ? "1" : "0");
        } else if (fieldValue instanceof Byte) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode(f.getByte(this)))
              .substring(Math.max(bitLimit, 57)));
        } else if (fieldValue instanceof Short) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode(f.getShort(this)))
              .substring(Math.max(bitLimit, 49)));
        } else if (fieldValue instanceof Integer) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode(f.getInt(this)))
              .substring(Math.max(bitLimit, 33)));
        } else if (fieldValue instanceof Long) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode(f.getLong(this)))
              .substring(Math.max(bitLimit, 1)));
        } else if (fieldValue instanceof Float) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode((int) f.getFloat(this)))
              .substring(Math.max(bitLimit, 33)));
        } else if (fieldValue instanceof Double) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode((long) f.getDouble(this)))
              .substring(Math.max(bitLimit, 1)));
        } else if (fieldValue instanceof Character) {
          genomeStringBuilder.append(BitOperations.toBinaryString(GrayCode.encode(Character.getNumericValue(f.getChar(this))))
              .substring(Math.max(bitLimit, 48)));
        }
      }
      return genomeStringBuilder.toString();
    } catch (IllegalArgumentException | IllegalAccessException e) {
      return null;
    }
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (Field f : allParamFields) {
      String subString = "";
      try {
        subString += f.getName() + " = " + f.get(this).toString() + System.lineSeparator();
      } catch (IllegalArgumentException | IllegalAccessException e) {
        continue;
      }
      stringBuilder.append(subString);
    }
    return stringBuilder.toString();
  }

}
